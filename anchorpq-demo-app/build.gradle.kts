// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.8.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.diffplug.spotless") version "8.2.1"
    // AnchorPQ plugin will be applied at app module level
}

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.8.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

allprojects {
    // Apply Spotless for code formatting
    apply(plugin = "com.diffplug.spotless")

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("**/build/**/*.kt")
            ktlint("1.1.1")
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint("1.1.1")
            trimTrailingWhitespace()
            endWithNewline()
        }
        java {
            target("**/*.java")
            googleJavaFormat("1.19.2").aosp()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}



