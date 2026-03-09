// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "9.0.1" apply false
    id("com.diffplug.spotless") version "8.3.0"
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
        classpath("com.android.tools.build:gradle:9.0.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.21")
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
