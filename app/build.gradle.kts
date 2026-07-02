import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}
val hasReleaseKeystore = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
    .all { keystoreProperties.getProperty(it).isNullOrBlank().not() }

android {
    namespace = "com.arktools.anlao"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.arktools.anlao"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.0.2"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeType = keystoreProperties.getProperty("storeType", "pkcs12")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    // ========== 核心框架 ==========
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // ========== DataStore（隐私同意状态持久化） ==========
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // ========== Coroutines ==========
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ========== Serialization（防沉迷 SDK 回调需要） ==========
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // ========== Material & CardView（广告 SDK 需要） ==========
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.cardview:cardview:1.0.0")

    // ========== 广告 SDK 必须依赖 ==========
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.2.0")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    // ========== Tosin 广告 SDK Core ==========
    implementation(files("libs/tosin-ad-1.1.2.aar"))
    implementation(files("libs/tosin-adx-2.9.65.aar"))

    // ========== GDT 优量汇 ==========
    implementation(files("libs/tosin-gdt-adapter-4.690.1560.aar"))

    // ========== KS 快手 ==========
    implementation(files("libs/tosin-ks-adapter-5.1.20.1.aar"))

    // ========== Sigmob ==========
    implementation(files("libs/sigmob/tosin-sigmob_common-adapter-1.9.4.aar"))
    implementation(files("libs/sigmob/tosin-sigmob_windsdk-adapter-4.25.11.aar"))

    // ========== TopOn ==========
    implementation(files("libs/topon/tosin-anythink_banner-adapter.aar"))
    implementation(files("libs/topon/tosin-anythink_china_core.aar"))
    implementation(files("libs/topon/tosin-anythink_core-adapter.aar"))
    implementation(files("libs/topon/tosin-anythink_interstitial-adapter.aar"))
    implementation(files("libs/topon/tosin-anythink_native-adapter.aar"))
    implementation(files("libs/topon/tosin-anythink_rewardvideo-adapter.aar"))
    implementation(files("libs/topon/tosin-anythink_splash-adapter.aar"))
    implementation(files("libs/topon/tosin-anythink_adx_sdk_kuying_necessary-adapter-6.5.48.aar"))
    implementation(files("libs/topon/tosin-anythink_network_adx_kuying_sdk_necessary-adapter.aar"))

    // ========== 优量汇/优投 ==========
    implementation(files("libs/yout/tosin-adalliance-adapter-4.7.7.aar"))

    // ========== AdGain ==========
    implementation(files("libs/adgain/tosin-adgainsdk-adapter-4.2.5.aar"))
    implementation(files("libs/adgain/tosin-adgainbeizi-adapter-4.2.3.5.aar"))
    implementation(files("libs/adgain/tosin-adgaingromore-adapter-4.2.5.aar"))
    implementation(files("libs/adgain/tosin-adgainjiguang-adapter-4.2.2.1.aar"))
    implementation(files("libs/adgain/tosin-adgaintaku-adapter-4.2.3.2.aar"))
    implementation(files("libs/adgain/tosin-adgaintobid-adapter-4.2.5.aar"))

    // ========== HX/互选 ==========
    implementation(files("libs/hx/tosin-hx-sdk-1.6.17.aar"))
    implementation(files("libs/hx/tosin-hx-gromore-adapter.aar"))
    implementation(files("libs/hx/tosin-hx-mediatom-adapter.aar"))
    implementation(files("libs/hx/tosin-hx-taku-adapter.aar"))
    implementation(files("libs/hx/tosin-hx-tobid-adapter.aar"))

    // ========== 加投/Advista ==========
    implementation(files("libs/jiatou/tosin-advista-adapter-1.9.2.aar"))

    // ========== 穿山甲/字节跳动 CSJ ==========
    implementation(files("libs/tosin-csj-adapter-7.6.1.1.aar"))

    // ========== 百度联盟 ==========
    implementation(files("libs/tosin-baidu-adapter-9.450.aar"))

    // ========== AdView ==========
    implementation(files("libs/adview/tosin-adview-adapter-5.0.5.aar"))

    // ========== 倍孜/Beizi ==========
    implementation(files("libs/beizi/tosin-beizi-adapter-5.3.0.3.aar"))

    // ========== 多盟/Domob ==========
    implementation(files("libs/dm/tosin-domob-adapter-3.8.2.aar"))

    // ========== FunLink ==========
    implementation(files("libs/funlink/tosin-funlink-adapter-2.9.0_77390768.aar"))
    implementation(files("libs/funlink/tosin-funlink_gromore-adapter-2.9.0_77328722.aar"))
    implementation(files("libs/funlink/tosin-funlink_taku-adapter-2.9.0_77328722.aar"))
    implementation(files("libs/funlink/tosin-funlink_tobid-adapter-2.9.0_77328722.aar"))

    // ========== 聚推/Jutui ==========
    implementation(files("libs/jutui/tosin-jutui-adapter-4.2.3.1.aar"))

    // ========== 美数/Mintegral ==========
    implementation(files("libs/ms/tosin-ms-adapter-3.0.4.1.aar"))

    // ========== 媒介/Maimeng ==========
    implementation(files("libs/maimeng/tosin-wm-adapter-7.9.19.25.aar"))

    // ========== TapTap 广告 ==========
    implementation(files("libs/taptap/tosin-taptap-adapter-4.2.4.8.aar"))

    // ========== 天璇/UBiX ==========
    implementation(files("libs/tianxuan/tosin-UBiX-adapter-2.10.1.11.aar"))

    // ========== 中辰/Zhongchen ==========
    implementation(files("libs/zhongchen/tosin-starsads-adapter-1.3.04.aar"))

    // ========== OAID SDK（设备标识，广告归因需要） ==========
    implementation(files("libs/oaid_sdk_1.0.25.aar"))

    // ========== TapTap SDK ==========
    implementation("com.taptap.sdk:tap-core:4.10.3")
    implementation("com.taptap.sdk:tap-login:4.10.3")
    implementation("com.taptap.sdk:tap-compliance:4.10.3")
}
