# Changelog

All notable changes to this project are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project uses
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
