# Railway Setup - Step by Step Guide

## The Problem
Your `DATABASE_URL` is **missing or empty** in the `flow-platform` service.

## Solution: Set DATABASE_URL in flow-platform

### Step 1: Get DATABASE_URL from PostgreSQL Service

1. **Go to Railway dashboard**
2. **Click on `flow-db`** (your PostgreSQL service)
3. **Click the "Variables" tab**
4. **Find `DATABASE_URL`** in the list
5. **Click the "Copy" button** (or manually select and copy the entire value)
   - It should look like: `postgresql://postgres:password@flow-db.railway.internal:5432/railway`
   - Make sure it starts with `postgresql://`

### Step 2: Set DATABASE_URL in API Service

1. **Go back to Railway dashboard**
2. **Click on `flow-platform`** (your API service)
3. **Click the "Variables" tab**
4. **Look for `DATABASE_URL`**:
   - If it exists: **Edit** it
   - If it doesn't exist: **Click "New Variable"** → Name: `DATABASE_URL`
5. **Paste the value** you copied from `flow-db`
6. **Remove any leading/trailing spaces** (the code will trim them, but best to remove manually)
7. **Click "Save"** or "Add"

### Step 3: Verify Other Variables

While you're in `flow-platform` → Variables, make sure you have:

- ✅ `DATABASE_URL` = (value from flow-db)
- ✅ `DATABASE_USER` = `postgres`
- ✅ `DATABASE_PASSWORD` = (password from flow-db, or extract from DATABASE_URL)
- ✅ `JWT_SECRET` = (generated secret, e.g., from `openssl rand -hex 32`)
- ✅ `MASTER_ENCRYPTION_KEY` = (generated secret, e.g., from `openssl rand -hex 32`)

### Step 4: Wait for Redeploy

Railway will automatically redeploy your service. Check the logs to see if it connects successfully.

## Quick Checklist

- [ ] Copied `DATABASE_URL` from `flow-db` → Variables
- [ ] Set `DATABASE_URL` in `flow-platform` → Variables
- [ ] Removed any leading/trailing spaces
- [ ] Verified it starts with `postgresql://`
- [ ] Saved the variable
- [ ] Checked Railway logs for successful connection

## Common Mistakes

1. **Setting DATABASE_URL in flow-db** ❌
   - Don't edit variables in `flow-db` - Railway manages those
   - Only **copy** from `flow-db`, **set** in `flow-platform`

2. **Empty or missing DATABASE_URL** ❌
   - Must be set in `flow-platform` service
   - Cannot be empty

3. **Wrong format** ❌
   - Must start with `postgresql://`
   - Must include `@` and `:`

4. **Leading/trailing spaces** ❌
   - Remove spaces before/after the URL
   - Code will trim, but best to remove manually

## Still Having Issues?

Check Railway logs - the new error messages will tell you exactly what's wrong:
- Missing → "DATABASE_URL is missing!"
- Empty → "DATABASE_URL is empty!"
- Wrong format → "DATABASE_URL format error!"

