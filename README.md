# 🛡️ AnchorPQ - Android Post-Quantum Integrity Framework

[![CI Build](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/ci.yml/badge.svg)](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/ci.yml)
[![CodeQL](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/codeql.yml/badge.svg)](https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Gradle Plugin](https://img.shields.io/badge/Gradle-8.0%2B-green.svg)](https://gradle.org/)
[![Android](https://img.shields.io/badge/Android-API%2026%2B-brightgreen.svg)](https://developer.android.com/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.8%2B-red.svg)](https://quarkus.io/)

A comprehensive framework for **build-time integrity verification** and **runtime server-anchored validation** of Android applications using **Merkle trees** and **post-quantum cryptography (ML-KEM/CRYSTALS-Kyber)**.

## 🏗️ Project Structure

This monorepo contains two main components:

| Component | Description | Documentation |
|-----------|-------------|---------------|
| **[Gradle Plugin](/)** | Build-time Merkle tree computation for Android apps | This README |
| **[Verification Server](anchorpq-server/)** | Quarkus backend for runtime integrity verification | [Server README](SERVER-README.md) |

```
AndroidPostQuantumIntegrityFramework/
├── src/                    # Gradle Plugin source code
├── anchorpq-server/        # Quarkus Verification Server
├── docker-compose.yml      # Docker setup for server + PostgreSQL
└── scripts/                # Helper scripts
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
| 🤖 **AGP 8+ Compatible** | Works with Android Gradle Plugin 8.x |
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
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        
        // Include the local integrity plugin JAR
        classpath(files("path/to/AndroidPostQuantumIntegrityFramework/build/libs/AndroidPostQuantumIntegrityFramework-1.0.0.jar"))
        
        // Plugin dependencies (required)
        classpath("org.bouncycastle:bcprov-jdk18on:1.78.1")
        classpath("org.bouncycastle:bcpkix-jdk18on:1.78.1")
        classpath("com.google.code.gson:gson:2.10.1")
    }
}

plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
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
    id("org.jetbrains.kotlin.android")
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
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
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

### Sample Node.js Server

```javascript
const express = require('express');
const app = express();
app.use(express.json());

// Store expected Merkle roots per version
const expectedRoots = {
  '1.0.0': {
    'debug': '45e2f3b4eab4253af3de4887a435b71cb6694f9d0b07026e23c9fdfda50afaa1',
    'release': 'abc123def456...'
  }
};

app.post('/api/verify', (req, res) => {
  const { merkle_root, version, variant, package_name } = req.body;
  
  const expected = expectedRoots[version]?.[variant];
  
  if (!expected) {
    return res.json({
      verified: false,
      message: `Unknown version/variant: ${version}/${variant}`
    });
  }
  
  const verified = merkle_root === expected;
  
  res.json({
    verified,
    message: verified 
      ? 'Integrity verified successfully'
      : 'Application has been tampered with!',
    expected_root: expected
  });
});

app.listen(8080, () => {
  console.log('Integrity verification server running on port 8080');
});
```

---

## 📚 Complete Example App

A complete example app is included in the `example-android-app/` directory. Here's its structure:

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
cd example-android-app

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

**Solution:** Use Java 8 or 11 compatibility:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}
```

#### 5. Gradle Version Compatibility

If you encounter configuration mutation errors, ensure you're using Gradle 8.x with AGP 8.x:

```properties
# gradle/wrapper/gradle-wrapper.properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.5-bin.zip
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
| Gradle | 8.0+ (8.5 recommended) |
| Java | 8, 11, or 17 |
| Android Gradle Plugin | 8.0+ |
| Android SDK | API 26+ (minSdk) |
| Kotlin | 1.9+ (optional) |

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
    classpath("org.bouncycastle:bcprov-jdk18on:1.78.1")
    classpath("org.bouncycastle:bcpkix-jdk18on:1.78.1")
    classpath("com.google.code.gson:gson:2.10.1")
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

