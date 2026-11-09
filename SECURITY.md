# Security Policy

## Supported versions

Security fixes are applied to the latest release only.

| Version | Supported |
|---|---|
| 1.2.x | Yes |
| 1.1.x and older | No, please update |

## Reporting a vulnerability

Please do not open a public issue for a security problem.

Report it privately in one of these ways:

1. Use GitHub's private reporting: go to the **Security** tab of this repository and choose
   **Report a vulnerability**.
2. If that is unavailable, contact the maintainer through the GitHub profile
   [Cortex-AI-IDE](https://github.com/Cortex-AI-IDE).

Include:

- A description of the problem and its impact
- Steps to reproduce, or a proof of concept
- The app version and Android version you tested
- Any suggested fix

## What to expect

- Acknowledgement within 3 days
- An assessment and a plan within 10 days
- Credit in the release notes if you want it

Please give the maintainer a reasonable amount of time to fix the issue before publishing details.

## Scope

In scope:

- Bugs that let a downloaded file or link escape the intended storage location
- Path traversal through the file name policy
- Command injection through a crafted link passed to the engine
- Intent redirection in the foreground service or the file list actions
- Exported components that should not be exported
- Any leak of user data, links, or files

Out of scope:

- The app failing to download from a site because the site changed. That is a normal engine issue,
  please open a regular bug report.
- Problems that require a rooted device or a modified system.
- Vulnerabilities in yt-dlp, FFmpeg or the youtubedl-android library. Report those upstream, and feel
  free to tell us so we can bump the dependency.

## How the app handles your data

- No analytics, no crash reporting, no advertising libraries.
- No account, no sign in, no cookies are stored.
- Links are used only to start the download you requested.
- Files are written to the public `Download/VideoSaver` folder through MediaStore.
- The only network traffic is the download itself and the optional engine update.

## Hardening notes for contributors

- All user input passes through `UrlHelper` validation before it reaches the engine.
- File names are trimmed by `FileNamePolicy` so a long title cannot break the storage write.
- The engine runs as a local process inside the app sandbox, not as a system service.
- Never log full URLs from users in a way that could be shared back, because links can contain
  private identifiers.
