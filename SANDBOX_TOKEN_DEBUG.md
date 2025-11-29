# Sandbox Token Debugging

## Issue
Sandbox tokens are returning 401 Unauthorized for both `/v1/markets` and `/v1/yield/rates` endpoints.

## Token Format
- Sandbox tokens: `tbd_sand_...` (e.g., `tbd_sand_a9d6c0f7a8ba47d0aaee42dc543b0dd1`)
- Production tokens: `tbd_prod_...` (e.g., `tbd_prod_4488510868e549a48a80221003395342`)

## Current Status
- ✅ Production: Both endpoints work
- ❌ Sandbox: Both endpoints return 401

## Debugging Steps

### 1. Check Railway Logs
Go to Railway dashboard → `flow-platform-staging` → **Logs** tab

Look for these messages when making a request:
- `✅ PAT token validated for account: ...` (success)
- `❌ Token validation failed - token prefix: ...` (failure)
- `❌ Authentication error: ...` (error)

### 2. Verify Token in Database
The token should exist in the `access_tokens` table in the staging database with:
- `token = 'tbd_sand_a9d6c0f7a8ba47d0aaee42dc543b0dd1'`
- `environment = 'sandbox'`
- `expires_at` should be NULL or in the future

### 3. Check Database Connection
Verify the staging database is connected:
- Railway dashboard → `flow-platform-staging` → **Variables** tab
- Check `DATABASE_URL` is set correctly
- Verify it points to `flow-DB-staging` (or the staging database)

### 4. Test Token Validation
The `validateToken` function:
1. Looks up token in `access_tokens` table
2. Checks if token exists
3. Checks if token is expired
4. Returns the token row if valid

If this fails, check:
- Database connection is working
- Token exists in staging database (not production)
- Token hasn't expired

## Next Steps
1. Check Railway logs for authentication debug messages
2. Verify token exists in staging database
3. If token doesn't exist, create it via the dashboard
4. If token exists but still fails, check database connection

