# Railway Staging Setup - Quick Guide

## ✅ Step 1: Staging Branch Created
The staging branch has been created and pushed to GitHub.

## Step 2: Create Staging Service in Railway

### 2.1: Add New Service
1. Go to your Railway project: https://railway.app
2. Click **"+ New"** button
3. Select **"GitHub Repo"**
4. Choose the **same repository** you're using for production
5. Railway will detect the Dockerfile automatically

### 2.2: Configure Service Settings
1. **Name the service**: `flow-platform-staging`
2. Go to **Settings** tab
3. Under **Source**:
   - **Branch**: Select `staging` (not `main`)
   - **Root Directory**: `flow-api`
4. Save settings

### 2.3: Add PostgreSQL Database
1. In the same Railway project, click **"+ New"**
2. Select **"Database"** → **"Add PostgreSQL"**
3. Name it: `flow-db-staging`
4. **Link it to `flow-platform-staging` service**:
   - Go to `flow-platform-staging` → **Settings**
   - Scroll to **Service Dependencies** (or **Connected Services**)
   - Click **"Add Service"** or **"Connect Database"**
   - Select `flow-db-staging`
   - Railway will **automatically** set `DATABASE_URL`

### 2.4: Generate Domain
1. Go to `flow-platform-staging` → **Settings**
2. Click **"Generate Domain"**
3. Copy the URL (should be something like `flow-platform-staging.up.railway.app`)

## Step 3: Set Environment Variables

### 3.1: Get Database Password
1. Go to `flow-db-staging` → **Variables** tab
2. Find `PGPASSWORD` or check the connection string
3. Copy the password value

### 3.2: Generate Secrets (Different from Production!)
Run these commands locally to generate NEW secrets for staging:

```bash
# Generate JWT_SECRET (different from production!)
openssl rand -hex 32

# Generate MASTER_ENCRYPTION_KEY (different from production!)
openssl rand -hex 32
```

**Important**: Use DIFFERENT secrets than production for security!

### 3.3: Set Variables in Railway
Go to `flow-platform-staging` → **Variables** tab and add:

```
ENVIRONMENT=staging
DATABASE_USER=postgres
DATABASE_PASSWORD=<from flow-db-staging PGPASSWORD>
JWT_SECRET=<generate new with: openssl rand -hex 32>
MASTER_ENCRYPTION_KEY=<generate new with: openssl rand -hex 32>
```

**Note**: `DATABASE_URL` should be **automatically set** by Railway when you link the database. Don't set it manually!

## Step 4: Verify Deployment

### 4.1: Check Deployment Logs
1. Go to `flow-platform-staging` → **Deployments** tab
2. Wait for deployment to complete
3. Check logs for:
   - "Database tables created successfully"
   - No errors

### 4.2: Test Health Endpoint
```bash
curl https://flow-platform-staging.up.railway.app/health
```

Should return:
```json
{
  "status": "healthy",
  "timestamp": "..."
}
```

### 4.3: Test Production (for comparison)
```bash
curl https://flow-platform-production.up.railway.app/health
```

Both should work!

## Step 5: Update Frontend Config (Optional)

If you want the frontend to default to staging when on a staging domain, the `config.js` already handles this automatically.

## Troubleshooting

### Staging not deploying?
- Check that `staging` branch exists and has been pushed
- Verify Railway service is set to deploy from `staging` branch
- Check Railway logs for errors

### Database connection issues?
- Ensure `flow-db-staging` is linked to `flow-platform-staging`
- Verify `DATABASE_URL` is auto-set by Railway
- Check that `DATABASE_PASSWORD` matches the database service

### Different secrets?
- Make sure you generated NEW secrets for staging (not the same as production)
- This is important for security - staging and production should have separate keys

## Next Steps

After staging is set up:
1. Test creating an account on staging
2. Test API calls with staging tokens
3. Verify staging uses testnet (Sepolia) and production uses mainnet
4. Both environments should work independently!

