-- =============================================================================
-- Migration V6: Add evaluation fields to submissions table
-- =============================================================================

ALTER TABLE submissions
ADD COLUMN IF NOT EXISTS verdict VARCHAR(50),
ADD COLUMN IF NOT EXISTS tests_passed INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS total_tests INTEGER NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS execution_time_ms BIGINT NOT NULL DEFAULT 0,
ADD COLUMN IF NOT EXISTS evaluation_details TEXT;

CREATE INDEX IF NOT EXISTS idx_submissions_verdict ON submissions(verdict);
