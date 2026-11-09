# Research: Android video downloader app (YouTube, Instagram, Facebook)

Date of research: 2026-11-08
Sources: official documentation and repository sources only, each claim carries its source URL.

## 1. Short answer

An Electron app cannot be built for Android. Electron is a desktop framework and
officially targets Windows, macOS and Linux only. The requested app (paste a URL,
download the video, store it on the device) is absolutely buildable on Android, but
with a different stack. The proven route used by the two largest open source Android
downloaders is: a native Android app that bundles the yt-dlp engine (with Python and
ffmpeg) inside the app.

## 2. Can Electron build Android apps?

**No.** Primary source, the official Electron introduction page:

> Electron is a framework for building desktop applications using JavaScript, HTML,
> and CSS. By embedding Chromium and Node.js into its binary, Electron allows you to
> maintain one JavaScript codebase and create cross-platform apps that work on
> Windows, macOS, and Linux.
>
> Source: https://raw.githubusercontent.com/electron/electron/main/docs/tutorial/introduction.md

Supporting evidence:

- The Electron documentation tree under `docs/tutorial` (checked through the GitHub
  API) contains distribution guides for Windows, macOS and Linux (windows-store-guide,
  mac-app-store-submission-guide, snapcraft, linux-desktop-actions) and no Android or
  iOS target of any kind.
- A GitHub repository search for Electron plus Android returns no working Electron
  runtime for Android. The projects that appear (Joplin, electerm, Keybase) ship
  Android builds made with entirely different stacks, not Electron.
- Reason: Electron embeds Chromium and Node.js. Android has no supported runtime for
  that pair, so there is no packaging path to an APK.

Practical consequence: an Electron desktop build is possible today, an Electron
Android build is not. The plan must choose a mobile stack that is not Electron.

## 3. Download engine for YouTube, Instagram and Facebook

**yt-dlp is the right engine.** It is the most maintained open source extractor and
it covers all three sites by name in its supported sites list:

| Site | Extractors listed in yt-dlp |
|---|---|
| YouTube | `youtube`, `youtube:clip`, `youtube:playlist`, `youtube:search`, `youtube:music:search_url` and more |
| Facebook | `facebook`, `facebook:ads`, `facebook:reel`, `FacebookPluginsVideo` |
| Instagram | `Instagram`, `instagram:story`, `instagram:tag`, `instagram:user` (profile listed as currently broken), `InstagramIOS` |

Source: https://raw.githubusercontent.com/yt-dlp/yt-dlp/master/supportedsites.md

### 3.1 What yt-dlp officially ships

| Platform | Official binary |
|---|---|
| Windows 10+ (x64, x86, arm64) | `yt-dlp.exe` and variants |
| macOS 10.15+ | `yt-dlp_macos` |
| Linux glibc / musl (x86_64, aarch64, armv7l) | `yt-dlp_linux*`, `yt-dlp_musllinux*` |
| Android | none |

Source: https://raw.githubusercontent.com/yt-dlp/yt-dlp/master/README.md (lines 78 to 119)

So yt-dlp does not ship an Android binary, but it runs on Android when a Python
runtime is provided. That is exactly what the Android wrapper libraries below do.

### 3.2 Known behaviour that affects the app design

- **YouTube needs a JavaScript engine.** The README states that `yt-dlp-ejs` is
  required for full YouTube support, and that without a JavaScript runtime some
  YouTube clients are skipped.
- **Some YouTube clients need authentication material.** The `player_client`
  documentation notes that certain clients require a `po_token` and that some only
  work with logged-in cookies. In practice: a small share of videos will need cookies
  or will fail, and this is true for every yt-dlp based app, including Seal.
- **Instagram profile extraction is currently broken** in the supported sites list
  (`instagram:user` marked "Currently broken"). Single post and story URLs are listed
  as working. This is an upstream extractor issue, not something the app layer can fix.
- **Sites change often.** yt-dlp updates frequently, so the app must be able to update
  the bundled engine without shipping a whole new app if possible.

Sources: yt-dlp README lines 213, 1862; supportedsites.md line 579.

### 3.3 How yt-dlp runs on a phone today (proven, in production)

**youtubedl-android** is an Android library that wraps the yt-dlp executable:

> Android library wrapper for yt-dlp executable ... yt-dlp executable and python 3.8
> are bundled in the library.
>
> Source: https://raw.githubusercontent.com/yausername/youtubedl-android/master/README.md

Facts from that README:

- Delivered as Gradle dependencies: `io.github.junkfood02.youtubedl-android:library`,
  `:ffmpeg` and optional `:aria2c`, all on Maven Central, current version 0.18.1.
