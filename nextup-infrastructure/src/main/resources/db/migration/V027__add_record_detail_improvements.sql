-- V024: 기록 상세화 (D-15 타점 경로, D-17 타순 위반, D-19 수비 이닝 이력, D-20 투구 수 제한)
-- Issue #229

-- D-15: 타점 경로 - game_events에 득점 주자 ID 목록 컬럼 추가
ALTER TABLE game_events
    ADD COLUMN IF NOT EXISTS scoring_runner_ids TEXT;

COMMENT ON COLUMN game_events.scoring_runner_ids IS '득점 주자 ID 목록 (CSV 형식: playerId1,playerId2,...). 타석별 득점 경로 추적용.';

-- D-20: 투구 수 제한 - competitions 테이블의 game_rules에 투구 수 제한 컬럼 추가
ALTER TABLE competitions
    ADD COLUMN IF NOT EXISTS pitch_count_limit INTEGER,
    ADD COLUMN IF NOT EXISTS pitch_count_warning_threshold INTEGER NOT NULL DEFAULT 10;

COMMENT ON COLUMN competitions.pitch_count_limit IS '투구 수 제한 (NULL이면 무제한). 사회인 야구 투수 보호 규칙.';
COMMENT ON COLUMN competitions.pitch_count_warning_threshold IS '투구 수 경고 임박 기준 (제한 대비 몇 구 전에 경고할지, 기본 10구).';

-- D-20: pitch_count_limit 유효성 제약
ALTER TABLE competitions
    ADD CONSTRAINT chk_pitch_count_limit_positive
        CHECK (pitch_count_limit IS NULL OR pitch_count_limit > 0);

ALTER TABLE competitions
    ADD CONSTRAINT chk_pitch_count_warning_threshold_positive
        CHECK (pitch_count_warning_threshold > 0);
