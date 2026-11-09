# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.1] - 2026-11-09

### Added

- The arm64 APK is hosted on the project website, so a download on a phone no longer depends on
  the GitHub release redirect. `SHA256SUMS.txt` is published next to it.
- A mobile first download and install guide at `docs/download.html`. It detects the phone, lists
  each architecture with its size and the device it suits, and gives a fix for each common install
  error.

### Changed

- The build now produces one APK per CPU architecture instead of a single universal APK. The
  recommended arm64 build is 59 MB instead of 198 MB, because the bundled Python runtime and
  FFmpeg are no longer duplicated four times. `arm64-v8a`, `armeabi-v7a` and `x86_64` are built,
  plus a universal fallback for unknown devices.
- Dropped the 32-bit `x86` target. It only ever served obsolete Intel devices; `x86_64` still
  covers emulators.
- Continuous integration now prints the APK sizes, writes a `SHA256SUMS.txt`, and publishes all
  builds as a single `apks` artifact.

## [1.2.0] - 2026-11-09

### Added

- TikTok support. Full links (`/@user/video/<id>`), mobile links (`m.tiktok.com`), short share
  links (`vm.tiktok.com`, `vt.tiktok.com`, `tiktok.com/t/...`) and embed hosts (`tiktokv.com`)
  are recognised and shown with a TikTok badge.
- A supported sites line under the header so new users see which platforms work before pasting.
- Unit tests for TikTok detection and for rejection of lookalike domains.

### Changed

- The header site list now reads "YouTube, Instagram, Facebook, TikTok".
- Documentation restructured for easier searching and contribution.

## [1.1.2] - 2026-11-09

### Fixed

- `[Errno 36] File name too long`. Long Facebook and Instagram titles overflowed the filesystem
  name limit. The engine now trims file names to 150 characters before saving.

## [1.1.1] - 2026-11-09

### Fixed

- Uploading a downloaded file to TikTok failed with "couldn't decode, select another video"
  because the saved file was sometimes WebM, MKV or AV1. Downloads now prefer H.264 video inside
  an MP4 container, and merged output is always written as MP4.

### Changed

- Format selection moved into a single `FormatPolicy` class so the rules are testable.

## [1.1.0] - 2026-11-09

### Added

- Bundled yt-dlp upgraded to 2026.10.26 as a raw resource, which overrides the older engine inside
  the library.
- Engine version is parsed from the bundled zipapp and shown in the header.
- An installed engine older than the bundled one is replaced on startup, so updates take effect
  without clearing app data.
- Readable error messages, extracted from engine output instead of the first warning line.

## [1.0.0] - 2026-11-09

### Added

- First public release.
- Paste a YouTube, Instagram or Facebook link and download it.
- Two concurrent downloads on a fixed thread pool.
- Foreground service with a progress notification.
- Saved file list with open, share and delete.
- MediaStore output into `Download/VideoSaver` on Android 10 and newer.
- Unit tests for link validation and platform detection.
