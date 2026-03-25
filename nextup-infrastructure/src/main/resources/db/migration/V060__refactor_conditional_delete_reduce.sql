-- V060: 조건부 삭제/축소 리팩토링
-- #475: Discipline -> PlayerBan, ActivityScore DROP, DOUBLE_ELIMINATION 삭제,
--       PlateAppearanceResult TRIPLE_PLAY/INTERFERENCE 통합, SeasonStats finalize 삭제

-- 1. Discipline -> PlayerBan: 기존 disciplines 테이블 DROP, 새 player_bans 테이블 생성
DROP TABLE IF EXISTS disciplines;

CREATE TABLE player_bans (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    competition_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    issued_by VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_player_bans_player_id ON player_bans(player_id);
CREATE INDEX idx_player_bans_competition_id ON player_bans(competition_id);

-- 2. ActivityScore -> 자동 집계: activity_scores 테이블 DROP
DROP TABLE IF EXISTS activity_scores;

-- 3. PlateAppearanceResult: triple_plays, batter_interferences, runner_interferences 컬럼 삭제
-- batting_records 테이블
ALTER TABLE batting_records DROP COLUMN IF EXISTS triple_plays;
ALTER TABLE batting_records DROP COLUMN IF EXISTS batter_interferences;
ALTER TABLE batting_records DROP COLUMN IF EXISTS runner_interferences;

-- season_batting_stats 테이블
ALTER TABLE season_batting_stats DROP COLUMN IF EXISTS batter_interferences;
ALTER TABLE season_batting_stats DROP COLUMN IF EXISTS runner_interferences;

-- career_batting_stats 테이블
ALTER TABLE career_batting_stats DROP COLUMN IF EXISTS batter_interferences;
ALTER TABLE career_batting_stats DROP COLUMN IF EXISTS runner_interferences;

-- 4. SeasonStats: is_finalized 컬럼 삭제
ALTER TABLE season_batting_stats DROP COLUMN IF EXISTS is_finalized;
ALTER TABLE season_pitching_stats DROP COLUMN IF EXISTS is_finalized;
ALTER TABLE season_fielding_stats DROP COLUMN IF EXISTS is_finalized;
