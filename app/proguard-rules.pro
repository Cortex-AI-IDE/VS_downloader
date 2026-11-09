# Keep rules for the download engine.
# youtubedl-android uses Jackson reflection for its video metadata mapper.
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**
