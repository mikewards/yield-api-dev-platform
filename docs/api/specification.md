# Ground API Specification

**Last Updated**: December 2025  
**API Version**: v1

## Base URLs

| Environment | URL |
|-------------|-----|
| Production | `https://flow-platform-production.up.railway.app` |
| Sandbox | `https://flow-platform-staging.up.railway.app` |
| Local | `http://localhost:8080` |

All endpoints are prefixed with `/v1/`

## Authentication

TBD uses OAuth 2.0-style authentication with short-lived access tokens and long-lived refresh tokens.

### Token Types

| Token | Lifetime | Purpose |
|-------|----------|---------|
| Access Token | 15 minutes | API request authentication (JWT) |
| Refresh Token | 30 days | Obtaining new access tokens |
| Personal Access Token (PAT) | Configurable | Application-scoped API keys |

### Using Tokens

```http
Authorization: Bearer <access_token>
```

---

## Authentication Endpoints

### POST `/v1/auth/authenticate`

Authenticate with username and password. Returns access and refresh tokens.

**Request:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzUxMiJ9...",
  "refresh_token": "tbd_refresh_abc123...",
  "token_type": "Bearer",
  "expires_in": 900,
  "account_id": "461c224b-1dc1-415a-8221-1a9543ec332e"
}
```

### POST `/v1/auth/refresh`

Exchange a refresh token for new access and refresh tokens.

**Request:**
```json
{
  "refresh_token": "tbd_refresh_abc123..."
}
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJIUzUxMiJ9...",
  "refresh_token": "tbd_refresh_def456...",
  "token_type": "Bearer",
  "expires_in": 900
}
```

**Notes:**
- Old refresh token is revoked (token rotation)
- Returns new refresh token for security

### POST `/v1/auth/logout`

Revoke all refresh tokens for the authenticated user.

**Headers:**
```http
Authorization: Bearer <access_token>
```

**Response:**
```json
{
  "message": "Successfully logged out"
}
```

---

## Account Endpoints

### POST `/v1/accounts`

Create a new developer account.

**Request:**
```json
{
  "username": "string (required, 3-50 chars, alphanumeric + underscore)",
  "password": "string (required, min 8 chars)",
  "email": "string (optional)"
}
```

**Response:**
```json
{
  "account_id": "461c224b-1dc1-415a-8221-1a9543ec332e",
  "username": "developer123",
  "created_at": "2025-12-03T10:30:00Z",
  "status": "active"
}
```

### GET `/v1/accounts/{account_id}`

Get account details. Requires authentication.

---

## Application Endpoints

### POST `/v1/applications`

Create a new application.

**Request:**
```json
{
  "name": "string (required)",
  "description": "string (optional)"
}
```

**Response:**
```json
{
  "application_id": "app_abc123...",
  "name": "My Production App",
  "description": "Production application",
  "status": "active",
  "created_at": "2025-12-03T10:30:00Z"
}
```

### GET `/v1/applications`

List all applications for your account.

### GET `/v1/applications/{application_id}`

Get application details.

### DELETE `/v1/applications/{application_id}`

Delete an application and all associated tokens/wallets.

---

## Application Token Endpoints

### POST `/v1/applications/{application_id}/tokens`

Create a new API key scoped to an application.

**Request:**
```json
{
  "name": "string (required)",
  "environment": "string (sandbox or production)",
  "expires_in": "integer (optional, seconds)"
}
```

**Response:**
```json
{
  "token_id": "tok_abc123...",
  "application_id": "app_abc123...",
  "access_token": "tbd_prod_abc123...",
  "name": "Production Server Key",
  "environment": "production",
  "created_at": "2025-12-03T10:30:00Z",
  "expires_at": null
}
```

### GET `/v1/applications/{application_id}/tokens`

List all API keys for an application.

### DELETE `/v1/applications/{application_id}/tokens/{token_id}`

Revoke an API key.

---

## Webhook Endpoints

Webhooks are powered by Svix for reliable delivery with automatic retries.

### GET `/v1/webhooks/event-types`

List all available webhook event types.

**Response:**
```json
{
  "event_types": [
    {
      "name": "deposit.completed",
      "description": "Triggered when funds are successfully deposited"
    },
    {
      "name": "withdrawal.completed",
      "description": "Triggered when funds are successfully withdrawn"
    }
  ]
}
```

### POST `/v1/webhooks`

Create a new webhook endpoint.

**Request:**
```json
{
  "url": "https://your-app.com/webhooks",
  "description": "Production webhook",
  "events": ["deposit.completed", "withdrawal.completed"]
}
```

**Response:**
```json
{
  "id": "ep_abc123...",
  "url": "https://your-app.com/webhooks",
  "description": "Production webhook",
  "events": ["deposit.completed", "withdrawal.completed"],
  "status": "active",
  "created_at": "2025-12-03T10:30:00Z"
}
```

### GET `/v1/webhooks`

List all webhook endpoints.

### GET `/v1/webhooks/{id}`

Get webhook endpoint details.

### DELETE `/v1/webhooks/{id}`

Delete a webhook endpoint.

### POST `/v1/webhooks/{id}/test`

Send a test event to a webhook endpoint.

**Request:**
```json
{
  "event_type": "deposit.completed"
}
```

### GET `/v1/webhooks/portal`

Get a magic link to the Svix App Portal for webhook debugging.

**Response:**
```json
{
  "url": "https://app.svix.com/login#key=...",
  "recent_messages": 5
}
```

---

## Webhook Event Types

| Event | Description |
|-------|-------------|
| `deposit.completed` | Funds deposited to yield account |
| `withdrawal.completed` | Funds withdrawn from yield account |
| `yield.accrued` | Yield accrued to account (daily) |
| `rate.changed` | Yield rate changed significantly (>0.5%) |
| `account.status.changed` | Yield account status changed |
| `application.created` | New application created |
| `api_key.created` | New API key generated |

### Webhook Payload Format

```json
{
  "event_type": "deposit.completed",
  "application_id": "app_abc123...",
  "application_name": "My App",
  "environment": "production",
  "data": {
    "yield_account_id": "ya_abc123...",
    "amount": "1000.00",
    "currency": "USDC",
    "timestamp": "2025-12-03T10:30:00Z"
  }
}
```

---

## Yield Account Endpoints

### POST `/v1/yield/accounts`

Create a new yield account.

**Request:**
```json
{
  "currency": "USDC",
  "initial_deposit": {
    "amount": "1000.00",
    "currency": "USDC"
  },
  "protocol_preference": "auto"
}
```

**Response:**
```json
{
  "account_id": "ya_abc123...",
  "currency": "USDC",
  "protocol": "morpho",
  "annual_yield_rate": 0.06,
  "status": "active",
  "balance": {
    "amount": "1000.00",
    "currency": "USDC"
  },
  "created_at": "2025-12-03T10:30:00Z"
}
```

### GET `/v1/yield/accounts`

List all yield accounts.

### GET `/v1/yield/accounts/{account_id}`

Get yield account details.

### POST `/v1/yield/accounts/{account_id}/deposit`

Deposit funds into a yield account.

### POST `/v1/yield/accounts/{account_id}/withdraw`

Withdraw funds from a yield account.

---

## Market Endpoints

### GET `/v1/markets`

List all available markets across Morpho and Aave protocols.

**Query Parameters:**
- `protocol` (optional): Filter by protocol (`morpho`, `aave`)
- `currency` (optional): Filter by currency

**Response:**
```json
{
  "markets": [
    {
      "currency": "USDC",
      "protocol": "morpho",
      "network": "ethereum_mainnet",
      "annual_yield_rate": 0.06,
      "apy": 0.0618,
      "updated_at": "2025-12-03T10:30:00Z"
    }
  ]
}
```

---

## Rate Endpoints

### GET `/v1/yield/rates`

Get current yield rates for all supported currencies and protocols.

**Response:**
```json
{
  "rates": [
    {
      "currency": "USDC",
      "protocol": "morpho",
      "network": "ethereum_mainnet",
      "annual_yield_rate": 0.06,
      "apy": 0.0618,
      "updated_at": "2025-12-03T10:30:00Z"
    }
  ]
}
```

---

## Rate Limiting

Rate limits are configurable per endpoint.

| Endpoint | Limit |
|----------|-------|
| Default | 100 requests/minute |
| `GET /v1/markets` | 75 requests/minute |
| `GET /v1/yield/rates` | 75 requests/minute |
| `GET /health` | 300 requests/minute |

### Headers

All responses include rate limit headers:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1701590400
```

