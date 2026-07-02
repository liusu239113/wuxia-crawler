# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# 避免 SDK 内部类被优化删除
-keep class com.tosin.sdk.** { *; }
-keepclassmembers class com.tosin.sdk.** { *; }
-keep class com.tosin.sdk.initsdk.** { *; }
-keepclassmembers class com.tosin.sdk.initsdk.** { *; }
-keep class com.tosin.sdk.loadAd.** { *; }
-keepclassmembers class com.tosin.sdk.loadAd.** { *; }
-keep class com.bun.miitmdid.** { *; }
-keep class com.tosin.sdk.datasource.bean.** { *; }



#快手 start==================
-keep class org.chromium.** {*;}
-keep class org.chromium.** { *; }
-keep class aegon.chrome.** { *; }
-keep class com.kwai.**{ *; }
-dontwarn com.kwai.**
-dontwarn com.kwad.**
-dontwarn com.ksad.**
-dontwarn aegon.chrome.**
#快手 end==================

#百度联盟 start==================
-ignorewarnings
-dontwarn com.baidu.mobads.sdk.api.**
-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class com.baidu.mobads.** { *; }
-keep class com.style.widget.** {*;}
-keep class com.component.** {*;}
-keep class com.baidu.ad.magic.flute.** {*;}
-keep class com.baidu.mobstat.forbes.** {*;}
#9.22版本新增加混淆，9.25版本不再要求
-keep class android.support.v7.widget.RecyclerView {*;}
-keepnames class android.support.v7.widget.RecyclerView$* {
    public <fields>;
    public <methods>;
}
-keep class android.support.v7.widget.LinearLayoutManager {*;}
-keep class android.support.v7.widget.PagerSnapHelper {*;}
-keep class android.support.v4.view.ViewCompat {*;}
-keep class android.support.v4.util.LongSparseArray {*;}
-keep class android.support.v4.util.ArraySet {*;}
-keep class android.support.v4.view.accessibility.AccessibilityNodeInfoCompat {*;}

#如果接入微信小游戏调起，需按微信要求添加以下keep
-keep class com.tencent.mm.opensdk.** {
    *;
}
-keep class com.tencent.wxop.** {
    *;
}
-keep class com.tencent.mm.sdk.** {
    *;
}
#百度联盟 end==================

#Adgain混淆 start==================
-dontwarn com.adgain.sdk.**
-keep class com.adgain.sdk.** {*;}
-keep interface com.adgain.**{ *;}
#Adgain end==================

#TopOn混淆 start==================
-keep public class com.anythink.**
-keepclassmembers class com.anythink.** {
   *;
}
-keep public class com.anythink.network.**
-keepclassmembers class com.anythink.network.** {
   public *;
}
-dontwarn com.anythink.hb.**
-keep class com.anythink.hb.**{ *;}
-dontwarn com.anythink.china.api.**
-keep class com.anythink.china.api.**{ *;}
-keep class com.anythink.myoffer.ui.**{ *;}
-keepclassmembers public class com.anythink.myoffer.ui.** {
   public *;
}
#TopOn end==================

#优推混淆 start==================
-keep class com.alliance.ssp.ad.** { *; }
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

-keep class com.gameley.templatesdk.** { *; }
-keep enum com.gameley.templatesdk.** { *; }
#优推 end==================

#funlink start==================
-keep class com.fl.** {*;}
#funlink  end==================

#上海佳投SDk start==================
-keep class com.jiaads.advista.sdk.AdvistaSdk {
    public static get*();
    public java.lang.String geVersion();
    public *** init(...);
    public *** load*(...);
    public *** saveCustomParams(...);
}
-keep class com.jiaads.advista.sdk.AdLoadParams {
    public *;
}
-keep class com.jiaads.advista.sdk.AdLoadParams$Builder {
    public *;
}
-keep class com.jiaads.advista.sdk.AdConfig {
    public *;
}
-keep class com.jiaads.advista.sdk.InitListener {
    public protected *;
}

-keep class com.jiaads.advista.mob.ad.listener.** {
    *;
}
-keep class com.jiaads.advista.mob.ad.TemplateAd {
     public com.jiaads.advista.mob.ad.AdControllerProxy getAdControllerProxy(...);
}
-keep class com.jiaads.advista.mob.ad.AdControllerProxy {
    public <methods>;
}
-keep class com.jiaads.advista.util.DeviceUtils {
    public static getDeviceInfoJson*(...);
    native <methods>;
}
-keep class com.jiaads.advista.mob.ad.self.NativeFeedsData {
    public *;
}
-keep public interface com.jiaads.advista.mob.ad.self.NativeAdEventListener {
    public protected *;
}
-keep public interface com.jiaads.advista.mob.ad.self.NativeAdListener {
    public protected *;
}
-keep public interface com.jiaads.advista.mob.ad.self.NativeVideoViewListener {
    public protected *;
}
-keep class com.jiaads.advista.mob.ad.self.NMPlayerView {
    public <methods>;
}
-keep class com.jiaads.advista.database.DatabaseHelper{
    native <methods>;
}
-keep class com.jiaads.advista.entity.AdsTemplate$** {
    *;
}
#上海佳投sdk  end==================

#天璇sdk  start==================
-keep class com.ubix.** { *;}
#天璇sdk  end==================

#美数sdk  start==================
-keep class com.meishu.sdk.** { *; }
#美数sdk  end==================

#多盟SDK start==================
-dontwarn com.domob.sdk.**
-keep class com.domob.sdk.**{*;}
-keep interface com.domob.sdk.**{*;}
#多盟sdk  end==================

#taptap start==================
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
-keepclassmembers class * extends com.tapsdk.tapad.protobuf.GeneratedMessageLite {
    <fields>;
}
-keepnames class * extends com.tapsdk.tapad.protobuf.GeneratedMessageLite
-keepnames class * extends com.tapsdk.tapad.protobuf.GeneratedMessageLite$Builder
-keeppackagenames com.tapsdk.tapad.**
#taptap  end==================

#欢效SDK start==================
-keep class com.huanxiao.sdk.** { *; }
-dontwarn com.huanxiao.sdk.**
#欢效SDK  end==================

# JuTui 混淆
-keep class com.jutui.ads.**{ *;}
