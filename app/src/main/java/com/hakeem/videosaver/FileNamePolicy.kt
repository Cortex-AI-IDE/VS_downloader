package com.hakeem.videosaver

/**
 * Caps yt-dlp output filenames. Facebook and Instagram titles can be
 * hundreds of characters long; without a cap the name exceeds the
 * filesystem limit of 255 bytes per filename and yt-dlp fails with
 * "Errno 36: File name too long".
 */
object FileNamePolicy {

    /**
     * Maximum length yt-dlp may give the output name. Leaves room for the
     * ".part" suffix used while downloading and the ".mp4" extension
     * (150 + 5 + 4 = 159 bytes, well under the 255 byte limit).
     */
    const val MAX_FILENAME_LENGTH = 150

    /** yt-dlp argument pairs (flag, value) that enforce the cap. */
    val ARGS: List<String> = listOf("--trim-filenames", MAX_FILENAME_LENGTH.toString())
}
