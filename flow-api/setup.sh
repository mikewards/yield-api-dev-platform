#!/bin/bash

# Flow API Setup Script
echo "🚀 Setting up Flow API Gateway..."

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if PostgreSQL is running
echo -e "${YELLOW}Checking PostgreSQL installation...${NC}"

# Try to find psql
PSQL_CMD=""
if command -v psql &> /dev/null; then
    PSQL_CMD="psql"
elif [ -f "/usr/local/bin/psql" ]; then
    PSQL_CMD="/usr/local/bin/psql"
elif [ -f "/opt/homebrew/bin/psql" ]; then
    PSQL_CMD="/opt/homebrew/bin/psql"
elif [ -d "/Applications/Postgres.app" ]; then
    # Find latest version in Postgres.app
    LATEST_VERSION=$(ls -t /Applications/Postgres.app/Contents/Versions/ 2>/dev/null | head -1)
    if [ -n "$LATEST_VERSION" ]; then
        PSQL_CMD="/Applications/Postgres.app/Contents/Versions/$LATEST_VERSION/bin/psql"
    fi
fi

if [ -z "$PSQL_CMD" ]; then
    echo -e "${RED}⚠️  psql not found in PATH${NC}"
    echo -e "${YELLOW}Please add PostgreSQL to your PATH or create the database manually:${NC}"
    echo "  CREATE DATABASE flow_api;"
    echo ""
    echo "Or find psql and add it to PATH:"
    echo "  export PATH=\"/path/to/postgresql/bin:\$PATH\""
    echo ""
    read -p "Press Enter to continue anyway (you'll need to create the database manually)..."
else
    echo -e "${GREEN}✓ Found psql at: $PSQL_CMD${NC}"
    
    # Check if database exists
    echo -e "${YELLOW}Checking if flow_api database exists...${NC}"
    DB_EXISTS=$($PSQL_CMD -lqt 2>/dev/null | cut -d \| -f 1 | grep -w flow_api | wc -l)
    
    if [ "$DB_EXISTS" -eq 0 ]; then
        echo -e "${YELLOW}Creating flow_api database...${NC}"
        $PSQL_CMD -U postgres -c "CREATE DATABASE flow_api;" 2>/dev/null || \
        $PSQL_CMD -c "CREATE DATABASE flow_api;" 2>/dev/null || \
        createdb flow_api 2>/dev/null || \
        echo -e "${RED}⚠️  Could not create database automatically. Please create it manually:${NC}"
        echo "  CREATE DATABASE flow_api;"
    else
        echo -e "${GREEN}✓ Database flow_api already exists${NC}"
    fi
fi

# Check environment variables
echo ""
echo -e "${YELLOW}Checking environment variables...${NC}"

if [ -z "$DATABASE_URL" ]; then
    echo -e "${YELLOW}⚠️  DATABASE_URL not set${NC}"
    echo "Setting default: jdbc:postgresql://localhost:5432/flow_api"
    export DATABASE_URL="jdbc:postgresql://localhost:5432/flow_api"
else
    echo -e "${GREEN}✓ DATABASE_URL is set${NC}"
fi

if [ -z "$DATABASE_USER" ]; then
    echo -e "${YELLOW}⚠️  DATABASE_USER not set${NC}"
    echo "Setting default: postgres"
    export DATABASE_USER="postgres"
else
    echo -e "${GREEN}✓ DATABASE_USER is set${NC}"
fi

if [ -z "$DATABASE_PASSWORD" ]; then
    echo -e "${YELLOW}⚠️  DATABASE_PASSWORD not set${NC}"
    echo -e "${YELLOW}You'll need to set this!${NC}"
    read -sp "Enter PostgreSQL password (or press Enter to skip): " DB_PASS
    echo ""
    if [ -n "$DB_PASS" ]; then
        export DATABASE_PASSWORD="$DB_PASS"
    fi
else
    echo -e "${GREEN}✓ DATABASE_PASSWORD is set${NC}"
fi

if [ -z "$JWT_SECRET" ]; then
    echo -e "${YELLOW}⚠️  JWT_SECRET not set${NC}"
    echo "Generating a random JWT secret..."
    export JWT_SECRET=$(openssl rand -hex 32)
    echo -e "${GREEN}✓ Generated JWT_SECRET${NC}"
else
    echo -e "${GREEN}✓ JWT_SECRET is set${NC}"
fi

# Create .env file
echo ""
echo -e "${YELLOW}Creating .env file...${NC}"
cat > .env << EOF
DATABASE_URL=$DATABASE_URL
DATABASE_USER=$DATABASE_USER
DATABASE_PASSWORD=$DATABASE_PASSWORD
JWT_SECRET=$JWT_SECRET
EOF

echo -e "${GREEN}✓ Created .env file${NC}"

# Check Java
echo ""
echo -e "${YELLOW}Checking Java installation...${NC}"

# Load SDKMAN if available
if [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    source "$HOME/.sdkman/bin/sdkman-init.sh"
fi

if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo -e "${GREEN}✓ Java found: $JAVA_VERSION${NC}"
    
    # Verify it's Java 17+
    if java -version 2>&1 | grep -q "1[789]\|2[0-9]"; then
        echo -e "${GREEN}✓ Java version is 17 or higher${NC}"
    else
        echo -e "${YELLOW}⚠️  Java version might be too old. Need Java 17+${NC}"
    fi
else
    echo -e "${RED}✗ Java not found. Please install Java 17+${NC}"
    echo -e "${YELLOW}Installing Java via SDKMAN...${NC}"
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install java 17.0.9-tem
    sdk default java 17.0.9-tem
fi

# Build the project
echo ""
echo -e "${YELLOW}Building Flow API...${NC}"
./gradlew build --no-daemon

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Build successful!${NC}"
    echo ""
    echo -e "${GREEN}🎉 Setup complete!${NC}"
    echo ""
    echo "To run the API:"
    echo "  ./gradlew run"
    echo ""
    echo "Or load environment variables first:"
    echo "  source .env"
    echo "  ./gradlew run"
    echo ""
    echo "The API will be available at: http://localhost:8080"
else
    echo -e "${RED}✗ Build failed. Please check the errors above.${NC}"
    exit 1
fi
