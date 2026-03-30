-- V062: Nullable UNIQUE 제약 수정 + @Version 컬럼 추가
-- Issue #482: team_id가 NULL이면 PostgreSQL UNIQUE 제약이 중복을 허용하는 문제 수정
--             + race condition 방지를 위한 optimistic locking version 컬럼 추가

-- ============================================================
-- Part 1: Nullable UNIQUE 제약 수정
-- PostgreSQL에서 UNIQUE(player_id, year, team_id) 시 team_id가 NULL이면
-- 동일 (player_id, year)에 대해 무한 중복 행이 허용됨.
-- 해결: 기존 UNIQUE 제거 후 partial unique index 2개로 대체
-- ============================================================

-- season_batting_stats
ALTER TABLE season_batting_stats
    DROP CONSTRAINT IF EXISTS uk_season_batting_stats_player_year_team;

CREATE UNIQUE INDEX uk_season_batting_stats_player_year_team
    ON season_batting_stats (player_id, year, team_id)
    WHERE team_id IS NOT NULL;

CREATE UNIQUE INDEX uk_season_batting_stats_player_year_no_team
    ON season_batting_stats (player_id, year)
    WHERE team_id IS NULL;

-- season_pitching_stats
ALTER TABLE season_pitching_stats
    DROP CONSTRAINT IF EXISTS uk_season_pitching_stats_player_year_team;

CREATE UNIQUE INDEX uk_season_pitching_stats_player_year_team
    ON season_pitching_stats (player_id, year, team_id)
    WHERE team_id IS NOT NULL;

CREATE UNIQUE INDEX uk_season_pitching_stats_player_year_no_team
    ON season_pitching_stats (player_id, year)
    WHERE team_id IS NULL;

-- season_fielding_stats
ALTER TABLE season_fielding_stats
    DROP CONSTRAINT IF EXISTS uk_season_fielding_stats_player_year_team;

CREATE UNIQUE INDEX uk_season_fielding_stats_player_year_team
    ON season_fielding_stats (player_id, year, team_id)
    WHERE team_id IS NOT NULL;

CREATE UNIQUE INDEX uk_season_fielding_stats_player_year_no_team
    ON season_fielding_stats (player_id, year)
    WHERE team_id IS NULL;

-- ============================================================
-- Part 2: @Version 컬럼 추가 (Optimistic Locking)
-- race condition 방지를 위해 version 컬럼 추가
-- ============================================================

-- booking_transfers
ALTER TABLE booking_transfers ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- lineup_submissions
ALTER TABLE lineup_submissions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- match_requests
ALTER TABLE match_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- attendance_polls
ALTER TABLE attendance_polls ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- mercenary_requests
ALTER TABLE mercenary_requests ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
