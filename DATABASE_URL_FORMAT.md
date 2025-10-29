# DATABASE_URL Format Fix

## The Problem

Your `DATABASE_URL` is malformed. The error shows it's trying to connect to:
```
postgres:password@flow-db.railway.internal
```

This is missing the `postgresql://` or `jdbc:postgresql://` prefix.

## Correct DATABASE_URL Format

### From Railway PostgreSQL Service

Railway provides `DATABASE_URL` in this format:
```
postgresql://postgres:password@flow-db.railway.internal:5432/railway
```

### What You Need in Railway Variables

**DATABASE_URL** should be the **complete value** from PostgreSQL service, like:
```
postgresql://postgres:uygHhRolnxtXPbZCsFfHjuTKWzZudoVn@flow-db.railway.internal:5432/railway
```

## How to Fix

### Step 1: Get Correct DATABASE_URL

1. **PostgreSQL service (flow-db)** → **Variables** tab
2. **Find `DATABASE_URL`** (or `POSTGRES_URL`)
3. **Copy the ENTIRE value** - it should start with `postgresql://`

### Step 2: Update in API Service

1. **API service (flow-platform)** → **Variables** tab
2. **Find `DATABASE_URL`**
3. **Edit** and paste the **complete value** from PostgreSQL service
4. **Save**

## What It Should Look Like

✅ **Correct:**
```
postgresql://postgres:password123@flow-db.railway.internal:5432/railway
```

❌ **Wrong (what you have):**
```
postgres:password@flow-db.railway.internal
```

## Common Mistakes

1. **Missing `postgresql://` prefix** - Make sure it starts with `postgresql://`
2. **Only copying part of the URL** - Copy the entire value
3. **Adding extra spaces** - No spaces in the URL
4. **Using placeholder text** - Must be the actual value from PostgreSQL service

## Verify

After updating, the URL should:
- Start with `postgresql://`
- Contain `@` (separates credentials from host)
- Contain `:` (separates host from port)
- End with database name (usually `/railway`)

## Still Having Issues?

Check Railway logs - the new error message will show what format it received, which will help debug.

