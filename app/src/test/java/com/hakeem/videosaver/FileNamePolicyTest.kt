package com.hakeem.videosaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the filename cap. Facebook and Instagram titles can be hundreds
 * of characters long; without --trim-filenames yt-dlp fails with
 * "Errno 36: File name too long" and the download dies.
 */
class FileNamePolicyTest {

    @Test
    fun `download request includes trim-filenames with a sane limit`() {
        val args = DownloadCenter.buildArgs("/storage/emulated/0/Download/stage")

        val flagIndex = args.indexOfFirst { it.first == "--trim-filenames" }
        assertTrue("missing --trim-filenames in $args", flagIndex >= 0)

        val limit = args[flagIndex].second?.toIntOrNull()
        assertTrue("limit must be a number, was '${args[flagIndex].second}'", limit != null)
        assertTrue("limit must be positive, was $limit", limit!! > 0)
        assertTrue("limit $limit must leave room for extension and part suffix", limit <= 200)
    }

    @Test
    fun `capped filename with extension and part suffix stays under the 255 byte limit`() {
        val limit = FileNamePolicy.MAX_FILENAME_LENGTH

        // yt-dlp writes "<name>.part" while downloading, then "<name>.mp4".
        val worstCase = limit + ".part".length + ".mp4".length
        assertTrue(
            "worst-case filename $worstCase bytes exceeds 255",
            worstCase <= 255,
        )
    }

    @Test
    fun `policy args carry the shared constant`() {
        assertEquals(
            listOf("--trim-filenames", FileNamePolicy.MAX_FILENAME_LENGTH.toString()),
            FileNamePolicy.ARGS,
        )
    }
}
