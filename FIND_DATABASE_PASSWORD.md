# How to Find Database Password in Railway

## Railway PostgreSQL Service Variables

Railway's PostgreSQL service typically provides these variables. Check your **PostgreSQL service (flow-db)** → **Variables** tab for:

### Common Variable Names:

1. **`PGPASSWORD`** - Most common
2. **`POSTGRES_PASSWORD`** - Alternative name
3. **`DATABASE_PASSWORD`** - Sometimes used
4. **`POSTGRES_PASSWORD`** - Another variant

### Or Extract from DATABASE_URL

If you see `DATABASE_URL` or `POSTGRES_URL`, it contains the password:

Format: `postgresql://postgres:PASSWORD@hostname:5432/database`

The password is between `postgres:` and `@`

Example:
```
postgresql://postgres:mysecretpassword123@hostname:5432/railway
                                    ^^^^^^^^^^^^^^^^^^^^
                                    This is the password
```

## Quick Steps:

1. **PostgreSQL service** → **Variables** tab
2. **Look for any of these:**
   - `PGPASSWORD` ← Most likely
   - `POSTGRES_PASSWORD`
   - `DATABASE_PASSWORD`
   - Or extract from `DATABASE_URL`

3. **Copy the value** (it's the actual password, not a placeholder)

4. **Paste into your API service** → `DATABASE_PASSWORD` variable

## What to Look For:

✅ **Good (actual password):**
```
PGPASSWORD: abc123xyz789
```

❌ **Bad (placeholder):**
```
PGPASSWORD: <Get from PostgreSQL service>
```

## If You Can't Find It:

1. Check `DATABASE_URL` in PostgreSQL service
2. Extract password from the connection string
3. Or Railway might auto-set it when services are linked

