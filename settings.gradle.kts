pluginManagement {
    repositories {
        maven(url = "https://jitpack.io")
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        flatDir {
            dirs("${rootProject.projectDir}/opencv/OpenCV-android-sdk/sdk/native/libs")
        }
    }
}

rootProject.name = "Face Guard 3D"
include(":app")
include(":opencv")
project(":opencv").projectDir = File("opencv/OpenCV-android-sdk/sdk/java")