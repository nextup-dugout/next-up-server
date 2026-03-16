-- #340: 스탯 엔티티 @Version 기반 Optimistic Locking 적용
-- 동시 갱신(Lost Update) 방지를 위해 version 컬럼 추가

ALTER TABLE season_batting_stats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE season_pitching_stats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE season_fielding_stats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE career_batting_stats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE career_pitching_stats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE career_fielding_stats ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
