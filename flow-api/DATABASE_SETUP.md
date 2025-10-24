# Database Setup Guide

## PostgreSQL Location

PostgreSQL 18 is installed at:
```
/Library/PostgreSQL/18/bin/
```

## Create Database - Option 1: Automated Script

Run the automated script:
```bash
cd flow-api
./create-database.sh
```

This will:
- Check if database already exists
- Try multiple authentication methods
- Prompt for password if needed

## Create Database - Option 2: Manual (with password)

If you know your PostgreSQL password:

```bash
# Set password as environment variable
export PGPASSWORD='your_postgres_password'

# Create database
/Library/PostgreSQL/18/bin/psql -U postgres -c "CREATE DATABASE flow_api;"

# Verify it was created
/Library/PostgreSQL/18/bin/psql -U postgres -l | grep flow_api
```

## Create Database - Option 3: Interactive psql

```bash
# Connect to PostgreSQL
/Library/PostgreSQL/18/bin/psql -U postgres

# In psql prompt:
CREATE DATABASE flow_api;

# Verify
\l

# Exit
\q
```

## Create Database - Option 4: Using pgAdmin (GUI)

1. Open pgAdmin (if installed)
2. Connect to your PostgreSQL server
3. Right-click "Databases" → "Create" → "Database"
4. Name: `flow_api`
5. Click "Save"

## Finding Your PostgreSQL Password

If you don't remember your PostgreSQL password:

### Option A: Reset Password
```bash
# Connect as superuser (if you have sudo access)
sudo -u postgres /Library/PostgreSQL/18/bin/psql

# In psql:
ALTER USER postgres PASSWORD 'new_password';
\q
```

### Option B: Check Installation Notes
- Check if you wrote it down during installation
- Check if it's in your password manager
- Default might be empty (try no password)

### Option C: Use Trust Authentication (Development Only)
Edit PostgreSQL config to allow local connections without password:
```bash
# Edit pg_hba.conf (location may vary)
sudo nano /Library/PostgreSQL/18/data/pg_hba.conf

# Change this line:
# host    all             all             127.0.0.1/32            md5
# To:
# host    all             all             127.0.0.1/32            trust

# Restart PostgreSQL
sudo launchctl unload /Library/LaunchDaemons/com.edb.launchd.postgresql-18.plist
sudo launchctl load /Library/LaunchDaemons/com.edb.launchd.postgresql-18.plist
```

⚠️ **Warning**: Trust authentication is NOT secure for production!

## Verify Database Created

```bash
/Library/PostgreSQL/18/bin/psql -U postgres -l | grep flow_api
```

Should show:
```
 flow_api  | postgres | UTF8     | en_US.UTF-8 | en_US.UTF-8 |
```

## Update .env File

After creating the database, update your `.env` file:

```bash
cd flow-api
nano .env
```

Make sure it has:
```
DATABASE_URL=jdbc:postgresql://localhost:5432/flow_api
DATABASE_USER=postgres
DATABASE_PASSWORD=your_actual_password
```

If your PostgreSQL has no password, you can leave `DATABASE_PASSWORD` empty or remove the line.

## Test Connection

Test the connection from your Kotlin app:
```bash
cd flow-api
source "$HOME/.sdkman/bin/sdkman-init.sh"
./gradlew run
```

The app will automatically create tables when it starts!
