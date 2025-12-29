# Flow API Setup Guide

## Quick Setup

### Option 1: Automated Setup (Recommended)

Run the setup script:

```bash
cd flow-api
./setup.sh
```

This will:
- ✅ Check PostgreSQL installation
- ✅ Create the `flow_api` database
- ✅ Set up environment variables
- ✅ Generate JWT secret
- ✅ Build the project

### Option 2: Manual Setup

#### 1. Create Database

Find your PostgreSQL installation and create the database:

**If psql is in your PATH:**
```bash
createdb flow_api
# OR
psql -U postgres -c "CREATE DATABASE flow_api;"
```

**If using Postgres.app on macOS:**
```bash
/Applications/Postgres.app/Contents/Versions/latest/bin/psql -c "CREATE DATABASE flow_api;"
```

**If using Homebrew PostgreSQL:**
```bash
/opt/homebrew/bin/psql -U postgres -c "CREATE DATABASE flow_api;"
```

#### 2. Set Environment Variables

Create a `.env` file or export these variables:

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/flow_api"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="your_postgres_password"
export JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters-long"
```

Or create a `.env` file:
```bash
cat > .env << EOF
DATABASE_URL=jdbc:postgresql://localhost:5432/flow_api
DATABASE_USER=postgres
DATABASE_PASSWORD=your_password
JWT_SECRET=$(openssl rand -hex 32)
EOF
```

#### 3. Build and Run

```bash
./gradlew build
./gradlew run
```

Or use the run script:
```bash
./run.sh
```

## Finding PostgreSQL on macOS

If `psql` is not in your PATH, try these locations:

```bash
# Homebrew
/opt/homebrew/bin/psql

# Postgres.app
/Applications/Postgres.app/Contents/Versions/latest/bin/psql

# Standard installation
/usr/local/bin/psql

# Add to PATH
export PATH="/path/to/postgresql/bin:$PATH"
```

## Testing the API

Once running, test with:

```bash
# Create an account
curl -X POST http://localhost:8080/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'

# Authenticate
curl -X POST http://localhost:8080/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

## Troubleshooting

### Database Connection Errors

1. **Check PostgreSQL is running:**
   ```bash
   # macOS
   brew services list
   # OR check Activity Monitor for postgres
   ```

2. **Verify connection:**
   ```bash
   psql -U postgres -d flow_api -c "SELECT 1;"
   ```

3. **Check credentials:**
   - Default user is usually `postgres`
   - Password might be empty or set during installation

### Port Already in Use

If port 8080 is taken:
```bash
# Find what's using it
lsof -i :8080

# Kill it
kill -9 <PID>

# Or change port in application.conf
```

### Build Errors

1. **Java version:**
   ```bash
   java -version  # Should be 17+
   ```

2. **Clean build:**
   ```bash
   ./gradlew clean build
   ```

## Next Steps

1. ✅ Database created
2. ✅ Environment variables set
3. ✅ Application running
4. 🎯 Test the API endpoints
5. 🎯 Create your first account
6. 🎯 Generate access tokens
7. 🎯 Create yield accounts

See `README.md` for full API documentation!
