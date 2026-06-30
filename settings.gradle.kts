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
    }
}
rootProject.name = "WuxiaCrawler"
include(":app")