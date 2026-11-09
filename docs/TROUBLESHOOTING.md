# Troubleshooting

Fixes for the problems people report most often. Every section starts with the exact message you
see, so you can search this page for it.

## "couldn't decode. select another video" when uploading to TikTok

**What you see:** you download a video with this app, pick it in the TikTok upload screen, and TikTok
refuses it with *couldn't decode, select another video*.

**Why it happens:** TikTok only accepts standard MP4 files with H.264 video and AAC audio. Older
versions of this app asked the engine for MP4 first but fell back to any format, so some downloads
were saved as WebM, MKV, AV1 or HEVC. Those files play fine in a gallery app but the TikTok
uploader cannot read them.

**Fix:**

1. Update the app to version 1.2.0 or newer.
2. Delete the old file from `Download/VideoSaver`.
3. Download the video again.
4. Upload the new file.

Files downloaded before version 1.1.1 keep their old format, so re downloading is required. From
version 1.1.1 onward every download prefers H.264 video inside an MP4 container.

**How to check a file:** open the file details in your gallery or file manager. The container should
read `MP4` or `video/mp4`. If it reads `WebM`, `video/x-matroska` or `AV1`, the file was made by an
older version.

## "ERROR: [youtube] ... The downloadable format is missing" or other engine messages

The engine is one version behind the website. Use the **Update engine** action in the app header.
It downloads the latest yt-dlp release and restarts the engine.

If the update fails because of no network, wait and try again. The app keeps working with the
bundled engine until a newer one is installed.

## "WARNING: Your yt-dlp version is older than 90 days"

**Why it happens:** yt-dlp checks its own age and prints this warning when the bundled copy is old,
because sites change and old extractors break. It is a warning, not an error.

**Fix:** install the latest APK, which ships an engine from 2026.10.26, or press **Update engine**.
The header shows the exact engine version, so you can confirm the change took effect.

If a warning ever appears as a download error, that is a bug in the app. Please report it.

## "ERROR: unable to open for writing: [Errno 36] File name too long"

**Why it happens:** Facebook and Instagram titles can be longer than the 255 byte limit for a file
name. The engine could not create the file.

**Fix:** update to version 1.1.2 or newer. The app now trims file names to 150 characters before the
download starts.

**Workaround on older versions:** download the same video from a link with a shorter title, or
rename the file after a successful download.

## Instagram link fails with "Unable to extract" or a broken profile error

Instagram changes its pages often, and some profile URLs are broken upstream in yt-dlp. Try in this
order:

1. Update the engine with the header action.
2. Make sure the post is public and your account can see it in a browser.
3. Try the direct post link (`/p/` or `/reel/`) instead of a profile link.
4. Wait for a newer yt-dlp release and update again.

Private accounts and login walled posts cannot be downloaded, because the app does not use your
cookies.

## Facebook link says "Cannot parse data" or "Unsupported URL"

1. Use the `facebook.com/watch?v=` or `fb.watch/` link, not the mobile share page with tracking
   parameters.
2. Update the engine.
3. Public videos only. Videos limited to friends or a group need a signed in session.

## TikTok photo or slideshow post fails

TikTok photo posts are image galleries, not videos, so there is no video stream to download. This is
expected. Regular video posts work, including short links from the share menu
(`vm.tiktok.com` and `vt.tiktok.com`).

## Download stops when the screen turns off

Android may kill background work on aggressive battery settings. The app runs a foreground service
with a progress notification to stay alive, so also check:

- Settings, Apps, Video Saver, Battery: allow unrestricted background usage.
- Disable battery optimisation for the app on Xiaomi, Oppo, Vivo and Samsung devices.

## "That does not look like a valid http link"

The paste field only accepts links that start with `http://` or `https://`. Copy the link with the
share button of the source app, not the visible text of a post.

## The saved file list is empty but files exist

The list reads the public `Download/VideoSaver` folder through MediaStore. If you moved files out of
that folder, they will not appear. To refresh the list, pull the app from the recent apps screen and
open it again.

## Engine fails to start on first launch

The first launch unpacks Python and yt-dlp, which needs about 200 MB of free space and a moment of
patience. If it still fails:

1. Free some storage.
2. Settings, Apps, Video Saver, Storage, Clear storage. This removes the unpacked engine, not your
   downloaded videos.
3. Open the app again and wait for "Engine ready".

## None of these helped

Open a bug report from the Issues tab. Include the app version, your Android version, the link type
(not the private link itself) and the exact error text. The bug template asks for exactly these
details.
