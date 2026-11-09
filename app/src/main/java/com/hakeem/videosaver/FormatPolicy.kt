package com.hakeem.videosaver

/**
 * Format choices for yt-dlp.
 *
 * Social apps such as TikTok only decode standard MP4 (H.264 video, AAC audio).
 * When yt-dlp falls back to its generic "best" choice it can deliver WebM/VP9,
 * HEVC or MKV files, which those apps reject with a "couldn't decode" error.
 * Everything here therefore prefers H.264 in MP4 first and forces the merged
 * container to MP4.
 */
object FormatPolicy {

    /** yt-dlp -f value: H.264 MP4 first, plain MP4 next, anything as last resort. */
    const val SELECTION =
        "bestvideo[ext=mp4][vcodec^=avc1]+bestaudio[ext=m4a]/" +
            "bestvideo[ext=mp4]+bestaudio[ext=m4a]/" +
            "best[ext=mp4][vcodec^=avc1]/" +
            "best[ext=mp4]/" +
            "best"

    /**
     * Extra yt-dlp arguments that keep the merged container MP4.
     * Remuxing is deliberately not forced: a failed remux aborts the whole
     * download, while an incompatible file is rare once SELECTION prefers avc1.
     */
    val EXTRA_ARGS: List<String> = listOf(
        "--merge-output-format", "mp4"
    )
}
