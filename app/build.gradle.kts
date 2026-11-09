plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.hakeem.videosaver"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.hakeem.videosaver"
        minSdk = 24
        targetSdk = 35
        versionCode = 6
        versionName = "1.2.1"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
    }

    // The Python runtime and ffmpeg are ~48 MB per CPU architecture, so a
    // single APK carrying all of them is nearly 200 MB. Splitting by ABI
    // ships one architecture per APK, which is roughly a third of the size.
    // arm64-v8a covers every phone made in the last several years,
    // armeabi-v7a covers older 32-bit devices, and x86_64 covers emulators.
    // The universal APK stays available as a fallback for anything unusual.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // The youtubedl-android library ships uncompressed python and ffmpeg
    // native libraries, which must be extracted from the APK at install time.
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // yt-dlp with a bundled Python 3.12 runtime, plus ffmpeg
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")

    testImplementation("junit:junit:4.13.2")
}
