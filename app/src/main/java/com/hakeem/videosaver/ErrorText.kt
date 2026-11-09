package com.hakeem.videosaver

/**
 * Collapses a long yt-dlp failure dump into one message a person can act on.
 *
 * Warnings (such as the "yt-dlp is older than 90 days" notice) and debug
 * chatter are never the reason a download failed, so a real error line is
 * preferred whenever one exists.
 */
object ErrorText {

    fun describe(detail: String): String {
        val lines = detail.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("[debug]") }
            .toList()

        val line = lines.firstOrNull { !it.startsWith("WARNING") }
            ?: lines.firstOrNull()
            ?: return "Download failed"

        return when {
            line.contains("Private", ignoreCase = true) ->
                "This media is private, sign in material such as cookies would be needed."
            line.contains("Video unavailable", ignoreCase = true) ->
                "Video unavailable, it may have been removed."
            line.contains("login", ignoreCase = true) || line.contains("cookies", ignoreCase = true) ->
                "This link needs an account. Try a public link."
            line.contains("Unsupported URL", ignoreCase = true) ->
                "This link is not supported."
            line.contains("HTTP Error 403", ignoreCase = true) ->
                "The site refused the request (403). Try again later or update the engine."
            line.length > 160 -> line.take(157) + "..."
            else -> line
        }
    }
}
