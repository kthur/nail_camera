# Add project specific ProGuard rules here.

# ---------------------------------------------------------------------------
# TensorFlow Lite
# ---------------------------------------------------------------------------
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.gpu.** { *; }
-dontwarn org.tensorflow.**

# ---------------------------------------------------------------------------
# Jetpack Compose
# ---------------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ---------------------------------------------------------------------------
# Room Database
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * extends androidx.room.RoomDatabase {
    abstract *;
}
-dontwarn androidx.room.**

# ---------------------------------------------------------------------------
# Kotlin Serialization
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.example.nailnutri.**$$serializer { *; }
-keepclassmembers class com.example.nailnutri.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.nailnutri.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------------------------------------------------------------------------
# Kotlin
# ---------------------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ---------------------------------------------------------------------------
# Google / Firebase
# ---------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ---------------------------------------------------------------------------
# General Android
# ---------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
