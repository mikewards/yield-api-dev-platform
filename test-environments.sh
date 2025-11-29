#!/bin/bash

# Test both sandbox and production environments
# Configure URLs by setting environment variables or editing this script

# Default URLs (can be overridden with environment variables)
SANDBOX_URL="${SANDBOX_API_URL:-https://api-sandbox.tbd.com}"
PRODUCTION_URL="${PRODUCTION_API_URL:-https://api.tbd.com}"

echo "🧪 Testing TBD API Environments"
echo "================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test Sandbox
echo "📦 Testing SANDBOX environment..."
echo "URL: $SANDBOX_URL/health"
SANDBOX_RESPONSE=$(curl -s -w "\n%{http_code}" "$SANDBOX_URL/health")
SANDBOX_BODY=$(echo "$SANDBOX_RESPONSE" | head -n -1)
SANDBOX_CODE=$(echo "$SANDBOX_RESPONSE" | tail -n 1)

if [ "$SANDBOX_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Sandbox: OK (HTTP $SANDBOX_CODE)${NC}"
    echo "$SANDBOX_BODY" | jq '.' 2>/dev/null || echo "$SANDBOX_BODY"
else
    echo -e "${RED}❌ Sandbox: FAILED (HTTP $SANDBOX_CODE)${NC}"
    echo "$SANDBOX_BODY"
fi

echo ""
echo "---"
echo ""

# Test Production
echo "🚀 Testing PRODUCTION environment..."
echo "URL: $PRODUCTION_URL/health"
PROD_RESPONSE=$(curl -s -w "\n%{http_code}" "$PRODUCTION_URL/health")
PROD_BODY=$(echo "$PROD_RESPONSE" | head -n -1)
PROD_CODE=$(echo "$PROD_RESPONSE" | tail -n 1)

if [ "$PROD_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Production: OK (HTTP $PROD_CODE)${NC}"
    echo "$PROD_BODY" | jq '.' 2>/dev/null || echo "$PROD_BODY"
else
    echo -e "${RED}❌ Production: FAILED (HTTP $PROD_CODE)${NC}"
    echo "$PROD_BODY"
fi

echo ""
echo "================================"

# Summary
if [ "$SANDBOX_CODE" = "200" ] && [ "$PROD_CODE" = "200" ]; then
    echo -e "${GREEN}🎉 Both environments are working!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  One or both environments need attention${NC}"
    exit 1
fi

