package com.hakeem.videosaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the format choices that keep downloads playable in social apps.
 * TikTok rejects anything that is not H.264/AAC in an MP4 container with
 * "couldn't decode", so these assertions protect against regressions that
 * would re-introduce WebM/AV1/HEVC results.
 */
class FormatPolicyTest {

    @Test
    fun `selection prefers avc1 mp4 first`() {
        val firstChoice = FormatPolicy.SELECTION.substringBefore('/')
        assertTrue(
            "first fallback must be H.264 in mp4, was: $firstChoice",
            firstChoice.contains("avc1") && firstChoice.contains("ext=mp4")
        )
    }

    @Test
    fun `selection has five documented fallbacks in order`() {
        val parts = FormatPolicy.SELECTION.split('/')
        assertEquals(5, parts.size)
        assertTrue(parts[0].contains("avc1"))
        assertTrue(parts[1].contains("bestvideo[ext=mp4]+bestaudio[ext=m4a]"))
        assertTrue(parts[2].contains("avc1"))
        assertTrue(parts[3] == "best[ext=mp4]")
        assertTrue(parts[4] == "best")
    }

    @Test
    fun `every branch but the last stays in mp4`() {
        val parts = FormatPolicy.SELECTION.split('/')
        parts.dropLast(1).forEach { branch ->
            assertTrue("branch must require ext=mp4: $branch", branch.contains("ext=mp4"))
        }
    }

    @Test
    fun `extra args force mp4 merge container`() {
        assertEquals(listOf("--merge-output-format", "mp4"), FormatPolicy.EXTRA_ARGS)
    }

    @Test
    fun `extra args come in balanced flag value pairs`() {
        assertTrue("args must pair up for chunked(2)", FormatPolicy.EXTRA_ARGS.size % 2 == 0)
        FormatPolicy.EXTRA_ARGS.chunked(2).forEach { pair ->
            assertTrue("flag must start with --: ${pair[0]}", pair[0].startsWith("--"))
            assertEquals("every flag needs a value", 2, pair.size)
        }
    }

    @Test
    fun `selection does not leak unsupported primaries`() {
        assertTrue(FormatPolicy.SELECTION.contains("ext=mp4"))
        assertTrue(!FormatPolicy.SELECTION.contains("ext=webm"))
        assertTrue(!FormatPolicy.SELECTION.contains("ext=mkv"))
    }
}
