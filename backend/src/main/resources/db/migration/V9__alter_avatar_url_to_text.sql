-- =============================================================================
-- Migration V9: Alter avatar_url column to TEXT for base64 image support
-- =============================================================================

ALTER TABLE users ALTER COLUMN avatar_url TYPE TEXT;
