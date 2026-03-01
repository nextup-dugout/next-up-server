-- V018: 구장 예약 양도 테이블 생성 (Issue #138)

CREATE TABLE booking_transfers (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT NOT NULL REFERENCES stadium_bookings(id),
    seller_team_id BIGINT NOT NULL,
    transfer_price DECIMAL(10, 2),
    message VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    buyer_team_id BIGINT,
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_transfer_status CHECK (status IN ('OPEN', 'ACCEPTED', 'CANCELLED', 'EXPIRED'))
);

-- 인덱스
CREATE INDEX idx_booking_transfers_booking ON booking_transfers(booking_id);
CREATE INDEX idx_booking_transfers_seller ON booking_transfers(seller_team_id);
CREATE INDEX idx_booking_transfers_status ON booking_transfers(status);
CREATE INDEX idx_booking_transfers_expires ON booking_transfers(expires_at) WHERE status = 'OPEN';
