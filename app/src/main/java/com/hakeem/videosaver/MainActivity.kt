package com.hakeem.videosaver

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.hakeem.videosaver.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var activeJobId: String? = null

    private val adapter by lazy {
        DownloadAdapter(
            onOpen = { file ->
                if (!DownloadsRepository.open(this, file)) {
                    toast(getString(R.string.open_failed))
                }
            },
            onShare = { file ->
                if (!DownloadsRepository.share(this, file)) {
                    toast(getString(R.string.share_failed))
                }
            },
            onDelete = { file ->
                if (!DownloadsRepository.delete(this, file)) {
                    toast(getString(R.string.delete_failed))
                } else {
                    refreshList()
                }
            }
        )
    }

    private val engineListener: (EngineManager.State, String?) -> Unit = { state, error ->
        runOnUiThread { renderEngine(state, error) }
    }

    private val jobListener: (DownloadCenter.Job) -> Unit = { job ->
        runOnUiThread { renderJob(job) }
    }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.list.layoutManager = LinearLayoutManager(this)
        binding.list.adapter = adapter

        binding.btnDownload.setOnClickListener { startDownload() }
        binding.btnPaste.setOnClickListener { pasteFromClipboard() }
        binding.btnCancel.setOnClickListener {
            activeJobId?.let { DownloadCenter.cancel(it) }
        }
        binding.btnUpdateEngine.setOnClickListener { updateEngine() }

        binding.inputUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                renderPlatformBadge(s?.toString().orEmpty())
            }
        })

        maybeFillFromClipboard()
        renderPlatformBadge(binding.inputUrl.text?.toString().orEmpty())
        renderEngine(EngineManager.state, EngineManager.lastError)
        refreshList()
        requestNotificationPermission()
    }

    override fun onStart() {
        super.onStart()
        EngineManager.addListener(engineListener)
        DownloadCenter.addListener(jobListener)
        // Recover a running job after rotation or returning to the app.
        val running = DownloadCenter.activeJobs().firstOrNull()
        if (running != null) {
            activeJobId = running.id
            renderJob(running)
        }
        refreshList()
    }

    override fun onStop() {
        EngineManager.removeListener(engineListener)
        DownloadCenter.removeListener(jobListener)
        super.onStop()
    }

    private fun startDownload() {
        val raw = binding.inputUrl.text?.toString().orEmpty()
        val url = UrlHelper.normalize(raw)
        if (url == null) {
            binding.layoutUrl.error =
                if (raw.isBlank()) getString(R.string.no_url) else getString(R.string.bad_url)
            return
        }

        binding.layoutUrl.error = null
        val platform = UrlHelper.detect(url)
        val job = DownloadCenter.submit(this, url, platform)

        binding.inputUrl.text?.clear()
        binding.inputUrl.clearFocus()
        hideKeyboard()

        activeJobId = job.id
        renderJob(job)
        DownloadService.start(this)
    }

    private fun renderPlatformBadge(text: String) {
        val platform = UrlHelper.detect(UrlHelper.normalize(text) ?: return)
        binding.platformBadge.visibility = View.VISIBLE
        binding.platformBadge.text = platform.label
    }

    private fun renderEngine(state: EngineManager.State, error: String?) {
        when (state) {
            EngineManager.State.READY -> {
                binding.engineStatus.text =
                    getString(R.string.engine_ready, EngineManager.version(this))
                binding.btnUpdateEngine.visibility = View.VISIBLE
            }
            EngineManager.State.INITIALIZING, EngineManager.State.IDLE -> {
                binding.engineStatus.text = getString(R.string.engine_preparing)
                binding.btnUpdateEngine.visibility = View.GONE
            }
            EngineManager.State.FAILED -> {
                binding.engineStatus.text =
                    getString(R.string.engine_failed, error ?: getString(R.string.unknown_error))
                binding.btnUpdateEngine.visibility = View.VISIBLE
            }
        }
    }

    private fun updateEngine() {
        binding.btnUpdateEngine.isEnabled = false
        binding.engineStatus.text = getString(R.string.engine_updating)
        EngineManager.update(this) { _, message ->
            runOnUiThread {
                binding.btnUpdateEngine.isEnabled = true
                toast(message)
                renderEngine(EngineManager.state, EngineManager.lastError)
            }
        }
    }

    private fun renderJob(job: DownloadCenter.Job) {
        if (job.isActive) {
            activeJobId = job.id
            binding.cardProgress.visibility = View.VISIBLE

            binding.progressTitle.text = when {
                job.title.isNotBlank() -> job.title
                job.status == DownloadCenter.Status.RUNNING -> getString(R.string.status_fetching)
                else -> getString(R.string.status_preparing)
            }

            val percent = job.progress.toInt().coerceIn(0, 100)
            binding.progressIndicator.progress = percent

            binding.progressStatus.text = when (job.status) {
                DownloadCenter.Status.QUEUED -> getString(R.string.status_queued)
                DownloadCenter.Status.SAVING -> getString(R.string.status_saving)
                else -> {
                    val line = job.lastLine
                    if (job.status == DownloadCenter.Status.RUNNING &&
                        line.startsWith("[download]") && line.length <= 70
                    ) {
                        line
                    } else {
                        getString(R.string.status_downloading, percent)
                    }
                }
            }

            val eta = formatEta(job.etaSeconds)
            binding.progressNumbers.text =
                if (eta.isEmpty()) "$percent%" else "$percent% · $eta"

            binding.btnCancel.isEnabled = job.status != DownloadCenter.Status.SAVING
            return
        }

        if (activeJobId != job.id) return
        activeJobId = null
        binding.cardProgress.visibility = View.GONE

        when (job.status) {
            DownloadCenter.Status.DONE -> {
                toast(getString(R.string.saved_toast, job.savedName))
                refreshList()
            }
            DownloadCenter.Status.FAILED -> toast(job.error ?: getString(R.string.download_failed))
            DownloadCenter.Status.CANCELED -> toast(getString(R.string.canceled))
            else -> Unit
        }
    }

    private fun refreshList() {
        val files = DownloadsRepository.list(this)
        adapter.submit(files)
        binding.emptyState.visibility = if (files.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun maybeFillFromClipboard() {
        if (!binding.inputUrl.text.isNullOrEmpty()) return
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val pasted = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()
            ?: return

        if (UrlHelper.normalize(pasted) != null) {
            binding.inputUrl.setText(pasted)
            binding.inputUrl.setSelection(pasted.length)
        }
    }

    private fun pasteFromClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val pasted = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(this)
            ?.toString()

        if (pasted.isNullOrBlank()) {
            toast(getString(R.string.clipboard_empty))
            return
        }
        binding.inputUrl.setText(pasted)
        binding.inputUrl.setSelection(pasted.length)
        binding.layoutUrl.error = null
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun formatEta(seconds: Long): String {
        if (seconds < 0L) return ""
        val minutes = seconds / 60
        val rest = seconds % 60
        return String.format(java.util.Locale.US, "%d:%02d", minutes, rest)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
