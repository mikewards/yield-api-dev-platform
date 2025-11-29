# Railway Staging & Production Setup Guide

This guide will help you set up separate staging and production environments on Railway.

## Overview

- **Production Service**: Deploys from `main` branch → `flow-platform-production.up.railway.app`
- **Staging Service**: Deploys from `staging` branch → `flow-platform-staging.up.railway.app`
- **Separate Databases**: Each environment has its own PostgreSQL database
- **Separate Environment Variables**: Each service has its own configuration

## Step 1: Create Production Service (If Not Already Created)

1. Go to Railway dashboard: https://railway.app
2. Create a new project (or use existing)
3. Click **"+ New"** → **"GitHub Repo"**
4. Select your repository
5. Railway will auto-detect the Dockerfile
6. Name the service: `flow-platform-production`

### Production Service Configuration

1. **Settings** → **Source**:
   - Branch: `main`
   - Root Directory: `flow-api`

2. **Variables** (Set these):
   ```
   ENVIRONMENT=production
   DATABASE_USER=postgres
   DATABASE_PASSWORD=<from production database>
   JWT_SECRET=<generate: openssl rand -hex 32>
   MASTER_ENCRYPTION_KEY=<generate: openssl rand -hex 32>
   ```
   - `DATABASE_URL` will be auto-set when you link the database

3. **Add PostgreSQL Database**:
   - Click **"+ New"** → **"Database"** → **"Add PostgreSQL"**
   - Name it: `flow-db-production`
   - Link it to `flow-platform-production` service
   - Railway will auto-set `DATABASE_URL`

4. **Generate Domain**:
   - Settings → **Generate Domain**
   - Copy the URL (e.g., `flow-platform-production.up.railway.app`)

## Step 2: Create Staging Service

1. In the same Railway project, click **"+ New"** → **"GitHub Repo"**
2. Select the **same repository**
3. Name the service: `flow-platform-staging`

### Staging Service Configuration

1. **Settings** → **Source**:
   - Branch: `staging` (we'll create this branch)
   - Root Directory: `flow-api`

2. **Variables** (Set these):
   ```
   ENVIRONMENT=staging
   DATABASE_USER=postgres
   DATABASE_PASSWORD=<from staging database>
   JWT_SECRET=<generate: openssl rand -hex 32> (different from production!)
   MASTER_ENCRYPTION_KEY=<generate: openssl rand -hex 32> (different from production!)
   ```
   - `DATABASE_URL` will be auto-set when you link the database

3. **Add PostgreSQL Database**:
   - Click **"+ New"** → **"Database"** → **"Add PostgreSQL"**
   - Name it: `flow-db-staging`
   - Link it to `flow-platform-staging` service
   - Railway will auto-set `DATABASE_URL`

4. **Generate Domain**:
   - Settings → **Generate Domain**
   - Copy the URL (e.g., `flow-platform-staging.up.railway.app`)

## Step 3: Create Staging Branch

```bash
# Create and push staging branch
git checkout -b staging
git push -u origin staging
```

## Step 4: Railway Branch Configuration

### For Production Service:
1. Go to `flow-platform-production` → **Settings**
2. Under **Source**, ensure branch is set to `main`
3. Railway will auto-deploy on pushes to `main`

### For Staging Service:
1. Go to `flow-platform-staging` → **Settings**
2. Under **Source**, set branch to `staging`
3. Railway will auto-deploy on pushes to `staging`

## Step 5: Workflow Going Forward

### Development Workflow:
1. **Make changes** on a feature branch
2. **Test locally** first
3. **Merge to `staging`** branch → Auto-deploys to staging
4. **Test on staging** environment
5. **Merge `staging` → `main`** → Auto-deploys to production

### Quick Commands:
```bash
# Work on feature
git checkout -b feature/my-feature
# ... make changes ...
git commit -m "Add feature"
git push origin feature/my-feature

# Deploy to staging
git checkout staging
git merge feature/my-feature
git push origin staging
# Railway auto-deploys to staging

# After testing, deploy to production
git checkout main
git merge staging
git push origin main
# Railway auto-deploys to production
```

## Step 6: Verify Both Services

### Test Production:
```bash
curl https://flow-platform-production.up.railway.app/health
```

### Test Staging:
```bash
curl https://flow-platform-staging.up.railway.app/health
```

Both should return:
```json
{
  "status": "healthy",
  "timestamp": "..."
}
```

## Important Notes

1. **Separate Secrets**: Use different `JWT_SECRET` and `MASTER_ENCRYPTION_KEY` for staging and production
2. **Separate Databases**: Staging and production have completely separate data
3. **Auto-Deploy**: Both services auto-deploy when you push to their respective branches
4. **Environment Variable**: `ENVIRONMENT=staging` or `ENVIRONMENT=production` helps with logging/monitoring

## Troubleshooting

### Staging not deploying?
- Check that `staging` branch exists and has been pushed
- Verify Railway service is set to deploy from `staging` branch
- Check Railway logs for errors

### Production not deploying?
- Verify Railway service is set to deploy from `main` branch
- Check that `main` branch has the latest code
- Review Railway deployment logs

### Database connection issues?
- Ensure databases are linked to their respective services
- Verify `DATABASE_URL` is auto-set by Railway
- Check that `DATABASE_PASSWORD` matches the database service

## Next Steps

After setting up both services:
1. Update `config.js` to support environment switching
2. Update API documentation with staging/production toggle
3. Test both environments thoroughly

