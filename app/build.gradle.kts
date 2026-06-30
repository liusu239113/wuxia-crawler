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
        versionCode = 1
        versionName = "1.0.0"
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
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    buildTypes {
        release {
            check(hasReleaseKeystore) {
                "Release signing is not configured. Create keystore.properties or configure GitHub Actions signing secrets."
            }
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
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
    implementation(files("libs/adgain/tosin-adgainsdk-adapter-4.2.7.2.aar"))
    implementation(files("libs/adgain/tosin-adgainbeizi-adapter-4.2.5.4.aar"))
    implementation(files("libs/adgain/tosin-adgaingromore-adapter-4.2.7.aar"))
    implementation(files("libs/adgain/tosin-adgainjiguang-adapter-4.2.2.1.aar"))
    implementation(files("libs/adgain/tosin-adgaintaku-adapter-4.2.7.aar"))
    implementation(files("libs/adgain/tosin-adgaintobid-adapter-4.2.7.aar"))
    implementation(files("libs/adgain/tosin-admate-adapter-4.2.7.aar"))
    implementation(files("libs/adgain/tosin-mediatom-adapter-4.2.5.2.aar"))

    // ========== HX/互选 ==========
    implementation(files("libs/hx/tosin-hx-sdk-1.6.17.aar"))
    implementation(files("libs/hx/tosin-hx-gromore-adapter.aar"))
    implementation(files("libs/hx/tosin-hx-mediatom-adapter.aar"))
    implementation(files("libs/hx/tosin-hx-taku-adapter.aar"))
    implementation(files("libs/hx/tosin-hx-tobid-adapter.aar"))

    // ========== 加投/Advista ==========
    implementation(files("libs/jiatou/tosin-advista-adapter-1.9.2.aar"))

    // ========== OAID SDK（设备标识，广告归因需要） ==========
    implementation(files("libs/oaid_sdk_1.0.25.aar"))
    implementation("com.github.gzu-liyujiang:Android_CN_OAID:4.2.9")

    // ========== TapTap SDK ==========
    implementation("com.taptap.sdk:tap-core:4.10.3")
    implementation("com.taptap.sdk:tap-login:4.10.3")
    implementation("com.taptap.sdk:tap-compliance:4.10.3")
}
