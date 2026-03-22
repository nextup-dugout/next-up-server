-- 시즌 타이틀에 대회 ID 컬럼 추가
-- 대회별 타이틀 조회 및 대회 종료 시 자동 부여를 위한 연결
ALTER TABLE season_awards ADD COLUMN competition_id BIGINT REFERENCES competitions(id);

CREATE INDEX idx_season_awards_competition ON season_awards(competition_id);
