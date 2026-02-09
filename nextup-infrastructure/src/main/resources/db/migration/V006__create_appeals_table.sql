-- 이의 제기/정정 신청 테이블 생성
CREATE TABLE appeals (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id),
    appealer_id BIGINT NOT NULL,
    appealer_name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL CHECK (type IN ('SCORING_ERROR', 'RECORD_CORRECTION', 'RULE_VIOLATION', 'OTHER')),
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewer_id BIGINT,
    reviewer_comment TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 인덱스 생성
CREATE INDEX idx_appeals_game_id ON appeals(game_id);
CREATE INDEX idx_appeals_appealer_id ON appeals(appealer_id);
CREATE INDEX idx_appeals_status ON appeals(status);
CREATE INDEX idx_appeals_game_status ON appeals(game_id, status);

-- 코멘트 추가
COMMENT ON TABLE appeals IS '경기 기록 이의 제기/정정 신청';
COMMENT ON COLUMN appeals.game_id IS '경기 ID';
COMMENT ON COLUMN appeals.appealer_id IS '신청자 ID (선수 또는 감독)';
COMMENT ON COLUMN appeals.appealer_name IS '신청자 이름';
COMMENT ON COLUMN appeals.type IS '이의 제기 유형: SCORING_ERROR(득점 오류), RECORD_CORRECTION(기록 정정), RULE_VIOLATION(규칙 위반), OTHER(기타)';
COMMENT ON COLUMN appeals.title IS '제목';
COMMENT ON COLUMN appeals.description IS '상세 설명';
COMMENT ON COLUMN appeals.status IS '처리 상태: PENDING(대기), APPROVED(승인), REJECTED(반려)';
COMMENT ON COLUMN appeals.reviewer_id IS '검토자 ID (관리자)';
COMMENT ON COLUMN appeals.reviewer_comment IS '검토 의견';
COMMENT ON COLUMN appeals.reviewed_at IS '검토 일시';
