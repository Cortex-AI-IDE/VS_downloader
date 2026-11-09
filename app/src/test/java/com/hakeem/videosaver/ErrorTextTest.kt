package com.hakeem.videosaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorTextTest {

    @Test
    fun `the real error wins over a stale engine warning`() {
        val detail = """
            WARNING: Your yt-dlp version (2025.11.12) is older than 90 days!
            ERROR: [youtube] aBcD123: Sign in to confirm your age
        """.trimIndent()
        assertEquals(
            "ERROR: [youtube] aBcD123: Sign in to confirm your age",
            ErrorText.describe(detail)
        )
    }

    @Test
    fun `debug lines are skipped when looking for the failure`() {
        val detail = "[debug] Command line: ['-X']\nERROR: Video unavailable"
        assertEquals("Video unavailable, it may have been removed.", ErrorText.describe(detail))
    }

    @Test
    fun `private media maps to a friendly message`() {
        assertEquals(
            "This media is private, sign in material such as cookies would be needed.",
            ErrorText.describe("ERROR: This video is private")
        )
    }

    @Test
    fun `unsupported links map to a friendly message`() {
        assertEquals(
            "This link is not supported.",
            ErrorText.describe("ERROR: [generic] Unsupported URL: https://example.com/x")
        )
    }

    @Test
    fun `sign in prompts map to the account message`() {
        assertEquals(
            "This link needs an account. Try a public link.",
            ErrorText.describe("ERROR: please log in and cookies are needed")
        )
    }

    @Test
    fun `http 403 keeps its own message`() {
        assertTrue(
            ErrorText.describe("ERROR: HTTP Error 403: Forbidden").contains("403")
        )
    }

    @Test
    fun `a very long line is truncated`() {
        val line = "ERROR: " + "x".repeat(300)
        val result = ErrorText.describe(line)
        assertEquals(160, result.length)
        assertTrue(result.endsWith("..."))
    }

    @Test
    fun `blank input reports a plain failure`() {
        assertEquals("Download failed", ErrorText.describe(""))
        assertEquals("Download failed", ErrorText.describe("   \n  \n"))
        assertEquals("Download failed", ErrorText.describe("[debug] only debug here"))
    }

    @Test
    fun `warnings alone are still shown rather than a bare failure`() {
        val warning = "WARNING: unable to confirm the playlist is complete"
        assertEquals(warning, ErrorText.describe(warning))
    }
}
