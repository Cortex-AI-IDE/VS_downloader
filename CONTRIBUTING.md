# Contributing to Video Saver

Thanks for taking the time to contribute. This document explains how to report a bug, propose a
feature, and send a pull request that is easy to review.

## Ways to help

- Report a bug with a clear reproduction.
- Improve the documentation or fix a typo.
- Translate the app into another language.
- Add a test for a case that is currently uncovered.
- Implement an item from the roadmap in the README.

## Before you start

1. Search the existing issues and pull requests. The problem may already be known.
2. For anything larger than a bug fix, open an issue first so the approach can be agreed before you
   write code.
3. Read [docs/BUILDING.md](docs/BUILDING.md) and make sure the project builds on your machine.

## Reporting a bug

Use the bug report template. A good report contains:

- Android version and phone model
- App version, shown in the header or in Settings
- The link type you used, for example a YouTube Shorts link or a TikTok short link
- The exact error text shown in the app
- Steps to reproduce

Do not paste private links or personal data. Replace the identifier in the URL with `XXXX`.

## Proposing a feature

Use the feature request template. Explain the problem first, then the solution you have in mind.
Features that fit the app are small, focused on downloading, and work offline without an account.

## Development workflow

```bash
git clone https://github.com/Cortex-AI-IDE/VS_downloader.git
cd VS_downloader
echo "sdk.dir=/path/to/android-sdk" > local.properties
./gradlew test
```

Run the tests before every commit:

```bash
./gradlew test
```

The project follows test driven development for logic changes. Write the failing test first, watch
it fail, then implement the smallest change that makes it pass.

### Code style

- Kotlin, four space indentation, no wildcard imports.
- Keep classes small and name them after what they do: `FormatPolicy`, `FileNamePolicy`.
- Put decisions that can be tested in plain Kotlin classes, without Android framework calls.
- Never hard code user facing text in Kotlin. Add it to `res/values/strings.xml`.
- Prefer clear code over clever code. If a comment is needed to explain what a block does, the
  block is usually too complex.

### Commit messages

Use the conventional format:

```
feat: add audio only download
fix: keep file names under the filesystem limit
docs: explain the TikTok decode error
test: cover short TikTok links
```

### Pull requests

- Keep each pull request to one concern.
- Describe what changed and why, and link the issue it closes.
- Include tests for new behaviour and say which commands you ran.
- Make sure `./gradlew test` and `./gradlew assembleDebug` both pass.
- Screenshots help a lot for interface changes.

The maintainer reviews within a few days. Small, focused pull requests are merged fastest.

## Adding support for a new site

The engine, yt-dlp, already supports hundreds of sites. To make a new site visible in the app:

1. Add the host to `UrlHelper.kt` and a new value to the `Platform` enum.
2. Add the display name to `res/values/strings.xml`.
3. Add tests to `UrlHelperTest.kt`, including a lookalike domain that must be rejected.
4. Update the supported sites table in the README and in `docs/index.html`.

## Translations

Strings live in `app/src/main/res/values/strings.xml`. Create
`app/src/main/res/values-<language>/strings.xml` with the same keys and open a pull request.

## License of contributions

By contributing you agree that your work is released under the GNU GPL v3.0, the license of this
project.
