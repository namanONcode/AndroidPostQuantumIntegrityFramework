#!/usr/bin/env python3
"""
AnchorPQ Example Client

This script demonstrates how a client would interact with the AnchorPQ server
to perform post-quantum secure integrity verification.

Note: This is a conceptual example. The actual ML-KEM operations would require
a proper PQC library like liboqs-python or pqcrypto.

Usage:
    python3 example-client.py [server_url]

Requirements:
    pip install requests cryptography
"""

import base64
import json
import requests
import secrets
import sys
from typing import Tuple

# Server URL
SERVER_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"


def get_server_public_key() -> dict:
    """Fetch the server's ML-KEM public key."""
    response = requests.get(f"{SERVER_URL}/public-key")
    response.raise_for_status()
    return response.json()


def simulate_mlkem_encapsulation(public_key_b64: str) -> Tuple[bytes, bytes]:
    """
    Simulate ML-KEM encapsulation.

    In a real implementation, this would use a proper ML-KEM library like:
    - liboqs-python
    - pqcrypto
    - Bouncy Castle (Java)

    Returns:
        Tuple of (encapsulated_key, shared_secret)
    """
    # This is a SIMULATION - do not use in production!
    # Real ML-KEM would parse the public key and perform encapsulation
    print("⚠️  WARNING: This is a simulated ML-KEM encapsulation for demonstration only!")

    # Simulated values (would be actual ML-KEM output in production)
    encapsulated_key = secrets.token_bytes(1088)  # ML-KEM-768 ciphertext size
    shared_secret = secrets.token_bytes(32)       # Shared secret is 32 bytes

    return encapsulated_key, shared_secret


def derive_aes_key(shared_secret: bytes, info: bytes = b"AnchorPQ-v1-IntegrityVerification") -> bytes:
    """
    Derive AES-256 key from shared secret using HKDF-SHA3-256.
    """
    from cryptography.hazmat.primitives import hashes
    from cryptography.hazmat.primitives.kdf.hkdf import HKDF

    # Note: cryptography library uses SHA-256 here; server uses SHA3-256
    # In production, ensure both sides use the same hash
    hkdf = HKDF(
        algorithm=hashes.SHA256(),  # Use SHA3-256 in production
        length=32,
        salt=None,
        info=info
    )
    return hkdf.derive(shared_secret)


def encrypt_payload(key: bytes, payload: dict) -> bytes:
    """
    Encrypt payload using AES-256-GCM.

    Returns: IV || Ciphertext || Auth Tag
    """
    from cryptography.hazmat.primitives.ciphers.aead import AESGCM

    plaintext = json.dumps(payload).encode('utf-8')
    iv = secrets.token_bytes(12)  # 12-byte IV for GCM

    aesgcm = AESGCM(key)
    ciphertext = aesgcm.encrypt(iv, plaintext, None)

    # Format: IV || Ciphertext (includes auth tag)
    return iv + ciphertext


def create_verification_request(
    merkle_root: str,
    version: str,
    variant: str,
    signer_fingerprint: str
) -> dict:
    """
    Create a complete verification request.
    """
    # Step 1: Get server's public key
    print(f"📡 Fetching public key from {SERVER_URL}...")
    key_info = get_server_public_key()
    print(f"   Key ID: {key_info['keyId']}")
    print(f"   Parameter Set: {key_info['parameterSet']}")

    # Step 2: Encapsulate shared secret (SIMULATION)
    print("\n🔐 Encapsulating shared secret...")
    encapsulated_key, shared_secret = simulate_mlkem_encapsulation(key_info['publicKey'])
    print(f"   Encapsulated key size: {len(encapsulated_key)} bytes")

    # Step 3: Derive AES key
    print("\n🔑 Deriving AES-256 key using HKDF...")
    aes_key = derive_aes_key(shared_secret)

    # Step 4: Create integrity payload
    payload = {
        "merkleRoot": merkle_root,
        "version": version,
        "variant": variant,
        "signerFingerprint": signer_fingerprint
    }
    print(f"\n📦 Integrity Payload:")
    print(f"   Version: {version}")
    print(f"   Variant: {variant}")
    print(f"   Merkle Root: {merkle_root[:16]}...")

    # Step 5: Encrypt payload
    print("\n🔒 Encrypting payload with AES-256-GCM...")
    encrypted_payload = encrypt_payload(aes_key, payload)

    # Step 6: Create request
    request = {
        "encapsulatedKey": base64.b64encode(encapsulated_key).decode('utf-8'),
        "encryptedPayload": base64.b64encode(encrypted_payload).decode('utf-8'),
        "timestamp": int(__import__('time').time() * 1000)
    }

    return request


def verify_integrity(request: dict) -> dict:
    """
    Send verification request to server.
    """
    print("\n📤 Sending verification request...")
    response = requests.post(
        f"{SERVER_URL}/verify",
        json=request,
        headers={"Content-Type": "application/json"}
    )
    return response.json(), response.status_code


def main():
    print("=" * 60)
    print("AnchorPQ Example Client")
    print("Post-Quantum Secure Integrity Verification")
    print("=" * 60)
    print(f"\nServer: {SERVER_URL}")

    # Example integrity values (these would come from the actual app)
    merkle_root = "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd"
    version = "1.0.0"
    variant = "release"
    signer_fingerprint = "fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe"

    try:
        # Create verification request
        request = create_verification_request(
            merkle_root=merkle_root,
            version=version,
            variant=variant,
            signer_fingerprint=signer_fingerprint
        )

        print("\n" + "=" * 60)
        print("⚠️  NOTE: The request will fail because this uses simulated")
        print("    ML-KEM values. In production, use proper ML-KEM library.")
        print("=" * 60)

        # Send request (will fail with simulated values)
        response, status_code = verify_integrity(request)

        print(f"\n📥 Response (HTTP {status_code}):")
        print(json.dumps(response, indent=2))

    except requests.exceptions.ConnectionError:
        print(f"\n❌ Error: Could not connect to server at {SERVER_URL}")
        print("   Make sure the server is running: docker-compose up")
    except Exception as e:
        print(f"\n❌ Error: {e}")


if __name__ == "__main__":
    main()

