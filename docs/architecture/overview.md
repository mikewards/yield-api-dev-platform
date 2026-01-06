# Ground Platform Architecture

**Last Updated**: December 2025  
**Status**: Active Development  
**Environments**: Staging & Production Deployed

## Executive Overview

TBD is a DeFi API platform that provides developers with a unified REST API to access yield-generating opportunities across multiple DeFi protocols (Morpho and Aave). The platform abstracts protocol complexity, handles compliance, and enables developers to easily integrate cryptocurrency yield generation into their applications.

**Core Value**: A single, beautiful REST API that wraps multiple DeFi protocols, enabling developers to earn yield without managing protocol-specific integrations, smart contracts, or compliance requirements.

## System Architecture

```
┌─────────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Cloudflare    │         │     Railway      │         │  PostgreSQL  │
│    Workers      │────────▶│   (Kotlin/Ktor)  │────────▶│   Database   │
│   (Frontend)    │         │    (Backend)     │         │              │
└─────────────────┘         └──────────────────┘         └──────────────┘
                                      │
                    ┌─────────────────┼─────────────────┐
                    │                 │                 │
            ┌───────▼────────┐ ┌──────▼───────┐ ┌──────▼──────┐
            │  Morpho Blue   │ │   Aave V3    │ │    Svix     │
            │   GraphQL API  │ │  GraphQL API │ │  Webhooks   │
            └────────────────┘ └──────────────┘ └─────────────┘
```

## Technology Stack

### Frontend
- **HTML5, CSS3, JavaScript** - Vanilla web technologies
- **Cloudflare Workers** - Edge hosting with clean URLs
- **TokenManager.js** - OAuth 2.0-style token refresh handling
- **Design**: Inspired by Square/Stripe (clean, professional)

### Backend
- **Kotlin 1.9.20** - Primary language
- **Ktor 2.3.5** - Lightweight web framework
- **PostgreSQL** - Relational database
- **Exposed** - Type-safe SQL framework
- **Web3j** - Ethereum blockchain integration
- **Svix** - Managed webhook delivery
- **BouncyCastle** - Cryptographic operations

### Infrastructure
- **Railway.app** - Backend hosting (staging & production)
- **Cloudflare Workers** - Frontend hosting
- **PostgreSQL** - Managed database on Railway
- **Svix** - Webhook infrastructure

## Core Components

### 1. Developer Portal & Documentation
- **Landing Page**: Modern, responsive design
- **API Reference**: Comprehensive documentation with interactive examples
- **Guides Section**: Integration patterns and use cases
- **SDK Playgrounds**: Interactive demos
- **Dashboard**: Application, API key, and webhook management

### 2. REST API Gateway
**Technology**: Kotlin, Ktor, PostgreSQL

**Features**:
- OAuth 2.0-style authentication (access tokens + refresh tokens)
- Multi-application support with environment-specific credentials
- Developer dashboard for application and key management
- Request/response logging (7-day retention)
- Configurable per-endpoint rate limiting
- Svix-powered webhook delivery

### 3. Authentication System

```
┌─────────────────────────────────────────────────────────────────┐
│                    TOKEN LIFECYCLE                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Login → Access Token (15 min) + Refresh Token (30 days)        │
│                                                                  │
│  Every 13 min: Frontend silently calls /v1/auth/refresh          │
│                → Gets new Access Token + new Refresh Token       │
│                                                                  │
│  Refresh Token expires → Graceful re-login modal                 │
│                                                                  │
│  Token rotation: Old refresh tokens revoked on use               │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

**Token Types**:
- **Access Token (JWT)**: 15-minute lifetime, used for API authentication
- **Refresh Token**: 30-day lifetime, stored in database, used to obtain new access tokens
- **Personal Access Token (PAT)**: Long-lived API keys scoped to applications

### 4. Webhook System (Svix)

**Event Types**:
- `deposit.completed` - Funds deposited to yield account
- `withdrawal.completed` - Funds withdrawn from yield account
- `yield.accrued` - Yield accrued to account (daily)
- `rate.changed` - Yield rate changed significantly (>0.5%)
- `account.status.changed` - Yield account status changed
- `application.created` - New application created
- `api_key.created` - New API key generated

**Features**:
- Automatic retries with exponential backoff
- Signature verification
- Developer portal for debugging (via Svix App Portal)
- Per-account isolation (multi-tenant safe)

### 5. Protocol Integration
**Working**:
- Morpho Blue API: Yield rate fetching, market listing
- Aave V3 API: Yield rate fetching, market listing
- Centralized `ProtocolService` with automatic protocol selection
- Parallel API calls for performance
- Graceful error handling with retry logic

**Architecture**: Single API surface that routes to appropriate protocol clients, automatically selecting best rates.

### 6. Security
- **JWT Authentication**: Short-lived tokens with refresh
- **Token Rotation**: Refresh tokens rotated on each use
- **AES-256-GCM Encryption**: Wallet private key encryption
- **Environment Separation**: Separate secrets per environment
- **CORS Protection**: Configurable CORS policies
- **Rate Limiting**: Per-endpoint configurable limits

### 7. Wallet & Key Management
- Segregated wallets per application
- Encrypted private key storage
- Gas wallet for transaction fees
- Token approval service for ERC20 tokens
- Web3 integration via web3j library

## Data Flow

### Authentication Flow (OAuth 2.0-style)

```
1. Client: POST /v1/auth/authenticate
   ↓
2. Validate Credentials (BCrypt)
   ↓
