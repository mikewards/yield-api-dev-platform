# TBD Platform

> A unified DeFi API platform that abstracts protocol complexity and enables developers to integrate cryptocurrency yield generation into their applications.

[![Deploy](https://railway.app/button.svg)](https://railway.app)

## Overview

TBD provides a single, beautiful REST API that wraps multiple DeFi protocols (Morpho and Aave), enabling developers to earn yield without managing protocol-specific integrations, smart contracts, or compliance requirements.

**Key Features:**
- **Unified API** - Single interface for multiple DeFi protocols
- **OAuth 2.0 Authentication** - Short-lived access tokens with automatic refresh
- **Real-time Webhooks** - Svix-powered event delivery with retries
- **Rate Limiting** - Configurable per-endpoint limits
- **Real-time Rates** - Current APY from Morpho and Aave
- **Developer Dashboard** - Application, API key, and webhook management
- **Request Logging** - 7-day API request/response logging

## Quick Start

### Prerequisites

- JDK 17+
- PostgreSQL 12+
- Git

### Local Development

```bash
# Clone repository
git clone https://github.com/wardmic4/flow-platform.git
cd flow-platform

# Set up database
createdb flow_api

# Configure environment
cd flow-api
cp .env.example .env
# Edit .env with your database credentials

# Start backend
./gradlew run

# Start frontend (in another terminal)
cd frontend && npx wrangler dev
```

Visit `http://localhost:8787` for the frontend and `http://localhost:8080` for the API.

**For detailed setup instructions, see [Development Setup](./docs/development/setup.md)**

## Project Structure

```
flow-platform/
├── flow-api/              # Kotlin backend (Ktor)
│   ├── src/main/kotlin/   # Source code
│   │   └── com/tbd/
│   │       ├── api/routes/     # API endpoints
│   │       ├── dto/            # Data transfer objects
│   │       ├── middleware/     # Auth, rate limiting, logging
│   │       ├── model/          # Database models
│   │       ├── service/        # Business logic
│   │       └── integration/    # Morpho/Aave clients
│   └── build.gradle.kts   # Build configuration
├── frontend/              # Frontend application
│   ├── pages/            # HTML pages
│   ├── styles/           # CSS files
│   ├── scripts/          # JavaScript files
│   │   └── token-manager.js  # OAuth token management
│   ├── sdk-demos/        # SDK demo pages
│   ├── worker.js         # Cloudflare Worker entry point
│   └── wrangler.jsonc    # Cloudflare Worker config
├── docs/                  # Documentation
│   ├── deployment/        # Deployment guides
│   ├── development/       # Development setup
│   ├── api/              # API documentation
│   └── architecture/      # System architecture
└── scripts/               # Build and deployment scripts
```

## Technology Stack

**Backend:**
- Kotlin 1.9.20
- Ktor 2.3.5
- PostgreSQL
- Exposed (SQL framework)
- Web3j (Ethereum integration)
- Svix (Webhooks)

**Frontend:**
- Vanilla HTML/CSS/JavaScript
- Cloudflare Workers (hosting)
- TokenManager.js (OAuth 2.0 token refresh)

**Infrastructure:**
- Railway.app (backend hosting)
- Cloudflare Workers (frontend hosting)
- PostgreSQL (database)

## Documentation

All documentation is organized in the [`docs/`](./docs/) directory:

- **[Development Setup](./docs/development/setup.md)** - Local development guide
- **[API Specification](./docs/api/specification.md)** - Complete API reference
- **[Deployment Guide](./docs/deployment/railway.md)** - Production deployment
- **[Architecture Overview](./docs/architecture/overview.md)** - System design
- **[Environment Variables](./docs/development/environment-variables.md)** - Configuration reference

## API Examples

### Create Account

```bash
curl -X POST http://localhost:8080/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "secure_password",
    "email": "dev@example.com"
  }'
```

### Authenticate (Returns Access + Refresh Tokens)

```bash
curl -X POST http://localhost:8080/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "secure_password"
  }'

# Response includes:
# - access_token (15 min)
# - refresh_token (30 days)
```

### Refresh Token

```bash
curl -X POST http://localhost:8080/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "tbd_refresh_..."
  }'
```

### Get Yield Rates

```bash
curl -X GET http://localhost:8080/v1/yield/rates \
  -H "Authorization: Bearer YOUR_API_KEY"
```

**See [API Specification](./docs/api/specification.md) for complete API reference**

## Deployment

### Railway (Backend)

1. Push code to GitHub
2. Connect repository to Railway
3. Add PostgreSQL database
4. Set environment variables (see below)
5. Deploy!

**See [Railway Deployment Guide](./docs/deployment/railway.md) for detailed instructions**

### Cloudflare Workers (Frontend)

```bash
cd frontend
npx wrangler deploy
```

**See [Cloudflare Deployment Guide](./docs/deployment/cloudflare.md) for detailed instructions**

## Environment Variables

Required variables:
- `DATABASE_URL` - PostgreSQL connection string
- `JWT_SECRET` - JWT signing secret (min 32 chars)
- `MASTER_ENCRYPTION_KEY` - Wallet encryption key (32 bytes hex)
- `ENVIRONMENT` - `development`, `sandbox`, or `production`
- `SVIX_API_KEY` - Svix webhook service API key

**See [Environment Variables Reference](./docs/development/environment-variables.md) for complete list**

## Testing

```bash
# Run tests
cd flow-api
./gradlew test

# Build
./gradlew build
```

## Contributing

This is a private project. For questions or contributions:

1. Check the [documentation](./docs/)
2. Review [API documentation](./docs/api/specification.md)
3. See [development setup](./docs/development/setup.md)

## License

MIT License

---

**Built for developers who want to integrate DeFi yield into their applications.**
