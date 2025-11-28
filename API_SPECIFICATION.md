# Flow API Specification

## Base URL
```
https://api.flow.com
```

## API Version
Current version: `2025-01-15`

All endpoints are prefixed with `/v1/`

## Authentication

All API requests (except account creation and authentication) require a Personal Access Token in the Authorization header:

```
Authorization: Bearer sk_live_1234567890abcdef
```

---

## API Endpoints

### Dev Essentials

#### Authentication

**POST** `/v1/auth/authenticate`
Authenticate with username and password to receive a temporary access token.

**Request Body:**
```json
{
  "username": "string (required)",
  "password": "string (required)"
}
```

**Response:**
```json
{
  "access_token": "sk_live_temp_...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "account_id": "acc_1234567890"
}
```

**POST** `/v1/auth/refresh`
Refresh an access token.

**POST** `/v1/auth/revoke`
Revoke an access token.

---

#### Accounts

**POST** `/v1/accounts`
Create a new developer account.

**Request Body:**
```json
{
  "username": "string (required, 3-50 chars, alphanumeric + underscore)",
  "password": "string (required, min 8 chars)",
  "email": "string (optional, email format)"
}
```

**Response:**
```json
{
  "account_id": "acc_1234567890abcdef",
  "username": "developer123",
  "created_at": "2025-01-15T10:30:00Z",
  "status": "active"
}
```

**GET** `/v1/accounts/{account_id}`
Get account details.

**PATCH** `/v1/accounts/{account_id}`
Update account information.

---

#### Access Tokens

**POST** `/v1/access-tokens`
Create a new Personal Access Token.

**Request Body:**
```json
{
  "name": "string (required, 1-100 chars)",
  "expires_in": "integer (optional, seconds, 3600-31536000)"
}
```

**Response:**
```json
{
  "token_id": "tok_1234567890abcdef",
  "access_token": "sk_live_1234567890abcdef",
  "name": "Production API Key",
  "created_at": "2025-01-15T10:30:00Z",
  "expires_at": null
}
```

**GET** `/v1/access-tokens`
List all access tokens for your account.

**DELETE** `/v1/access-tokens/{token_id}`
Revoke an access token.

---

#### Applications

**POST** `/v1/applications`
Create a new application. Automatically generates an Ethereum wallet for the application.

**Request Body:**
```json
{
  "name": "string (required, 1-100 chars)",
  "description": "string (optional, max 500 chars)",
  "environment": "string (required, enum: sandbox, production)",
  "webhook_url": "string (optional, valid URL)",
  "allowed_origins": ["array of allowed CORS origins"],
  "permissions": ["array of permission strings"],
  "sandbox_rpc_url": "string (required, valid HTTPS URL)",
  "production_rpc_url": "string (required, valid HTTPS URL)"
}
```

**Note:** RPC URLs are required for blockchain interactions. Get free API keys from:
- **Infura**: https://infura.io → Create project → Copy API key → Use `https://sepolia.infura.io/v3/YOUR_KEY` (sandbox) or `https://mainnet.infura.io/v3/YOUR_KEY` (production)
- **Alchemy**: https://alchemy.com → Create app → Copy API key → Use `https://eth-sepolia.g.alchemy.com/v2/YOUR_KEY` (sandbox) or `https://eth-mainnet.g.alchemy.com/v2/YOUR_KEY` (production)

**Response:**
```json
{
  "application_id": "app_1234567890abcdef",
  "name": "My Production App",
  "description": "Production application for mainnet",
  "environment": "production",
  "status": "active",
  "webhook_url": "https://myapp.com/webhooks",
  "webhook_secret": "whsec_...",
  "allowed_origins": ["https://myapp.com"],
  "permissions": ["yield:read", "yield:write"],
  "sandbox_rpc_url": "https://sepolia.infura.io/v3/...",
  "production_rpc_url": "https://mainnet.infura.io/v3/...",
  "created_at": "2025-01-15T10:30:00Z",
  "updated_at": "2025-01-15T10:30:00Z"
}
```

