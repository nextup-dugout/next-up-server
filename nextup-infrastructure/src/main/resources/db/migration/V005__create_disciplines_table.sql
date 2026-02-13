-- 징계 관리 테이블 생성
CREATE TABLE disciplines (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id),
    competition_id BIGINT NOT NULL REFERENCES competitions(id),
    type VARCHAR(20) NOT NULL CHECK (type IN ('WARNING', 'SUSPENSION', 'BAN')),
    reason TEXT NOT NULL,
    suspension_games INTEGER,
    issued_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    issued_by VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SERVED', 'CANCELLED')),
    served_games INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 인덱스 생성
CREATE INDEX idx_disciplines_player_id ON disciplines(player_id);
CREATE INDEX idx_disciplines_competition_id ON disciplines(competition_id);
CREATE INDEX idx_disciplines_status ON disciplines(status);
CREATE INDEX idx_disciplines_player_competition_status ON disciplines(player_id, competition_id, status);

-- 코멘트 추가
COMMENT ON TABLE disciplines IS '선수 징계 정보';
COMMENT ON COLUMN disciplines.type IS '징계 유형: WARNING(경고), SUSPENSION(출장정지), BAN(영구제재)';
COMMENT ON COLUMN disciplines.reason IS '징계 사유';
COMMENT ON COLUMN disciplines.suspension_games IS '출장 정지 경기 수 (SUSPENSION 타입인 경우)';
COMMENT ON COLUMN disciplines.issued_at IS '징계 발급일시';
COMMENT ON COLUMN disciplines.expires_at IS '징계 만료일시 (선택사항)';
COMMENT ON COLUMN disciplines.issued_by IS '징계 발급자 (관리자 이름 또는 ID)';
COMMENT ON COLUMN disciplines.status IS '징계 상태: ACTIVE(활성), SERVED(이행완료), CANCELLED(취소)';
COMMENT ON COLUMN disciplines.served_games IS '소화한 경기 수 (SUSPENSION 타입인 경우)';
