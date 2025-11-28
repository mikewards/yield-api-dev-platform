#!/bin/bash

# Test both staging and production environments

echo "🧪 Testing TBD API Environments"
echo "================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test Staging
echo "📦 Testing STAGING environment..."
echo "URL: https://flow-platform-staging.up.railway.app/health"
STAGING_RESPONSE=$(curl -s -w "\n%{http_code}" https://flow-platform-staging.up.railway.app/health)
STAGING_BODY=$(echo "$STAGING_RESPONSE" | head -n -1)
STAGING_CODE=$(echo "$STAGING_RESPONSE" | tail -n 1)

if [ "$STAGING_CODE" = "200" ]; then
    echo -e "${GREEN}✅ Staging: OK (HTTP $STAGING_CODE)${NC}"
    echo "$STAGING_BODY" | jq '.' 2>/dev/null || echo "$STAGING_BODY"
else
    echo -e "${RED}❌ Staging: FAILED (HTTP $STAGING_CODE)${NC}"
    echo "$STAGING_BODY"
fi

echo ""
echo "---"
echo ""

# Test Production
echo "🚀 Testing PRODUCTION environment..."
echo "URL: https://flow-platform-production.up.railway.app/health"
PROD_RESPONSE=$(curl -s -w "\n%{http_code}" https://flow-platform-production.up.railway.app/health)
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
if [ "$STAGING_CODE" = "200" ] && [ "$PROD_CODE" = "200" ]; then
    echo -e "${GREEN}🎉 Both environments are working!${NC}"
    exit 0
else
    echo -e "${YELLOW}⚠️  One or both environments need attention${NC}"
    exit 1
fi

