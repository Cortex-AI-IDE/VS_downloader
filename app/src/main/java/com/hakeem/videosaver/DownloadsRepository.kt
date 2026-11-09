package com.hakeem.videosaver

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Puts finished downloads into the public Download folder and lists them again.
 *
 * Android 10 and newer only allow apps to contribute to shared storage through
 * MediaStore, older versions get a direct file copy into Download/VideoSaver.
 */
object DownloadsRepository {

    const val SUBDIR = "VideoSaver"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    data class SavedFile(
        val uri: Uri,
        val name: String,
        val size: Long,
        val dateAdded: Long,
        val mime: String,
        /** Present only on Android 9 and older, where files are handled directly. */
        val filePath: String? = null
    )

    data class SaveResult(val uri: Uri, val name: String)

    /** Copies [src] from app private storage into Download/VideoSaver and deletes nothing. */
    fun save(context: Context, src: File): SaveResult {
        val safeName = sanitize(src.name)
        val mime = mimeFor(safeName)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, src, safeName, mime)
        } else {
            saveLegacy(context, src, safeName, mime)
        }
    }

    private fun saveWithMediaStore(context: Context, src: File, name: String, mime: String): SaveResult {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + SUBDIR)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Could not create a file in Download/$SUBDIR")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                src.inputStream().use { input -> input.copyTo(out) }
            } ?: throw IOException("Could not open the destination file")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }

        return SaveResult(uri, name)
    }

    private fun saveLegacy(context: Context, src: File, name: String, mime: String): SaveResult {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            SUBDIR
        )
        if (!dir.isDirectory && !dir.mkdirs()) {
            throw IOException("Cannot create Download/$SUBDIR")
        }

        var dest = File(dir, name)
        val base = name.substringBeforeLast('.')
        val ext = name.substringAfterLast('.', "")
        var counter = 1
        while (dest.exists()) {
            val suffix = " ($counter)"
            dest = if (ext.isEmpty()) File(dir, base + suffix) else File(dir, "$base$suffix.$ext")
            counter++
        }

        src.copyTo(dest, overwrite = true)
        MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf(mime), null)

        return SaveResult(fileUri(context, dest), dest.name)
    }

    /** Saved files, newest first. */
    fun list(context: Context): List<SavedFile> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listFromMediaStore(context)
        } else {
            listLegacy(context)
        }
    }

    private fun listFromMediaStore(context: Context): List<SavedFile> {
        val result = mutableListOf<SavedFile>()
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.SIZE,
            MediaStore.Downloads.DATE_ADDED,
            MediaStore.Downloads.MIME_TYPE,
            MediaStore.Downloads.RELATIVE_PATH
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%$SUBDIR%")
        val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            val nameIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.SIZE)
            val dateIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_ADDED)
            val mimeIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.MIME_TYPE)
            val pathIdx = cursor.getColumnIndexOrThrow(MediaStore.Downloads.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathIdx)?.trimEnd('/') ?: ""
                if (!path.endsWith(SUBDIR)) continue

                val id = cursor.getLong(idIdx)
                result.add(
                    SavedFile(
                        uri = Uri.withAppendedPath(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id.toString()),
                        name = cursor.getString(nameIdx) ?: "file",
                        size = cursor.getLong(sizeIdx),
                        dateAdded = cursor.getLong(dateIdx) * 1000L,
                        mime = cursor.getString(mimeIdx) ?: "video/*"
                    )
                )
            }
        }
        return result
    }

    private fun listLegacy(context: Context): List<SavedFile> {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            SUBDIR
        )
        val files = dir.listFiles() ?: return emptyList()
        return files
            .filter { it.isFile }
            .sortedByDescending { it.lastModified() }
            .map { file ->
                SavedFile(
                    uri = fileUri(context, file),
                    name = file.name,
                    size = file.length(),
                    dateAdded = file.lastModified(),
                    mime = mimeFor(file.name),
                    filePath = file.absolutePath
                )
            }
    }

    fun delete(context: Context, file: SavedFile): Boolean {
        return if (file.filePath != null) {
            val deleted = File(file.filePath).delete()
            if (deleted) {
                MediaScannerConnection.scanFile(context, arrayOf(file.filePath), arrayOf(file.mime), null)
            }
            deleted
        } else {
            runCatching {
                context.contentResolver.delete(file.uri, null, null) == 1
            }.getOrDefault(false)
        }
    }

    fun open(context: Context, file: SavedFile): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(file.uri, file.mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return startActivity(context, intent)
    }

    fun share(context: Context, file: SavedFile): Boolean {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = file.mime
            putExtra(Intent.EXTRA_STREAM, file.uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return startActivity(context, Intent.createChooser(intent, context.getString(R.string.share_title)))
    }

    private fun startActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            false
        }
    }

    private fun fileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, context.packageName + AUTHORITY_SUFFIX, file)
    }

    private fun mimeFor(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val fromExt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        if (fromExt != null) return fromExt
        return if (ext in setOf("mkv", "webm", "flv", "avi", "mov", "ts")) "video/*" else "video/mp4"
    }

    private fun sanitize(name: String): String {
        val cleaned = name
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.')
        return cleaned.ifBlank { "video_" + System.currentTimeMillis() }
    }

    fun formatDate(millis: Long): String {
        val format = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        return format.format(Date(millis))
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824L -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576L -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
            bytes >= 1_024L -> String.format(Locale.US, "%.0f KB", bytes / 1_024.0)
            else -> "$bytes B"
        }
    }
}
