-- V037: 시즌 통계에 팀 ID 추가 (이적 시 팀별 기록 분리 지원)

-- season_batting_stats
ALTER TABLE season_batting_stats ADD COLUMN team_id BIGINT;

-- 기존 unique constraint 삭제 후 새 constraint 추가 (team_id 포함)
ALTER TABLE season_batting_stats DROP CONSTRAINT IF EXISTS uk_season_batting_stats_player_year;
ALTER TABLE season_batting_stats ADD CONSTRAINT uk_season_batting_stats_player_year_team
    UNIQUE (player_id, year, team_id);

CREATE INDEX IF NOT EXISTS idx_season_batting_stats_team ON season_batting_stats (team_id);

-- season_pitching_stats
ALTER TABLE season_pitching_stats ADD COLUMN team_id BIGINT;

ALTER TABLE season_pitching_stats DROP CONSTRAINT IF EXISTS uk_season_pitching_stats_player_year;
ALTER TABLE season_pitching_stats ADD CONSTRAINT uk_season_pitching_stats_player_year_team
    UNIQUE (player_id, year, team_id);

CREATE INDEX IF NOT EXISTS idx_season_pitching_stats_team ON season_pitching_stats (team_id);

-- season_fielding_stats
ALTER TABLE season_fielding_stats ADD COLUMN team_id BIGINT;

ALTER TABLE season_fielding_stats DROP CONSTRAINT IF EXISTS uk_season_fielding_stats_player_year;
ALTER TABLE season_fielding_stats ADD CONSTRAINT uk_season_fielding_stats_player_year_team
    UNIQUE (player_id, year, team_id);

CREATE INDEX IF NOT EXISTS idx_season_fielding_stats_team ON season_fielding_stats (team_id);
