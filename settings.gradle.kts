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
        google()  // Google's Maven repository (contains ML Kit)
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // For MPAndroidChart
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") } // For Compose
        // For OpenCV
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "Struku"
include(":app")
