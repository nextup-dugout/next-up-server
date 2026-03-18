-- M-4: fielding_records 테이블에 triple_plays 컬럼 추가
ALTER TABLE fielding_records ADD COLUMN triple_plays INTEGER NOT NULL DEFAULT 0;

-- M-6: pitching_records 테이블에 도루 허용/도루 저지 컬럼 추가
ALTER TABLE pitching_records ADD COLUMN stolen_bases_allowed INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pitching_records ADD COLUMN runners_caught_stealing INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pitching_records ADD COLUMN pickoffs INTEGER NOT NULL DEFAULT 0;
