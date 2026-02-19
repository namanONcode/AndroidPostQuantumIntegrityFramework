# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep Bouncy Castle classes for post-quantum cryptography
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# Keep Retrofit interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep GSON serialization models
-keep class com.anchorpq.demo.model.** { *; }
-keep class com.anchorpq.demo.network.** { *; }

# Keep AnchorPQ BuildConfig fields
-keep class com.anchorpq.demo.BuildConfig { *; }

