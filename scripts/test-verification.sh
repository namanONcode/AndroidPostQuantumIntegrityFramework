#!/bin/bash

# Test script to verify the AnchorPQ server is working correctly
# This simulates what the Android app does

set -e

SERVER_URL="${SERVER_URL:-http://localhost:8080}"
MERKLE_ROOT="67157e27b5c17eea5558075ecc20f37830e6776730cf41855378cfdfd57cf8d4"

echo "=================================="
echo "AnchorPQ Server Verification Test"
echo "=================================="
echo "Server URL: $SERVER_URL"
echo "Merkle Root: $MERKLE_ROOT"
echo ""

# Step 1: Test health endpoint
echo "Step 1: Testing health endpoint..."
HEALTH_RESPONSE=$(curl -s "$SERVER_URL/health")
echo "Health: $HEALTH_RESPONSE"
echo ""

# Step 2: Fetch public key
echo "Step 2: Fetching public key..."
PUBLIC_KEY_RESPONSE=$(curl -s "$SERVER_URL/public-key")
echo "Public Key Response (first 200 chars):"
echo "$PUBLIC_KEY_RESPONSE" | head -c 200
echo "..."
echo ""

# Extract key ID and algorithm
KEY_ID=$(echo "$PUBLIC_KEY_RESPONSE" | grep -o '"keyId":"[^"]*"' | cut -d'"' -f4)
ALGORITHM=$(echo "$PUBLIC_KEY_RESPONSE" | grep -o '"algorithm":"[^"]*"' | cut -d'"' -f4)
PARAM_SET=$(echo "$PUBLIC_KEY_RESPONSE" | grep -o '"parameterSet":"[^"]*"' | cut -d'"' -f4)

echo "Key ID: $KEY_ID"
echo "Algorithm: $ALGORITHM"
echo "Parameter Set: $PARAM_SET"
echo ""

# Step 3: Test verify endpoint with invalid data (expect rejection)
echo "Step 3: Testing verify endpoint with invalid data..."
VERIFY_RESPONSE=$(curl -s -X POST "$SERVER_URL/verify" \
  -H "Content-Type: application/json" \
  -d '{
    "encapsulatedKey": "dGVzdA==",
    "encryptedPayload": "dGVzdA==",
    "timestamp": '$(($(date +%s) * 1000))'
  }')
echo "Verify Response: $VERIFY_RESPONSE"
echo ""

# Step 4: Check database for the Merkle root
echo "Step 4: Checking database for Merkle root..."
docker exec anchorpq-db psql -U anchorpq -d anchorpq -t -c "SELECT merkle_root FROM integrity_records WHERE version='1.0.0' AND variant='debug';"
echo ""

echo "=================================="
echo "Test Complete!"
echo "=================================="
echo ""
echo "The server is running and responding to requests."
echo "To test with the Android app:"
echo "1. Install the APK on an emulator or device"
echo "2. The emulator should use server URL: http://10.0.2.2:8080"
echo "3. Tap 'Verify Integrity' button"
echo ""

