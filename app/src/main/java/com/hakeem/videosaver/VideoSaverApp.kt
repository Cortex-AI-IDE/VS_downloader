package com.hakeem.videosaver

import android.app.Application

class VideoSaverApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Prepare the engine in the background so the first paste starts quickly.
        EngineManager.ensureReadyAsync(this)
    }
}
