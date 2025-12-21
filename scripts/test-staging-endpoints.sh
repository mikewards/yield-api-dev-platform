#!/bin/bash

# Test GET endpoints with staging credentials
# Usage: STAGING_TOKEN='your-token' ./test-staging-endpoints.sh

STAGING_URL="https://flow-platform-staging.up.railway.app"
TOKEN="${STAGING_TOKEN:-}"

if [ -z "$TOKEN" ]; then
    echo "❌ Error: STAGING_TOKEN not set"
    echo ""
    echo "Usage:"
    echo "  STAGING_TOKEN='your-sandbox-token' ./test-staging-endpoints.sh"
    echo ""
    exit 1
fi

echo "🧪 Testing TBD API Endpoints (Staging)"
echo "======================================"
echo "URL: $STAGING_URL"
echo "Token: ${TOKEN:0:20}..."
echo ""
echo "======================================"
echo ""

# Test function
test_endpoint() {
    local name="$1"
    local path="$2"
    local query="$3"
    
    full_path="$path"
    if [ -n "$query" ]; then
        full_path="$path?$query"
    fi
    
    echo "📋 $name"
    echo "   GET $full_path"
    
    response=$(curl -s -w "\n%{http_code}" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        "$STAGING_URL$full_path")
    
    http_code=$(echo "$response" | tail -n 1)
    body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "200" ]; then
        echo "   ✅ Status: $http_code"
        echo "   Response:"
        echo "$body" | jq '.' 2>/dev/null | head -20 || echo "$body" | head -c 300
        echo ""
    else
        echo "   ❌ Status: $http_code"
        echo "   Response: $body" | head -c 200
        echo ""
    fi
    echo "---"
    echo ""
}

# Test endpoints that should integrate with Aave/Morpho

echo "🔌 PROTOCOL INTEGRATION ENDPOINTS"
echo "=================================="
echo ""

# 1. Yield Rates (should work - already implemented)
test_endpoint "Get Yield Rates (all)" "/v1/yield/rates" ""
test_endpoint "Get Yield Rates (USDC)" "/v1/yield/rates" "currency=USDC"
test_endpoint "Get Yield Rates (Morpho only)" "/v1/yield/rates" "protocol=morpho"
test_endpoint "Get Yield Rates (Aave only)" "/v1/yield/rates" "protocol=aave"

# 2. Markets (needs implementation)
test_endpoint "List Markets (all)" "/v1/markets" ""
test_endpoint "List Markets (Morpho)" "/v1/markets" "protocol=morpho"
test_endpoint "List Markets (Aave)" "/v1/markets" "protocol=aave"
test_endpoint "List Markets (USDC)" "/v1/markets" "currency=USDC"

# 3. Positions (database only, but test anyway)
test_endpoint "List Positions" "/v1/yield/positions" ""

# 4. Yield Accounts (database only)
test_endpoint "List Yield Accounts" "/v1/yield/accounts" ""

echo ""
echo "✅ Testing complete!"
echo ""
echo "Summary:"
echo "- /v1/yield/rates: Should work (already implemented)"
echo "- /v1/markets: May return empty (needs implementation)"
echo "- /v1/yield/positions: May return empty (needs implementation)"
echo "- /v1/yield/accounts: Should work (database only)"

