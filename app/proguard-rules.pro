# ========== 关闭 R8 优化（保留压缩+混淆） ==========
-dontoptimize

# ========== 通用：忽略所有第三方 SDK 缺失类的警告 ==========
-ignorewarnings
-dontwarn **

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
-keep class com.tosin.sdk.** { *; }
-keepclassmembers class com.tosin.sdk.** { *; }
-keep class com.tosin.sdk.initsdk.** { *; }
-keepclassmembers class com.tosin.sdk.initsdk.** { *; }
-keep class com.tosin.sdk.loadAd.** { *; }
-keepclassmembers class com.tosin.sdk.loadAd.** { *; }
-keep class com.tosin.sdk.datasource.bean.** { *; }
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

# ========== 快手 SDK ==========
-keep class org.chromium.** { *; }
-keep class aegon.chrome.** { *; }
-keep class com.kwai.** { *; }
-dontwarn com.kwai.**
-dontwarn com.kwad.**
-dontwarn com.ksad.**
-dontwarn aegon.chrome.**

# ========== 百度联盟 ==========
-keep class com.baidu.mobads.** { *; }
-keep class com.style.widget.** { *; }
-keep class com.component.** { *; }
-keep class com.baidu.ad.magic.flute.** { *; }
-keep class com.baidu.mobstat.forbes.** { *; }
-keep class android.support.v7.widget.RecyclerView { *; }
-keepnames class android.support.v7.widget.RecyclerView$* { public <fields>; public <methods>; }
-keep class android.support.v7.widget.LinearLayoutManager { *; }
-keep class android.support.v7.widget.PagerSnapHelper { *; }
-keep class android.support.v4.view.ViewCompat { *; }
-keep class android.support.v4.util.LongSparseArray { *; }
-keep class android.support.v4.util.ArraySet { *; }
-keep class android.support.v4.view.accessibility.AccessibilityNodeInfoCompat { *; }

# ========== Adgain SDK ==========
-dontwarn com.adgain.sdk.**
-keep class com.adgain.sdk.** { *; }
-keep interface com.adgain.** { *; }

# ========== TopOn SDK ==========
-keep public class com.anythink.** { *; }
-keepclassmembers class com.anythink.** { *; }
-dontwarn com.anythink.hb.**
-keep class com.anythink.hb.** { *; }
-dontwarn com.anythink.china.api.**
-keep class com.anythink.china.api.** { *; }
-keep class com.anythink.myoffer.ui.** { *; }
-keepclassmembers public class com.anythink.myoffer.ui.** { public *; }

# ========== FunLink SDK ==========
-keep class com.fl.** { *; }

# ========== 上海佳投 Advista SDK ==========
-keep class com.jiaads.advista.sdk.AdvistaSdk { public static get*(); public java.lang.String geVersion(); public *** init(...); public *** load*(...); public *** saveCustomParams(...); }
-keep class com.jiaads.advista.sdk.AdLoadParams { public *; }
-keep class com.jiaads.advista.sdk.AdLoadParams$Builder { public *; }
-keep class com.jiaads.advista.sdk.AdConfig { public *; }
-keep class com.jiaads.advista.sdk.InitListener { public protected *; }
-keep class com.jiaads.advista.mob.ad.listener.** { *; }
-keep class com.jiaads.advista.mob.ad.TemplateAd { public com.jiaads.advista.mob.ad.AdControllerProxy getAdControllerProxy(...); }
-keep class com.jiaads.advista.mob.ad.AdControllerProxy { public <methods>; }
-keep class com.jiaads.advista.util.DeviceUtils { public static getDeviceInfoJson*(...); native <methods>; }
-keep class com.jiaads.advista.mob.ad.self.NativeFeedsData { public *; }
-keep public interface com.jiaads.advista.mob.ad.self.NativeAdEventListener { public protected *; }
-keep public interface com.jiaads.advista.mob.ad.self.NativeAdListener { public protected *; }
-keep public interface com.jiaads.advista.mob.ad.self.NativeVideoViewListener { public protected *; }
-keep class com.jiaads.advista.mob.ad.self.NMPlayerView { public <methods>; }
-keep class com.jiaads.advista.database.DatabaseHelper { native <methods>; }
-keep class com.jiaads.advista.entity.AdsTemplate$** { *; }

# ========== 天璇 UBiX SDK ==========
-keep class com.ubix.** { *; }

# ========== 美数 Meishu SDK ==========
-keep class com.meishu.sdk.** { *; }

# ========== 多盟 Domob SDK ==========
-dontwarn com.domob.sdk.**
-keep class com.domob.sdk.** { *; }
-keep interface com.domob.sdk.** { *; }

# ========== TapTap SDK ==========
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn com.tapsdk.tapad.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-keepattributes JavascriptInterface
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keepclasseswithmembers class * { public <init>(android.content.Context, android.util.AttributeSet); }
-keepclasseswithmembers class * { public <init>(android.content.Context, android.util.AttributeSet, int); }
-keep class * implements android.os.Parcelable { public static final android.os.Parcelable$Creator *; }
-keepclassmembers class * extends com.tapsdk.tapad.protobuf.GeneratedMessageLite { <fields>; }
-keepnames class * extends com.tapsdk.tapad.protobuf.GeneratedMessageLite
-keepnames class * extends com.tapsdk.tapad.protobuf.GeneratedMessageLite$Builder
-keeppackagenames com.tapsdk.tapad.**

# ========== 欢效 Huanxiao SDK ==========
-keep class com.huanxiao.sdk.** { *; }
-dontwarn com.huanxiao.sdk.**

# ========== 聚推 JuTui SDK ==========
-keep class com.jutui.ads.** { *; }

# ========== 通用属性 ==========
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod
