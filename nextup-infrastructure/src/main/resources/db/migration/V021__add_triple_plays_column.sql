-- batting_records 테이블에 triple_plays 컬럼 추가
ALTER TABLE batting_records ADD COLUMN triple_plays INTEGER NOT NULL DEFAULT 0;
