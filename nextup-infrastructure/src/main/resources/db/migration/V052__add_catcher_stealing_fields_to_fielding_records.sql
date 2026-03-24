-- M-6: fielding_records 테이블에 포수용 도루 저지/허용 컬럼 추가
ALTER TABLE fielding_records ADD COLUMN caught_stealing INTEGER NOT NULL DEFAULT 0;
ALTER TABLE fielding_records ADD COLUMN stolen_bases_allowed INTEGER NOT NULL DEFAULT 0;

-- 시즌/통산 수비 통계 테이블에 삼중살/도루 저지/도루 허용 컬럼 추가
ALTER TABLE season_fielding_stats ADD COLUMN triple_plays INTEGER NOT NULL DEFAULT 0;
ALTER TABLE season_fielding_stats ADD COLUMN caught_stealing INTEGER NOT NULL DEFAULT 0;
ALTER TABLE season_fielding_stats ADD COLUMN stolen_bases_allowed INTEGER NOT NULL DEFAULT 0;

ALTER TABLE career_fielding_stats ADD COLUMN triple_plays INTEGER NOT NULL DEFAULT 0;
ALTER TABLE career_fielding_stats ADD COLUMN caught_stealing INTEGER NOT NULL DEFAULT 0;
ALTER TABLE career_fielding_stats ADD COLUMN stolen_bases_allowed INTEGER NOT NULL DEFAULT 0;
