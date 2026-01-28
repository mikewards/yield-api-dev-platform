# Flow Platform - Agent Context

Paste this into a new agent when starting work on this project.

---

## Overview

Flow Platform (also called "Ground Platform") is a DeFi API platform providing a unified REST API for yield generation across Morpho and Aave protocols. It abstracts protocol complexity and handles compliance.

**GitHub:** https://github.com/wardmic4/flow-platform

**Stack:** Kotlin/Ktor backend, vanilla HTML/JS frontend on Cloudflare Pages

**Deployments:**
- Production API: `flow-platform-production.up.railway.app`
- Staging API: `flow-platform-flow-platform-staging.up.railway.app`
- Frontend: Cloudflare Pages (`ground-platform`)

## Directory Structure

```
flow-api/           Kotlin backend (Ktor)
frontend/           Static HTML/JS frontend
sdks/               Embeddable UI SDKs (Applications, Wallets, Yield Accounts)
docs/               Documentation
scripts/            Utility scripts
```

## Key API Endpoints

### Authentication
- `POST /v1/users/register` - Register new user
- `POST /v1/users/login` - Login (returns JWT + refresh token)
- `GET /v1/users/me` - Get current user profile
- `POST /v1/auth/refresh` - Refresh access token

### Businesses (RCAC - Resource-Centric Access Control)
- `GET /v1/businesses` - List user's businesses
- `POST /v1/businesses` - Create business
- `POST /v1/businesses/{id}/invitations` - Invite member
- Role management: create, assign, revoke roles

### Applications
- `POST /v1/applications` - Create application
- `GET /v1/applications` - List applications
- `POST /v1/applications/{id}/tokens` - Create API key
- `POST /v1/applications/{id}/wallets` - Create wallet

### Yield
- `POST /v1/yield/accounts` - Create yield account
- `POST /v1/yield/accounts/{id}/deposit` - Deposit funds
- `POST /v1/yield/accounts/{id}/withdraw` - Withdraw funds
- `GET /v1/yield/rates` - Get current yield rates
- `GET /v1/markets` - List Morpho + Aave markets

### Webhooks (Svix)
- `POST /v1/webhooks` - Create webhook endpoint
- `POST /v1/webhooks/{id}/test` - Send test webhook

### Logs
- `GET /v1/logs` - Request logs (7-day retention)
- `GET /v1/logs/stats` - Log statistics

## Database Schema (PostgreSQL + Exposed ORM)

### RCAC Tables (New Model)
- `Users` - Individual people
- `Businesses` - Organizations
- `BusinessMemberships` - User-business links
- `Roles` - Custom roles per business
- `UserRoles` - Role assignments
- `ResourceAccess` - Fine-grained permissions (who can access what)
- `AuditLogs` - Access change audit trail

### Resource Tables
- `Applications` - Developer apps
- `ApplicationWallets` - Ethereum wallets per app
- `YieldAccounts` - Yield account records
- `AccessTokens` - API keys (PATs)
- `RefreshTokens` - OAuth refresh tokens
- `RequestLogs` - API request logging

### Legacy Tables (Migration in Progress)
- `Accounts` - Old user model (being migrated to Users)

## Services (`flow-api/src/main/kotlin/com/ground/service/`)

- `UserService` - User CRUD, authentication (RCAC)
- `BusinessService` - Business CRUD, members, invitations
- `ApplicationService` - App CRUD, API keys
- `WalletService` - Wallet creation/management
- `YieldService` - Yield accounts, deposits, withdrawals
- `PermissionService` - RCAC permission checking
- `RoleService` - Role management
- `WebhookService` - Svix webhook delivery
- `EncryptionService` - AES-256-GCM for wallet keys
- `TokenService` - JWT generation, refresh tokens
- `MorphoMarketService` - Morpho protocol integration
- `RequestLogService` - Request logging
- `RcacMigrationService` - Legacy to RCAC migration

## Environment Variables

**Required:**
- `DATABASE_URL` - PostgreSQL connection
- `JWT_SECRET` - JWT signing (32+ chars)
- `TOKEN_HASH_KEY` - Refresh token hashing (32+ chars)
- `MASTER_ENCRYPTION_KEY` - Wallet key encryption (32 bytes hex)
- `SVIX_API_KEY` - Webhook service

**Optional:**
- `ENVIRONMENT` - `development`, `sandbox`, `production`
- `SENTRY_DSN` - Error tracking
- `LOG_LEVEL` - `DEBUG`, `INFO`, `WARN`, `ERROR`

## Middleware Stack

1. CORS (allows all hosts)
2. Rate Limiting (100 req/min default, 5 req/min login)
3. Bearer Token Auth (PAT or JWT)
4. Request Logging (async to DB)
5. Sentry (if configured)

## Known TODOs and Incomplete Features

1. `TransactionRoutes.kt` - Transaction listing returns empty (not implemented)
2. `PositionRoutes.kt` - Position listing returns empty (not implemented)
3. `UserAuthRoutes.kt` - Refresh token not stored in DB, MFA partial
4. `dashboard-team.html` - Edit member roles modal not implemented
5. Frontend config - Update to final domain when DNS configured

## Active Development

- **RCAC Migration** - Moving from legacy Account model to Users + Businesses
- **Team Management** - Business members, roles, invitations
- **Webhook System** - Svix integration
- **Protocol Integration** - Morpho and Aave parallel API calls

## Frontend Pages

- `index.html` - Landing page
- `dashboard.html` - Main dashboard
- `dashboard-team.html` - Team management
- `dashboard-webhooks.html` - Webhook management
- `dashboard-logs.html` - Request logs
- `dashboard-settings.html` - Account settings
- `signin.html` - Authentication
- `api-reference.html` - API docs

## SDKs (`sdks/`)

Embeddable UI components:
- `sdk-applications/` - Application management
- `sdk-wallets/` - Wallet management
- `sdk-yield-accounts/` - Yield account management

## Commit Strategy

Commit and push to GitHub after every substantive change.
