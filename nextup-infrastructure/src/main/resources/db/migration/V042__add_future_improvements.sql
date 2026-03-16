-- V042: L-1~L-13 향후 개선사항 (도메인 정밀화/운영 안정성)

-- L-1: 포지션별 수비 기록 분리 - FieldingRecord에 position 필드 추가
ALTER TABLE fielding_records ADD COLUMN position VARCHAR(30);
CREATE INDEX idx_fielding_records_position ON fielding_records(position);

-- L-3: 용병 쿼터제 - GameRules에 max_mercenary_count 필드 추가
ALTER TABLE competitions ADD COLUMN max_mercenary_count INTEGER;

-- L-8: 시즌 통계 아카이브/확정 메커니즘
ALTER TABLE season_batting_stats ADD COLUMN is_finalized BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE season_pitching_stats ADD COLUMN is_finalized BOOLEAN NOT NULL DEFAULT FALSE;
