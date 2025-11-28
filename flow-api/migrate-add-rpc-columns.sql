-- Migration: Add RPC URL columns to applications table
-- Run this if you have existing applications table without these columns

ALTER TABLE applications 
ADD COLUMN IF NOT EXISTS sandbox_rpc_url VARCHAR(500),
ADD COLUMN IF NOT EXISTS production_rpc_url VARCHAR(500);

-- Verify columns were added
SELECT column_name, data_type, character_maximum_length 
FROM information_schema.columns 
WHERE table_name = 'applications' 
AND column_name IN ('sandbox_rpc_url', 'production_rpc_url');

