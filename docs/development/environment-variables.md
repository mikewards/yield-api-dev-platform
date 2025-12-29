# Environment Variables Reference

Complete reference for all environment variables used in the TBD platform.

**Last Updated**: December 2025

## Required Variables

### Database

| Variable | Description | Example |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/flow_api` |
| `DATABASE_USER` | Database username | `postgres` |
| `DATABASE_PASSWORD` | Database password | `your_secure_password` |

**Note**: `DATABASE_URL` is automatically set by Railway when you link a database.

### Security

| Variable | Description | How to Generate |
|----------|-------------|-----------------|
| `JWT_SECRET` | Secret key for JWT token signing (min 32 chars) | `openssl rand -hex 32` |
| `MASTER_ENCRYPTION_KEY` | AES-256 encryption key for wallet private keys (32 bytes hex) | `openssl rand -hex 32` |

### Environment

| Variable | Description | Values |
|----------|-------------|--------|
| `ENVIRONMENT` | Current environment | `development`, `sandbox`, `production` |

### Webhooks

| Variable | Description | How to Get |
|----------|-------------|------------|
| `SVIX_API_KEY` | Svix webhook service API key | Create account at [svix.com](https://svix.com), copy API key from dashboard |

## Optional Variables

### Error Tracking

| Variable | Description | Example |
|----------|-------------|---------|
| `SENTRY_DSN` | Sentry error tracking URL | `https://xxx@sentry.io/xxx` |

### Logging

| Variable | Description | Values | Default |
|----------|-------------|--------|---------|
| `LOG_LEVEL` | Logging verbosity | `DEBUG`, `INFO`, `WARN`, `ERROR` | `INFO` |

## Environment-Specific Configuration

### Development

```bash
ENVIRONMENT=development
DATABASE_URL=jdbc:postgresql://localhost:5432/flow_api
DATABASE_USER=postgres
DATABASE_PASSWORD=local_password
JWT_SECRET=<generate with: openssl rand -hex 32>
MASTER_ENCRYPTION_KEY=<generate with: openssl rand -hex 32>
SVIX_API_KEY=<optional for local dev>
LOG_LEVEL=DEBUG
```

### Staging/Sandbox

```bash
ENVIRONMENT=sandbox
DATABASE_URL=<auto-set by Railway>
DATABASE_USER=postgres
DATABASE_PASSWORD=<from Railway database>
JWT_SECRET=<different from production>
MASTER_ENCRYPTION_KEY=<different from production>
SVIX_API_KEY=<from Svix dashboard>
LOG_LEVEL=INFO
```

### Production

```bash
ENVIRONMENT=production
DATABASE_URL=<auto-set by Railway>
DATABASE_USER=postgres
DATABASE_PASSWORD=<from Railway database>
JWT_SECRET=<strong, unique secret>
MASTER_ENCRYPTION_KEY=<strong, unique key>
SVIX_API_KEY=<from Svix dashboard>
SENTRY_DSN=<your Sentry DSN>
LOG_LEVEL=WARN
```

## Security Best Practices

1. **Never commit secrets**: Use environment variables, never hardcode
2. **Use different secrets per environment**: Never reuse production secrets in staging
3. **Rotate secrets regularly**: Especially if compromised
4. **Use strong secrets**: Minimum 32 characters for JWT_SECRET
5. **Limit access**: Only grant access to secrets to those who need them

## Generating Secrets

### JWT Secret

```bash
openssl rand -hex 32
```

### Encryption Key

```bash
openssl rand -hex 32
```

### Quick Generate Script

```bash
#!/bin/bash
echo "JWT_SECRET=$(openssl rand -hex 32)"
echo "MASTER_ENCRYPTION_KEY=$(openssl rand -hex 32)"
```

## Railway Configuration

Railway automatically sets `DATABASE_URL` when you link a database. You need to manually set:

| Variable | Required | Notes |
|----------|----------|-------|
| `DATABASE_USER` | Yes | Usually `postgres` |
| `DATABASE_PASSWORD` | Yes | From database settings |
| `JWT_SECRET` | Yes | Generate unique for each environment |
| `MASTER_ENCRYPTION_KEY` | Yes | Generate unique for each environment |
| `ENVIRONMENT` | Yes | `sandbox` for staging, `production` for prod |
| `SVIX_API_KEY` | Yes | From Svix dashboard |
| `SENTRY_DSN` | Optional | Production only |

## Local Development

Create a `.env` file in `flow-api/` directory:

```bash
ENVIRONMENT=development
DATABASE_URL=jdbc:postgresql://localhost:5432/flow_api
DATABASE_USER=postgres
DATABASE_PASSWORD=your_local_password
JWT_SECRET=dev_secret_at_least_32_characters_long
MASTER_ENCRYPTION_KEY=dev_key_at_least_32_characters_long
# SVIX_API_KEY=optional_for_local
```

The application reads from `.env` file in development mode.

## Svix Setup

1. Create account at [svix.com](https://www.svix.com/)
2. Create a new application in the Svix dashboard
3. Copy the API key (starts with `svix_sk_...`)
4. Set `SVIX_API_KEY` in your environment

The backend will automatically:
- Register event types on startup
- Create Svix applications per user account
- Handle webhook delivery and retries

## Related Documentation

- [Development Setup](./setup.md)
- [Railway Deployment](../deployment/railway.md)
- [Architecture Overview](../architecture/overview.md)
