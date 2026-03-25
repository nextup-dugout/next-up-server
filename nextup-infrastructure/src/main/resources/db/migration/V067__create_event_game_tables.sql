-- ============================================
-- Migration: V067__create_event_game_tables
-- Create event game (pickup game) domain tables
-- ============================================

CREATE TABLE IF NOT EXISTS event_games (
    id BIGSERIAL PRIMARY KEY,
    organizer_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    scheduled_at TIMESTAMP NOT NULL,
    location VARCHAR(200),
    field_name VARCHAR(100),
    max_participants INTEGER NOT NULL,
    innings INTEGER NOT NULL DEFAULT 7,
    status VARCHAR(20) NOT NULL DEFAULT 'RECRUITING',
    team_a_name VARCHAR(50) NOT NULL DEFAULT 'Team A',
    team_b_name VARCHAR(50) NOT NULL DEFAULT 'Team B',
    team_a_score INTEGER,
    team_b_score INTEGER,
    started_at TIMESTAMP,
    ended_at TIMESTAMP,
    cancel_reason VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_event_games_status
    ON event_games (status);
CREATE INDEX IF NOT EXISTS idx_event_games_scheduled_at
    ON event_games (scheduled_at);
CREATE INDEX IF NOT EXISTS idx_event_games_organizer
    ON event_games (organizer_id);

CREATE TABLE IF NOT EXISTS event_game_participants (
    id BIGSERIAL PRIMARY KEY,
    event_game_id BIGINT NOT NULL REFERENCES event_games(id),
    player_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    team_assignment VARCHAR(10),
    message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_event_game_participants_game_player UNIQUE (event_game_id, player_id)
);

CREATE INDEX IF NOT EXISTS idx_event_game_participants_game
    ON event_game_participants (event_game_id);
CREATE INDEX IF NOT EXISTS idx_event_game_participants_player
    ON event_game_participants (player_id);
CREATE INDEX IF NOT EXISTS idx_event_game_participants_status
    ON event_game_participants (status);

-- Rollback
-- DROP TABLE IF EXISTS event_game_participants;
-- DROP TABLE IF EXISTS event_games;
