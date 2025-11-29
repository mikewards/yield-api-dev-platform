# Fix Railway Service Names - Step by Step

## Problem
Railway services are being renamed unexpectedly. We need to ensure:
- **Staging**: `flow-platform-staging` → deploys from `staging` branch
- **Production**: `flow-platform-production` → deploys from `main` branch

## Solution: Fix Each Service Individually

### Step 1: Verify Current State

1. Go to Railway dashboard: https://railway.app
2. Check your project
3. Note the current names of both services

### Step 2: Fix Staging Service (`flow-platform-staging`)

1. **Click on the staging service** (the one that should deploy from `staging` branch)
2. **Settings** → **General**:
   - **Service Name**: Change to `flow-platform-staging`
   - **Description**: "Staging environment - deploys from staging branch"
3. **Settings** → **Source**:
   - **Branch**: Must be `staging`
   - **Root Directory**: Leave empty (or set to `.` if needed)
   - **Auto Deploy**: Should be **ON**
4. **Click "Save"** or "Update"

### Step 3: Fix Production Service (`flow-platform-production`)

1. **Click on the production service** (the one that should deploy from `main` branch)
2. **Settings** → **General**:
   - **Service Name**: Change to `flow-platform-production`
   - **Description**: "Production environment - deploys from main branch"
3. **Settings** → **Source**:
   - **Branch**: Must be `main`
   - **Root Directory**: Leave empty (or set to `.` if needed)
   - **Auto Deploy**: Should be **ON**
4. **Click "Save"** or "Update"

### Step 4: Verify Database Names

1. **For Staging Database**:
   - Click on `flow-DB-staging` (or the staging database)
   - **Settings** → **General**:
     - **Service Name**: `flow-DB-staging`
   - **Settings** → **Variables**:
     - Copy the `DATABASE_URL` value
   - **Settings** → **Linked Services**:
     - Should be linked to `flow-platform-staging`

2. **For Production Database**:
   - Click on `flow-DB-production` (or the production database)
   - **Settings** → **General**:
     - **Service Name**: `flow-DB-production`
   - **Settings** → **Variables**:
     - Copy the `DATABASE_URL` value
   - **Settings** → **Linked Services**:
     - Should be linked to `flow-platform-production`

### Step 5: Verify Environment Variables

**For `flow-platform-staging` service:**
- Go to **Variables** tab
- Ensure these are set:
  ```
  ENVIRONMENT=staging
  DATABASE_USER=postgres
  DATABASE_PASSWORD=<from flow-DB-staging>
  DATABASE_URL=<auto-set from flow-DB-staging>
  JWT_SECRET=<staging-specific secret>
  MASTER_ENCRYPTION_KEY=<staging-specific secret>
  ```

**For `flow-platform-production` service:**
- Go to **Variables** tab
- Ensure these are set:
  ```
  ENVIRONMENT=production
  DATABASE_USER=postgres
  DATABASE_PASSWORD=<from flow-DB-production>
  DATABASE_URL=<auto-set from flow-DB-production>
  JWT_SECRET=<production-specific secret>
  MASTER_ENCRYPTION_KEY=<production-specific secret>
  ```

### Step 6: Trigger Manual Deployment

After fixing the names and branch settings:

1. **For Staging**:
   - Go to `flow-platform-staging` → **Deployments** tab
   - Click **"Redeploy"** or **"Deploy Latest"**
   - Verify it's deploying from `staging` branch

2. **For Production**:
   - Go to `flow-platform-production` → **Deployments** tab
   - Click **"Redeploy"** or **"Deploy Latest"**
   - Verify it's deploying from `main` branch

### Step 7: Verify Deployment

**Check Staging:**
```bash
curl https://flow-platform-staging.up.railway.app/health
```

**Check Production:**
```bash
curl https://flow-platform-production.up.railway.app/health
```

## Troubleshooting

### If Names Keep Changing Back

1. **Check for duplicate services**: Railway might have created duplicates
2. **Delete and recreate**: If names keep syncing, delete one service and recreate it with the correct name
3. **Check Railway project settings**: There might be a project-level setting affecting names

### If Deployments Aren't Triggering

1. **Check branch settings**: Ensure each service is pointing to the correct branch
2. **Check GitHub connection**: Verify Railway is connected to your GitHub repo
3. **Manually trigger**: Use "Redeploy" button in Railway dashboard
4. **Check logs**: Look for errors in the deployment logs

### If You Can't Find Settings

Railway UI might vary. Look for:
- **Settings** tab (most common)
- **Configure** button
- **Service menu** (three dots ⋮) → **Settings**
- **General** section within Settings

## Quick Checklist

- [ ] Staging service named: `flow-platform-staging`
- [ ] Staging service branch: `staging`
- [ ] Production service named: `flow-platform-production`
- [ ] Production service branch: `main`
- [ ] Staging database named: `flow-DB-staging`
- [ ] Production database named: `flow-DB-production`
- [ ] Each service linked to correct database
- [ ] Environment variables set correctly
- [ ] Both services have "Auto Deploy" enabled
- [ ] Manual deployment triggered to verify

