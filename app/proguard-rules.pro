# NoteD Production Proguard/R8 Configuration Rules

# 1. Keep Kotlin and Serialization Attributes
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# 2. Room Database Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomOpenHelper
-keep class * {
    @androidx.room.Dao *;
    @androidx.room.Entity *;
}
-dontwarn androidx.room.paging.**

# 3. Moshi & JSON Parsing Obfuscation Prevention
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.repository.** { *; }
-keep @com.squareup.moshi.JsonClass class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# 4. Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @retrofit2.http.** <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# 5. Jetpack Compose and Lifecycle Integration
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.compose.** { *; }

# 6. Android Biometric and Encryption Security API Guidance
-dontwarn androidx.biometric.**
-keep class androidx.biometric.** { *; }
-keep class androidx.security.crypto.** { *; }