**GET** `/v1/applications`
List all applications for your account.

**Response:**
```json
{
  "applications": [
    {
      "application_id": "app_1234567890abcdef",
      "name": "My Production App",
      "environment": "production",
      "status": "active",
      "created_at": "2025-01-15T10:30:00Z"
    }
  ]
}
```

**GET** `/v1/applications/{application_id}`
Get application details.

**PATCH** `/v1/applications/{application_id}`
Update application settings.

**Request Body:**
```json
{
  "name": "string (optional)",
  "description": "string (optional)",
  "webhook_url": "string (optional)",
  "allowed_origins": ["array (optional)"],
  "permissions": ["array (optional)"],
  "status": "string (optional, enum: active, inactive, suspended)",
  "sandbox_rpc_url": "string (optional, valid HTTPS URL)",
  "production_rpc_url": "string (optional, valid HTTPS URL)"
}
```

**DELETE** `/v1/applications/{application_id}`
Delete an application and all associated wallets/tokens.

**POST** `/v1/applications/{application_id}/webhook-secret/regenerate`
Regenerate webhook secret for an application.

---

#### Application Wallets

**POST** `/v1/applications/{application_id}/wallets`
Create a new Ethereum wallet for an application. Applications can have multiple wallets.

**Request Body:**
```json
{
  "label": "string (optional, max 100 chars)",
  "chain": "string (optional, default: ethereum, enum: ethereum, polygon, arbitrum)"
}
```

**Response:**
```json
{
  "wallet_id": "wal_1234567890abcdef",
  "address": "0x742d35Cc6634C0532925a3b844Bc9e7595f0bEb",
  "environment": "sandbox",
  "chain": "ethereum",
  "label": "Secondary Wallet",
  "status": "active",
  "created_at": "2025-01-15T10:30:00Z"
}
```

**GET** `/v1/applications/{application_id}/wallets`
List all wallets for an application.

**GET** `/v1/applications/{application_id}/wallets/{wallet_id}`
Get wallet details.

**DELETE** `/v1/applications/{application_id}/wallets/{wallet_id}`
Archive a wallet (soft delete).

---

#### Application Tokens

**POST** `/v1/applications/{application_id}/tokens`
Create a new API key scoped to an application.

**Request Body:**
```json
{
  "name": "string (required, 1-100 chars)",
  "expires_in": "integer (optional, seconds)"
}
```

**Response:**
```json
{
  "token_id": "tok_1234567890abcdef",
  "application_id": "app_1234567890abcdef",
  "access_token": "flow_prod_1234567890abcdef",
  "name": "Production Server Key",
  "environment": "production",
  "permissions": null,
  "created_at": "2025-01-15T10:30:00Z",
  "expires_at": null,
  "last_used_at": null
}
```

**GET** `/v1/applications/{application_id}/tokens`
List all API keys for an application.

**DELETE** `/v1/applications/{application_id}/tokens/{token_id}`
Revoke an API key.

---

#### Webhooks

**POST** `/v1/webhooks`
Create a webhook subscription.

**Request Body:**
```json
{
  "url": "string (required, valid URL)",
  "events": ["array of event types"],
  "secret": "string (optional, for signature verification)"
}
```

**GET** `/v1/webhooks`
List all webhook subscriptions.

**DELETE** `/v1/webhooks/{webhook_id}`
Delete a webhook subscription.

---

### Yield

#### Yield Accounts

**POST** `/v1/yield/accounts`
Create a new yield account.

**Request Body:**
```json
{
  "currency": "string (required, enum: USDC, USDT, DAI, ETH, WBTC)",
  "initial_deposit": {
    "amount": "string (required)",
    "currency": "string (required)"
  },
  "protocol_preference": "string (optional, enum: auto, morpho, aave, default: auto)"
}
```

