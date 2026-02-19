#!/bin/bash
# =============================================================================
# AnchorPQ Database Seed Script
# =============================================================================
# This script seeds the database with example integrity records for testing.
# Usage: ./seed-data.sh [SERVER_URL]

SERVER_URL="${1:-http://localhost:8080}"

echo "Seeding AnchorPQ database at $SERVER_URL..."

# Wait for server to be ready
echo "Waiting for server to be ready..."
for i in {1..30}; do
    if curl -s "$SERVER_URL/health/ready" > /dev/null 2>&1; then
        echo "Server is ready!"
        break
    fi
    echo "Waiting... ($i/30)"
    sleep 2
done

# Seed example integrity records
echo ""
echo "Creating example integrity records..."

# Record 1: v1.0.0 release
curl -s -X POST "$SERVER_URL/admin/records" \
    -H "Content-Type: application/json" \
    -d '{
        "version": "1.0.0",
        "variant": "release",
        "merkleRoot": "a1b2c3d4e5f6789012345678901234567890123456789012345678901234abcd",
        "signerFingerprint": "fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe",
        "description": "Initial release version"
    }' | jq .

echo ""

# Record 2: v1.0.0 debug
curl -s -X POST "$SERVER_URL/admin/records" \
    -H "Content-Type: application/json" \
    -d '{
        "version": "1.0.0",
        "variant": "debug",
        "merkleRoot": "b2c3d4e5f67890123456789012345678901234567890123456789012345bcdee",
        "signerFingerprint": "fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe",
        "description": "Initial debug build"
    }' | jq .

echo ""

# Record 3: v1.0.1 release
curl -s -X POST "$SERVER_URL/admin/records" \
    -H "Content-Type: application/json" \
    -d '{
        "version": "1.0.1",
        "variant": "release",
        "merkleRoot": "c3d4e5f678901234567890123456789012345678901234567890123456cdefab",
        "signerFingerprint": "fedcba0987654321fedcba0987654321fedcba0987654321fedcba09876543fe",
        "description": "Bug fix release"
    }' | jq .

echo ""
echo "Seeding complete!"

# List all records
echo ""
echo "Current integrity records:"
curl -s "$SERVER_URL/admin/records" | jq .

