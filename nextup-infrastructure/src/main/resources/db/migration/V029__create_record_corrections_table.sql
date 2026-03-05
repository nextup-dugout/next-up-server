-- 기록 정정 이력 테이블
-- 관리자가 타격/투수 기록을 정정할 때의 변경 전/후 이력을 저장합니다.
CREATE TABLE record_corrections (
    id                 BIGSERIAL      PRIMARY KEY,
    game_id            BIGINT         NOT NULL,
    admin_user_id      BIGINT         NOT NULL,
    correction_type    VARCHAR(20)    NOT NULL,
    target_record_id   BIGINT         NOT NULL,
    field_name         VARCHAR(100)   NOT NULL,
    old_value          VARCHAR(500)   NOT NULL,
    new_value          VARCHAR(500)   NOT NULL,
    reason             TEXT           NOT NULL,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

-- 인덱스
CREATE INDEX idx_record_corrections_game_id
    ON record_corrections (game_id);

CREATE INDEX idx_record_corrections_target
    ON record_corrections (correction_type, target_record_id);

CREATE INDEX idx_record_corrections_admin
    ON record_corrections (admin_user_id);
