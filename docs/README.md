# Ground Platform Documentation

Welcome to the TBD platform documentation. This directory contains all technical documentation organized by topic.

## Documentation Structure

### [Deployment](./deployment/)
Complete guides for deploying the platform to production.

- **[Railway Deployment](./deployment/railway.md)** - Backend API deployment on Railway
- **[Cloudflare Workers](./deployment/cloudflare.md)** - Frontend deployment on Cloudflare

### [Development](./development/)
Local development setup and configuration.

- **[Setup Guide](./development/setup.md)** - Getting started with local development
- **[Environment Variables](./development/environment-variables.md)** - Complete variable reference

### [API](./api/)
API documentation and testing guides.

- **[API Specification](./api/specification.md)** - Complete API reference
- **[API Testing](./api/testing.md)** - Testing API endpoints

### [Architecture](./architecture/)
System architecture and design documentation.

- **[Overview](./architecture/overview.md)** - System architecture and design

### [Archive](./archive/)
Historical documentation and status updates (preserved for reference).

## Quick Start

1. **New to the project?** Start with [Development Setup](./development/setup.md)
2. **Deploying?** See [Railway Deployment](./deployment/railway.md)
3. **Using the API?** Check [API Specification](./api/specification.md)
4. **Understanding the system?** Read [Architecture Overview](./architecture/overview.md)

## Key Concepts

### Authentication
TBD uses OAuth 2.0-style authentication with:
- **Access Tokens**: Short-lived (15 min) JWT tokens for API access
- **Refresh Tokens**: Long-lived (30 days) tokens for obtaining new access tokens
- **Personal Access Tokens (PATs)**: Application-scoped API keys

### Webhooks
Real-time event delivery powered by Svix:
- Automatic retries with exponential backoff
- Signature verification for security
- Developer portal for debugging

### Rate Limiting
Configurable per-endpoint rate limits:
- Default: 100 requests/minute
- Headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`

## Documentation Standards

When updating documentation:
1. Keep it current with code changes
2. Include working code examples
3. Update related docs when making changes
4. Move outdated docs to `archive/` instead of deleting

## Finding What You Need

| Question | Documentation |
|----------|---------------|
| Setting up locally? | [Development Setup](./development/setup.md) |
| Deploying to production? | [Deployment Guides](./deployment/) |
| API questions? | [API Specification](./api/specification.md) |
| Understanding architecture? | [Architecture Overview](./architecture/overview.md) |
| Environment variables? | [Environment Variables](./development/environment-variables.md) |

---

**Last Updated**: December 2025
