#!/bin/bash

# Load SDKMAN and Java
if [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

# Load environment variables from .env if it exists
if [ -f .env ]; then
    echo "Loading environment variables from .env..."
    export $(cat .env | grep -v '^#' | xargs)
fi

# Check required environment variables
if [ -z "$DATABASE_URL" ]; then
    echo "⚠️  DATABASE_URL not set. Using default: jdbc:postgresql://localhost:5432/flow_api"
    export DATABASE_URL="jdbc:postgresql://localhost:5432/flow_api"
fi

if [ -z "$DATABASE_USER" ]; then
    echo "⚠️  DATABASE_USER not set. Using default: postgres"
    export DATABASE_USER="postgres"
fi

if [ -z "$JWT_SECRET" ]; then
    echo "⚠️  JWT_SECRET not set. Generating one..."
    export JWT_SECRET=$(openssl rand -hex 32)
    echo "Generated JWT_SECRET (save this for production!)"
fi

echo "🚀 Starting Flow API Gateway..."
echo "Database: $DATABASE_URL"
echo "User: $DATABASE_USER"
echo ""
echo "API will be available at: http://localhost:8080"
echo ""

./gradlew run
