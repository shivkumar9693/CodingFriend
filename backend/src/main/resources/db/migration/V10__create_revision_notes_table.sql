-- =============================================================================
-- Migration V10: Create revision_notes table for AI Post-Mortem & Spaced Repetition Notebook
-- =============================================================================

CREATE TABLE IF NOT EXISTS revision_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    problem_id UUID REFERENCES problems(id) ON DELETE SET NULL,
    submission_id UUID REFERENCES submissions(id) ON DELETE SET NULL,
    problem_title VARCHAR(255) NOT NULL,
    problem_slug VARCHAR(255),
    language VARCHAR(50) NOT NULL DEFAULT 'JAVA',
    mistake_category VARCHAR(100) NOT NULL,
    concept_summary VARCHAR(500) NOT NULL,
    failed_input TEXT,
    actual_output TEXT,
    expected_output TEXT,
    student_flow_trace TEXT NOT NULL,
    correct_flow_trace TEXT NOT NULL,
    key_takeaway TEXT NOT NULL,
    submitted_code TEXT,
    user_reflections TEXT,
    mastery_status VARCHAR(50) NOT NULL DEFAULT 'TO_REVISE',
    is_bookmarked BOOLEAN NOT NULL DEFAULT FALSE,
    review_count INT NOT NULL DEFAULT 0,
    last_reviewed_at TIMESTAMPTZ,
    next_review_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_revision_notes_user ON revision_notes(user_id);
CREATE INDEX IF NOT EXISTS idx_revision_notes_problem ON revision_notes(problem_id);
CREATE INDEX IF NOT EXISTS idx_revision_notes_mastery ON revision_notes(user_id, mastery_status);
CREATE INDEX IF NOT EXISTS idx_revision_notes_bookmark ON revision_notes(user_id, is_bookmarked);
CREATE INDEX IF NOT EXISTS idx_revision_notes_created_at ON revision_notes(created_at DESC);
