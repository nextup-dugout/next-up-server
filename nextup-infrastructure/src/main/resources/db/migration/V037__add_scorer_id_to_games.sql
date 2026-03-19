-- #332: 기록원 경기 독점 잠금 메커니즘
ALTER TABLE games ADD COLUMN scorer_id BIGINT;

CREATE INDEX idx_games_scorer_id ON games(scorer_id) WHERE scorer_id IS NOT NULL;
