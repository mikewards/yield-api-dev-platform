# Quick Database Creation

## ✅ PostgreSQL is Running!

PostgreSQL 18 is installed and running at:
```
/Library/PostgreSQL/18/bin/psql
```

## 🚀 Create Database (Choose One Method)

### Method 1: Run the Script (Easiest)

Open a terminal and run:
```bash
cd /Users/ed/Desktop/P1/flow-api
./create-database.sh
```

When prompted, enter your PostgreSQL password for user `postgres`.

### Method 2: Manual Command

Open a terminal and run:
```bash
# Set your password (replace 'your_password' with actual password)
export PGPASSWORD='your_password'

# Create database
/Library/PostgreSQL/18/bin/psql -U postgres -c "CREATE DATABASE flow_api;"

# Verify
/Library/PostgreSQL/18/bin/psql -U postgres -l | grep flow_api
```

### Method 3: Interactive psql

```bash
# Connect (will prompt for password)
/Library/PostgreSQL/18/bin/psql -U postgres

# In the psql prompt, type:
CREATE DATABASE flow_api;

# List databases to verify
\l

# Exit
\q
```

### Method 4: Using pgAdmin (GUI)

1. Open pgAdmin (if you have it installed)
2. Connect to your PostgreSQL server
3. Right-click "Databases" → "Create" → "Database"
4. Name: `flow_api`
5. Click "Save"

## 🔑 Finding Your PostgreSQL Password

If you don't remember your password:

1. **Check installation notes** - You may have written it down
2. **Try common defaults:**
   - Empty password (just press Enter)
   - `postgres`
   - `admin`
   - Your macOS user password

3. **Reset password** (if you have admin access):
   ```bash
   sudo -u postgres /Library/PostgreSQL/18/bin/psql
   ALTER USER postgres PASSWORD 'new_password';
   \q
   ```

## ✅ Verify Database Created

After creating, verify it exists:
```bash
/Library/PostgreSQL/18/bin/psql -U postgres -l | grep flow_api
```

You should see:
```
 flow_api  | postgres | UTF8     | ...
```

## 📝 Next Step: Update .env

After creating the database, update your `.env` file with the password:

```bash
cd /Users/ed/Desktop/P1/flow-api
nano .env
```

Update the `DATABASE_PASSWORD` line with your actual password.

## 🎯 Then Run the API!

Once the database is created and `.env` is updated:

```bash
cd /Users/ed/Desktop/P1/flow-api
./run.sh
```

The application will automatically create all tables when it starts!
