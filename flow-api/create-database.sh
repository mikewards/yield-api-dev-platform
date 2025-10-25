#!/bin/bash

# Script to create flow_api database
# This handles password authentication

PSQL_CMD="/Library/PostgreSQL/18/bin/psql"

echo "🗄️  Creating flow_api database..."

# Check if database already exists
DB_EXISTS=$($PSQL_CMD -U postgres -lqt 2>/dev/null | cut -d \| -f 1 | grep -w flow_api | wc -l | tr -d ' ')

if [ "$DB_EXISTS" -eq "1" ]; then
    echo "✓ Database 'flow_api' already exists!"
    exit 0
fi

# Try to create database
echo "Attempting to create database..."

# Method 1: Try with PGPASSWORD if set
if [ -n "$PGPASSWORD" ]; then
    echo "Using PGPASSWORD environment variable..."
    PGPASSWORD=$PGPASSWORD $PSQL_CMD -U postgres -c "CREATE DATABASE flow_api;" 2>&1
    if [ $? -eq 0 ]; then
        echo "✓ Database 'flow_api' created successfully!"
        exit 0
    fi
fi

# Method 2: Try with current user (if trust authentication)
echo "Trying with current user: $(whoami)..."
$PSQL_CMD -U $(whoami) -d postgres -c "CREATE DATABASE flow_api;" 2>&1
if [ $? -eq 0 ]; then
    echo "✓ Database 'flow_api' created successfully!"
    exit 0
fi

# Method 3: Prompt for password
echo ""
echo "PostgreSQL requires authentication."
echo "Please enter your PostgreSQL password for user 'postgres':"
echo ""
read -sp "Password: " PGPASS
echo ""

if [ -n "$PGPASS" ]; then
    PGPASSWORD=$PGPASS $PSQL_CMD -U postgres -c "CREATE DATABASE flow_api;" 2>&1
    if [ $? -eq 0 ]; then
        echo "✓ Database 'flow_api' created successfully!"
        echo ""
        echo "💡 Tip: You can set PGPASSWORD environment variable to avoid prompts:"
        echo "   export PGPASSWORD='your_password'"
        exit 0
    else
        echo "✗ Failed to create database. Please check your password."
        exit 1
    fi
else
    echo "✗ No password provided. Cannot create database."
    echo ""
    echo "You can create it manually:"
    echo "  $PSQL_CMD -U postgres"
    echo "  CREATE DATABASE flow_api;"
    echo "  \\q"
    exit 1
fi
