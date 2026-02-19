# AnchorPQ Demo Application

**Post-Quantum Secure Android Integrity Verification Demo**

[![Android](https://img.shields.io/badge/Android-24%2B-green.svg)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Overview

This Android application demonstrates the complete AnchorPQ integrity verification flow:

1. **Merkle Root Generation**: The AnchorPQ Gradle plugin computes a deterministic Merkle root of the application's compiled bytecode
2. **ML-KEM Key Exchange**: Post-quantum secure key encapsulation using CRYSTALS-Kyber
3. **AES-GCM Encryption**: Symmetric encryption of the integrity payload
4. **Server Verification**: Backend validates the integrity against canonical records

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Demo Android App                                │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐  │
│  │   MainActivity  │───▶│  MainViewModel  │───▶│   Repository    │  │
│  │     (UI)        │    │    (State)      │    │   (Network)     │  │
│  └─────────────────┘    └─────────────────┘    └────────┬────────┘  │
│                                                          │          │
│                         ┌────────────────────────────────┘          │
│                         ▼                                           │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │                     Crypto Layer                             │   │
│  │  ┌───────────────┐         ┌───────────────────────────┐    │   │
│  │  │  MLKemClient  │────────▶│  IntegrityEncryptionService │   │   │
│  │  │  (Kyber-768)  │         │  (AES-256-GCM)            │    │   │
│  │  └───────────────┘         └───────────────────────────┘    │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└──────────────────────────────────┬──────────────────────────────────┘
                                   │ HTTPS
                                   ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     AnchorPQ Server                                  │
│  ┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐  │
│  │  /public-key    │    │    /verify      │    │  PostgreSQL     │  │
│  │  (ML-KEM PK)    │    │  (Validation)   │    │  (Canonical DB) │  │
│  └─────────────────┘    └─────────────────┘    └─────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## Verification Flow

```
┌─────────────┐                               ┌─────────────────┐
│  Demo App   │                               │  AnchorPQ Server│
└──────┬──────┘                               └────────┬────────┘
       │                                               │
       │  1. GET /public-key                          │
       │──────────────────────────────────────────────▶│
       │                                               │
       │  2. ML-KEM Public Key                        │
       │◀──────────────────────────────────────────────│
       │                                               │
       │  3. Encapsulate shared secret                │
       │     K, ct = ML-KEM.Encaps(pk)                │
       │                                               │
       │  4. Derive AES key via HKDF                  │
       │     aes_key = HKDF(K)                        │
       │                                               │
       │  5. Encrypt payload                          │
       │     payload = {merkleRoot, version, ...}     │
       │     enc = AES-GCM(aes_key, payload)         │
       │                                               │
       │  6. POST /verify                             │
       │     {encapsulatedKey: ct, encryptedPayload: enc}
       │──────────────────────────────────────────────▶│
       │                                               │
       │                                    7. Decaps K'│
       │                                    8. Decrypt  │
       │                                    9. Verify   │
       │                                               │
       │  10. Verification Response                   │
       │      {status: APPROVED/RESTRICTED/REJECTED}  │
       │◀──────────────────────────────────────────────│
       │                                               │
```

## Quick Start

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Docker and Docker Compose (for server)
- AnchorPQ Gradle Plugin (published to Maven Local)

### 1. Build the Plugin

```bash
# From the root repository
cd /path/to/AndroidPostQuantumIntegrityFramework
./gradlew publishToMavenLocal
```

### 2. Start the Server

```bash
# From the root repository
docker-compose up --build
```

### 3. Build the Demo App

```bash
cd anchorpq-demo-app
./gradlew assembleDebug
```

### 4. Install and Run

```bash
# Install on emulator (server accessible at 10.0.2.2:8080)
adb install app/build/outputs/apk/debug/app-debug.apk

# Or use Android Studio to run
```

### 5. Seed the Database (Optional)

If you want to test APPROVED responses, seed the server with the demo app's Merkle root:

```bash
# Get the Merkle root
MERKLE_ROOT=$(cat app/build/anchorpq/debug/merkle-root.txt)

# Seed the server
curl -X POST http://localhost:8080/admin/records \
  -H "Content-Type: application/json" \
  -d '{
    "version": "1.0.0",
    "variant": "debug",
    "merkleRoot": "'$MERKLE_ROOT'",
    "description": "Demo app canonical root"
  }'
```

## Project Structure

```
anchorpq-demo-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/anchorpq/demo/
│   │   │   │   ├── crypto/           # ML-KEM and encryption
│   │   │   │   │   ├── MLKemClient.kt
│   │   │   │   │   └── IntegrityEncryptionService.kt
│   │   │   │   ├── model/            # Data models
│   │   │   │   │   ├── PublicKeyResponse.kt
│   │   │   │   │   ├── VerificationRequest.kt
│   │   │   │   │   └── VerificationResponse.kt
│   │   │   │   ├── network/          # API client
│   │   │   │   │   ├── AnchorPQApi.kt
│   │   │   │   │   ├── ApiClient.kt
│   │   │   │   │   └── IntegrityRepository.kt
│   │   │   │   ├── ui/               # UI components
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   └── MainViewModel.kt
│   │   │   │   └── util/             # Utilities
│   │   │   │       └── IntegrityConfig.kt
│   │   │   └── res/                  # Resources
│   │   ├── androidTest/              # Instrumentation tests
│   │   └── test/                     # Unit tests
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Plugin Configuration

The demo app uses the AnchorPQ Gradle plugin:

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.github.namanoncode.anchorpq")  // AnchorPQ plugin
}

anchorpq {
    enabled.set(true)
    algorithm.set("SHA3-256")
    injectBuildConfig.set(true)  // Injects MERKLE_ROOT into BuildConfig
    version.set("1.0.0")
}
```

## Cryptographic Details

### ML-KEM (CRYSTALS-Kyber)

- **Parameter Set**: Kyber-768 (NIST Security Level 3)
- **Public Key Size**: 1,184 bytes
- **Ciphertext Size**: 1,088 bytes
- **Shared Secret**: 32 bytes

### AES-GCM

- **Key Size**: 256 bits (derived from ML-KEM shared secret)
- **Nonce Size**: 12 bytes (96 bits)
- **Tag Size**: 16 bytes (128 bits)
- **Format**: `IV || Ciphertext || AuthTag`

### Hash Algorithm

- **Merkle Tree**: SHA3-256
- **Key Derivation**: HKDF-SHA3-256

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Bouncy Castle | 1.78.1 | ML-KEM cryptography |
| Retrofit | 2.9.0 | HTTP client |
| OkHttp | 4.12.0 | Network layer |
| Gson | 2.10.1 | JSON serialization |
| Kotlin Coroutines | 1.7.3 | Async operations |

## Testing

### Unit Tests

```bash
./gradlew testDebugUnitTest
```

Tests include:
- ML-KEM encapsulation correctness
- Encryption/decryption validity
- ViewModel state transitions

### Instrumentation Tests

```bash
./gradlew connectedDebugAndroidTest
```

Tests include:
- UI element visibility
- Button interactions
- Integration with server (if running)

## Security Considerations

### Threat Model

**Protected Against:**
- Quantum computer attacks on key exchange
- Man-in-the-middle attacks (with TLS)
- Replay attacks (via nonces and timestamps)
- Application tampering (Merkle root mismatch)

**Not Protected Against:**
- Rooted/jailbroken device attacks
- Runtime hooking (Frida, Xposed)
- Debugging/reverse engineering
- Physical device compromise

### Assumptions

1. **Network**: TLS is used in production
2. **Server**: Server is authoritative and trusted
3. **Build**: Build environment is secure
4. **Crypto**: Bouncy Castle implementation is correct

## Troubleshooting

### Common Issues

**"Server unreachable" error:**
- Ensure Docker containers are running: `docker-compose ps`
- For emulator: Server URL should be `http://10.0.2.2:8080`
- For physical device: Use actual server IP

**"Merkle root not found":**
- Ensure plugin is applied and `injectBuildConfig` is true
- Check `app/build/anchorpq/debug/merkle-root.txt` exists
- Rebuild: `./gradlew clean assembleDebug`

**"REJECTED" status:**
- The server doesn't have a matching canonical root
- Seed the database with the correct Merkle root

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](../LICENSE) file for details.

## Contributing

Please read [CONTRIBUTING.md](../CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

