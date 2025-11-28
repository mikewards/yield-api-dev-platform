# Quick Start Guide

## Prerequisites

1. **Java 17+** - Download from [Adoptium](https://adoptium.net/)
2. **PostgreSQL** - Download from [PostgreSQL.org](https://www.postgresql.org/download/)
3. **Gradle** (optional - wrapper included)

## Setup Steps

### 1. Create Database

```bash
createdb flow_api
```

Or using PostgreSQL CLI:
```sql
CREATE DATABASE flow_api;
```

### 2. Set Environment Variables

Create a `.env` file or export these variables:

```bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/flow_api"
export DATABASE_USER="postgres"
export DATABASE_PASSWORD="your_password"
export JWT_SECRET="your-super-secret-jwt-key-minimum-32-characters-long"
```

### 3. Build and Run

```bash
cd flow-api
./gradlew build
./gradlew run
```

Or on Windows:
```bash
gradlew.bat build
gradlew.bat run
```

### 4. Test the API

The server will start on `http://localhost:8080`

Try creating an account:
```bash
curl -X POST http://localhost:8080/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'
```

## Troubleshooting

### Database Connection Issues
- Make sure PostgreSQL is running: `pg_isready`
- Check your connection string matches your PostgreSQL setup
- Verify username and password are correct

### Port Already in Use
- Change the port in `application.conf` or set `PORT` environment variable
- Kill the process using port 8080: `lsof -ti:8080 | xargs kill`

### Build Errors
- Make sure you have Java 17+: `java -version`
- Try cleaning: `./gradlew clean build`

## Next Steps

1. Authenticate to get a token
2. Create a Personal Access Token
3. Create a yield account
4. Make deposits and withdrawals

See the main README.md for full API documentation!
