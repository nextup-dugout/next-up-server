-- V059: BookingTransfer 과잉 로직 삭제 + 단순 양도 재설계 (Issue #473)
-- 기존 가격 거래(transferPrice) + OPEN/ACCEPTED/CANCELLED/EXPIRED 상태머신을 삭제하고,
-- 단순 양도(fromTeam → toTeam, PENDING/ACCEPTED/REJECTED)로 재설계

-- 기존 테이블 삭제
DROP TABLE IF EXISTS booking_transfers;

-- 새 테이블 생성
CREATE TABLE booking_transfers (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES stadium_bookings(id),
    from_team_id BIGINT NOT NULL,
    to_team_id BIGINT NOT NULL,
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transfer_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT chk_transfer_different_teams CHECK (from_team_id != to_team_id)
);

-- 인덱스
CREATE INDEX idx_booking_transfers_booking ON booking_transfers(booking_id);
CREATE INDEX idx_booking_transfers_from_team ON booking_transfers(from_team_id);
CREATE INDEX idx_booking_transfers_to_team ON booking_transfers(to_team_id);
CREATE INDEX idx_booking_transfers_status ON booking_transfers(status);
CREATE INDEX idx_booking_transfers_pending ON booking_transfers(booking_id) WHERE status = 'PENDING';
