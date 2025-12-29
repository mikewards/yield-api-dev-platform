# Railway Deployment Guide

Complete guide for deploying the TBD API to Railway with separate staging and production environments.

**Last Updated**: December 2025

## Overview

- **Production Service**: Deploys from `main` branch → `flow-platform-production.up.railway.app`
- **Staging Service**: Deploys from `staging` branch → `flow-platform-staging.up.railway.app`
- **Separate Databases**: Each environment has its own PostgreSQL database
- **Separate Environment Variables**: Each service has its own configuration

## Prerequisites

- Railway account (https://railway.app)
- GitHub repository connected to Railway
- Svix account for webhooks (https://svix.com)

## Production Setup

### 1. Create Production Service

1. Go to Railway dashboard: https://railway.app
2. Create a new project (or use existing)
3. Click **"+ New"** → **"GitHub Repo"**
4. Select your repository
5. Railway will auto-detect the Dockerfile
6. Name the service: `flow-platform-production`

### 2. Configure Production Service

**Settings** → **Source**:
- Branch: `main`
- Root Directory: `flow-api`

### 3. Set Environment Variables

Navigate to **Variables** tab and set:

| Variable | Value | Notes |
|----------|-------|-------|
| `ENVIRONMENT` | `production` | Required |
| `DATABASE_USER` | `postgres` | Usually default |
| `DATABASE_PASSWORD` | `<from database>` | From Railway database settings |
| `JWT_SECRET` | `<generate>` | `openssl rand -hex 32` |
| `MASTER_ENCRYPTION_KEY` | `<generate>` | `openssl rand -hex 32` |
| `SVIX_API_KEY` | `<from Svix>` | From Svix dashboard |
| `SENTRY_DSN` | `<optional>` | For error tracking |

**Note**: `DATABASE_URL` is auto-set when you link the database.

### 4. Add PostgreSQL Database

1. Click **"+ New"** → **"Database"** → **"Add PostgreSQL"**
2. Name it: `flow-db-production`
3. Link it to `flow-platform-production` service
4. Railway auto-sets `DATABASE_URL`

### 5. Generate Domain

1. Settings → **Generate Domain**
2. Copy the URL (e.g., `flow-platform-production.up.railway.app`)

## Staging Setup

### 1. Create Staging Service

1. In the same Railway project, click **"+ New"** → **"GitHub Repo"**
2. Select the **same repository**
3. Name the service: `flow-platform-staging`

### 2. Configure Staging Service

**Settings** → **Source**:
- Branch: `staging`
- Root Directory: `flow-api`

### 3. Set Staging Variables

| Variable | Value | Notes |
|----------|-------|-------|
| `ENVIRONMENT` | `sandbox` | Required |
| `DATABASE_USER` | `postgres` | Usually default |
| `DATABASE_PASSWORD` | `<from staging DB>` | Different from production |
| `JWT_SECRET` | `<generate new>` | Different from production |
| `MASTER_ENCRYPTION_KEY` | `<generate new>` | Different from production |
| `SVIX_API_KEY` | `<from Svix>` | Same key works for both |

### 4. Add Staging Database

1. Click **"+ New"** → **"Database"** → **"Add PostgreSQL"**
2. Name it: `flow-db-staging`
3. Link it to `flow-platform-staging` service

### 5. Generate Staging Domain

1. Settings → **Generate Domain**
2. Copy the URL (e.g., `flow-platform-staging.up.railway.app`)

## Environment Variables Summary

### Required for All Environments

| Variable | Description | How to Generate |
|----------|-------------|-----------------|
| `ENVIRONMENT` | `production` or `sandbox` | Set manually |
| `DATABASE_URL` | PostgreSQL connection string | Auto-set by Railway |
| `DATABASE_USER` | Database username | Usually `postgres` |
| `DATABASE_PASSWORD` | Database password | From Railway database |
| `JWT_SECRET` | JWT signing secret | `openssl rand -hex 32` |
| `MASTER_ENCRYPTION_KEY` | Wallet encryption key | `openssl rand -hex 32` |
| `SVIX_API_KEY` | Svix webhook API key | From Svix dashboard |

### Optional

| Variable | Description |
|----------|-------------|
| `SENTRY_DSN` | Sentry error tracking URL |
| `LOG_LEVEL` | `DEBUG`, `INFO`, `WARN`, `ERROR` |

## Database

Railway automatically creates and manages PostgreSQL databases.

### Auto-Created Tables

On first deployment, these tables are created automatically:
- `accounts`
- `applications`
- `application_wallets`
- `access_tokens`
- `refresh_tokens`
- `yield_accounts`
- `positions`
- `transactions`
- `webhooks`
- `request_logs`

### Manual Database Access

1. Go to your database service in Railway
2. Click **"Connect"** tab
3. Copy connection details if needed

## Deployment Process

### Automatic Deployment

- **Production**: Auto-deploys on push to `main` branch
- **Staging**: Auto-deploys on push to `staging` branch

### Recommended Deployment Flow

```bash
# Deploy to both branches (keeps them in sync)
git add -A && git commit -m "Your changes" && \
git push origin main && \
git checkout staging && git merge main -X theirs && \
git push origin staging && git checkout main
```

### Manual Deployment

1. Go to service in Railway dashboard
2. Click **"Deploy"** → **"Deploy Now"**

## Networking & Domains

### Public URLs

Each service gets a unique Railway URL:
- Production: `flow-platform-production.up.railway.app`
- Staging: `flow-platform-staging.up.railway.app`

### Custom Domains

1. Go to service **Settings** → **Networking**
2. Click **"Custom Domain"**
3. Add your domain and configure DNS

## Monitoring

### View Logs

1. Go to service in Railway dashboard
2. Click **"Logs"** tab
3. Real-time logs are displayed

### Key Log Messages

| Log | Meaning |
|-----|---------|
| `🔐 User authenticated` | Successful login |
| `🔑 Generated refresh token` | New refresh token created |
| `🔄 Rotating refresh token` | Token rotation (security) |
| `🚪 Revoked X refresh tokens` | Logout completed |
| `✅ PAT token validated` | API key authenticated |
| `✅ JWT token validated` | Dashboard auth success |

### Metrics

Railway provides:
- CPU usage
- Memory usage
- Network traffic

For detailed monitoring, integrate with Sentry.

## Troubleshooting

### Service Won't Start

1. Check **Logs** tab for errors
2. Verify all required environment variables are set
3. Check database connection (verify `DATABASE_URL`)

### Database Connection Issues

1. Ensure database is linked to service
2. Verify `DATABASE_URL` is set
3. Check database is running

### Webhook Issues

1. Verify `SVIX_API_KEY` is set
2. Check Svix dashboard for delivery logs
3. Test with `/v1/webhooks/{id}/test` endpoint

### Authentication Issues

1. Verify `JWT_SECRET` is set (min 32 chars)
2. Check token format in Authorization header
3. Verify token hasn't expired

## Best Practices

1. **Never commit secrets**: Use Railway environment variables
2. **Separate environments**: Keep staging and production completely separate
3. **Test in staging**: Always test changes in staging before production
4. **Monitor logs**: Check logs after deployments
5. **Use different secrets**: Never reuse production secrets in staging
6. **Keep branches in sync**: Deploy to both main and staging

## Related Documentation

- [Cloudflare Deployment](./cloudflare.md) - Frontend deployment
- [Development Setup](../development/setup.md) - Local development
- [Environment Variables](../development/environment-variables.md) - Complete variable reference
