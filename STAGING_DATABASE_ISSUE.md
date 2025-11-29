# Staging Database Issue

## Problem
Sandbox tokens return 401 Unauthorized because staging is connecting to a **different database** than production.

## Root Cause
- **Production service** → Connects to **Production database** → Has production tokens ✅
- **Staging service** → Connects to **Staging database** → Doesn't have tokens (they're in production DB) ❌

## Solution Options

### Option 1: Use Same Database (Quick Fix)
Point staging service to the production database:

1. Railway dashboard → `flow-platform-staging` → **Variables** tab
2. Find `DATABASE_URL`
3. Copy the `DATABASE_URL` from `flow-platform-production` service
4. Paste it into `flow-platform-staging` service
5. Also copy `DATABASE_USER` and `DATABASE_PASSWORD` if they're different

**Pros:** Quick fix, tokens work immediately
**Cons:** Staging and production share the same data (not ideal for testing)

### Option 2: Copy Tokens to Staging Database (Proper Solution)
Create tokens in the staging database:

1. Create a new sandbox token via the dashboard
2. Ensure it's created in the staging environment
3. Or manually insert tokens into staging database

**Pros:** Proper separation of environments
**Cons:** Need to manage tokens in both databases

### Option 3: Check Current Database Connection
Verify which database staging is using:

1. Railway dashboard → `flow-platform-staging` → **Logs** tab
2. Look for: `🔍 DEBUG: Parsed - host=..., port=..., database=...`
3. Compare with production logs to see if they're different databases

## Recommended: Option 1 (Quick Fix)
For now, use the same database for both environments. Later, we can set up proper token management.

