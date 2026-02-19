#!/bin/bash
# =============================================================================
# AnchorPQ Database Seed Script
# Seeds the server database with canonical Merkle roots for demo app
# =============================================================================

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

SERVER_URL="${SERVER_URL:-http://localhost:8080}"
DEMO_APP_DIR="$(dirname "$0")/../anchorpq-demo-app"

echo -e "${BLUE}AnchorPQ Database Seed Script${NC}"
echo "================================"

# Function to seed a record
seed_record() {
    local version="$1"
    local variant="$2"
    local merkle_root="$3"
    local description="$4"

    echo -e "${YELLOW}Seeding: version=$version, variant=$variant${NC}"

    RESPONSE=$(curl -s -X POST "$SERVER_URL/admin/records" \
        -H "Content-Type: application/json" \
        -d "{
            \"version\": \"$version\",
            \"variant\": \"$variant\",
            \"merkleRoot\": \"$merkle_root\",
            \"description\": \"$description\"
        }")

    echo "Response: $RESPONSE"
}

# Check if server is running
echo "Checking server at $SERVER_URL..."
if ! curl -s "$SERVER_URL/health/ready" > /dev/null 2>&1; then
    echo -e "${YELLOW}Warning: Server may not be running${NC}"
fi

# Try to get Merkle root from demo app build
DEBUG_ROOT_FILE="$DEMO_APP_DIR/app/build/anchorpq/debug/merkle-root.txt"
RELEASE_ROOT_FILE="$DEMO_APP_DIR/app/build/anchorpq/release/merkle-root.txt"

if [ -f "$DEBUG_ROOT_FILE" ]; then
    DEBUG_ROOT=$(cat "$DEBUG_ROOT_FILE")
    echo -e "${GREEN}Found debug Merkle root: $DEBUG_ROOT${NC}"
    seed_record "1.0.0" "debug" "$DEBUG_ROOT" "Demo app debug build"
else
    echo "Debug Merkle root not found, using placeholder"
    seed_record "1.0.0" "debug" "DEMO_DEBUG_PLACEHOLDER" "Demo app debug build (placeholder)"
fi

if [ -f "$RELEASE_ROOT_FILE" ]; then
    RELEASE_ROOT=$(cat "$RELEASE_ROOT_FILE")
    echo -e "${GREEN}Found release Merkle root: $RELEASE_ROOT${NC}"
    seed_record "1.0.0" "release" "$RELEASE_ROOT" "Demo app release build"
else
    echo "Release Merkle root not found, using placeholder"
    seed_record "1.0.0" "release" "DEMO_RELEASE_PLACEHOLDER" "Demo app release build (placeholder)"
fi

echo ""
echo -e "${GREEN}Seeding complete!${NC}"
echo ""
echo "To verify, check the records:"
echo "  curl $SERVER_URL/admin/records"

