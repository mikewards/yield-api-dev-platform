# TBD Platform

> A unified DeFi API platform that abstracts protocol complexity and enables developers to integrate cryptocurrency yield generation into their applications.

[![Deploy](https://railway.app/button.svg)](https://railway.app)

## Overview

TBD provides a single, beautiful REST API that wraps multiple DeFi protocols (Morpho and Aave), enabling developers to earn yield without managing protocol-specific integrations, smart contracts, or compliance requirements.

**Key Features:**
- **Unified API** - Single interface for multiple DeFi protocols
- **Secure** - JWT authentication, encrypted wallet storage
- **Real-time Rates** - Current APY from Morpho and Aave
- **Developer-Focused** - Complete documentation, interactive examples
- **Fast** - Optimized for performance with parallel protocol calls

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
python3 -m http.server 3000
```

Visit `http://localhost:3000` for the frontend and `http://localhost:8080` for the API.

**For detailed setup instructions, see [Development Setup](./docs/development/setup.md)**

## Project Structure

```
flow-platform/
├── flow-api/              # Kotlin backend (Ktor)
│   ├── src/main/kotlin/   # Source code
│   └── build.gradle.kts   # Build configuration
├── frontend/              # Frontend application
│   ├── pages/            # HTML pages
│   ├── styles/           # CSS files
│   ├── scripts/          # JavaScript files
│   └── sdk-demos/        # SDK demo pages
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

**Frontend:**
- Vanilla HTML/CSS/JavaScript
- Cloudflare Pages (hosting)

**Infrastructure:**
- Railway.app (backend hosting)
- Cloudflare Pages (frontend hosting)
- PostgreSQL (database)

## Documentation

All documentation is organized in the [`docs/`](./docs/) directory:

- **[Development Setup](./docs/development/setup.md)** - Local development guide
- **[API Specification](./docs/api/specification.md)** - Complete API reference
- **[Deployment Guide](./docs/deployment/railway.md)** - Production deployment
- **[Architecture Overview](./docs/architecture/overview.md)** - System design

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

### Authenticate

```bash
curl -X POST http://localhost:8080/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "developer",
    "password": "secure_password"
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
4. Set environment variables
5. Deploy!

**See [Railway Deployment Guide](./docs/deployment/railway.md) for detailed instructions**

### Cloudflare Pages (Frontend)

1. Connect GitHub repository
2. Configure build settings (no build needed)
3. Deploy!

**See [Cloudflare Deployment Guide](./docs/deployment/cloudflare.md) for detailed instructions**

## Environment Variables

Required variables:
- `DATABASE_URL` - PostgreSQL connection string
- `JWT_SECRET` - JWT signing secret (min 32 chars)
- `MASTER_ENCRYPTION_KEY` - Wallet encryption key (32 bytes hex)
- `ENVIRONMENT` - `development`, `sandbox`, or `production`

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
