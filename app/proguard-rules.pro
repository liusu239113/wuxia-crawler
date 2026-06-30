# Keep Compose
-keep class androidx.compose.** { *; }

# Keep Kotlin metadata
-keepattributes RuntimeVisibleAnnotations
-keepattributes *Annotation*
-keepattributes InnerClasses

# Keep kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.arktools.anlao.**$$serializer { *; }
-keepclassmembers class com.arktools.anlao.** {
    *** Companion;
}
-keepclasseswithmembers class com.arktools.anlao.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep DataStore preferences keys
-keepclassmembers class * extends androidx.datastore.preferences.protobuf.GeneratedMessageLite {
    <fields>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========== 广告 SDK 保护规则 ==========
# Tosin SDK
-keep class com.tosin.sdk.** { *; }
-dontwarn com.tosin.sdk.**

# 穿山甲 (如果包含在适配器内)
-keep class com.bytedance.sdk.** { *; }
-dontwarn com.bytedance.sdk.**

# 优量汇 GDT
-keep class com.qq.e.** { *; }
-dontwarn com.qq.e.**

# 快手
-keep class com.kwad.sdk.** { *; }
-dontwarn com.kwad.sdk.**

# Sigmob
-keep class com.sigmob.** { *; }
-dontwarn com.sigmob.**

# TopOn
-keep class com.anythink.** { *; }
-dontwarn com.anythink.**

# AdGain
-keep class com.adgain.** { *; }
-dontwarn com.adgain.**

# HX/互选
-keep class com.hx.sdk.** { *; }
-dontwarn com.hx.sdk.**

# 优量汇/优推 (AdAlliance)
-keep class com.yout.sdk.** { *; }
-dontwarn com.yout.sdk.**

# OAID
-keep class com.bun.miitmdid.** { *; }
-dontwarn com.bun.miitmdid.**

# ========== TapTap SDK ==========
-keep class com.taptap.sdk.** { *; }
-dontwarn com.taptap.sdk.**

# ========== 通用 ==========
# Retrofit / OkHttp
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# RxJava
-keep class io.reactivex.** { *; }
-dontwarn io.reactivex.**

# Glide
-keep class com.bumptech.glide.** { *; }
-dontwarn com.bumptech.glide.**

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
