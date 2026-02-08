-- V002: Create league_schedules table
-- Description: 대진표 관리 시스템 (LeagueSchedule)

CREATE TABLE league_schedules (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES competitions(id),
    round INT NOT NULL,
    match_number INT NOT NULL,
    home_team_id BIGINT NOT NULL REFERENCES teams(id),
    away_team_id BIGINT NOT NULL REFERENCES teams(id),
    scheduled_date DATE NOT NULL,
    scheduled_time TIME,
    venue VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    game_id BIGINT REFERENCES games(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Unique Constraints
    CONSTRAINT uk_league_schedules_competition_round_match UNIQUE (competition_id, round, match_number)
);

-- Indexes for performance
CREATE INDEX idx_league_schedules_competition ON league_schedules(competition_id);
CREATE INDEX idx_league_schedules_date ON league_schedules(scheduled_date);
CREATE INDEX idx_league_schedules_status ON league_schedules(status);
CREATE INDEX idx_league_schedules_game ON league_schedules(game_id);

COMMENT ON TABLE league_schedules IS '대진표 - 대회 내 라운드별 경기 일정 관리';
COMMENT ON COLUMN league_schedules.round IS '라운드 (1차전, 2차전 등)';
COMMENT ON COLUMN league_schedules.match_number IS '라운드 내 경기 번호';
COMMENT ON COLUMN league_schedules.status IS 'SCHEDULED(예정), GAME_CREATED(경기생성), POSTPONED(연기), CANCELLED(취소), COMPLETED(완료)';
