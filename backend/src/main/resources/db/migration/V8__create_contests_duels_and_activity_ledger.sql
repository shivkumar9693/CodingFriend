-- =============================================================================
-- Migration V8: Create contests, contest_problems, contest_participants, duels, and user_daily_activity
-- =============================================================================

-- Add contest & duel ratings to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS contest_rating INT DEFAULT 1200;
ALTER TABLE users ADD COLUMN IF NOT EXISTS duel_rating INT DEFAULT 1200;
ALTER TABLE users ADD COLUMN IF NOT EXISTS current_streak INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS max_streak INT DEFAULT 0;

-- 1. Contests Table
CREATE TABLE IF NOT EXISTS contests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    duration_minutes INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'UPCOMING',
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_contests_slug ON contests(slug);
CREATE INDEX IF NOT EXISTS idx_contests_status ON contests(status);
CREATE INDEX IF NOT EXISTS idx_contests_start_time ON contests(start_time);

-- 2. Contest Problems Table
CREATE TABLE IF NOT EXISTS contest_problems (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contest_id UUID NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    problem_order INT NOT NULL DEFAULT 1,
    points INT NOT NULL DEFAULT 500,
    CONSTRAINT uq_contest_problem UNIQUE (contest_id, problem_id)
);

CREATE INDEX IF NOT EXISTS idx_contest_problems_contest ON contest_problems(contest_id);

-- 3. Contest Participants Table
CREATE TABLE IF NOT EXISTS contest_participants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contest_id UUID NOT NULL REFERENCES contests(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    total_score INT DEFAULT 0,
    total_penalty_minutes INT DEFAULT 0,
    final_rank INT,
    rating_before INT DEFAULT 1200,
    rating_after INT DEFAULT 1200,
    tab_switch_count INT DEFAULT 0,
    CONSTRAINT uq_contest_user UNIQUE (contest_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_contest_participants_contest ON contest_participants(contest_id);
CREATE INDEX IF NOT EXISTS idx_contest_participants_user ON contest_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_contest_participants_score ON contest_participants(contest_id, total_score DESC, total_penalty_minutes ASC);

-- 4. Add contest_id to Submissions table
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS contest_id UUID REFERENCES contests(id) ON DELETE SET NULL;
CREATE INDEX IF NOT EXISTS idx_submissions_contest_id ON submissions(contest_id);

-- 5. 1v1 Duels Table
CREATE TABLE IF NOT EXISTS duels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_code VARCHAR(32) UNIQUE NOT NULL,
    player1_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    player2_id UUID REFERENCES users(id) ON DELETE SET NULL,
    problem_id UUID NOT NULL REFERENCES problems(id) ON DELETE CASCADE,
    difficulty VARCHAR(50) NOT NULL DEFAULT 'EASY',
    status VARCHAR(50) NOT NULL DEFAULT 'WAITING',
    winner_id UUID REFERENCES users(id) ON DELETE SET NULL,
    start_time TIMESTAMPTZ,
    end_time TIMESTAMPTZ,
    player1_progress INT DEFAULT 0,
    player2_progress INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_duels_room_code ON duels(room_code);
CREATE INDEX IF NOT EXISTS idx_duels_status ON duels(status);
CREATE INDEX IF NOT EXISTS idx_duels_player1 ON duels(player1_id);
CREATE INDEX IF NOT EXISTS idx_duels_player2 ON duels(player2_id);

-- 6. User Daily Activity Table (For Heatmap and Streak computation)
CREATE TABLE IF NOT EXISTS user_daily_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    activity_date DATE NOT NULL,
    submission_count INT NOT NULL DEFAULT 1,
    problems_solved INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_activity_date UNIQUE (user_id, activity_date)
);

CREATE INDEX IF NOT EXISTS idx_user_activity_user_date ON user_daily_activity(user_id, activity_date DESC);
