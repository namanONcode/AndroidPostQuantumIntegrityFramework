pluginManagement {
    includeBuild("..") // Resolve AnchorPQ plugin from the root project
    repositories {
        mavenLocal()  // For locally published AnchorPQ plugin
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "AnchorPQ-Demo"
include(":app")

