-- M-17: 기록 정정 요청 승인 워크플로우 테이블
CREATE TABLE correction_requests (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    requester_user_id BIGINT NOT NULL,
    correction_type VARCHAR(20) NOT NULL,
    target_record_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    new_value VARCHAR(500) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_user_id BIGINT,
    review_comment TEXT,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_correction_requests_game_id ON correction_requests(game_id);
CREATE INDEX idx_correction_requests_status ON correction_requests(status);
CREATE INDEX idx_correction_requests_requester ON correction_requests(requester_user_id);
