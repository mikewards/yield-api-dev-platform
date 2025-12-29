# Development Setup Guide

Complete guide for setting up the TBD platform for local development.

**Last Updated**: December 2025

## Prerequisites

- **JDK 17+** - For Kotlin backend
- **PostgreSQL 12+** - Database
- **Node.js 18+** - For frontend (Cloudflare Workers)
- **Git** - Version control

### Installing Prerequisites

#### macOS (using Homebrew)

```bash
# Install Java
brew install openjdk@17

# Install PostgreSQL
brew install postgresql@14
brew services start postgresql@14

# Install Node.js
brew install node
```

#### Linux (Ubuntu/Debian)

```bash
# Install Java
sudo apt update
sudo apt install openjdk-17-jdk

# Install PostgreSQL
sudo apt install postgresql-14

# Install Node.js
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs
```

## Project Setup

### 1. Clone Repository

```bash
git clone https://github.com/wardmic4/flow-platform.git
cd flow-platform
```

### 2. Database Setup

#### Create Database

```bash
# Using psql
createdb flow_api

# Or using PostgreSQL client
psql -U postgres -c "CREATE DATABASE flow_api;"
```

#### Verify Database

```bash
psql -U postgres -d flow_api -c "SELECT version();"
```

### 3. Backend Setup

#### Configure Environment Variables

```bash
cd flow-api
```

Create `.env` file:

```bash
ENVIRONMENT=development
DATABASE_URL=jdbc:postgresql://localhost:5432/flow_api
DATABASE_USER=postgres
DATABASE_PASSWORD=your_password
JWT_SECRET=$(openssl rand -hex 32)
MASTER_ENCRYPTION_KEY=$(openssl rand -hex 32)
# SVIX_API_KEY=optional_for_local_dev
```

#### Generate Secrets

```bash
# Generate JWT secret
openssl rand -hex 32

# Generate encryption key
openssl rand -hex 32
```

#### Start Backend

```bash
cd flow-api
./gradlew run

# Or using the run script
./run.sh
```

Backend will start on `http://localhost:8080`

### 4. Frontend Setup

The frontend uses Cloudflare Workers for clean URL routing.

#### Install Wrangler CLI

```bash
npm install -g wrangler
```

#### Start Frontend Development Server

```bash
cd frontend
npx wrangler dev
```

Frontend will be available at `http://localhost:8787`

#### Alternative: Simple HTTP Server

For quick testing without clean URLs:

```bash
cd frontend
python3 -m http.server 3000
```

## Development Workflow

### Running Tests

```bash
cd flow-api
./gradlew test
```

### Building

```bash
cd flow-api
./gradlew build
```

### Database Migrations

The application automatically creates/updates database schema on first run. No manual migrations needed.

Tables created automatically:
- `accounts`
- `applications`
- `application_wallets`
- `access_tokens`
- `refresh_tokens`
- `yield_accounts`
- `positions`
- `transactions`
- `webhooks`
- `request_logs`

### Hot Reload

For backend development:
- Use IntelliJ IDEA with Kotlin plugin
- Enable "Build project automatically"
- Use Ktor's development mode (auto-reload on changes)

## Project Structure

```
flow-platform/
├── flow-api/                  # Kotlin backend
│   ├── src/
│   │   └── main/
│   │       ├── kotlin/
│   │       │   └── com/tbd/
│   │       │       ├── api/routes/      # API endpoints
│   │       │       ├── dto/             # Data transfer objects
│   │       │       ├── middleware/      # Auth, rate limiting, logging
│   │       │       ├── model/           # Database models
│   │       │       ├── service/         # Business logic
│   │       │       └── integration/     # Morpho/Aave clients
│   │       └── resources/
│   │           └── application.conf     # Configuration
│   └── build.gradle.kts
├── frontend/                  # Frontend application
│   ├── pages/                # HTML pages
│   ├── styles/               # CSS files
│   ├── scripts/              # JavaScript files
│   │   ├── token-manager.js  # OAuth token management
│   │   ├── config.js         # API configuration
│   │   └── nav-auth.js       # Navigation auth state
│   ├── sdk-demos/            # SDK demo pages
│   ├── worker.js             # Cloudflare Worker (URL routing)
│   └── wrangler.jsonc        # Cloudflare config
└── docs/                     # Documentation
```

## Common Issues

### Database Connection Failed

1. Verify PostgreSQL is running:
   ```bash
   # macOS
   brew services list
   
   # Linux
   sudo systemctl status postgresql
   ```
2. Check database credentials in `.env`
3. Verify database exists: `psql -U postgres -l`

### Port Already in Use

```bash
# Find process using port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Gradle Build Fails

```bash
# Clean and rebuild
cd flow-api
./gradlew clean build
```

### Frontend Not Connecting to Backend

1. Check backend is running on port 8080
2. Check CORS is configured correctly
3. Verify `config.js` has correct API URL

## IDE Setup

### IntelliJ IDEA (Recommended)

1. Open project in IntelliJ
2. Import Gradle project
3. Configure JDK 17
4. Install Kotlin plugin (if not already installed)

### VS Code

1. Install Kotlin extension
2. Install Gradle extension
3. Configure Java home: `Cmd+Shift+P` → "Java: Configure Java Runtime"

## Testing the Setup

### 1. Test Backend Health

```bash
curl http://localhost:8080/health
```

Expected: `{"status":"ok"}`

### 2. Create Test Account

```bash
curl -X POST http://localhost:8080/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpassword123",
    "email": "test@example.com"
  }'
```

### 3. Test Authentication

```bash
curl -X POST http://localhost:8080/v1/auth/authenticate \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "testpassword123"
  }'
```

Response includes:
- `access_token` (15 min JWT)
- `refresh_token` (30 day)
- `expires_in` (900 seconds)

### 4. Test Token Refresh

```bash
curl -X POST http://localhost:8080/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refresh_token": "tbd_refresh_..."
  }'
```

## Next Steps

- [API Documentation](../api/specification.md)
- [Environment Variables](./environment-variables.md)
- [Railway Deployment](../deployment/railway.md)
