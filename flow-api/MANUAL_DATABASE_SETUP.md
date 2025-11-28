# Manual Database Setup

Since `psql` isn't in your PATH, here are ways to create the database:

## Option 1: Find PostgreSQL and Add to PATH

### If using Postgres.app:
```bash
# Find the latest version
ls /Applications/Postgres.app/Contents/Versions/

# Add to PATH (add to ~/.zshrc or ~/.bash_profile)
export PATH="/Applications/Postgres.app/Contents/Versions/latest/bin:$PATH"

# Then create database
createdb flow_api
```

### If using Homebrew PostgreSQL:
```bash
# Add to PATH
export PATH="/opt/homebrew/bin:$PATH"

# Create database
createdb flow_api
```

### If using standard installation:
```bash
# Find PostgreSQL
find /usr/local -name psql 2>/dev/null

# Add to PATH
export PATH="/usr/local/pgsql/bin:$PATH"

# Create database
createdb flow_api
```

## Option 2: Use Full Path

```bash
# Postgres.app
/Applications/Postgres.app/Contents/Versions/latest/bin/psql -U postgres -c "CREATE DATABASE flow_api;"

# Homebrew
/opt/homebrew/bin/psql -U postgres -c "CREATE DATABASE flow_api;"
```

## Option 3: Use pgAdmin (GUI)

1. Open pgAdmin
2. Connect to your PostgreSQL server
3. Right-click "Databases" → "Create" → "Database"
4. Name it: `flow_api`
5. Click "Save"

## Option 4: Let the App Create It (If Permissions Allow)

The application will try to create tables, but you need the database first. If you have superuser access, you can try:

```bash
# Connect as postgres user and create
psql -U postgres
CREATE DATABASE flow_api;
\q
```

## Verify Database Created

Once created, verify:
```bash
# If psql is available
psql -U postgres -l | grep flow_api

# Or connect to it
psql -U postgres -d flow_api -c "SELECT 1;"
```

## Next Steps

After creating the database:

1. Update `.env` file with your PostgreSQL password:
```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/flow_api
DATABASE_USER=postgres
DATABASE_PASSWORD=your_actual_password
JWT_SECRET=your_jwt_secret_from_setup
```

2. Run the application:
```bash
./run.sh
```
