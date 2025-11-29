# Database Tables Not Created - Fix Guide

## Why Tables Are Missing

The database has no tables because:
1. **The app hasn't started successfully yet** - It's failing on missing environment variables
2. **Once the app starts**, it will automatically create all tables

## Solution: Set Environment Variables First

### Step 1: Set Required Variables in Railway

Go to your **API service** → **Variables** tab and add:

```
DATABASE_URL=<from PostgreSQL service>
DATABASE_USER=postgres
DATABASE_PASSWORD=<from PostgreSQL service>
JWT_SECRET=<generate: openssl rand -hex 32>
MASTER_ENCRYPTION_KEY=<generate: openssl rand -hex 32>
```

### Step 2: Get Database Connection Info

1. Click on your **PostgreSQL service** (flow-db)
2. Go to **Variables** tab
3. Find `DATABASE_URL` or connection string
4. Copy the values

### Step 3: Redeploy

After setting variables:
- Railway will auto-redeploy
- App will start successfully
- Tables will be created automatically

## Verify Tables Are Created

After the app starts successfully:

1. Go to your **PostgreSQL service** in Railway
2. Click **"Query"** or **"Data"** tab
3. You should see these tables:
   - `accounts`
   - `applications`
   - `application_wallets`
   - `access_tokens`
   - `yield_accounts`
   - `positions`
   - `transactions`
   - `webhooks`

## If Tables Still Don't Appear

### Check App Logs

1. Go to your **API service** → **Deployments** → **Latest deployment** → **Logs**
2. Look for:
   - ✅ "Database tables created successfully" = Good!
   - ❌ Any database errors = Need to fix

### Manual Table Creation (If Needed)

If tables still don't create, you can run this SQL in Railway's PostgreSQL query editor:

```sql
-- This will be created automatically, but if needed:
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

But the app should create all tables automatically once it starts!

## Current Status

- ✅ Code is ready to create tables
- ⚠️ App needs environment variables to start
- ⚠️ Once app starts → tables will be created automatically

**Next Step:** Set the environment variables in Railway, then the app will start and create tables!

