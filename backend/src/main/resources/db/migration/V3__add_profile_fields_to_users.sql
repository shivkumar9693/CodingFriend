-- =============================================================================
-- Migration V3: Add minimal profile fields to users table
-- =============================================================================

ALTER TABLE users ADD COLUMN IF NOT EXISTS bio VARCHAR(500);
ALTER TABLE users ADD COLUMN IF NOT EXISTS github_username VARCHAR(100);
