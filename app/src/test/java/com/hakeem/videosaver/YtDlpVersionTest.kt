package com.hakeem.videosaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class YtDlpVersionTest {

    private fun zipWithVersion(version: String, prefix: String = ""): ByteArray {
        val bos = ByteArrayOutputStream()
        bos.write(prefix.toByteArray(Charsets.UTF_8))
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("yt_dlp/YoutubeDL.py"))
            zip.write("pass\n".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("yt_dlp/version.py"))
            zip.write("__version__ = '$version'\n".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    @Test
    fun `reads the version from a zipapp that starts with a shebang`() {
        val bytes = zipWithVersion("2026.10.26", prefix = "#!/usr/bin/env python3\n")
        assertEquals("2026.10.26", YtDlpVersion.fromZip(bytes.inputStream()))
    }

    @Test
    fun `reads the version from a plain zip without a prefix`() {
        assertEquals("2025.11.12", YtDlpVersion.fromZip(zipWithVersion("2025.11.12").inputStream()))
    }

    @Test
    fun `returns null when the archive has no version file`() {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            zip.putNextEntry(ZipEntry("readme.txt"))
            zip.write("hi".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        assertNull(YtDlpVersion.fromZip(bos.toByteArray().inputStream()))
    }

    @Test
    fun `compares date style versions segment by segment`() {
        assertTrue(YtDlpVersion.compare("2026.10.26", "2025.11.12") > 0)
        assertTrue(YtDlpVersion.compare("2025.11.12", "2026.10.26") < 0)
        assertEquals(0, YtDlpVersion.compare("2026.10.26", "2026.10.26"))
        assertTrue(YtDlpVersion.compare("2026.10.26.1", "2026.10.26") > 0)
    }

    @Test
    fun `an older extracted engine needs a refresh`() {
        assertTrue(YtDlpVersion.needsRefresh("2025.11.12", "2026.10.26"))
    }

    @Test
    fun `the same or a newer extracted engine is kept`() {
        assertFalse(YtDlpVersion.needsRefresh("2026.10.26", "2026.10.26"))
        assertFalse(YtDlpVersion.needsRefresh("2026.11.05", "2026.10.26"))
    }

    @Test
    fun `an unreadable extracted engine is replaced but a missing bundle changes nothing`() {
        assertTrue(YtDlpVersion.needsRefresh(null, "2026.10.26"))
        assertFalse(YtDlpVersion.needsRefresh("2025.11.12", null))
    }

    @Test
    fun `the engine bundled in res raw parses and is not older than the expected floor`() {
        val candidates = listOf(
            File("src/main/res/raw/ytdlp"),
            File("app/src/main/res/raw/ytdlp")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: fail("bundled engine file not found in ${candidates.map { it.path }}").let { return }

        val version = YtDlpVersion.fromZip(file)
            ?: fail("bundled engine has no parseable yt_dlp/version.py").let { return }

        assertTrue(
            "bundled yt-dlp $version is older than the 2026.10.26 floor, update res/raw/ytdlp",
            YtDlpVersion.compare(version, "2026.10.26") >= 0
        )
    }
}
