# ========== 通用：忽略所有第三方 SDK 缺失类的警告 ==========
-dontwarn com.tosin.sdk.**
-dontwarn com.bytedance.sdk.**
-dontwarn com.qq.e.**
-dontwarn com.kwad.sdk.**
-dontwarn com.sigmob.**
-dontwarn com.anythink.**
-dontwarn com.adgain.**
-dontwarn com.hx.sdk.**
-dontwarn com.yout.sdk.**
-dontwarn com.ubix.**
-dontwarn com.wangmai.**
-dontwarn com.bun.miitmdid.**
-dontwarn com.advista.**
-dontwarn com.taptap.sdk.**
-dontwarn com.tapsdk.**
-dontwarn com.baidu.**
-dontwarn org.apache.http.**
-dontwarn org.json.**
-dontwarn dalvik.system.**
-dontwarn javax.annotation.**
-dontwarn com.android.org.conscrypt.**
-dontwarn javax.net.ssl.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn kotlin.reflect.jvm.internal.**
-dontwarn kotlinx.coroutines.debug.**

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

# Keep our app
-keep class com.arktools.anlao.** { *; }

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
