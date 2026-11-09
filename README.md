# Video Saver

An Android app that downloads video from a pasted link and stores it on the device.
Paste a YouTube, Instagram, Facebook or TikTok link, press Download, and the file lands in
the phone's `Download/VideoSaver` folder with an entry in the app's list.

## How it works

| Layer | Choice |
|---|---|
| App language | Kotlin, single Activity with View Binding, Material 3 dark theme |
| Download engine | yt-dlp 2026.10.26 with Python 3.11+ bundled in the APK |
| Engine wrapper | `io.github.junkfood02.youtubedl-android` 0.18.1 (library and ffmpeg) |
| Concurrency | Two downloads at a time on a fixed thread pool |
| Storage | MediaStore into `Download/VideoSaver` on Android 10+, direct file copy before that |
| Background work | Foreground service with a progress notification |

Electron could not be used here because it only targets desktop platforms. The full
research behind this stack is in [docs/research.md](docs/research.md).

## Project layout

```
app/src/main/java/com/hakeem/videosaver/
  VideoSaverApp.kt      Application, warms up the engine on startup
  EngineManager.kt      Initializes and updates yt-dlp plus ffmpeg
  DownloadCenter.kt     Runs downloads, parses progress, saves results
  DownloadsRepository.kt MediaStore listing, open, share, delete
  DownloadService.kt    Foreground service with progress notification
  MainActivity.kt       Paste field, progress card, saved file list
  DownloadAdapter.kt    List of saved files
  UrlHelper.kt          Link validation and platform detection (unit tested)
```

## Build requirements

* JDK 17
* Android SDK with platform 35 and build-tools 35
* About 6 GB of free disk for the SDK and Gradle caches

Set the tool paths, then build:

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

Install it on a connected device or a running emulator:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Run the unit tests:

```bash
./gradlew test
```

## Behaviour worth knowing

* The engine unpacks Python and yt-dlp on first launch, which takes a moment. The
  status line in the header shows the state.
* The engine can be updated in the app with the "Update engine" action, because
  yt-dlp needs new releases when the sites change.
* On Android 10 and newer, scoped storage allows apps to write only into
  `Download/` and `Documents/` through MediaStore, so downloads go through it.
* Some Instagram profile pages are reported as broken by yt-dlp upstream, and a
  small share of YouTube videos need sign-in cookies before they can be fetched.
* TikTok works with full links (`/@user/video/<id>`), mobile links (`m.tiktok.com`)
  and the short share links (`vm.tiktok.com`, `vt.tiktok.com`, `tiktok.com/t/...`).
  TikTok photo or slideshow posts have no video stream, so they cannot be downloaded.
* Every download prefers H.264 video in an MP4 container, so the saved file can be
  uploaded to social apps such as TikTok without a "could not decode" error.
* Distribution: apps of this kind are usually shipped as a signed APK or through
  F-Droid rather than Google Play.

## Rights

Downloading media can conflict with the terms of the site and with copyright.
Use the app for content you are allowed to save.
