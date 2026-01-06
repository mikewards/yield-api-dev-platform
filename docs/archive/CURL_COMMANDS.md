# Ground API cURL Commands

## Markets Endpoint

### Production
```bash
curl https://flow-platform-production.up.railway.app/v1/markets \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"
```

### Sandbox (Staging)
```bash
curl https://flow-platform-flow-platform-staging.up.railway.app/v1/markets \
  -H "Authorization: Bearer tbd_sand_008f5ac39af84526a6a0d959d9428819"
```

### Markets with Filters
```bash
# Filter by protocol (morpho or aave)
curl "https://flow-platform-production.up.railway.app/v1/markets?protocol=morpho" \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"

# Filter by currency
curl "https://flow-platform-production.up.railway.app/v1/markets?currency=USDC" \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"

# Filter by both
curl "https://flow-platform-production.up.railway.app/v1/markets?protocol=aave&currency=USDC" \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"
```

## Yield Rates Endpoint

### Production
```bash
curl https://flow-platform-production.up.railway.app/v1/yield/rates \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"
```

### Sandbox (Staging)
```bash
curl https://flow-platform-flow-platform-staging.up.railway.app/v1/yield/rates \
  -H "Authorization: Bearer tbd_sand_008f5ac39af84526a6a0d959d9428819"
```

### Rates with Filters
```bash
# Filter by currency
curl "https://flow-platform-production.up.railway.app/v1/yield/rates?currency=USDC" \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"

# Filter by protocol
curl "https://flow-platform-production.up.railway.app/v1/yield/rates?protocol=morpho" \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"

# Filter by both
curl "https://flow-platform-production.up.railway.app/v1/yield/rates?currency=USDC&protocol=aave" \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN"
```

## Pretty Print with jq

Add `| jq .` to any command for formatted JSON output:

```bash
curl https://flow-platform-production.up.railway.app/v1/markets \
  -H "Authorization: Bearer YOUR_PRODUCTION_TOKEN" | jq .
```

## Notes

- Replace `YOUR_PRODUCTION_TOKEN` with your actual production Personal Access Token
- Sandbox token: `tbd_sand_008f5ac39af84526a6a0d959d9428819` (for testing)
- Production URL: `https://flow-platform-production.up.railway.app`
- Sandbox URL: `https://flow-platform-flow-platform-staging.up.railway.app`

