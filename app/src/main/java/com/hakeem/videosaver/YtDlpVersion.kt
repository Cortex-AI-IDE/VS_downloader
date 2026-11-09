package com.hakeem.videosaver

import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.zip.ZipInputStream

/**
 * Reads the yt-dlp version out of a yt-dlp zipapp and decides whether an
 * already extracted engine is stale enough to be replaced by the bundled one.
 *
 * A yt-dlp release file starts with `#!/usr/bin/env python3` and is a zip
 * archive behind that prefix, so the local file header has to be located
 * before the archive can be read sequentially.
 */
object YtDlpVersion {

    private const val VERSION_ENTRY = "yt_dlp/version.py"
    private val LOCAL_HEADER = byteArrayOf(0x50, 0x4B, 0x03, 0x04) // "PK\003\004"
    private val VERSION_LINE = Regex("""__version__\s*=\s*['"]([^'"]+)['"]""")
    private const val MAX_PREFIX_SCAN = 4096

    /** Version string from the zipapp in [file], or null when unreadable. */
    fun fromZip(file: File): String? =
        runCatching { file.inputStream().use { fromZip(it) } }.getOrNull()

    /** Version string from a zipapp stream (shebang prefix tolerated), or null. */
    fun fromZip(input: InputStream): String? {
        val head = ByteArray(MAX_PREFIX_SCAN)
        var used = 0
        while (used < head.size) {
            val n = input.read(head, used, head.size - used)
            if (n < 0) break
            used += n
        }
        val start = indexOfLocalHeader(head, used).takeIf { it >= 0 } ?: return null
        val archive = SequenceInputStream(
            ByteArrayInputStream(head, start, used - start),
            input
        )
        ZipInputStream(archive).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == VERSION_ENTRY) {
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    return VERSION_LINE.find(text)?.groupValues?.get(1)
                }
                entry = zip.nextEntry
            }
        }
        return null
    }

    /**
     * Compares dotted, date style versions such as "2026.10.26".
     * Missing segments count as zero, so "2026.10.26.1" is newer than "2026.10.26".
     */
    fun compare(a: String, b: String): Int {
        val left = a.split('.')
        val right = b.split('.')
        for (i in 0 until maxOf(left.size, right.size)) {
            val l = left.getOrNull(i)?.toIntOrNull() ?: 0
            val r = right.getOrNull(i)?.toIntOrNull() ?: 0
            if (l != r) return l.compareTo(r)
        }
        return 0
    }

    /**
     * True when the extracted engine [current] should give way to [bundled]:
     * the bundle is newer, the extracted version cannot be read, or there is
     * no bundle to compare against only when the extracted one is unreadable.
     * A null bundle never forces a downgrade of a working engine.
     */
    fun needsRefresh(current: String?, bundled: String?): Boolean = when {
        bundled == null -> false
        current == null -> true
        else -> compare(current, bundled) < 0
    }

    private fun indexOfLocalHeader(bytes: ByteArray, length: Int): Int {
        if (length < LOCAL_HEADER.size) return -1
        outer@ for (i in 0..length - LOCAL_HEADER.size) {
            for (j in LOCAL_HEADER.indices) {
                if (bytes[i + j] != LOCAL_HEADER[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
