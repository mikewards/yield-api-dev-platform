# Railway Staging Setup - Using Duplicate Environment

## ✅ Yes, Duplicate Production Environment!

Railway's "Duplicate Environment" feature is a great way to quickly set up staging. Here's how:

## Step 1: Duplicate Production Environment

1. Go to Railway dashboard: https://railway.app
2. Find your **production environment** (or the service `flow-platform-production`)
3. Click the **"..." menu** (three dots) or look for **"Duplicate"** option
4. Select **"Duplicate Environment"** or **"Copy Environment"**
5. Railway will create a copy with all services and variables

## Step 2: Rename the Duplicated Service

1. The duplicated service will likely be named something like `flow-platform-production-copy`
2. Click on it → **Settings** → **Name**
3. Rename it to: `flow-platform-staging`

## Step 3: Update Branch Configuration ⚠️ CRITICAL

1. Go to `flow-platform-staging` → **Settings**
2. Under **Source** → **Branch**:
   - **Change from `main` to `staging`** ⚠️
   - This is CRITICAL - otherwise it will deploy production code!
3. **Root Directory**: Should already be `flow-api` (keep it)

## Step 4: Update Environment Variables ⚠️ CRITICAL

Go to `flow-platform-staging` → **Variables** tab and update:

### Must Change:
```
ENVIRONMENT=staging  ← Change from "production" to "staging"
```

### Must Generate NEW Secrets:
```
JWT_SECRET=4f9cf977a66b1cbd429a6e983978f31b62f681ca8122c67538695a9572bbb0ae
MASTER_ENCRYPTION_KEY=a03b219f5f465078191bc942780460e3ecc4d7c8ae1d7bcbfbf2cfc4c44551e6
```

**Important**: 
- Delete the OLD production secrets
- Add the NEW staging secrets above
- Staging and production MUST have different secrets!

### Keep As-Is (Railway auto-manages):
```
DATABASE_URL  ← Railway will auto-set this when you link the database
DATABASE_USER=postgres  ← Keep this
```

### Update Database Password:
```
DATABASE_PASSWORD=<get from flow-db-staging PGPASSWORD>
```

1. Go to the duplicated database (likely named `flow-db-production-copy`)
2. Rename it to: `flow-db-staging`
3. Go to `flow-db-staging` → **Variables** tab
4. Find `PGPASSWORD`
5. Copy the value
6. Go back to `flow-platform-staging` → **Variables**
7. Update `DATABASE_PASSWORD` with the value from `flow-db-staging`

## Step 5: Verify Database Link

1. Go to `flow-platform-staging` → **Settings**
2. Scroll to **Service Dependencies** (or **Connected Services**)
3. Verify `flow-db-staging` is linked
4. If not linked, click **"Add Service"** → Select `flow-db-staging`
5. Railway will auto-set `DATABASE_URL`

## Step 6: Generate Domain

1. Go to `flow-platform-staging` → **Settings**
2. Click **"Generate Domain"**
3. Should be: `flow-platform-staging.up.railway.app`

## Step 7: Verify Deployment

1. Railway should automatically deploy from the `staging` branch
2. Go to `flow-platform-staging` → **Deployments** tab
3. Wait for deployment to complete
4. Check logs for: "Database tables created successfully"

## ✅ Quick Checklist

After duplicating, make sure you:

- [ ] Renamed service to `flow-platform-staging`
- [ ] Renamed database to `flow-db-staging`
- [ ] Changed branch from `main` to `staging` ⚠️
- [ ] Changed `ENVIRONMENT=production` to `ENVIRONMENT=staging` ⚠️
- [ ] Replaced `JWT_SECRET` with new staging secret ⚠️
- [ ] Replaced `MASTER_ENCRYPTION_KEY` with new staging secret ⚠️
- [ ] Updated `DATABASE_PASSWORD` from `flow-db-staging`
- [ ] Verified database is linked
- [ ] Generated domain
- [ ] Tested health endpoint

## 🎯 Summary

**Duplicating is faster**, but you MUST:
1. Change branch to `staging`
2. Change `ENVIRONMENT` to `staging`
3. Generate NEW secrets (don't reuse production secrets!)

The database will be separate automatically (which is what we want), but make sure it's properly linked to the staging service.

## Test Both Environments

```bash
# Staging
curl https://flow-platform-staging.up.railway.app/health

# Production
curl https://flow-platform-production.up.railway.app/health
```

Both should work independently! 🚀

