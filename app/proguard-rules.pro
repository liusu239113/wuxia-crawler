# ========== 通用：忽略所有第三方 SDK 缺失类的警告 ==========
-ignorewarnings
-dontwarn **

# 禁用 R8 优化（保留 shrinking 和 obfuscation），防止 R8 合并/删除方法导致 NoSuchMethodError
-dontoptimize

# Keep Compose - 防止 R8 删除/合并 Compose 内部类导致 NoSuchMethodError
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
# 强制保留 KeyframesSpec 相关类（R8 optimize 模式会合并/删除导致 CircularProgressIndicator 崩溃）
-keep,allowobfuscation class androidx.compose.animation.core.KeyframesSpec** { *; }
-keep,allowobfuscation class androidx.compose.animation.core.KeyframeEntity** { *; }
-keepclassmembers class androidx.compose.animation.core.KeyframesSpec$KeyframesSpecConfig {
    *** at(...);
}

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

# ========== 广告 SDK：必须 keep，否则 release 包会丢回调/崩溃 ==========
-keep class com.tosin.** { *; }
-dontwarn com.tosin.**
-keep class com.qq.e.** { *; }
-dontwarn com.qq.e.**
-keep class com.kwad.** { *; }
-dontwarn com.kwad.**
-keep class com.sigmob.** { *; }
-dontwarn com.sigmob.**
-keep class com.anythink.** { *; }
-dontwarn com.anythink.**
-keep class com.bun.miitmdid.** { *; }
-dontwarn com.bun.miitmdid.**
-keep class com.taptap.sdk.** { *; }
-dontwarn com.taptap.sdk.**
-keep class com.bytedance.sdk.** { *; }
-dontwarn com.bytedance.sdk.**
-keep class com.baidu.mobads.** { *; }
-dontwarn com.baidu.mobads.**
-keep class com.adview.** { *; }
-dontwarn com.adview.**
-keep class com.beizi.** { *; }
-dontwarn com.beizi.**
-keep class cn.domob.** { *; }
-dontwarn cn.domob.**
-keep class com.funlink.** { *; }
-dontwarn com.funlink.**
-keep class com.jutui.** { *; }
-dontwarn com.jutui.**
-keep class com.mintegral.** { *; }
-dontwarn com.mintegral.**
-keep class com.wemob.** { *; }
-dontwarn com.wemob.**
-keep class com.ubix.** { *; }
-dontwarn com.ubix.**
-keep class com.starsads.** { *; }
-dontwarn com.starsads.**
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