**Response:**
```json
{
  "account_id": "ya_1234567890abcdef",
  "currency": "USDC",
  "protocol": "morpho",
  "annual_yield_rate": 0.06,
  "status": "active",
  "created_at": "2025-01-15T10:30:00Z",
  "balance": {
    "amount": "1000.00",
    "currency": "USDC"
  }
}
```

**GET** `/v1/yield/accounts`
List all yield accounts.

**Query Parameters:**
- `currency` (optional): Filter by currency
- `protocol` (optional): Filter by protocol (morpho, aave)
- `status` (optional): Filter by status
- `limit` (optional): Number of results (default: 20, max: 100)
- `cursor` (optional): Pagination cursor

**GET** `/v1/yield/accounts/{account_id}`
Get yield account details.

**POST** `/v1/yield/accounts/{account_id}/deposit`
Deposit funds into a yield account.

**Request Body:**
```json
{
  "amount": "string (required)",
  "currency": "string (required)"
}
```

**POST** `/v1/yield/accounts/{account_id}/withdraw`
Withdraw funds from a yield account.

**Request Body:**
```json
{
  "amount": "string (required)",
  "currency": "string (required)",
  "destination_address": "string (required, wallet address)"
}
```

---

#### Positions

**GET** `/v1/yield/positions`
List all yield positions.

**Query Parameters:**
- `account_id` (optional): Filter by account
- `currency` (optional): Filter by currency
- `protocol` (optional): Filter by protocol
- `limit` (optional): Number of results
- `cursor` (optional): Pagination cursor

**Response:**
```json
{
  "positions": [
    {
      "position_id": "pos_1234567890abcdef",
      "account_id": "ya_1234567890abcdef",
      "currency": "USDC",
      "protocol": "morpho",
      "principal": {
        "amount": "1000.00",
        "currency": "USDC"
      },
      "accrued_yield": {
        "amount": "5.00",
        "currency": "USDC"
      },
      "annual_yield_rate": 0.06,
      "created_at": "2025-01-15T10:30:00Z"
    }
  ],
  "cursor": "cursor_string"
}
```

**GET** `/v1/yield/positions/{position_id}`
Get position details.

---

#### Yield Rates

**GET** `/v1/yield/rates`
Get current yield rates for all supported currencies and protocols.

**Query Parameters:**
- `currency` (optional): Filter by currency
- `protocol` (optional): Filter by protocol

**Response:**
```json
{
  "rates": [
    {
      "currency": "USDC",
      "protocol": "morpho",
      "annual_yield_rate": 0.06,
      "apy": 0.0618,
      "updated_at": "2025-01-15T10:30:00Z"
    },
    {
      "currency": "USDC",
      "protocol": "aave",
      "annual_yield_rate": 0.06,
      "apy": 0.0612,
      "updated_at": "2025-01-15T10:30:00Z"
    }
  ]
}
```

**GET** `/v1/yield/rates/history`
Get historical yield rates.

**Query Parameters:**
- `currency` (optional): Filter by currency
- `protocol` (optional): Filter by protocol
- `start_date` (optional): Start date (ISO 8601)
- `end_date` (optional): End date (ISO 8601)
- `granularity` (optional): hourly, daily, weekly (default: daily)

---

#### Yield History

**GET** `/v1/yield/history`
Get yield accrual history.

**Query Parameters:**
- `account_id` (optional): Filter by account
- `position_id` (optional): Filter by position
- `start_date` (optional): Start date (ISO 8601)
- `end_date` (optional): End date (ISO 8601)
- `limit` (optional): Number of results
- `cursor` (optional): Pagination cursor

**Response:**
```json
{
  "history": [
    {
      "entry_id": "hist_1234567890abcdef",
      "account_id": "ya_1234567890abcdef",
      "position_id": "pos_1234567890abcdef",
      "yield_amount": {
        "amount": "0.16",
        "currency": "USDC"
      },
      "period_start": "2025-01-15T00:00:00Z",
      "period_end": "2025-01-15T23:59:59Z",
      "created_at": "2025-01-16T00:00:00Z"
    }
  ],
  "cursor": "cursor_string"
}
```

