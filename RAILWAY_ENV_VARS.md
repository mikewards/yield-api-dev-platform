# Railway Environment Variables Setup

## ⚠️ Required: Set These Environment Variables in Railway

Your application is failing because these environment variables are missing. Set them in Railway:

### Step 1: Go to Railway Dashboard
1. Click on your **service** (the API service)
2. Go to **Variables** tab
3. Add these variables:

### Required Variables:

```
DATABASE_URL=<Railway auto-sets this from PostgreSQL service>
DATABASE_USER=postgres
DATABASE_PASSWORD=<Get from your PostgreSQL service>
JWT_SECRET=<Generate: openssl rand -hex 32>
MASTER_ENCRYPTION_KEY=<Generate: openssl rand -hex 32>
```

### Step 2: Get Database Password

1. In Railway, click on your **PostgreSQL database service**
2. Go to **Variables** tab
3. Find `PGPASSWORD` or check the connection string
4. Copy the password value

### Step 3: Generate Secrets

Run these commands locally to generate secrets:

```bash
# Generate JWT_SECRET
openssl rand -hex 32

# Generate MASTER_ENCRYPTION_KEY  
openssl rand -hex 32
```

Copy each output and paste into Railway variables.

### Step 4: Set Variables in Railway

1. **Service** → **Variables** tab
2. Click **"New Variable"** for each:
   - `DATABASE_USER` = `postgres`
   - `DATABASE_PASSWORD` = (from PostgreSQL service)
   - `JWT_SECRET` = (generated value)
   - `MASTER_ENCRYPTION_KEY` = (generated value)

**Note:** `DATABASE_URL` should be **automatically set** by Railway when you link the PostgreSQL service. If it's not there:
- Make sure PostgreSQL service is linked to your API service
- Or manually set `DATABASE_URL` from the PostgreSQL service connection string

### Step 5: Redeploy

After setting variables, Railway should auto-redeploy. If not, trigger a manual redeploy.

## Quick Copy-Paste for Railway Variables

```
DATABASE_USER=postgres
DATABASE_PASSWORD=<from PostgreSQL>
JWT_SECRET=<generate with: openssl rand -hex 32>
MASTER_ENCRYPTION_KEY=<generate with: openssl rand -hex 32>
```

## Verify Variables Are Set

After setting, check the **Variables** tab shows all variables listed above.

## Troubleshooting

### "DATABASE_URL not found"
- Ensure PostgreSQL service is linked to your API service
- Check PostgreSQL service → Variables for connection details
- Manually set `DATABASE_URL` if needed

### "Still getting config errors"
- Verify all variables are set (no typos)
- Check variable names match exactly (case-sensitive)
- Redeploy after setting variables

