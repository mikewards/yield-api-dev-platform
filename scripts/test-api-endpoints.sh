#!/bin/bash

# Test API Endpoints with Staging Credentials
# This script tests all GET endpoints that should integrate with Aave/Morpho

# Configuration
STAGING_URL="${STAGING_API_URL:-https://flow-platform-staging.up.railway.app}"
TOKEN="${STAGING_TOKEN:-}"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "🧪 Testing TBD API Endpoints (Staging)"
echo "========================================"
echo ""
echo "API URL: $STAGING_URL"
if [ -z "$TOKEN" ]; then
    echo -e "${YELLOW}⚠️  STAGING_TOKEN not set. Set it with:${NC}"
    echo "   export STAGING_TOKEN='your-sandbox-token-here'"
    echo ""
    echo "Or pass it as an argument:"
    echo "   STAGING_TOKEN='your-token' ./test-api-endpoints.sh"
    echo ""
    exit 1
fi
echo "Token: ${TOKEN:0:20}..."
echo ""
echo "========================================"
echo ""

# Test counter
PASSED=0
FAILED=0
NOT_IMPLEMENTED=0

# Function to test an endpoint
test_endpoint() {
    local name="$1"
    local method="$2"
    local path="$3"
    local description="$4"
    
    echo -e "${BLUE}Testing: $name${NC}"
    echo "  $method $path"
    echo "  $description"
    
    if [ "$method" = "GET" ]; then
        RESPONSE=$(curl -s -w "\n%{http_code}" -X GET \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "$STAGING_URL$path")
    else
        RESPONSE=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            "$STAGING_URL$path")
    fi
    
    HTTP_CODE=$(echo "$RESPONSE" | tail -n 1)
    BODY=$(echo "$RESPONSE" | head -n -1)
    
    if [ "$HTTP_CODE" = "200" ] || [ "$HTTP_CODE" = "201" ]; then
        echo -e "  ${GREEN}✅ PASSED (HTTP $HTTP_CODE)${NC}"
        echo "  Response:"
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY" | head -c 200
        echo ""
        ((PASSED++))
    elif [ "$HTTP_CODE" = "404" ]; then
        echo -e "  ${YELLOW}⚠️  NOT IMPLEMENTED (HTTP 404)${NC}"
        echo ""
        ((NOT_IMPLEMENTED++))
    elif [ "$HTTP_CODE" = "401" ]; then
        echo -e "  ${RED}❌ AUTH FAILED (HTTP 401)${NC}"
        echo "  Check your token!"
        echo ""
        ((FAILED++))
    else
        echo -e "  ${RED}❌ FAILED (HTTP $HTTP_CODE)${NC}"
        echo "  Response:"
        echo "$BODY" | head -c 200
        echo ""
        ((FAILED++))
    fi
    echo "---"
    echo ""
}

# Test endpoints that should integrate with Aave/Morpho

echo "📊 PROTOCOL INTEGRATION ENDPOINTS"
echo "=================================="
echo ""

# 1. Yield Rates (already working)
test_endpoint \
    "Get Yield Rates" \
    "GET" \
    "/v1/yield/rates" \
    "Get current yield rates from Morpho and Aave"

# 2. Yield Rates with currency filter
test_endpoint \
    "Get Yield Rates (USDC)" \
    "GET" \
    "/v1/yield/rates?currency=USDC" \
    "Get USDC rates from both protocols"

# 3. Yield Rates with protocol filter
test_endpoint \
    "Get Yield Rates (Morpho only)" \
    "GET" \
    "/v1/yield/rates?protocol=morpho" \
    "Get rates from Morpho only"

test_endpoint \
    "Get Yield Rates (Aave only)" \
    "GET" \
    "/v1/yield/rates?protocol=aave" \
    "Get rates from Aave only"

# 4. Markets endpoint (should list Morpho/Aave markets)
test_endpoint \
    "List Markets" \
    "GET" \
    "/v1/markets" \
    "List all available markets from Morpho and Aave"

# 5. Markets with protocol filter
test_endpoint \
    "List Markets (Morpho)" \
    "GET" \
    "/v1/markets?protocol=morpho" \
    "List Morpho markets"

test_endpoint \
    "List Markets (Aave)" \
    "GET" \
    "/v1/markets?protocol=aave" \
    "List Aave markets"

# 6. Positions (might need protocol data)
test_endpoint \
    "List Positions" \
    "GET" \
    "/v1/yield/positions" \
    "List yield positions (may include protocol data)"

# 7. Yield Accounts (database only, but test anyway)
test_endpoint \
    "List Yield Accounts" \
    "GET" \
    "/v1/yield/accounts" \
    "List user's yield accounts"

echo ""
echo "📋 SUMMARY"
echo "========================================"
echo -e "${GREEN}✅ Passed: $PASSED${NC}"
echo -e "${RED}❌ Failed: $FAILED${NC}"
echo -e "${YELLOW}⚠️  Not Implemented: $NOT_IMPLEMENTED${NC}"
echo ""

if [ $FAILED -eq 0 ] && [ $NOT_IMPLEMENTED -eq 0 ]; then
    echo -e "${GREEN}🎉 All endpoints working!${NC}"
    exit 0
elif [ $FAILED -eq 0 ]; then
    echo -e "${YELLOW}⚠️  Some endpoints not yet implemented${NC}"
    exit 0
else
    echo -e "${RED}❌ Some endpoints failed${NC}"
    exit 1
fi

