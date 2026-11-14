# Moshi code-generated JsonAdapters and reflective Kotlin fallback
-keep class com.example.data.*JsonAdapter { *; }
-keep @com.squareup.moshi.JsonClass class com.example.data.** { *; }
-keepnames class kotlin.Metadata { *; }

# Keep domain models & enums referenced by Room/Compose/BackupManager
-keep enum com.example.domain.** { *; }

# Firebase App Check & Google Play Services resource lookups
-keep class com.google.firebase.appcheck.** { *; }
-keepclassmembers class com.example.ai.AppCheckHelperKt { *; }

# Preserve line numbers for crash stack traces while hiding file names
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
