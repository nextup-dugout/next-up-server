-- V047: L-8 시즌 통계 아카이브 메커니즘 - season_fielding_stats에 is_finalized 컬럼 추가
-- season_batting_stats, season_pitching_stats는 V042에서 이미 추가됨

ALTER TABLE season_fielding_stats ADD COLUMN is_finalized BOOLEAN NOT NULL DEFAULT FALSE;
