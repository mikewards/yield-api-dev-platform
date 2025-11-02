# Railway Fresh Start - Clean Setup

## Option 1: Reset Variables (Recommended - 5 minutes)

### Step 1: Clear flow-platform Variables
1. Go to Railway → `flow-platform` service
2. Click **Variables** tab
3. **Delete ALL existing variables** (or at least `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`)
4. We'll add them back correctly

### Step 2: Get Values from flow-db
1. Go to Railway → `flow-db` service
2. Click **Variables** tab
3. **Copy these values** (don't edit anything here):
   - `DATABASE_URL` → Copy the entire value
   - `PGPASSWORD` or `POSTGRES_PASSWORD` → Copy the password
   - `POSTGRES_USER` → Usually `postgres` (or copy if different)

### Step 3: Generate Secrets
Run these commands locally to generate secrets:
```bash
openssl rand -hex 32  # For JWT_SECRET
openssl rand -hex 32  # For MASTER_ENCRYPTION_KEY
```

### Step 4: Set All Variables in flow-platform
Go to `flow-platform` → Variables → Add these:

1. **DATABASE_URL**
   - Value: (paste from flow-db → DATABASE_URL)
   - Should start with `postgresql://`

2. **DATABASE_USER**
   - Value: `postgres`

3. **DATABASE_PASSWORD**
   - Value: (paste from flow-db → PGPASSWORD or POSTGRES_PASSWORD)

4. **JWT_SECRET**
   - Value: (paste first generated secret)

5. **MASTER_ENCRYPTION_KEY**
   - Value: (paste second generated secret)

### Step 5: Save and Wait
- Railway will auto-redeploy
- Check logs - should connect successfully!

---

## Option 2: Delete and Recreate (10 minutes)

If you want a completely clean slate:

### Step 1: Delete Services
1. Railway → `flow-platform` → Settings → Delete Service
2. Railway → `flow-db` → Settings → Delete Service

### Step 2: Create PostgreSQL Service
1. Railway → New → Database → PostgreSQL
2. Name it `flow-db`
3. Wait for it to provision

### Step 3: Create API Service
1. Railway → New → GitHub Repo → Select `flow-platform` repo
2. Name it `flow-platform`
3. Railway will auto-detect Dockerfile

### Step 4: Link Services
1. `flow-platform` → Settings → Add Service → `flow-db`
2. Railway will auto-set `DATABASE_URL` (but we'll verify)

### Step 5: Set Variables
Follow **Option 1, Step 2-4** above

---

## Quick Command to Generate Secrets

Run this locally:
```bash
echo "JWT_SECRET=$(openssl rand -hex 32)"
echo "MASTER_ENCRYPTION_KEY=$(openssl rand -hex 32)"
```

Copy the output values.

---

## What We're Setting

| Variable | Where to Get | Example |
|----------|-------------|---------|
| `DATABASE_URL` | flow-db → Variables | `postgresql://postgres:pass@flow-db.railway.internal:5432/railway` |
| `DATABASE_USER` | Always `postgres` | `postgres` |
| `DATABASE_PASSWORD` | flow-db → Variables → `PGPASSWORD` | `abc123...` |
| `JWT_SECRET` | Generate with `openssl rand -hex 32` | `a1b2c3...` |
| `MASTER_ENCRYPTION_KEY` | Generate with `openssl rand -hex 32` | `x9y8z7...` |

---

## After Setup

Check Railway logs - you should see:
```
✅ Database tables created successfully
```

If you see errors, the new error messages will tell you exactly what's wrong.

