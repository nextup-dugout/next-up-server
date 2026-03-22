-- L-13: GameRules에 순위 동률 타이브레이커 기준 순서 컬럼 추가
ALTER TABLE competitions ADD COLUMN IF NOT EXISTS standings_tiebreaker_order VARCHAR(255) NOT NULL DEFAULT 'HEAD_TO_HEAD,RUN_DIFFERENTIAL,RUNS_SCORED';
