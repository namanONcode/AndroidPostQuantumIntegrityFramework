# AnchorPQ Verification Server

**Post-Quantum Secure Mobile Application Integrity Verification Server**

[![Java 17](https://img.shields.io/badge/Java-17-blue.svg)](https://openjdk.java.net/projects/jdk/17/)
[![Quarkus](https://img.shields.io/badge/Quarkus-3.x-red.svg)](https://quarkus.io/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Overview

AnchorPQ Server is a production-ready backend service for verifying Android application integrity using post-quantum cryptography. It establishes a secure communication channel using **ML-KEM (CRYSTALS-Kyber)** and verifies application integrity by comparing Merkle root hashes against canonical server-stored values.

### Key Features

- 🔐 **Post-Quantum Security**: Uses ML-KEM (CRYSTALS-Kyber) for key encapsulation
- 🌲 **Merkle Tree Verification**: Server-anchored integrity validation
- ⚡ **High Performance**: Built on Quarkus reactive stack
- 🐳 **Container Ready**: Full Docker and Docker Compose support
- 📊 **Production Ready**: Health checks, OpenAPI docs, rate limiting
- 🔒 **Secure by Design**: Constant-time comparisons, secure key handling

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Android Client                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  1. Fetch server's ML-KEM public key                        │   │
│  │  2. Compute integrity Merkle root                           │   │
│  │  3. Encapsulate shared secret with server's public key      │   │
│  │  4. Derive AES-256 key using HKDF-SHA3                      │   │
│  │  5. Encrypt integrity payload with AES-GCM                  │   │
│  │  6. Send encapsulated key + encrypted payload to server     │   │
│  └─────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                      AnchorPQ Server                                 │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  1. Decapsulate shared secret using ML-KEM private key      │   │
│  │  2. Derive AES-256 key using HKDF-SHA3                      │   │
│  │  3. Decrypt integrity payload with AES-GCM                  │   │
│  │  4. Query canonical Merkle root from database               │   │
│  │  5. Compare roots (constant-time)                           │   │
│  │  6. Return verification decision                            │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐          │
│  │   ML-KEM     │    │   AES-GCM    │    │  PostgreSQL  │          │
│  │  Kyber-768   │    │   256-bit    │    │   Database   │          │
│  └──────────────┘    └──────────────┘    └──────────────┘          │
└─────────────────────────────────────────────────────────────────────┘
```

## Cryptographic Flow

### Key Encapsulation (ML-KEM)

```
Client                                    Server
   │                                         │
   │──── GET /public-key ───────────────────>│
   │<─── ML-KEM Public Key ──────────────────│
   │                                         │
   │  [Encapsulate shared secret]            │
   │  K, ct = ML-KEM.Encaps(pk)              │
   │                                         │
   │──── POST /verify ──────────────────────>│
   │     { encapsulatedKey: ct,              │
   │       encryptedPayload: ... }           │
   │                                         │
   │                    [Decapsulate secret] │
   │                    K' = ML-KEM.Decaps(sk, ct)
   │                                         │
   │<─── Verification Response ──────────────│
   │                                         │
```

### Symmetric Encryption (AES-256-GCM)

1. **Key Derivation**: HKDF-SHA3-256 with context info
2. **Encryption**: AES-256-GCM with random 12-byte IV
3. **Format**: `IV (12 bytes) || Ciphertext || Auth Tag (16 bytes)`

## API Reference

### `GET /public-key`

Returns the server's ML-KEM public key for client key encapsulation.

**Response:**
```json
{
  "publicKey": "base64-encoded-public-key",
  "parameterSet": "ML-KEM-768",
  "algorithm": "ML-KEM",
  "generatedAt": 1708300000000,
  "keyId": "uuid-string"
}
```

### `POST /verify`

Verifies application integrity from an encrypted request.

**Request:**
```json
{
  "encapsulatedKey": "base64-encoded-kem-ciphertext",
  "encryptedPayload": "base64-encoded-aes-gcm-ciphertext",
  "timestamp": 1708300000000
}
```

**Decrypted Payload Format:**
```json
{
  "merkleRoot": "64-char-hex-string",
  "version": "1.0.0",
  "variant": "release",
  "signerFingerprint": "64-char-hex-string"
}
```

**Response:**
```json
{
  "status": "APPROVED",
  "message": "Integrity verified successfully",
  "timestamp": 1708300000000
}
```

**Status Values:**
- `APPROVED` - Integrity verified, application is authentic
- `RESTRICTED` - Partial verification, some discrepancies (e.g., unknown signer)
- `REJECTED` - Verification failed, application may be tampered

### `POST /admin/records`

Creates or updates a canonical integrity record.

**Request:**
```json
{
  "version": "1.0.0",
  "variant": "release",
  "merkleRoot": "64-char-hex-string",
  "signerFingerprint": "64-char-hex-string",
  "description": "Optional description"
}
```

### `GET /admin/records`

Lists all integrity records with optional filtering.

**Query Parameters:**
- `version` - Filter by version
- `variant` - Filter by variant

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for development)
- Maven 3.8+ (for development)

### Running with Docker Compose

```bash
# Clone the repository
git clone https://github.com/namanoncode/AndroidPostQuantumIntegrityFramework.git
cd AndroidPostQuantumIntegrityFramework

# Start the services
docker-compose up --build

# The server will be available at http://localhost:8080
```

### Running for Development

```bash
cd anchorpq-server

# Start in dev mode with live reload
./mvnw quarkus:dev

# The server will be available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui
```

### Building for Production

```bash
cd anchorpq-server

# Build the application
./mvnw package -DskipTests

# Build Docker image
docker build -t anchorpq/anchorpq-server:latest .

# Run the container
docker run -p 8080:8080 \
  -e DB_HOST=your-postgres-host \
  -e DB_USERNAME=anchorpq \
  -e DB_PASSWORD=your-secret \
  anchorpq/anchorpq-server:latest
```

## Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `anchorpq` |
| `DB_USERNAME` | Database username | `anchorpq` |
| `DB_PASSWORD` | Database password | `anchorpq_secret` |
| `MLKEM_KEY_PATH` | Path to persist ML-KEM keys | (in-memory) |
| `JAVA_OPTS` | JVM options | `-Xms256m -Xmx512m` |

### application.properties

Key configuration options:

```properties
# ML-KEM parameter set: ML-KEM-512, ML-KEM-768, ML-KEM-1024
anchorpq.crypto.mlkem.parameter-set=ML-KEM-768

# Rate limiting
anchorpq.ratelimit.enabled=true
anchorpq.ratelimit.requests-per-minute=60

# HKDF key derivation
anchorpq.crypto.hkdf.algorithm=SHA3-256
anchorpq.crypto.hkdf.info=AnchorPQ-v1-IntegrityVerification
```

## Testing

```bash
cd anchorpq-server

# Run all tests
./mvnw test

# Run with coverage
./mvnw test jacoco:report
```

## API Documentation

When running, OpenAPI documentation is available at:
- **Swagger UI**: http://localhost:8080/swagger-ui
- **OpenAPI JSON**: http://localhost:8080/openapi

## Health Endpoints

- **Liveness**: http://localhost:8080/health/live
- **Readiness**: http://localhost:8080/health/ready
- **Full Health**: http://localhost:8080/health

## Security Considerations

### Threat Model

**In Scope:**
- Post-quantum secure key exchange (ML-KEM)
- Integrity verification via Merkle root comparison
- Server-anchored trust model
- Replay protection via timestamps
- Rate limiting to prevent abuse

**Out of Scope:**
- Hardware-backed attestation (Play Integrity API)
- Runtime anti-hooking/anti-debugging
- Root/jailbreak detection
- Network-level attacks (assumed TLS)
- Physical device security

### Security Assumptions

1. **TLS Required**: All communication should use HTTPS in production
2. **Database Security**: PostgreSQL should be properly secured and access-controlled
3. **Key Management**: ML-KEM keys are generated at startup; consider HSM for production
4. **Admin Endpoints**: `/admin/*` endpoints should be protected with authentication

### Best Practices

1. Always use TLS in production
2. Protect admin endpoints with authentication
3. Regularly rotate ML-KEM keys
4. Monitor verification failures for anomalies
5. Keep Bouncy Castle updated for security patches

## Project Structure

```
anchorpq-server/
├── src/main/java/com/anchorpq/server/
│   ├── config/          # Configuration classes
│   ├── crypto/          # Cryptographic services (ML-KEM, AES-GCM)
│   ├── model/           # Data models and entities
│   ├── repository/      # Database repositories
│   ├── resource/        # REST endpoints
│   └── service/         # Business logic services
├── src/main/resources/
│   └── application.properties
├── src/test/
│   ├── java/            # Test classes
│   └── resources/       # Test configuration
├── Dockerfile
└── pom.xml
```

## Dependencies

- **Quarkus 3.x** - Supersonic Subatomic Java Framework
- **Bouncy Castle 1.77** - Post-quantum cryptography (ML-KEM)
- **PostgreSQL** - Primary database
- **H2** - In-memory database for testing

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

## Contributing

Please read [CONTRIBUTING.md](../CONTRIBUTING.md) for details on our code of conduct and the process for submitting pull requests.

## Acknowledgments

- [Bouncy Castle](https://www.bouncycastle.org/) for post-quantum cryptography implementation
- [NIST](https://www.nist.gov/) for ML-KEM (CRYSTALS-Kyber) standardization
- [Quarkus](https://quarkus.io/) for the excellent Java framework

