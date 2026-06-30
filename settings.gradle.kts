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
    }
}
rootProject.name = "WuxiaCrawler"
include(":app")