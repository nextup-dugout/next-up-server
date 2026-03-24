-- ============================================
-- Migration: V063__add_competition_type_to_season_stats
-- Description: 시즌 통계 테이블에 competition_type 컬럼 추가
--              FRIENDLY 경기 통계가 공식 순위를 오염시키는 버그 방지
-- ============================================

-- 1. season_batting_stats에 competition_type 컬럼 추가
ALTER TABLE season_batting_stats
    ADD COLUMN competition_type VARCHAR(20) NOT NULL DEFAULT 'LEAGUE';

-- 2. season_pitching_stats에 competition_type 컬럼 추가
ALTER TABLE season_pitching_stats
    ADD COLUMN competition_type VARCHAR(20) NOT NULL DEFAULT 'LEAGUE';

-- 3. season_fielding_stats에 competition_type 컬럼 추가
ALTER TABLE season_fielding_stats
    ADD COLUMN competition_type VARCHAR(20) NOT NULL DEFAULT 'LEAGUE';

-- 4. 기존 unique constraint 삭제 후 competition_type 포함한 새 constraint 생성

-- season_batting_stats
ALTER TABLE season_batting_stats
    DROP CONSTRAINT IF EXISTS uk_season_batting_stats_player_year_team;
ALTER TABLE season_batting_stats
    ADD CONSTRAINT uk_season_batting_stats_player_year_team_ct
        UNIQUE (player_id, year, team_id, competition_type);

-- season_pitching_stats
ALTER TABLE season_pitching_stats
    DROP CONSTRAINT IF EXISTS uk_season_pitching_stats_player_year_team;
ALTER TABLE season_pitching_stats
    ADD CONSTRAINT uk_season_pitching_stats_player_year_team_ct
        UNIQUE (player_id, year, team_id, competition_type);

-- season_fielding_stats
ALTER TABLE season_fielding_stats
    DROP CONSTRAINT IF EXISTS uk_season_fielding_stats_player_year_team;
ALTER TABLE season_fielding_stats
    ADD CONSTRAINT uk_season_fielding_stats_player_year_team_ct
        UNIQUE (player_id, year, team_id, competition_type);

-- 5. competition_type 인덱스 생성 (리더보드 쿼리 최적화)
CREATE INDEX idx_season_batting_stats_comp_type ON season_batting_stats (competition_type);
CREATE INDEX idx_season_pitching_stats_comp_type ON season_pitching_stats (competition_type);
CREATE INDEX idx_season_fielding_stats_comp_type ON season_fielding_stats (competition_type);

-- Rollback
-- ALTER TABLE season_batting_stats DROP CONSTRAINT IF EXISTS uk_season_batting_stats_player_year_team_ct;
-- ALTER TABLE season_batting_stats ADD CONSTRAINT uk_season_batting_stats_player_year_team UNIQUE (player_id, year, team_id);
-- ALTER TABLE season_batting_stats DROP COLUMN IF EXISTS competition_type;
-- DROP INDEX IF EXISTS idx_season_batting_stats_comp_type;
-- ALTER TABLE season_pitching_stats DROP CONSTRAINT IF EXISTS uk_season_pitching_stats_player_year_team_ct;
-- ALTER TABLE season_pitching_stats ADD CONSTRAINT uk_season_pitching_stats_player_year_team UNIQUE (player_id, year, team_id);
-- ALTER TABLE season_pitching_stats DROP COLUMN IF EXISTS competition_type;
-- DROP INDEX IF EXISTS idx_season_pitching_stats_comp_type;
-- ALTER TABLE season_fielding_stats DROP CONSTRAINT IF EXISTS uk_season_fielding_stats_player_year_team_ct;
-- ALTER TABLE season_fielding_stats ADD CONSTRAINT uk_season_fielding_stats_player_year_team UNIQUE (player_id, year, team_id);
-- ALTER TABLE season_fielding_stats DROP COLUMN IF EXISTS competition_type;
-- DROP INDEX IF EXISTS idx_season_fielding_stats_comp_type;
