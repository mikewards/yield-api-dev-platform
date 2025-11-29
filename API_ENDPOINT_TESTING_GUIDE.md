# API Endpoint Testing Guide

This guide helps you test all GET endpoints that integrate with Aave and Morpho using your staging credentials.

## Quick Start

1. Get your staging (sandbox) token from the dashboard
2. Run the test script:
   ```bash
   STAGING_TOKEN='your-sandbox-token' ./test-staging-endpoints.sh
   ```

## Endpoints Status

### ✅ Working Endpoints

#### `/v1/yield/rates`
- **Status**: ✅ Fully implemented
- **Integration**: Fetches rates from both Morpho and Aave
- **Query Parameters**:
  - `currency` (optional): Filter by currency (USDC, USDT, DAI, ETH, WBTC)
  - `protocol` (optional): Filter by protocol (morpho, aave)
- **Example**:
  ```bash
  curl https://flow-platform-staging.up.railway.app/v1/yield/rates \
    -H "Authorization: Bearer YOUR_TOKEN"
  ```

### 🆕 Newly Implemented Endpoints

#### `/v1/markets`
- **Status**: ✅ Just implemented
- **Integration**: Lists all markets from both Morpho and Aave
- **Query Parameters**:
  - `protocol` (optional): Filter by protocol (morpho, aave)
  - `currency` (optional): Filter by currency
- **Example**:
  ```bash
  curl https://flow-platform-staging.up.railway.app/v1/markets \
    -H "Authorization: Bearer YOUR_TOKEN"
  
  # Filter by protocol
  curl https://flow-platform-staging.up.railway.app/v1/markets?protocol=morpho \
    -H "Authorization: Bearer YOUR_TOKEN"
  
  # Filter by currency
  curl https://flow-platform-staging.up.railway.app/v1/markets?currency=USDC \
    -H "Authorization: Bearer YOUR_TOKEN"
  ```

### ⚠️ Not Yet Implemented

#### `/v1/yield/positions`
- **Status**: Returns empty list (TODO)
- **Integration**: Should show user positions across protocols
- **Note**: Currently returns `[]` - needs implementation

#### `/v1/yield/accounts`
- **Status**: ✅ Works (database only)
- **Integration**: No protocol integration - just lists user's yield accounts from database
- **Note**: This is expected - it's a database query, not a protocol query

## Testing Checklist

Use the test script to verify all endpoints:

```bash
# Set your token
export STAGING_TOKEN='your-sandbox-token-here'

# Run tests
./test-staging-endpoints.sh
```

### Manual Testing

#### 1. Test Yield Rates (All)
```bash
curl https://flow-platform-staging.up.railway.app/v1/yield/rates \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json"
```

**Expected**: Returns rates for USDC, USDT, DAI, ETH, WBTC from both protocols

#### 2. Test Yield Rates (USDC only)
```bash
curl https://flow-platform-staging.up.railway.app/v1/yield/rates?currency=USDC \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Returns USDC rates from Morpho and Aave

#### 3. Test Yield Rates (Morpho only)
```bash
curl https://flow-platform-staging.up.railway.app/v1/yield/rates?protocol=morpho \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Returns rates from Morpho only

#### 4. Test Markets (All)
```bash
curl https://flow-platform-staging.up.railway.app/v1/markets \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Returns list of all markets from both protocols with APY

#### 5. Test Markets (Morpho)
```bash
curl https://flow-platform-staging.up.railway.app/v1/markets?protocol=morpho \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Returns only Morpho markets

#### 6. Test Markets (Aave)
```bash
curl https://flow-platform-staging.up.railway.app/v1/markets?protocol=aave \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**Expected**: Returns only Aave markets

## Response Formats

### Yield Rates Response
```json
{
  "rates": [
    {
      "currency": "USDC",
      "protocol": "morpho",
      "annual_yield_rate": 0.044,
      "apy": 0.044,
      "updated_at": "2025-01-15T10:30:00Z"
    },
    {
      "currency": "USDC",
      "protocol": "aave",
      "annual_yield_rate": 0.036,
      "apy": 0.036,
      "updated_at": "2025-01-15T10:30:00Z"
    }
  ]
}
```

### Markets Response
```json
{
  "markets": [
    {
      "market_id": "morpho_0x123...",
      "protocol": "morpho",
      "currency": "USDC",
      "currency_address": "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
      "apy": 0.044,
      "status": "active",
      "updated_at": "2025-01-15T10:30:00Z"
    },
    {
      "market_id": "aave_usdc",
      "protocol": "aave",
      "currency": "USDC",
      "currency_address": "0xA0b86991c6218b36c1d19D4a2e9Eb0cE3606eB48",
      "apy": 0.036,
      "status": "active",
      "updated_at": "2025-01-15T10:30:00Z"
    }
  ]
}
```

## Error Handling

All endpoints handle errors gracefully:
- **No markets found**: Returns empty list or 0.0 APY with note
- **API errors**: Returns error message in response
- **Network errors**: Retries with exponential backoff

## Next Steps

1. ✅ Test `/v1/yield/rates` - Should work
2. ✅ Test `/v1/markets` - Just implemented, needs testing
3. ⏭️ Implement `/v1/yield/positions` - Show user positions
4. ⏭️ Add market details endpoint `/v1/markets/{market_id}`

## Troubleshooting

### 401 Unauthorized
- Check your token is valid
- Make sure you're using a sandbox token (not production)
- Token might have expired

### Empty responses
- Check if protocols have active markets for the currency
- Some currencies might not be available on both protocols
- Check Railway logs for API errors

### Slow responses
- First request might be slow (cold start)
- Protocol APIs might be slow
- Retry logic adds some delay on errors