3. Generate Access Token (JWT, 15 min)
   ↓
4. Generate Refresh Token (30 days, stored in DB)
   ↓
5. Return both tokens
   ↓
6. Client: Store tokens, schedule refresh
   ↓
7. Every 13 min: POST /v1/auth/refresh
   ↓
8. Rotate tokens (old refresh revoked)
   ↓
9. Return new token pair
```

### Token Refresh Flow (Silent)

```
1. TokenManager detects token expiring in < 2 min
   ↓
2. POST /v1/auth/refresh with refresh_token
   ↓
3. Backend validates refresh token
   ↓
4. Revokes old refresh token (rotation)
   ↓
5. Generates new access + refresh tokens
   ↓
6. Frontend stores new tokens
   ↓
7. Schedule next refresh
```

### API Request Flow

```
1. Client Request
   ↓
2. API Gateway (Ktor)
   ↓
3. Rate Limiting Middleware
   ↓
4. Authentication Middleware
   ├─→ JWT validation (dashboard)
   └─→ PAT validation (API calls)
   ↓
5. Request Logging (async)
   ↓
6. Route Handler
   ↓
7. Service Layer
   ├─→ MorphoClient (parallel)
   └─→ AaveClient (parallel)
   ↓
8. Response
   ↓
9. Response Logging (async)
```

### Webhook Delivery Flow

```
1. Event occurs (deposit, withdrawal, etc.)
   ↓
2. Route handler calls WebhookService.sendEvent()
   ↓
3. WebhookService formats payload with:
   - Event type
   - Application ID & name
   - Environment
   - Event-specific data
   ↓
4. Svix delivers to registered endpoints
   ↓
5. Automatic retries on failure
   ↓
6. Developer can view delivery logs in Svix portal
```

## Database Schema

### Core Tables
| Table | Purpose |
|-------|---------|
| `accounts` | User accounts (developers) |
| `applications` | Developer applications |
| `access_tokens` | API keys (PATs) |
| `refresh_tokens` | OAuth refresh tokens |
| `yield_accounts` | Yield account records |
| `application_wallets` | Ethereum wallets per application |
| `request_logs` | API request/response logging (7-day retention) |
| `transactions` | Transaction records |
| `positions` | Yield position tracking |
| `webhooks` | Webhook subscription metadata |

### Key Relationships
```
accounts
  └── applications (1:many)
        ├── access_tokens (1:many)
        └── application_wallets (1:many)
  └── refresh_tokens (1:many)
  └── yield_accounts (1:many)
```

## Deployment Architecture

### Staging Environment
- **Branch**: `staging`
- **Backend**: `flow-platform-staging.up.railway.app`
- **Database**: Separate staging database
- **Purpose**: Testing and development

### Production Environment
- **Branch**: `main`
- **Backend**: `flow-platform-production.up.railway.app`
- **Database**: Separate production database
- **Purpose**: Live production traffic

### Frontend (Cloudflare Workers)
- **Deployment**: `npx wrangler deploy` from `frontend/`
- **Routing**: Clean URLs via worker.js
- **Domain**: Configurable via Cloudflare dashboard

## Security Architecture

### Authentication
- JWT tokens for temporary access (15 min)
- Refresh tokens for session continuity (30 days)
- Token rotation on every refresh (security)
- Personal Access Tokens (PATs) for API access
- Server-side logout revokes all refresh tokens

### Encryption
- AES-256-GCM for wallet private keys
- Separate encryption keys per environment
- Keys stored in environment variables

### Network Security
- HTTPS only (enforced by Railway/Cloudflare)
- CORS protection
- Per-endpoint rate limiting

## Monitoring & Logging

### Request Logging
- All API requests logged to `request_logs` table
- 7-day retention period (auto-cleanup job)
- Includes: method, path, status, duration, IP, user agent, request/response bodies

### Error Tracking
- Sentry integration for error tracking
- Automatic error reporting
- Stack traces and context

### Health Checks
- `GET /health` endpoint
- Database connectivity checks
- Service status monitoring

### Webhook Monitoring
- Svix dashboard for delivery status
- App Portal for per-user debugging
- Retry visibility and debugging

## API Endpoints Summary

### Authentication
| Endpoint | Description |
|----------|-------------|
| `POST /v1/auth/authenticate` | Login, returns access + refresh tokens |
| `POST /v1/auth/refresh` | Exchange refresh token for new tokens |
| `POST /v1/auth/logout` | Revoke all refresh tokens |

### Resources
| Resource | Endpoints |
|----------|-----------|
| Accounts | Create, Get |
| Applications | CRUD, Tokens, Wallets |
| Yield Accounts | Create, List, Deposit, Withdraw |
| Markets | List (Morpho + Aave) |
| Rates | Current yield rates |
| Webhooks | Create, List, Delete, Test, Portal |
| Logs | Request/response logs |

## Future Architecture Considerations

### Scalability
- Horizontal scaling via Railway
- Database connection pooling (HikariCP)
- Stateless API design

### Performance
- Background streaming for market data (if needed)
- Redis for distributed rate limiting (currently in-memory)
- Response caching (if real-time not required)

### Microservices (Future)
- Currently monolithic (single service)
- Could split into: Auth service, Protocol service, Wallet service
- Not needed until significant scale

## Related Documentation

- [API Specification](../api/specification.md)
- [Development Setup](../development/setup.md)
- [Deployment Guide](../deployment/railway.md)
- [Environment Variables](../development/environment-variables.md)