---

### Markets

**GET** `/v1/markets`
List all available markets across Morpho and Aave protocols.

**Query Parameters:**
- `protocol` (optional): Filter by protocol (morpho, aave)
- `currency` (optional): Filter by currency
- `status` (optional): Filter by status (active, paused)

**Response:**
```json
{
  "markets": [
    {
      "market_id": "mkt_1234567890abcdef",
      "currency": "USDC",
      "protocol": "morpho",
      "name": "USDC Market (Morpho)",
      "current_apy": 0.0618,
      "tvl": "1000000.00",
      "available_capacity": "500000.00",
      "status": "active",
      "updated_at": "2025-01-15T10:30:00Z"
    }
  ]
}
```

**GET** `/v1/markets/{market_id}`
Get market details.

---

### Transactions

**GET** `/v1/transactions`
List all transactions.

**Query Parameters:**
- `account_id` (optional): Filter by account
- `type` (optional): Filter by type (deposit, withdraw, yield_accrual)
- `status` (optional): Filter by status (pending, completed, failed)
- `start_date` (optional): Start date (ISO 8601)
- `end_date` (optional): End date (ISO 8601)
- `limit` (optional): Number of results
- `cursor` (optional): Pagination cursor

**Response:**
```json
{
  "transactions": [
    {
      "transaction_id": "txn_1234567890abcdef",
      "account_id": "ya_1234567890abcdef",
      "type": "deposit",
      "status": "completed",
      "amount": {
        "amount": "1000.00",
        "currency": "USDC"
      },
      "created_at": "2025-01-15T10:30:00Z",
      "completed_at": "2025-01-15T10:30:05Z"
    }
  ],
  "cursor": "cursor_string"
}
```

**GET** `/v1/transactions/{transaction_id}`
Get transaction details.

---

## Error Responses

All errors follow this format:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "type": "error_type",
    "param": "field_name (optional)"
  }
}
```

### Common Error Codes

- `INVALID_REQUEST` - Invalid request parameters
- `UNAUTHORIZED` - Authentication required or invalid
- `FORBIDDEN` - Insufficient permissions
- `NOT_FOUND` - Resource not found
- `RATE_LIMIT_EXCEEDED` - Too many requests
- `SERVER_ERROR` - Internal server error
- `INSUFFICIENT_FUNDS` - Insufficient funds for operation
- `INVALID_CREDENTIALS` - Invalid username or password
- `USERNAME_TAKEN` - Username already exists

---

## Rate Limiting

- **Rate Limit**: 100 requests per minute per access token
- **Headers**: Rate limit information is included in response headers:
  - `X-RateLimit-Limit`: Maximum requests allowed
  - `X-RateLimit-Remaining`: Remaining requests
  - `X-RateLimit-Reset`: Time when limit resets (Unix timestamp)

---

## Webhook Events

Flow sends webhooks for the following events:

- `yield_account.created`
- `yield_account.updated`
- `yield_account.deposit`
- `yield_account.withdraw`
- `position.created`
- `position.updated`
- `yield.accrued`
- `transaction.completed`
- `transaction.failed`

---

## Protocol Integration

Flow wraps the following DeFi protocols:

### Morpho
- Markets API: https://docs.morpho.org/tools/offchain/api/get-started/
- Flow automatically routes to Morpho markets when optimal

### Aave
- API: https://aave-api-v2.aave.com/
- Flow automatically routes to Aave markets when optimal

Flow ensures 6% annual yield by intelligently routing between protocols and managing positions across both platforms.

---

## Support

For API support, visit:
- Documentation: https://flow.com/api-reference
- Getting Started: https://flow.com/getting-started
- Support: support@flow.com