- It requires `android:extractNativeLibs="true"`, ABI filters for arm64, arm32, x86
  and x86_64, and ABI splits to keep APK size down.
- **Storage limit:** on Android 10 and above, scoped storage means the app can write
  directly only into `Download/` and `Documents/` unless extra permission handling is
  added. This matches the requirement "store locally on device" and needs a deliberate
  storage design (MediaStore or SAF).
- The README points to **Seal** and **dvd** as real apps built on this library.

Apps already shipping this approach:

| App | Stars | What it is | Source |
|---|---|---|---|
| Seal | 30,463 | Video/audio downloader for Android, based on yt-dlp | https://github.com/JunkFood02/Seal |
| YTDLnis | 11,020 | Full featured audio/video downloader for Android using yt-dlp | https://github.com/deniscerri/ytdlnis |

Seal's build files confirm the wiring: `io.github.junkfood02.youtubedl-android:ffmpeg`
appears in its version catalog, and its APK splits per ABI (arm64, armeabi, x86,
x86_64) exactly as the library README prescribes.
Sources: https://raw.githubusercontent.com/JunkFood02/Seal/main/gradle/libs.versions.toml
and https://raw.githubusercontent.com/JunkFood02/Seal/main/app/build.gradle.kts

### 3.4 Alternative way to run Python on Android: Chaquopy

> Chaquopy provides everything you need to include Python components in an Android
> app, including full integration with Android Studio's standard Gradle build system
> and simple APIs for calling Python code from Java/Kotlin and back.
>
> Source: https://chaquo.com/ (version 17.2.0, released 2026-10-26)

Chaquopy is a general Python SDK. It can run yt-dlp in principle, but it does not ship
yt-dlp or ffmpeg ready made, so more integration work is needed compared to
youtubedl-android, which is purpose built for this.

### 3.5 Distribution reality

Both Seal and YTDLnis distribute through F-Droid and GitHub releases. Neither lists a
Google Play badge. Video downloaders for YouTube commonly sit outside Google Play
because of platform terms of service. Plan for sideloading (APK) or F-Droid style
distribution, and treat Play Store publication as uncertain.

Sources: Seal README line 108, YTDLnis README line 42.

### 3.6 Legal note

Downloading content from YouTube, Instagram and Facebook can conflict with their terms
of service, and copyright applies to the content itself. The app should download only
what the user has rights to save, and this should be stated in the app. This is a
product decision for the owner of the app, recorded here so it is not discovered late.

## 4. Mobile stacks that can carry this app

| Stack | Runs on Android | Can run the yt-dlp engine | Web UI reuse with a desktop Electron build | Verdict |
|---|---|---|---|---|
| Electron | No (desktop only, section 2) | not applicable | not applicable | Not usable for Android |
| Native Android (Kotlin + Jetpack Compose) with youtubedl-android | Yes | Yes, bundled Python 3.8, yt-dlp, ffmpeg | No | Most direct, proven by Seal and YTDLnis |
| Capacitor (web UI in a native container) with a native plugin that calls youtubedl-android | Yes | Yes, behind a plugin | High: the same HTML/CSS/JS can also run inside an Electron desktop build | Keeps a web development style while running native on the phone |
| Tauri 2 (Rust core + web UI) | Yes, Android and iOS are documented targets | No ready made path to bundle Python and yt-dlp; Rust has no equivalent extractor for these three sites | High | Modern, but the download engine gap is the blocker |
| React Native | Yes | Only through a native module, same work as Capacitor | Partial | Viable, no advantage over Capacitor here |

Sources: Capacitor introduction https://capacitorjs.com/docs ("run natively on iOS,
Android, and more using modern web tooling"), Tauri v2 documentation navigation
(https://v2.tauri.app/start/ lists Google Play, iOS and Android distribution plus
mobile plugin development).

## 5. Recommendation

1. Build the Android app natively (Kotlin) using youtubedl-android, because that is
   the configuration proven in production by Seal and YTDLnis, it is fully on device,
   and it needs no server.
2. If a web style UI matters, or a desktop companion matters later, use Capacitor with
   the same HTML/CSS/JS and a native plugin for the download engine. The identical web
   UI can then also ship in an Electron desktop app, which Electron does support.
3. Storage design must follow scoped storage: write through MediaStore into the
   device `Download/` folder, or use the Storage Access Framework for a user picked
   folder.
4. Ship the engine so it can be updated, because yt-dlp breaks when the sites change.
5. Distribute as a signed APK (and F-Droid if desired), not Google Play, unless a
   policy check says otherwise later.
