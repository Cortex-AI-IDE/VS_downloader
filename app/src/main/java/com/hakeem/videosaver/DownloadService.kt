package com.hakeem.videosaver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/**
 * Foreground service holding a download notification, so a download
 * is not killed while the app is in the background.
 */
class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 42
        private const val EXTRA_CANCEL_ALL = "cancel_all"

        fun start(context: Context) {
            val intent = Intent(context, DownloadService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun cancelAll(context: Context) {
            val intent = Intent(context, DownloadService::class.java).putExtra(EXTRA_CANCEL_ALL, true)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private val jobListener: (DownloadCenter.Job) -> Unit = {
        mainHandler.post { refresh() }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(getString(R.string.notif_waiting), indeterminate = true)
        )
        DownloadCenter.addListener(jobListener)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra(EXTRA_CANCEL_ALL, false) == true) {
            DownloadCenter.cancelActive()
        }
        refresh()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        DownloadCenter.removeListener(jobListener)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun refresh() {
        val active = DownloadCenter.activeJobs()
        if (active.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val job = active.first()
        val label = when (job.status) {
            DownloadCenter.Status.QUEUED -> getString(R.string.status_queued)
            DownloadCenter.Status.SAVING -> getString(R.string.status_saving)
            else -> getString(R.string.status_downloading, job.progress.toInt().coerceIn(0, 100))
        }
        val title = job.title.ifBlank { getString(R.string.app_name) }
        val notification = buildNotification(
            text = "$label · $title",
            indeterminate = false,
            progress = job.progress.toInt().coerceIn(0, 100)
        )
        notificationManager()?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(text: String, indeterminate: Boolean, progress: Int = 0): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, DownloadService::class.java).putExtra(EXTRA_CANCEL_ALL, true),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_download)
            .setContentTitle(getString(R.string.notif_title))
            .setContentText(text)
            .setSubText(getString(R.string.app_name))
            .setContentIntent(openIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
            .addAction(0, getString(R.string.cancel), cancelIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = notificationManager() ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationManager(): NotificationManager? {
        return getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    }
}
