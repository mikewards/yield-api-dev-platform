# Railway Build Fix

The error indicates Railway can't find `flow-api/src`. This means Railway might be building from a subdirectory.

## Solution: Set Root Directory in Railway

1. **Go to your Railway dashboard**
2. **Click on your service** (the API service)
3. **Go to Settings** → **Build & Deploy**
4. **Set "Root Directory"** to: `.` (root) or leave it empty
5. **Make sure "Dockerfile Path"** is: `Dockerfile`
6. **Save and redeploy**

## Alternative: Use flow-api as Root

If the above doesn't work, you can set Railway to build from `flow-api`:

1. **Set "Root Directory"** to: `flow-api`
2. Railway will use `flow-api/Dockerfile` automatically
3. But then we need to update the Dockerfile paths

## Quick Fix: Update Railway Settings

In Railway dashboard:
- Service → Settings → Build & Deploy
- Root Directory: `.` (or empty for root)
- Dockerfile Path: `Dockerfile`
- Save and redeploy

