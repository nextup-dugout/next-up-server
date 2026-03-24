-- #479: 8인 경기 라인업 하드코딩 수정
-- GameRules에 min_batting_order_count 컬럼 추가 (기본값 9, 사회인 야구 8인 경기 허용)
ALTER TABLE competitions
    ADD COLUMN IF NOT EXISTS min_batting_order_count INTEGER NOT NULL DEFAULT 9;