### Rate Limit Exceeded

```json
{
  "error": {
    "code": "RATE_LIMIT_EXCEEDED",
    "message": "Too many requests. Please retry after X seconds.",
    "type": "rate_limit_error"
  }
}
```

---

## Error Responses

All errors follow this format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "type": "error_type"
  }
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `INVALID_REQUEST` | 400 | Invalid request parameters |
| `UNAUTHORIZED` | 401 | Authentication required or invalid |
| `INVALID_CREDENTIALS` | 401 | Invalid username or password |
| `INVALID_TOKEN` | 401 | Refresh token expired or invalid |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `SERVER_ERROR` | 500 | Internal server error |

---

## Health Check

### GET `/health`

Check API health status.

**Response:**
```json
{
  "status": "ok"
}
```

---

## SDKs & Integration

### JavaScript/TypeScript

```javascript
// Using TokenManager for automatic refresh
const response = await TokenManager.apiCall(
  `${API_BASE_URL}/v1/yield/accounts`,
  { method: 'GET' }
);
```

### cURL Examples

```bash
# Authenticate
curl -X POST https://flow-platform-production.up.railway.app/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{"username": "your_username", "password": "your_password"}'

# Get markets (with API key)
curl https://flow-platform-production.up.railway.app/v1/markets \
  -H "Authorization: Bearer tbd_prod_your_api_key"
```

---

## Support

- Documentation: [/guides](/guides)
- API Reference: [/api-reference](/api-reference)
- Quick Start: [/quickstart](/quickstart)
