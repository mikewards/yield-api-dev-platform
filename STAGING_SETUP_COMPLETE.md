# Staging Setup - What's Done & What's Next

## ✅ Completed Steps

1. **Staging branch created and pushed**
   - Branch: `staging`
   - Pushed to: `origin/staging`
   - Status: ✅ Ready for Railway deployment

## 🔑 Generated Secrets for Staging

I've generated unique secrets for your staging environment. **Use these in Railway**:

```
JWT_SECRET=4f9cf977a66b1cbd429a6e983978f31b62f681ca8122c67538695a9572bbb0ae
MASTER_ENCRYPTION_KEY=a03b219f5f465078191bc942780460e3ecc4d7c8ae1d7bcbfbf2cfc4c44551e6
```

**Important**: These are DIFFERENT from production for security!

## 📋 Next Steps in Railway Dashboard

### Step 1: Create Staging Service
1. Go to https://railway.app
2. Open your project (the one with `flow-platform-production`)
3. Click **"+ New"** → **"GitHub Repo"**
4. Select the **same repository** (`wardmic4/flow-platform`)
5. Railway will auto-detect the Dockerfile
6. **Name it**: `flow-platform-staging`

### Step 2: Configure Branch
1. Go to `flow-platform-staging` → **Settings**
2. Under **Source**:
   - **Branch**: Select `staging` (NOT `main`)
   - **Root Directory**: `flow-api`
3. Save

### Step 3: Create Staging Database
1. In the same Railway project, click **"+ New"**
2. Select **"Database"** → **"Add PostgreSQL"**
3. **Name it**: `flow-db-staging`
4. **Link it**:
   - Go to `flow-platform-staging` → **Settings**
   - Scroll to **Service Dependencies**
   - Click **"Add Service"** or **"Connect Database"**
   - Select `flow-db-staging`
   - Railway will auto-set `DATABASE_URL`

### Step 4: Get Database Password
1. Go to `flow-db-staging` → **Variables** tab
2. Find `PGPASSWORD`
3. Copy the value

### Step 5: Set Environment Variables
Go to `flow-platform-staging` → **Variables** tab and add:

```
ENVIRONMENT=staging
DATABASE_USER=postgres
DATABASE_PASSWORD=<paste from flow-db-staging PGPASSWORD>
JWT_SECRET=4f9cf977a66b1cbd429a6e983978f31b62f681ca8122c67538695a9572bbb0ae
MASTER_ENCRYPTION_KEY=a03b219f5f465078191bc942780460e3ecc4d7c8ae1d7bcbfbf2cfc4c44551e6
```

**Note**: `DATABASE_URL` should appear automatically after linking the database. Don't set it manually!

### Step 6: Generate Domain
1. Go to `flow-platform-staging` → **Settings**
2. Click **"Generate Domain"**
3. Copy the URL (should be `flow-platform-staging.up.railway.app`)

### Step 7: Wait for Deployment
1. Railway will automatically start deploying
2. Go to `flow-platform-staging` → **Deployments** tab
3. Wait for deployment to complete
4. Check logs for: "Database tables created successfully"

## ✅ Verification

Once deployed, test both environments:

### Test Staging:
```bash
curl https://flow-platform-staging.up.railway.app/health
```

### Test Production:
```bash
curl https://flow-platform-production.up.railway.app/health
```

Both should return:
```json
{
  "status": "healthy",
  "timestamp": "..."
}
```

## 🎯 Summary

- ✅ Staging branch created and pushed
- ✅ Secrets generated for staging
- ⏳ **You need to**: Set up Railway staging service (follow steps above)
- ⏳ **You need to**: Configure environment variables in Railway
- ⏳ **You need to**: Test both environments

## 📚 Reference

- Full guide: `RAILWAY_STAGING_PRODUCTION_SETUP.md`
- Quick guide: `RAILWAY_STAGING_QUICK_SETUP.md`

Once you complete the Railway setup steps above, both staging and production will be fully operational! 🚀

