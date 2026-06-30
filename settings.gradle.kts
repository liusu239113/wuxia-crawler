pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TapTap SDK 仓库
        maven { url = uri("https://releases.tap.io/repository/tds-public") }
        // OAID SDK
        maven { url = uri("https://jitpack.io") }
        // Huawei Maven（OAID依赖）
        maven { url = uri("https://developer.huawei.com/repo/") }
        // Honor Maven
        maven { url = uri("https://developer.honor.com/repo/") }
    }
}
rootProject.name = "WuxiaCrawler"
include(":app")