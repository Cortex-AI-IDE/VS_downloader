# Building Video Saver from source

These instructions build a debug APK that you can install on your own phone. The build does not
need Android Studio, but Android Studio works too.

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 | Android Gradle Plugin 8.7 needs JDK 17, not 21 |
| Android SDK | platform 35, build tools 35 | Install with `sdkmanager` or Android Studio |
| Gradle | 8.9 | Provided by the wrapper, no separate install |
| Disk space | about 6 GB | SDK, Gradle cache and the build output |

The project builds with Android Gradle Plugin 8.7.3 and Kotlin 2.0.21.

## 1. Clone the repository

```bash
git clone https://github.com/Cortex-AI-IDE/VS_downloader.git
cd VS_downloader
```

## 2. Point the build at your SDK

Create `local.properties` in the project root:

```properties
sdk.dir=/absolute/path/to/android-sdk
```

On Linux this is often `$HOME/Android/Sdk`, on macOS `$HOME/Library/Android/sdk`, and on Windows
`C:\\Users\\you\\AppData\\Local\\Android\\Sdk`.

`local.properties` is ignored by git and must not be committed.

## 3. Set the JDK

```bash
export JAVA_HOME=/path/to/jdk-17
```

Check that the right version is active:

```bash
java -version
```

The output must report version 17.

## 4. Build

```bash
./gradlew assembleDebug
```

The build writes one APK per CPU architecture:

```
app/build/outputs/apk/debug/app-arm64-v8a-debug.apk      59 MB
app/build/outputs/apk/debug/app-armeabi-v7a-debug.apk    52 MB
app/build/outputs/apk/debug/app-x86_64-debug.apk         62 MB
app/build/outputs/apk/debug/app-universal-debug.apk     151 MB
```

The split APKs are small because the bundled Python runtime and FFmpeg are around 48 MB per
architecture. The universal APK carries all of them and is only needed when the target device is
unknown.

## 5. Install

With a phone connected over USB and USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

Use the APK that matches the device. `adb shell getprop ro.product.cpu.abi` prints the
architecture of a connected phone.

Or copy the APK to the phone and open it. Android will ask you to allow installs from that source.

## Run the tests

```bash
./gradlew test
```

The HTML report is written to `app/build/reports/tests/testDebugUnitTest/index.html`.

The suite covers link parsing, format choice, file naming, error text and engine version handling.
All tests are plain JVM tests and run in a few seconds.

## APK sizes

`app/build.gradle.kts` already splits the build by ABI, so every `assembleDebug` or
`assembleRelease` produces one APK per architecture plus a universal fallback. Drop `x86` from the
list because 32-bit Intel devices are effectively gone; `x86_64` still covers emulators.

Changing the split list is a one line edit:

```kotlin
splits {
    abi {
        isEnable = true
        reset()
        include("arm64-v8a", "armeabi-v7a", "x86_64")
        isUniversalApk = true
    }
}
```

`isUniversalApk = true` keeps a single APK that runs anywhere. Set it to `false` if you only ever
want the small per-architecture files.

A signed release build also needs a keystore:

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias videosaver
```

```bash
./gradlew assembleRelease
```

Keep the keystore and its password outside the repository. Never commit a `.jks` file.

## Common build problems

### "SDK location not found"

`local.properties` is missing or `ANDROID_HOME` is not set. See step 2.

### "Unsupported class file major version" or "invalid source release"

Gradle is running on the wrong JDK. Set `JAVA_HOME` to JDK 17 and run again.

### "Failed to install the following SDK components"

The SDK is missing platform 35 or build tools 35. Install them:

```bash
sdkmanager "platforms;android-35" "build-tools;35.0.0"
```

### The build is very slow the first time

Gradle downloads the Android Gradle Plugin, Kotlin and the AndroidX libraries. The first build can
take several minutes. Later builds are fast.

### "Execution failed for task ':app:mergeDebugNativeLibs'" or a large APK warning

The engine libraries are intentionally uncompressed and must be extracted at install time. This is
configured with `useLegacyPackaging = true` in `app/build.gradle.kts`. Do not change it.

## Continuous integration

Every push and pull request runs the same steps on GitHub Actions, see
`.github/workflows/android.yml`. The workflow uploads the debug APK as an artifact, so you can
download a build without compiling anything.
