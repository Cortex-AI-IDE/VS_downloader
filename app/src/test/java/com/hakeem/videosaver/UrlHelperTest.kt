package com.hakeem.videosaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlHelperTest {

    @Test
    fun `adds https when the scheme is missing`() {
        assertEquals(
            "https://youtube.com/watch?v=abc",
            UrlHelper.normalize("youtube.com/watch?v=abc")
        )
    }

    @Test
    fun `keeps an existing https scheme and trims whitespace`() {
        assertEquals(
            "https://youtu.be/abc",
            UrlHelper.normalize("  https://youtu.be/abc  ")
        )
    }

    @Test
    fun `rejects empty input and plain text`() {
        assertNull(UrlHelper.normalize(""))
        assertNull(UrlHelper.normalize("     "))
        assertNull(UrlHelper.normalize("download this video please"))
    }

    @Test
    fun `rejects non http schemes`() {
        assertNull(UrlHelper.normalize("ftp://example.com/file.mp4"))
        assertNull(UrlHelper.normalize("javascript:alert(1)"))
    }

    @Test
    fun `rejects a host without a dot`() {
        assertNull(UrlHelper.normalize("https://localhost/video"))
    }

    @Test
    fun `detects YouTube links`() {
        assertEquals(UrlHelper.Platform.YOUTUBE, UrlHelper.detect("https://www.youtube.com/watch?v=1"))
        assertEquals(UrlHelper.Platform.YOUTUBE, UrlHelper.detect("https://m.youtube.com/watch?v=1"))
        assertEquals(UrlHelper.Platform.YOUTUBE, UrlHelper.detect("https://youtu.be/abc123"))
    }

    @Test
    fun `detects Instagram links`() {
        assertEquals(UrlHelper.Platform.INSTAGRAM, UrlHelper.detect("https://www.instagram.com/reel/xyz/"))
        assertEquals(UrlHelper.Platform.INSTAGRAM, UrlHelper.detect("https://instagr.am/p/abc/"))
    }

    @Test
    fun `detects Facebook links`() {
        assertEquals(UrlHelper.Platform.FACEBOOK, UrlHelper.detect("https://www.facebook.com/watch/?v=1"))
        assertEquals(UrlHelper.Platform.FACEBOOK, UrlHelper.detect("https://fb.watch/abc/"))
        assertEquals(UrlHelper.Platform.FACEBOOK, UrlHelper.detect("https://m.facebook.com/videos/1/"))
    }

    @Test
    fun `detects TikTok links`() {
        assertEquals(UrlHelper.Platform.TIKTOK, UrlHelper.detect("https://www.tiktok.com/@user/video/7400000000000000000"))
        assertEquals(UrlHelper.Platform.TIKTOK, UrlHelper.detect("https://tiktok.com/@user/video/1"))
        assertEquals(UrlHelper.Platform.TIKTOK, UrlHelper.detect("https://m.tiktok.com/@user/video/1"))
        assertEquals(UrlHelper.Platform.TIKTOK, UrlHelper.detect("https://vm.tiktok.com/ZMabcdefg/"))
        assertEquals(UrlHelper.Platform.TIKTOK, UrlHelper.detect("https://vt.tiktok.com/ZSabcdefg/"))
        assertEquals(UrlHelper.Platform.TIKTOK, UrlHelper.detect("https://www.tiktokv.com/share/video/7400000000000000000"))
    }

    @Test
    fun `does not mistake lookalike hosts for TikTok`() {
        assertEquals(UrlHelper.Platform.OTHER, UrlHelper.detect("https://nottiktok.com/@user/video/1"))
        assertEquals(UrlHelper.Platform.OTHER, UrlHelper.detect("https://tiktok.com.evil.example/@user/video/1"))
    }

    @Test
    fun `keeps other sites as OTHER instead of rejecting them`() {
        assertEquals(UrlHelper.Platform.OTHER, UrlHelper.detect("https://vimeo.com/123"))
    }

    @Test
    fun `end of a host decides the platform not the query string`() {
        assertEquals(
            UrlHelper.Platform.FACEBOOK,
            UrlHelper.detect("https://fb.watch/abc?u=https://youtube.com/watch")
        )
    }
}
