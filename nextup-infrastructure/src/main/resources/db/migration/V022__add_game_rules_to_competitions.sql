-- #185: 사회인 야구 규칙 설정 체계(GameRules) 구축
-- competitions 테이블에 경기 규칙 관련 컬럼 추가
-- 기존 데이터 호환성을 위해 모든 컬럼에 DEFAULT 값 설정

ALTER TABLE competitions
    ADD COLUMN IF NOT EXISTS default_innings                  INT            NOT NULL DEFAULT 9,
    ADD COLUMN IF NOT EXISTS mercy_rule_enabled               BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS mercy_run_difference             INT            NULL,
    ADD COLUMN IF NOT EXISTS mercy_minimum_inning             INT            NULL,
    ADD COLUMN IF NOT EXISTS max_extra_innings                INT            NULL,
    ADD COLUMN IF NOT EXISTS tied_game_result                 VARCHAR(20)    NOT NULL DEFAULT 'DRAW',
    ADD COLUMN IF NOT EXISTS tiebreaker_enabled               BOOLEAN        NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS forfeit_score                    INT            NOT NULL DEFAULT 7,
    ADD COLUMN IF NOT EXISTS starter_win_qualification_outs   INT            NOT NULL DEFAULT 15,
    ADD COLUMN IF NOT EXISTS qualification_pa_multiplier      DOUBLE PRECISION NOT NULL DEFAULT 3.1,
    ADD COLUMN IF NOT EXISTS qualification_ip_multiplier      DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    ADD COLUMN IF NOT EXISTS time_limit_minutes               INT            NULL;
