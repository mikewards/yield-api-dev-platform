# Fix Railway Environment Variables - Step by Step

## Current Problem

Your variables have placeholder text instead of real values:
- `DATABASE_PASSWORD: <Get from PostgreSQL service>` ❌
- `JWT_SECRET: <generate: openssl rand -hex 32>` ❌
- `MASTER_ENCRYPTION_KEY: <generate: openssl rand -hex 32>` ❌

## Step-by-Step Fix

### Step 1: Get DATABASE_PASSWORD

1. **Go to your PostgreSQL service** (flow-db) in Railway
2. **Click "Variables" tab**
3. **Look for one of these:**
   - `PGPASSWORD`
   - `POSTGRES_PASSWORD`
   - `DATABASE_PASSWORD`
   - Or check the connection string in `DATABASE_URL` or `POSTGRES_URL`
4. **Copy the password value**

### Step 2: Generate JWT_SECRET

I've generated one for you below. Copy it.

### Step 3: Generate MASTER_ENCRYPTION_KEY

I've generated one for you below. Copy it.

### Step 4: Update Variables in Railway

1. **Go to your API service** (flow-platform)
2. **Variables tab**
3. **For each variable, click "Edit" and replace the placeholder:**

**DATABASE_PASSWORD:**
- Remove: `<Get from PostgreSQL service>`
- Paste: (the password from PostgreSQL service)

**JWT_SECRET:**
- Remove: `<generate: openssl rand -hex 32>`
- Paste: (the generated secret below)

**MASTER_ENCRYPTION_KEY:**
- Remove: `<generate: openssl rand -hex 32>`
- Paste: (the generated secret below)

### Step 5: Also Check DATABASE_URL

Make sure `DATABASE_URL` is set to the actual value from PostgreSQL service, not placeholder text!

## Generated Secrets

Use these values (generated securely):

**JWT_SECRET:**
```
[See output from openssl command]
```

**MASTER_ENCRYPTION_KEY:**
```
[See output from openssl command]
```

## Quick Checklist

- [ ] DATABASE_URL = actual value from PostgreSQL (not placeholder)
- [ ] DATABASE_USER = `postgres` (correct)
- [ ] DATABASE_PASSWORD = actual password (not `<Get from...>`)
- [ ] JWT_SECRET = generated secret (not `<generate:...>`)
- [ ] MASTER_ENCRYPTION_KEY = generated secret (not `<generate:...>`)

After updating all variables, Railway will auto-redeploy and the app should start!

