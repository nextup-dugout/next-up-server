-- V015: Create certificates table
-- Description: 기록 증명서 관리 시스템

-- ===========================================================================
-- certificates 테이블
-- ===========================================================================
CREATE TABLE certificates (
    id BIGSERIAL PRIMARY KEY,
    issue_number VARCHAR(36) NOT NULL UNIQUE,
    player_id BIGINT NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'VALID',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_cert_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,

    -- Constraints
    CONSTRAINT chk_cert_status CHECK (status IN ('VALID', 'EXPIRED', 'REVOKED'))
);

-- Indexes for performance
CREATE UNIQUE INDEX idx_cert_issue_number ON certificates(issue_number);
CREATE INDEX idx_cert_player_id ON certificates(player_id);
CREATE INDEX idx_cert_status ON certificates(status);
CREATE INDEX idx_cert_expires_at ON certificates(expires_at);
CREATE INDEX idx_cert_player_status ON certificates(player_id, status);

COMMENT ON TABLE certificates IS '기록 증명서 - 선수의 경기 기록을 증명하는 공식 문서';
COMMENT ON COLUMN certificates.issue_number IS '발급 번호 (UUID)';
COMMENT ON COLUMN certificates.status IS 'VALID(유효), EXPIRED(만료), REVOKED(취소)';
COMMENT ON COLUMN certificates.issued_at IS '발급 일시';
COMMENT ON COLUMN certificates.expires_at IS '만료 일시';
