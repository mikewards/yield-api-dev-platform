#!/bin/bash

# Script to add RPC URL columns to applications table
# This fixes the "Failed to load application details" error

echo "🔧 Adding RPC URL columns to applications table..."

# Try to find psql
PSQL_PATH=""
if [ -f "/Library/PostgreSQL/18/bin/psql" ]; then
    PSQL_PATH="/Library/PostgreSQL/18/bin/psql"
elif [ -f "/Applications/Postgres.app/Contents/Versions/latest/bin/psql" ]; then
    PSQL_PATH="/Applications/Postgres.app/Contents/Versions/latest/bin/psql"
elif command -v psql &> /dev/null; then
    PSQL_PATH="psql"
else
    echo "❌ Error: psql not found. Please install PostgreSQL or add it to PATH."
    exit 1
fi

# Read database password from .env if it exists
if [ -f ".env" ]; then
    source .env
    export PGPASSWORD="${DATABASE_PASSWORD:-}"
fi

# Run migration
$PSQL_PATH -U postgres -d flow_api -f migrate-add-rpc-columns.sql

if [ $? -eq 0 ]; then
    echo "✅ Migration completed successfully!"
    echo ""
    echo "The applications table now has:"
    echo "  - sandbox_rpc_url"
    echo "  - production_rpc_url"
else
    echo "❌ Migration failed. You may need to run it manually:"
    echo ""
    echo "  $PSQL_PATH -U postgres -d flow_api -f migrate-add-rpc-columns.sql"
    echo ""
    echo "Or manually run the SQL:"
    echo "  ALTER TABLE applications ADD COLUMN IF NOT EXISTS sandbox_rpc_url VARCHAR(500);"
    echo "  ALTER TABLE applications ADD COLUMN IF NOT EXISTS production_rpc_url VARCHAR(500);"
fi

