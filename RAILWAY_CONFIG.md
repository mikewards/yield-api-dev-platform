# Railway Configuration Guide

## Option 1: Use Railway CLI (Easiest)

Install Railway CLI and set the root directory:

```bash
# Install Railway CLI
npm i -g @railway/cli

# Login
railway login

# Link your project
railway link

# Set root directory to root (.)
railway variables set RAILWAY_ROOT_DIR=.
```

## Option 2: Railway Dashboard - Service Settings

1. **Go to your Railway project dashboard**
2. **Click on your service** (the one that's failing)
3. Look for one of these:
   - **"Settings"** tab → Look for "Root Directory" or "Build Settings"
   - **"Deploy"** tab → Look for build configuration
   - **"Variables"** tab → This is for environment variables, not build settings
   - **Service menu (three dots)** → Settings or Configure

## Option 3: Delete and Recreate Service

If you can't find the settings:

1. **Delete the current service** in Railway
2. **Create a new service** from the same GitHub repo
3. **When creating**, Railway might ask for:
   - Root Directory: Set to `.` (root) or `flow-api`
   - Dockerfile Path: `Dockerfile` or `flow-api/Dockerfile`

## Option 4: Use flow-api as Root (Simplest)

Instead of fixing the root directory, we can configure Railway to build from `flow-api`:

1. **In Railway dashboard**, when you create/configure the service
2. **Set the service root** to `flow-api` 
3. Railway will use `flow-api/Dockerfile` automatically
4. The existing `flow-api/Dockerfile` should work

## Option 5: Check Railway.json Location

Railway might be looking for `railway.json` in the wrong place. Try:

1. **Move `railway.json` to `flow-api/` directory**
2. **Or** ensure Railway knows to look in root

## What to Look For in Railway UI

Common locations for build settings:
- **Service Settings** → **Build** section
- **Deploy** tab → **Build Configuration**
- **Settings** → **Build & Deploy** (might be named differently)
- **Configure** button on the service
- **Service menu** (⋮) → **Settings**

## Quick Test

Try this: In Railway, look for any field that says:
- "Root Directory"
- "Working Directory" 
- "Build Path"
- "Source Directory"
- "Docker Context"

Set it to either:
- `.` (for root - then use root Dockerfile)
- `flow-api` (for flow-api directory - then use flow-api/Dockerfile)

