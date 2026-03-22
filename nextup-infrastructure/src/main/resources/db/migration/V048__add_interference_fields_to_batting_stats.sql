-- L-2: 방해 유형 세분화 — 타격방해/주루방해 통계 분리 필드 추가

-- batting_records 테이블에 방해 유형별 카운트 컬럼 추가
ALTER TABLE batting_records
    ADD COLUMN batter_interferences INT NOT NULL DEFAULT 0,
    ADD COLUMN runner_interferences INT NOT NULL DEFAULT 0;

-- season_batting_stats 테이블에 방해 유형별 카운트 컬럼 추가
ALTER TABLE season_batting_stats
    ADD COLUMN batter_interferences INT NOT NULL DEFAULT 0,
    ADD COLUMN runner_interferences INT NOT NULL DEFAULT 0;

-- career_batting_stats 테이블에 방해 유형별 카운트 컬럼 추가
ALTER TABLE career_batting_stats
    ADD COLUMN batter_interferences INT NOT NULL DEFAULT 0,
    ADD COLUMN runner_interferences INT NOT NULL DEFAULT 0;

-- 기존 INTERFERENCE 데이터를 BATTER_INTERFERENCE로 마이그레이션
-- plate_appearance_result 컬럼이 'INTERFERENCE'인 기록은 대부분 타격방해이므로 batter_interferences로 이관
-- (실제 데이터가 있는 경우에만 적용됨)
COMMENT ON COLUMN batting_records.batter_interferences IS 'L-2: 타격방해 횟수';
COMMENT ON COLUMN batting_records.runner_interferences IS 'L-2: 주루방해 횟수';
COMMENT ON COLUMN season_batting_stats.batter_interferences IS 'L-2: 타격방해 횟수';
COMMENT ON COLUMN season_batting_stats.runner_interferences IS 'L-2: 주루방해 횟수';
COMMENT ON COLUMN career_batting_stats.batter_interferences IS 'L-2: 타격방해 횟수';
COMMENT ON COLUMN career_batting_stats.runner_interferences IS 'L-2: 주루방해 횟수';
