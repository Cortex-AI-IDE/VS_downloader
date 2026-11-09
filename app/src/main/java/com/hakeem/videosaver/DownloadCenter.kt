package com.hakeem.videosaver

import android.content.Context
import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Runs yt-dlp downloads on a small thread pool, two at a time,
 * and publishes every state change to registered listeners.
 */
object DownloadCenter {

    enum class Status { QUEUED, RUNNING, SAVING, DONE, FAILED, CANCELED }

    class Job(
        val id: String,
        val url: String,
        val platform: UrlHelper.Platform
    ) {
        @Volatile var status: Status = Status.QUEUED
        @Volatile var progress: Float = 0f
        @Volatile var etaSeconds: Long = -1L
        @Volatile var title: String = ""
        @Volatile var lastLine: String = ""
        @Volatile var error: String? = null
        @Volatile var savedUri: Uri? = null
        @Volatile var savedName: String = ""
        @Volatile var cancelRequested: Boolean = false

        val isActive: Boolean
            get() = status == Status.QUEUED || status == Status.RUNNING || status == Status.SAVING
    }

    private const val MAX_CONCURRENT = 2
    private const val STAGING_DIR = "stage"

    private val executor = Executors.newFixedThreadPool(MAX_CONCURRENT) { r -> Thread(r, "download") }
    private val jobs = ConcurrentHashMap<String, Job>()
    private val order = CopyOnWriteArrayList<String>()
    private val listeners = CopyOnWriteArrayList<(Job) -> Unit>()

    fun addListener(listener: (Job) -> Unit) {
        listeners.add(listener)
    }

    fun removeListener(listener: (Job) -> Unit) {
        listeners.remove(listener)
    }

    fun job(id: String): Job? = jobs[id]

    fun activeJobs(): List<Job> = order.mapNotNull { jobs[it] }.filter { it.isActive }

    fun recentJobs(): List<Job> = order.mapNotNull { jobs[it] }.reversed()

    /** Queues a download. Returns immediately. */
    fun submit(context: Context, url: String, platform: UrlHelper.Platform): Job {
        val job = Job(UUID.randomUUID().toString(), url, platform)
        jobs[job.id] = job
        order.add(job.id)
        emit(job)

        val app = context.applicationContext
        executor.execute { runJob(app, job) }
        return job
    }

    /** Cancels one job, whether it is queued or already running. */
    fun cancel(id: String) {
        val job = jobs[id] ?: return
        if (!job.isActive) return
        job.cancelRequested = true
        runCatching { YoutubeDL.destroyProcessById(job.id) }
        if (job.status == Status.QUEUED) {
            job.status = Status.CANCELED
            job.error = "Canceled"
            emit(job)
        }
    }

    fun cancelActive() {
        activeJobs().forEach { cancel(it.id) }
    }

    private fun runJob(app: Context, job: Job) {
        if (job.cancelRequested) {
            job.status = Status.CANCELED
            job.error = "Canceled"
            emit(job)
            return
        }

        job.status = Status.RUNNING
        emit(job)

        val stage = File(app.getExternalFilesDir(null) ?: app.filesDir, STAGING_DIR)
        if (!stage.isDirectory && !stage.mkdirs()) {
            fail(job, "Cannot create the working folder")
            return
        }
        val existingBefore = stage.listFiles()?.mapNotNull { it.name }?.toSet() ?: emptySet()

        try {
            if (!EngineManager.ensureReadyBlocking(app)) {
                fail(job, "Download engine failed to start: ${EngineManager.lastError ?: "unknown error"}")
                return
            }

            val request = buildRequest(job.url, stage)
            val response = YoutubeDL.execute(request, job.id) { progress, eta, line ->
                if (job.cancelRequested) return@execute
                if (progress >= 0f) job.progress = progress
                if (eta >= 0L) job.etaSeconds = eta
                job.lastLine = line
                emit(job)
            }

            if (job.cancelRequested) {
                cleanupPartials(stage, existingBefore)
                job.status = Status.CANCELED
                job.error = "Canceled"
                emit(job)
                return
            }

            if (response.exitCode != 0) {
                val detail = response.err?.takeIf { it.isNotBlank() } ?: response.out.orEmpty()
                fail(job, ErrorText.describe(detail))
                cleanupPartials(stage, existingBefore)
                return
            }

            val produced = stage.listFiles()
                ?.filter { it.isFile && it.name !in existingBefore && !isPartial(it.name) }
                ?.filter { it.length() > 0 }
                .orEmpty()

            val mainFile = produced.maxByOrNull { it.length() }
            if (mainFile == null) {
                fail(job, "No file was produced. The link may be private, deleted or unsupported.")
                cleanupPartials(stage, existingBefore)
                return
            }

            job.title = mainFile.nameWithoutExtension
            job.status = Status.SAVING
            emit(job)

            val saved = DownloadsRepository.save(app, mainFile)
            mainFile.delete()
            cleanupPartials(stage, existingBefore)

            job.savedUri = saved.uri
            job.savedName = saved.name
            job.progress = 100f
            job.status = Status.DONE
            emit(job)
        } catch (e: InterruptedException) {
            cleanupPartials(stage, existingBefore)
            job.status = Status.CANCELED
            job.error = "Canceled"
            emit(job)
        } catch (t: Throwable) {
            cleanupPartials(stage, existingBefore)
            if (job.cancelRequested || t is YoutubeDL.CanceledException) {
                job.status = Status.CANCELED
                job.error = "Canceled"
            } else {
                job.status = Status.FAILED
                job.error = t.message?.takeIf { it.isNotBlank() } ?: "Download failed"
            }
            emit(job)
        }
    }

    /**
     * Every yt-dlp option the app sends, as (flag, value) pairs in order.
     * A null value means the flag is passed on its own. Exposed so tests
     * can guard the wiring without needing the YoutubeDLRequest API.
     */
    internal fun buildArgs(stagePath: String): List<Pair<String, String?>> {
        val outputTemplate = File(stagePath, "%(title)s [%(id)s].%(ext)s").absolutePath
        return listOf(
            "--newline" to null,
            "--no-mtime" to null,
            "--no-playlist" to null,
            *FileNamePolicy.ARGS.chunked(2).map { (flag, value) -> flag to value }.toTypedArray(),
            "-f" to FormatPolicy.SELECTION,
            *FormatPolicy.EXTRA_ARGS.chunked(2).map { (flag, value) -> flag to value }.toTypedArray(),
            "-o" to outputTemplate,
        )
    }

    private fun buildRequest(url: String, stage: File): YoutubeDLRequest {
        val request = YoutubeDLRequest(url)
        buildArgs(stage.absolutePath).forEach { (flag, value) ->
            if (value == null) request.addOption(flag) else request.addOption(flag, value)
        }
        return request
    }

    private fun fail(job: Job, message: String) {
        job.status = Status.FAILED
        job.error = message
        emit(job)
    }

    private fun isPartial(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".part") ||
            lower.endsWith(".ytdl") ||
            lower.endsWith(".temp") ||
            lower.endsWith(".temp.mp4") ||
            lower.endsWith(".frag")
    }

    private fun cleanupPartials(stage: File, keep: Set<String>) {
        stage.listFiles()?.forEach { file ->
            if (file.name !in keep && (isPartial(file.name) || file.length() == 0L)) {
                file.delete()
            }
        }
    }

    private fun emit(job: Job) {
        val snapshot = listeners.toList()
        snapshot.forEach { it(job) }
    }
}
