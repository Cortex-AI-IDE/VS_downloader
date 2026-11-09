package com.hakeem.videosaver

import java.net.URI

/**
 * Pure URL logic: normalisation and platform detection.
 * Kept free of Android classes so it can be unit tested on the JVM.
 */
object UrlHelper {

    enum class Platform(val label: String) {
        YOUTUBE("YouTube"),
        INSTAGRAM("Instagram"),
        FACEBOOK("Facebook"),
        TIKTOK("TikTok"),
        OTHER("Other site")
    }

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

    /**
     * Returns a downloadable http(s) URL, or null when the input is not a usable link.
     * "youtube.com/watch?v=abc" becomes "https://youtube.com/watch?v=abc".
     */
    fun normalize(raw: String): String? {
        val text = raw.trim()
        // A pasted link contains no spaces, any other text does.
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null

        val candidate = if (SCHEME.containsMatchIn(text)) text else "https://$text"

        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "http" && scheme != "https") return null

        val host = uri.host ?: return null
        if (!host.contains('.')) return null
        if (host.startsWith(".") || host.endsWith(".")) return null

        return candidate
    }

    fun detect(url: String): Platform {
        val host = runCatching { URI(url).host?.lowercase() ?: "" }.getOrDefault("")
        if (host.isEmpty()) return Platform.OTHER

        return when {
            host == "youtu.be" || host.endsWith(".youtu.be") ||
                host == "youtube.com" || host.endsWith(".youtube.com") ||
                host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") ||
                host == "yt.be" || host.endsWith(".yt.be") -> Platform.YOUTUBE

            host == "instagram.com" || host.endsWith(".instagram.com") ||
                host == "instagr.am" || host.endsWith(".instagr.am") -> Platform.INSTAGRAM

            host == "facebook.com" || host.endsWith(".facebook.com") ||
                host == "fb.com" || host.endsWith(".fb.com") ||
                host == "fb.watch" || host.endsWith(".fb.watch") -> Platform.FACEBOOK

            // Covers tiktok.com, www./m./vm./vt.tiktok.com, the /t/ short links
            // and the tiktokv.com embed hosts.
            host == "tiktok.com" || host.endsWith(".tiktok.com") ||
                host == "tiktokv.com" || host.endsWith(".tiktokv.com") -> Platform.TIKTOK

            else -> Platform.OTHER
        }
    }
}
