package com.hakeem.videosaver

import android.content.Context
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * Prepares the download engine once per process.
 *
 * [YoutubeDL.init] unpacks the bundled Python runtime and yt-dlp into the app
 * files directory, [FFmpeg.init] does the same for ffmpeg. Both are needed
 * before any download can run, so every download path waits on [ensureReadyBlocking].
 */
object EngineManager {

    enum class State { IDLE, INITIALIZING, READY, FAILED }

    @Volatile
    var state: State = State.IDLE
        private set

    @Volatile
    var lastError: String? = null
        private set

    /** yt-dlp version actually sitting on disk, read once the engine is ready. */
    @Volatile
    private var engineVersion: String? = null

    private val executor = Executors.newSingleThreadExecutor { r -> Thread(r, "engine-init") }
    private val listeners = CopyOnWriteArrayList<(State, String?) -> Unit>()

    fun addListener(listener: (State, String?) -> Unit) {
        listeners.add(listener)
        listener(state, lastError)
    }

    fun removeListener(listener: (State, String?) -> Unit) {
        listeners.remove(listener)
    }

    /** Starts initialisation in the background, at most once per process. */
    fun ensureReadyAsync(context: Context) {
        if (state == State.READY) return
        val app = context.applicationContext
        executor.execute { ensureReadyBlocking(app) }
    }

    /**
     * Blocks the calling thread until the engine is ready.
     * Safe to call from several threads, initialisation runs once.
     */
    @Synchronized
    fun ensureReadyBlocking(context: Context): Boolean {
        if (state == State.READY) return true

        state = State.INITIALIZING
        notifyListeners()

        val app = context.applicationContext
        return try {
            refreshStaleEngine(app)
            YoutubeDL.init(app)
            FFmpeg.init(app)
            engineVersion = readEngineVersion(app)
            lastError = null
            state = State.READY
            notifyListeners()
            true
        } catch (t: Throwable) {
            lastError = t.message ?: t.javaClass.simpleName
            state = State.FAILED
            notifyListeners()
            false
        }
    }

    /** yt-dlp version currently on disk, "unknown" before init. */
    fun version(context: Context): String {
        engineVersion?.let { return it }
        if (state != State.READY) return "unknown"
        return runCatching { YoutubeDL.version(context.applicationContext) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: "unknown"
    }

    /** Updates yt-dlp itself from the stable channel. Runs on the engine thread. */
    fun update(context: Context, onDone: (Boolean, String) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            if (!ensureReadyBlocking(app)) {
                onDone(false, lastError ?: "Download engine is not available")
                return@execute
            }
            try {
                val status = YoutubeDL.updateYoutubeDL(app, YoutubeDL.UpdateChannel.STABLE)
                engineVersion = readEngineVersion(app)
                val message = if (status == YoutubeDL.UpdateStatus.DONE) {
                    "Engine updated to yt-dlp " + version(app)
                } else {
                    "Engine is already up to date"
                }
                onDone(true, message)
            } catch (t: Throwable) {
                onDone(false, t.message ?: "Engine update failed")
            }
        }
    }

    /**
     * Replaces an extracted engine that is older than the one shipped in this
     * APK. The library only copies res/raw/ytdlp when no file exists yet, so
     * without this an app update would keep running the old yt-dlp forever.
     * A newer engine installed through [update] is never downgraded.
     */
    private fun refreshStaleEngine(app: Context) {
        val extracted = extractedEngineFile(app)
        if (!extracted.exists()) return
        val current = YtDlpVersion.fromZip(extracted)
        val bundled = bundledEngineVersion(app)
        if (YtDlpVersion.needsRefresh(current, bundled)) {
            extracted.delete()
        }
    }

    /** Version of the engine the library will extract, from res/raw/ytdlp. */
    private fun bundledEngineVersion(app: Context): String? =
        runCatching {
            app.resources.openRawResource(R.raw.ytdlp).use { YtDlpVersion.fromZip(it) }
        }.getOrNull()

    /** Version of the engine file on disk, falling back to the bundled one. */
    private fun readEngineVersion(app: Context): String? =
        YtDlpVersion.fromZip(extractedEngineFile(app)) ?: bundledEngineVersion(app)

    /** Where the library unpacks yt-dlp: noBackupFilesDir/youtubedl-android/yt-dlp/yt-dlp. */
    private fun extractedEngineFile(app: Context): File =
        File(
            app.noBackupFilesDir,
            "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}/${YoutubeDL.ytdlpBin}"
        )

    private fun notifyListeners() {
        val snapshot = listeners.toList()
        snapshot.forEach { it(state, lastError) }
    }
}
