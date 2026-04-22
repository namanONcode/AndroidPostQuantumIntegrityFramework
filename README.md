# 🛡️ AnchorPQ - Android Post-Quantum Integrity Framework

<a href="https://www.producthunt.com/products/anchor-pq-gradle-plugin?embed=true&amp;utm_source=badge-featured&amp;utm_medium=badge&amp;utm_campaign=badge-anchor-pq-gradle-plugin" target="_blank" rel="noopener noreferrer"><img alt="Anchor-pq Gradle Plugin - Android Post-Quantum Integrity Framework | Product Hunt" width="250" height="54" src="https://api.producthunt.com/widgets/embed-image/v1/featured.svg?post_id=1088480&amp;theme=dark&amp;t=1772459318315"></a>

[![CI Build](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/ci.yml/badge.svg)](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/ci.yml)
[![CodeQL](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/codeql.yml/badge.svg)](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Gradle Plugin](https://img.shields.io/badge/Gradle-9.3%2B-green.svg)](https://gradle.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg)](https://developer.android.com/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.8%2B-red.svg)](https://quarkus.io/)

A comprehensive framework for **build-time integrity verification** and **runtime server-anchored validation** of Android applications using **Merkle trees** and **post-quantum cryptography (ML-KEM/CRYSTALS-Kyber)**.

## 🎥 Demo

See real-time APK tampering detection using post-quantum cryptography:

[![Watch the demo](https://img.youtube.com/vi/Ek7sttNZsLM/maxresdefault.jpg)](https://youtu.be/Ek7sttNZsLM)

## 🏗️ Project Structure

This monorepo contains three main components:

| Component | Description | Documentation |
|-----------|-------------|---------------|
| **[Gradle Plugin](/)** | Build-time Merkle tree computation for Android apps | This README |
| **[Verification Server](anchorpq-server/)** | Quarkus backend for runtime integrity verification | [Server README](SERVER-README.md) |
| **[Demo App](anchorpq-demo-app/)** | Production-grade Android demo with ML-KEM | [Demo README](anchorpq-demo-app/README.md) |

```
AndroidPostQuantumIntegrityFramework/
├── src/                    # Gradle Plugin source code
├── anchorpq-server/        # Quarkus Verification Server
├── anchorpq-demo-app/      # Production-grade Android Demo App
├── scripts/                # Helper scripts (E2E tests, DB seeding)
├── docker-compose.yml      # Docker setup for server + PostgreSQL
└── .github/workflows/      # CI/CD pipeline
```

---

## 📋 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Architecture](#-architecture)
- [How It Works](#-how-it-works)
- [Installation](#-installation)
- [Quick Start](#-quick-start)
- [Configuration](#-configuration)
- [Generated Outputs](#-generated-outputs)
- [Runtime Integration](#-runtime-integration)
- [Server-Side Verification](#-server-side-verification)
- [Verification Server](#-verification-server)
- [Complete Example App](#-complete-example-app)
- [API Reference](#-api-reference)
- [Security Model](#-security-model)
- [Troubleshooting](#-troubleshooting)
- [Requirements](#-requirements)
- [License](#-license)

---

## 🌟 Overview

### What is this framework?

The Android Post-Quantum Integrity Framework consists of:

1. **Gradle Plugin (Build Time)**: Computes a cryptographic fingerprint (Merkle root) of your app's compiled code
2. **Verification Server (Runtime)**: Validates app integrity using server-anchored trust model
3. **Post-Quantum Security**: Uses ML-KEM (CRYSTALS-Kyber) for quantum-resistant communication

### Why do you need it?

- **Detect Tampering**: Immediately know if your APK has been modified
- **Prevent Piracy**: Stop unauthorized redistribution of modified apps
- **Compliance**: Meet security requirements for sensitive applications
- **Future-Proof**: Post-quantum cryptography protects against quantum computer attacks

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🌳 **Merkle Tree Computation** | Deterministic hash tree of all compiled `.class` files |
| 📄 **Integrity Metadata** | JSON/XML files with build information and Merkle root |
| 📱 **Assets Integration** | Automatically packages integrity data into your APK |
| 🔐 **ML-KEM Encryption** | Post-quantum secure communication for runtime reporting |
| 🤖 **AGP 9+ Compatible** | Works with Android Gradle Plugin 9.x with built-in Kotlin |
| ⚡ **Automatic Integration** | Hooks into standard build lifecycle |
| 🔧 **Configurable** | Multiple hash algorithms and output formats |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BUILD TIME                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌────────────────────┐    ┌──────────────────────┐    │
│  │   Compile    │    │   GenerateMerkle   │    │  GenerateMetadata    │    │
│  │   Kotlin/    │───▶│      Task          │───▶│      Task            │    │
│  │   Java       │    │  (Hash all .class) │    │  (Create JSON/XML)   │    │
│  └──────────────┘    └────────────────────┘    └──────────────────────┘    │
│                               │                          │                  │
│                               ▼                          ▼                  │
│                      ┌────────────────┐        ┌─────────────────┐         │
│                      │ merkle-root.txt│        │ integrity.json  │         │
│                      │ (SHA-256 hash) │        │ (Full metadata) │         │
│                      └────────────────┘        └─────────────────┘         │
│                                                          │                  │
│                               ┌───────────────────────────┘                  │
│                               ▼                                              │
│                      ┌─────────────────────┐                                │
│                      │  CopyIntegrityAssets │                                │
│                      │       Task          │                                │
│                      │ (Copy to APK assets)│                                │
│                      └─────────────────────┘                                │
│                                                                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                              RUNTIME                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌────────────────────┐    ┌──────────────────────┐    │
│  │  App Starts  │    │  Read integrity    │    │  Compare with        │    │
│  │              │───▶│  .json from assets │───▶│  server expected     │    │
│  │              │    │                    │    │  Merkle root         │    │
│  └──────────────┘    └────────────────────┘    └──────────────────────┘    │
│                                                          │                  │
│                               ┌───────────────────────────┘                  │
│                               ▼                                              │
│                      ┌─────────────────────┐                                │
│                      │   MATCH?            │                                │
│                      │   ✅ Allow app      │                                │
│                      │   ❌ Block/Alert    │                                │
│                      └─────────────────────┘                                │
│                                                                              │
│  Optional: ML-KEM encrypted communication with verification server          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 🔍 How It Works

### Step 1: Merkle Tree Construction

The plugin creates a binary hash tree from your compiled bytecode:

```
                    ┌─────────────────┐
                    │   Merkle Root   │
                    │  (Final Hash)   │
                    └────────┬────────┘
                             │
              ┌──────────────┴──────────────┐
              │                             │
        ┌─────┴─────┐                 ┌─────┴─────┐
        │  Hash AB  │                 │  Hash CD  │
        └─────┬─────┘                 └─────┬─────┘
              │                             │
       ┌──────┴──────┐               ┌──────┴──────┐
       │             │               │             │
   ┌───┴───┐    ┌───┴───┐       ┌───┴───┐    ┌───┴───┐
   │Hash A │    │Hash B │       │Hash C │    │Hash D │
   └───┬───┘    └───┬───┘       └───┬───┘    └───┬───┘
       │            │               │            │
   ┌───┴───┐    ┌───┴───┐       ┌───┴───┐    ┌───┴───┐
   │A.class│    │B.class│       │C.class│    │D.class│
   └───────┘    └───────┘       └───────┘    └───────┘
```

**Properties:**
- ✅ **Deterministic**: Same code always produces the same root
- ✅ **Tamper-evident**: Any change to any file changes the root
- ✅ **Efficient**: O(log n) verification with Merkle proofs

### Step 2: File Exclusions

The plugin automatically excludes generated files that change every build:

| Excluded Pattern | Reason |
|-----------------|--------|
| `R.class`, `R$*.class` | Resource IDs change frequently |
| `BuildConfig.class` | Contains build-specific values |
| `*$$*.class` | Dagger/Hilt generated code |
| `*_Factory.class` | Dependency injection factories |
| `Hilt_*.class` | Hilt generated classes |

### Step 3: Runtime Verification Flow

```
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  App Start  │────▶│  Load Merkle    │────▶│  Send to Server │
│             │     │  Root from      │     │  for Validation │
│             │     │  assets/        │     │                 │
└─────────────┘     │  integrity.json │     └────────┬────────┘
                    └─────────────────┘              │
                                                     ▼
┌─────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Block App   │◀────│  Roots Don't    │◀────│  Server Compare │
│ Show Error  │     │  Match!         │     │  with Expected  │
└─────────────┘     └─────────────────┘     └────────┬────────┘
                                                     │
                    ┌─────────────────┐              │
                    │  Roots Match!   │◀─────────────┘
                    │  Allow App      │
                    └─────────────────┘
```

---

## 📦 Installation

Post-quantum integrity verification plugin for Android applications using Merkle trees and ML-KEM (CRYSTALS-Kyber).

### Using the plugins DSL

Add this plugin to your build using the plugins DSL:

```kotlin
plugins {
  id("io.github.namanoncode.anchorpq") version "1.0.0"
}
```

See also:

Adding the plugin to build logic for usage in precompiled script plugins.
See the relevant documentation for more information.

Add this plugin as a dependency to `<convention-plugins-build>/build.gradle(.kts)`:

```kotlin
dependencies {
  implementation("io.github.namanoncode.anchorpq:io.github.namanoncode.anchorpq.gradle.plugin:1.0.0")
}
```

It can then be applied in the precompiled script plugin:
```kotlin
plugins {
  id("io.github.namanoncode.anchorpq")
}
```

### The legacy method of plugin application

See the relevant documentation for more information.

```kotlin
buildscript {
  repositories {
    gradlePluginPortal()
  }
  dependencies {
    classpath("io.github.namanoncode.anchorpq:io.github.namanoncode.anchorpq.gradle.plugin:1.0.0")
  }
}

apply(plugin = "io.github.namanoncode.anchorpq")
```

### Method 1: Local JAR (Development)

**Step 1:** Build the plugin JAR

```bash
cd AndroidPostQuantumIntegrityFramework
./gradlew clean build
```

This creates: `build/libs/AndroidPostQuantumIntegrityFramework-1.0.0.jar`

**Step 2:** In your Android project's `build.gradle.kts` (root level):

```kotlin
// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
        // Local plugin JAR
        flatDir {
            dirs("path/to/AndroidPostQuantumIntegrityFramework/build/libs")
        }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.0.1")
        
        // Include the local integrity plugin JAR
        classpath(files("path/to/AndroidPostQuantumIntegrityFramework/build/libs/AndroidPostQuantumIntegrityFramework-1.0.0.jar"))
        
        // Plugin dependencies (required)
        classpath("org.bouncycastle:bcprov-jdk18on:1.83")
        classpath("org.bouncycastle:bcpkix-jdk18on:1.83")
        classpath("com.google.code.gson:gson:2.13.2")
    }
}

plugins {
    id("com.android.application") version "9.0.1" apply false
}
```

**Step 3:** In your Android project's `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Include local plugin
        flatDir {
            dirs("path/to/AndroidPostQuantumIntegrityFramework/build/libs")
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "YourApp"
include(":app")
```

### Method 2: Maven Local (Recommended for Teams)

```bash
cd AndroidPostQuantumIntegrityFramework
./gradlew publishToMavenLocal
```

Then in your project:

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

---

## 🚀 Quick Start

### Step 1: Apply the Plugin

In your app module's `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("io.github.namanoncode.anchorpq")  // Add this line
}

android {
    namespace = "com.yourcompany.yourapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourcompany.yourapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        // Add MERKLE_ROOT placeholder to BuildConfig
        buildConfigField("String", "MERKLE_ROOT", "\"GENERATED_AT_BUILD_TIME\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // Required for Bouncy Castle JAR files
    packaging {
        resources {
            excludes += listOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/BCKEY.DSA",
                "META-INF/BCKEY.SF"
            )
        }
    }
}

// Configure the Anchor PQ Integrity plugin
anchorpq {
    enabled = true
    algorithm = "SHA-256"
    injectBuildConfig = true
    version = android.defaultConfig.versionName ?: "1.0.0"
    mlKemEnabled = true
}

dependencies {
    // Bouncy Castle for ML-KEM (post-quantum crypto)
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.13.2")
    
    // Your other dependencies...
}
```

### Step 2: Create gradle.properties

Create or update `gradle.properties` in your project root:

```properties
# AndroidX settings
android.useAndroidX=true
android.enableJetifier=false

# Kotlin
kotlin.code.style=official

# Build performance
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
```

### Step 3: Create local.properties

Create `local.properties` in your project root:

```properties
sdk.dir=/path/to/your/Android/Sdk
```

### Step 4: Build Your App

```bash
./gradlew assembleDebug
```

You should see output like:

```
> Task :app:generateMerkleRootDebug
Generating Merkle root for variant: debug
Classes directory: .../app/build/intermediates/javac/debug/classes
Found 42 class files for integrity computation
Merkle root computed: 45e2f3b4eab4253af3de4887a435b71cb6694f9d0b07026e23c9fdfda50afaa1

> Task :app:generateIntegrityMetadataDebug
Generating integrity metadata for variant: debug
Integrity metadata written to: .../app/build/anchorpq/debug/integrity.json

> Task :app:copyIntegrityAssetsDebug
Copied integrity.json to: .../app/src/main/assets/integrity.json
```

---

## ⚙️ Configuration

### Full Configuration Options

```kotlin
anchorpq {
    // Enable/disable the plugin entirely
    // Default: true
    enabled = true
    
    // Hash algorithm for Merkle tree computation
    // Options: "SHA-256", "SHA-384", "SHA-512", "SHA3-256", "SHA3-512"
    // Default: "SHA-256"
    algorithm = "SHA-256"
    
    // Inject MERKLE_ROOT into BuildConfig
    // Default: true
    injectBuildConfig = true
    
    // Application version (included in metadata)
    // Default: "1.0.0"
    version = "1.0.0"
    
    // Optional: Signing certificate SHA-256 fingerprint
    // This helps verify the app was signed with the expected key
    signerFingerprint = "AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99:AA:BB:CC:DD:EE:FF:00:11:22:33:44:55:66:77:88:99"
    
    // Enable ML-KEM (post-quantum) encryption for runtime reporting
    // Default: false
    mlKemEnabled = true
    
    // Backend endpoint for integrity reporting (optional)
    reportingEndpoint = "https://api.yourserver.com/verify"
}
```

### Algorithm Comparison

| Algorithm | Hash Size | Security Level | Performance |
|-----------|-----------|----------------|-------------|
| SHA-256 | 256 bits | Standard | ⚡ Fast |
| SHA-384 | 384 bits | High | ⚡ Fast |
| SHA-512 | 512 bits | Very High | ⚡ Fast |
| SHA3-256 | 256 bits | Quantum-Resistant | 🔄 Medium |
| SHA3-512 | 512 bits | Quantum-Resistant | 🔄 Medium |

---

## 📁 Generated Outputs

After building, the plugin generates the following files:

### 1. Merkle Root File

**Location:** `app/build/anchorpq/{variant}/merkle-root.txt`

```
45e2f3b4eab4253af3de4887a435b71cb6694f9d0b07026e23c9fdfda50afaa1
```

### 2. Integrity Metadata (JSON)

**Location:** `app/build/anchorpq/{variant}/integrity.json`

```json
{
  "version": "1.0.0",
  "variant": "debug",
  "hashAlgorithm": "SHA-256",
  "merkleRoot": "45e2f3b4eab4253af3de4887a435b71cb6694f9d0b07026e23c9fdfda50afaa1",
  "timestamp": "2026-02-18T15:10:52.044713221Z",
  "leafCount": 42,
  "plugin": {
    "name": "Anchor PQ Integrity Plugin",
    "pluginVersion": "1.0.0"
  }
}
```

### 3. Integrity Metadata (XML)

**Location:** `app/build/anchorpq/{variant}/integrity.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<integrity>
    <version>1.0.0</version>
    <variant>debug</variant>
    <hashAlgorithm>SHA-256</hashAlgorithm>
    <merkleRoot>45e2f3b4eab4253af3de4887a435b71cb6694f9d0b07026e23c9fdfda50afaa1</merkleRoot>
    <timestamp>2026-02-18T15:10:52.044713221Z</timestamp>
</integrity>
```

### 4. APK Assets

**Location in APK:** `assets/integrity.json`

The `integrity.json` is automatically copied to your app's assets folder and packaged into the APK.

---

## 📱 Runtime Integration

### Reading the Merkle Root in Your App

Create an Application class to read and cache the Merkle root:

```kotlin
package com.yourcompany.yourapp

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import java.io.InputStreamReader

class MyApplication : Application() {

    companion object {
        private const val TAG = "IntegrityCheck"
        private const val INTEGRITY_ASSET_FILE = "integrity.json"
        
        @Volatile
        var integrityVerified: Boolean = false
            private set
    }

    private var cachedMerkleRoot: String? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App starting with Merkle root: ${getMerkleRoot()}")
    }

    /**
     * Get the Merkle root from assets/integrity.json
     */
    fun getMerkleRoot(): String {
        cachedMerkleRoot?.let { return it }

        try {
            assets.open(INTEGRITY_ASSET_FILE).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val data = Gson().fromJson(reader, IntegrityMetadata::class.java)
                    data?.merkleRoot?.let {
                        cachedMerkleRoot = it
                        Log.i(TAG, "Loaded Merkle root: ${it.take(16)}...")
                        return it
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load integrity.json: ${e.message}")
        }
        return "UNKNOWN"
    }

    data class IntegrityMetadata(
        val version: String?,
        val variant: String?,
        val hashAlgorithm: String?,
        val merkleRoot: String?,
        val timestamp: String?,
        val leafCount: Int?
    )

    fun setVerificationResult(verified: Boolean) {
        integrityVerified = verified
    }
}
```

Don't forget to register your Application class in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApplication"
    ... >
```

### Verifying Integrity at App Launch

Create an Activity that verifies integrity before allowing access:

```kotlin
package com.yourcompany.yourapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IntegrityCheckActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_integrity_check)
        
        verifyIntegrity()
    }

    private fun verifyIntegrity() {
        lifecycleScope.launch {
            val app = application as MyApplication
            val merkleRoot = app.getMerkleRoot()
            
            // Send to your verification server
            val result = withContext(Dispatchers.IO) {
                verifyWithServer(
                    merkleRoot = merkleRoot,
                    version = BuildConfig.VERSION_NAME,
                    variant = BuildConfig.BUILD_TYPE
                )
            }
            
            if (result.isVerified) {
                app.setVerificationResult(true)
                proceedToMainActivity()
            } else {
                showTamperingDetected(result.reason)
            }
        }
    }

    private suspend fun verifyWithServer(
        merkleRoot: String,
        version: String,
        variant: String
    ): VerificationResult {
        // Implement your server verification here
        // For demo, we'll simulate a successful verification
        delay(1000)
        return VerificationResult(true, null)
    }

    private fun proceedToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showTamperingDetected(reason: String?) {
        // Show error UI and block the app
        Log.e("Integrity", "Tampering detected: $reason")
    }

    data class VerificationResult(
        val isVerified: Boolean,
        val reason: String?
    )
}
```

---

## 🖥️ Server-Side Verification

### Expected Request Format

Your app should send a POST request to your verification server:

```json
{
  "merkle_root": "45e2f3b4eab4253af3de4887a435b71cb6694f9d0b07026e23c9fdfda50afaa1",
  "version": "1.0.0",
  "variant": "release",
  "package_name": "com.yourcompany.yourapp",
  "timestamp": 1708268452000
}
```

### Expected Response Format

```json
{
  "verified": true,
  "message": "Integrity verified successfully",
  "expected_root": "45e2f3b4eab4253af3de4887a435b71cb6694f9d0b07026e23c9fdfda50afaa1"
}
```

Or if verification fails:

```json
{
  "verified": false,
  "message": "Application has been tampered with! Merkle root mismatch.",
  "expected_root": "abc123..."
}
```

### 🖥️ Example AnchorPQ Server (Recommended)

The repository provides a complete, production-ready **AnchorPQ Verification Server** built with Quarkus. To run it:

```bash
# Start the server and PostgreSQL database using Docker Compose
docker-compose up --build

# Or run using Maven (requires PostgreSQL running on localhost:5432)
cd anchorpq-server
./mvnw compile quarkus:dev
```

See [Server Configuration](#-verification-server) below for more details.



---

## 📚 Complete Example App

### AnchorPQ Demo Application

A complete production-grade demo application is included in the `anchorpq-demo-app/` directory. This demo showcases the full integrity verification flow with real post-quantum cryptography.

**Features:**
- 🔐 Real ML-KEM (CRYSTALS-Kyber) key encapsulation
- 🔒 AES-256-GCM encryption of integrity payloads
- 🌲 Merkle root display and verification
- 📱 Material Design UI with status indicators
- 🧪 Unit and instrumentation tests

**Quick Start:**

```bash
# 1. Build and publish the plugin locally
./gradlew publishToMavenLocal

# 2. Start the verification server
docker-compose up --build

# 3. Build the demo app
cd anchorpq-demo-app
./gradlew assembleDebug

# 4. Install on emulator
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Demo App Architecture:**

```
anchorpq-demo-app/
├── app/
│   ├── src/main/java/com/anchorpq/demo/
│   │   ├── crypto/           # ML-KEM client implementation
│   │   │   ├── MLKemClient.kt
│   │   │   └── IntegrityEncryptionService.kt
│   │   ├── model/            # API data models
│   │   ├── network/          # Retrofit API client
│   │   └── ui/               # MainActivity & ViewModel
│   └── build.gradle.kts      # Plugin integration
├── build.gradle.kts
└── README.md                 # Detailed documentation
```

For complete documentation, see [anchorpq-demo-app/README.md](anchorpq-demo-app/README.md).

### Verification Flow Demo

When you click "Verify Integrity" in the demo app:

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Demo App      │────▶│  Fetch ML-KEM   │────▶│  Encapsulate    │
│  Click Verify   │     │  Public Key     │     │  Shared Secret  │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                                                        ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Display Result │◀────│  Server Returns │◀────│  Encrypt &      │
│  APPROVED/      │     │  APPROVED/      │     │  Send Payload   │
│  REJECTED       │     │  REJECTED       │     │                 │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Legacy Example (Reference Only)

A simpler example app structure is shown below for reference:

```
example-android-app/
├── app/
│   ├── build.gradle.kts          # App build config with plugin
│   ├── proguard-rules.pro        # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/
│       │   └── integrity.json    # Generated at build time
│       ├── java/com/example/secureapp/
│       │   ├── SecureApplication.kt      # Reads Merkle root
│       │   ├── IntegrityCheckActivity.kt # Verifies on startup
│       │   ├── IntegrityVerifier.kt      # Server communication
│       │   ├── MainActivity.kt           # Main app (after verification)
│       │   └── CompromisedActivity.kt    # Shown if tampered
│       └── res/
│           ├── layout/
│           │   ├── activity_integrity_check.xml
│           │   ├── activity_main.xml
│           │   └── activity_compromised.xml
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Project settings
├── gradle.properties             # Gradle properties
└── local.properties              # SDK location
```

### Building the Example App

```bash
# From the repository root
cd anchorpq-demo-app

# Build the debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```


---

## 📖 API Reference

### Gradle Tasks

| Task Name | Description |
|-----------|-------------|
| `generateMerkleRootDebug` | Compute Merkle root for debug variant |
| `generateMerkleRootRelease` | Compute Merkle root for release variant |
| `generateIntegrityMetadataDebug` | Generate integrity.json for debug |
| `generateIntegrityMetadataRelease` | Generate integrity.json for release |
| `copyIntegrityAssetsDebug` | Copy integrity.json to debug assets |
| `copyIntegrityAssetsRelease` | Copy integrity.json to release assets |

### Running Tasks Manually

```bash
# Generate Merkle root only
./gradlew :app:generateMerkleRootDebug

# Generate all integrity files
./gradlew :app:generateIntegrityMetadataDebug

# Full build (runs all tasks automatically)
./gradlew :app:assembleDebug
```

### MerkleTree Class (Java API)

```java
// Create a Merkle tree from leaf hashes
List<byte[]> leafHashes = new ArrayList<>();
leafHashes.add(HashUtils.hash(file1Content, "SHA-256"));
leafHashes.add(HashUtils.hash(file2Content, "SHA-256"));

MerkleTree tree = new MerkleTree(leafHashes, "SHA-256");

// Get the root hash
String rootHex = tree.getRootHex();
byte[] rootBytes = tree.getRoot();

// Get tree statistics
int height = tree.getHeight();
int leafCount = tree.getLeafCount();

// Generate proof for a leaf
MerkleTree.ProofNode[] proof = tree.generateProof(0);

// Verify a proof
boolean valid = tree.verifyProof(leafHash, proof);
```

### HashUtils Class (Java API)

```java
// Hash a byte array
byte[] hash = HashUtils.hash(data, "SHA-256");

// Hash concatenation of two hashes
byte[] combined = HashUtils.hashConcat(left, right, "SHA-256");

// Convert to hex string
String hex = HashUtils.toHex(hash);

// Check if algorithm is supported
boolean supported = HashUtils.isAlgorithmSupported("SHA-256");
```

---

## 🔒 Security Model

### What This Framework Protects Against

| Threat | Protection | Level |
|--------|------------|-------|
| APK Modification | Merkle root changes | ✅ Strong |
| Code Injection | Hash verification detects changes | ✅ Strong |
| Replay Attacks | Timestamp + nonce in requests | ✅ Strong |
| Network Eavesdropping | ML-KEM encryption (optional) | ✅ Strong |
| Quantum Attacks | Post-quantum cryptography | ✅ Future-proof |

### What This Framework Does NOT Protect Against

| Threat | Why | Mitigation |
|--------|-----|------------|
| Runtime Hooking | No runtime protection | Use obfuscation, native code |
| Root/Jailbreak | Can bypass verification | Use SafetyNet/Play Integrity |
| Memory Tampering | No memory protection | Use hardware security |
| Key Extraction | Keys in app memory | Use Android Keystore/HSM |
| Build Environment Compromise | Trust assumption | Secure CI/CD pipeline |

### Trust Assumptions

1. **Build Environment**: The machine running `./gradlew build` is not compromised
2. **Plugin Integrity**: The Anchor PQ plugin JAR is authentic
3. **Server Security**: Your verification server is properly secured
4. **Key Management**: ML-KEM keys are properly protected

---

## 🔧 Troubleshooting

### Common Issues

#### 1. "Plugin not found" Error

```
Plugin [id: 'io.github.namanoncode.anchorpq'] was not found
```

**Solution:** Ensure the plugin JAR is in the classpath:

```kotlin
buildscript {
    dependencies {
        classpath(files("path/to/AndroidPostQuantumIntegrityFramework-1.0.0.jar"))
    }
}
```

#### 2. "Classes directory does not exist" Error

```
Classes directory does not exist: .../intermediates/javac/debug/classes
```

**Solution:** The plugin runs before compilation. Ensure you're running a full build:

```bash
./gradlew clean assembleDebug
```

#### 3. Bouncy Castle Merge Conflicts

```
3 files found with path 'META-INF/versions/9/OSGI-INF/MANIFEST.MF'
```

**Solution:** Add packaging options to your `build.gradle.kts`:

```kotlin
android {
    packaging {
        resources {
            excludes += listOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/BCKEY.DSA",
                "META-INF/BCKEY.SF"
            )
        }
    }
}
```

#### 4. JDK Compatibility Issues

```
Error while executing process jlink with arguments...
```

**Solution:** Use a standard JDK 17 (e.g., Eclipse Temurin). GraalVM JDK may have jlink incompatibilities with Android SDK:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

#### 5. Gradle Version Compatibility

AGP 9.0+ requires Gradle 9.3.1+:

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.3.1-all.zip
```

### Debug Logging

Enable info logging to see plugin details:

```bash
./gradlew assembleDebug --info | grep -i "anchor\|merkle\|integrity"
```

---

## 🖥️ Verification Server

The AnchorPQ Verification Server is a production-ready Quarkus backend that validates application integrity at runtime.

### Quick Start with Docker

```bash
# Start server with PostgreSQL
docker-compose up -d

# Server available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui
```

### Server Features

| Feature | Description |
|---------|-------------|
| 🔐 **ML-KEM Key Exchange** | Post-quantum secure communication |
| 🌲 **Merkle Root Verification** | Server-anchored integrity validation |
| 🗄️ **PostgreSQL Storage** | Canonical integrity records database |
| 📊 **OpenAPI/Swagger** | Interactive API documentation |
| 🐳 **Docker Ready** | Production containerization |
| ⚡ **Rate Limiting** | Basic abuse protection |

### API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/public-key` | GET | Fetch ML-KEM public key |
| `/verify` | POST | Verify integrity (encrypted) |
| `/admin/records` | GET/POST | Manage canonical records |
| `/health` | GET | Health check |

### Server Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      Android Client                              │
│  1. Compute Merkle root from APK                                │
│  2. Fetch server's ML-KEM public key                            │
│  3. Encapsulate shared secret → derive AES key                  │
│  4. Encrypt integrity payload with AES-GCM                      │
│  5. POST /verify with encrypted request                         │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   AnchorPQ Server (Quarkus)                      │
│  1. ML-KEM decapsulate → recover shared secret                  │
│  2. Derive AES key using HKDF-SHA3-256                          │
│  3. Decrypt integrity payload                                    │
│  4. Compare Merkle root with canonical database record          │
│  5. Return: APPROVED | RESTRICTED | REJECTED                    │
└─────────────────────────────────────────────────────────────────┘
```

### Server Configuration

```properties
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=anchorpq
DB_USERNAME=anchorpq
DB_PASSWORD=your_secure_password

# ML-KEM (CRYSTALS-Kyber)
anchorpq.crypto.mlkem.parameter-set=ML-KEM-768
```

### CI/CD Integration

Register canonical Merkle roots after each build:

```bash
# In your CI pipeline after successful build
curl -X POST http://your-server:8080/admin/records \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "variant": "release",
    "merkleRoot": "'$(cat build/integrity/release/merkle-root.txt)'",
    "signerFingerprint": "your-signer-sha256"
  }'
```

📖 **Full Documentation**: See [anchorpq-server/README.md](SERVER-README.md) for complete server documentation.

---

## 📋 Requirements

| Requirement | Version |
|-------------|---------|
| Gradle | 9.3.1+ |
| Java | 17 |
| Android Gradle Plugin | 9.0+ |
| Android SDK | API 24+ (minSdk) |
| Kotlin | Built-in with AGP 9.0 |

### Server Requirements

| Requirement | Version |
|-------------|---------|
| Java | 17+ |
| Docker | 20.10+ (for containerized deployment) |
| PostgreSQL | 14+ |

### Dependencies

The plugin requires these dependencies on the classpath:

```kotlin
dependencies {
    classpath("org.bouncycastle:bcprov-jdk18on:1.83")
    classpath("org.bouncycastle:bcpkix-jdk18on:1.83")
    classpath("com.google.code.gson:gson:2.13.2")
}
```

---

## 🧪 Running Tests

```bash
# Run all plugin tests
./gradlew test

# Run specific test class
./gradlew test --tests "*MerkleTreeTest*"

# Run with verbose output
./gradlew test --info
```

### Test Coverage

| Test Class | Tests | Description |
|------------|-------|-------------|
| MerkleTreeTest | 16 | Merkle tree construction, proof generation/verification |
| HashUtilsTest | 8 | Hash algorithms, hex conversion |
| MLKemHelperTest | 6 | Post-quantum encryption |
| AnchorPQPluginTest | 4 | Plugin integration |

---

## 📄 License

```
Copyright 2026 Anchor PQ Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

## 🤝 Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/issues)
- **Discussions**: [GitHub Discussions](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/discussions)
- **Author**: [namanoncode](https://github.com/namanoncode)

---

## 🙏 Acknowledgments

- [Bouncy Castle](https://www.bouncycastle.org/) - Post-quantum cryptography implementation
- [NIST PQC](https://csrc.nist.gov/projects/post-quantum-cryptography) - ML-KEM standardization
- Android Open Source Project

---

**Made with ❤️ for secure Android development**

