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
        maven { url = uri("https://jitpack.io") } // For MPAndroidChart
        maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") } // For Compose
        // For TensorFlow
        maven { url = uri("https://storage.googleapis.com/download.tensorflow.org/repos/tensorflow") }
        // For OpenCV
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "Struku"
include(":app")
