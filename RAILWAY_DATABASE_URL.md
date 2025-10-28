# How to Get DATABASE_URL from Railway

## The Problem

You set `DATABASE_URL` to the placeholder text `<Railway should auto-set this from PostgreSQL>` instead of the actual database connection string.

## Solution: Get the Real DATABASE_URL

### Step 1: Go to PostgreSQL Service

1. In Railway dashboard, click on your **PostgreSQL service** (flow-db)
2. Go to **Variables** tab
3. Look for one of these variables:
   - `DATABASE_URL` (this is what you need!)
   - `POSTGRES_URL`
   - `POSTGRES_PRIVATE_URL`

### Step 2: Copy the Actual Value

The value should look like:
```
postgresql://postgres:password@hostname:5432/railway
```

Or it might be a full JDBC URL:
```
jdbc:postgresql://hostname:5432/railway?user=postgres&password=xxxxx
```

### Step 3: Update DATABASE_URL in API Service

1. Go to your **API service** (flow-platform)
2. **Variables** tab
3. Find `DATABASE_URL`
4. **Edit** it and paste the **actual value** from PostgreSQL service
5. **Save**

### Step 4: If Railway Auto-Sets It

Railway can automatically set `DATABASE_URL` when you link services:

1. In your **API service** → **Settings**
2. Look for **"Connect Database"** or **"Add Database"**
3. Select your PostgreSQL service
4. Railway will automatically add `DATABASE_URL`

## What DATABASE_URL Should Look Like

✅ **Correct format:**
```
postgresql://postgres:password123@containers-us-west-123.railway.app:5432/railway
```

Or:
```
jdbc:postgresql://containers-us-west-123.railway.app:5432/railway?user=postgres&password=password123
```

❌ **Wrong (what you have now):**
```
<Railway should auto-set this from PostgreSQL>
```

## Quick Fix Steps

1. **PostgreSQL service** → **Variables** → Copy `DATABASE_URL` value
2. **API service** → **Variables** → Edit `DATABASE_URL` → Paste real value
3. **Save** → Railway will redeploy automatically

## Alternative: Link Services

If you haven't linked the services:

1. **API service** → **Settings** or **Variables**
2. Look for **"Connect Database"** or **"Add Database"** button
3. Select your PostgreSQL service
4. Railway will auto-set `DATABASE_URL`

After fixing this, the app should start successfully!

