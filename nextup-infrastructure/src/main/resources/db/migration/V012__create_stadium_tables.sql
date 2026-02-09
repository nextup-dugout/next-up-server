-- 구장 테이블
CREATE TABLE stadiums (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    capacity INTEGER,
    facilities VARCHAR(500),
    contact_info VARCHAR(255),
    image_urls VARCHAR(1000),
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 구장 인덱스
CREATE INDEX idx_stadiums_is_active ON stadiums(is_active);
CREATE INDEX idx_stadiums_location ON stadiums(latitude, longitude);

-- 구장 슬롯 테이블
CREATE TABLE stadium_slots (
    id BIGSERIAL PRIMARY KEY,
    stadium_id BIGINT NOT NULL REFERENCES stadiums(id),
    date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    price DECIMAL(10, 2),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_slot_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'MAINTENANCE'))
);

-- 구장 슬롯 인덱스
CREATE INDEX idx_stadium_slots_stadium_date ON stadium_slots(stadium_id, date);
CREATE INDEX idx_stadium_slots_status ON stadium_slots(status);

-- 구장 예약 테이블
CREATE TABLE stadium_bookings (
    id BIGSERIAL PRIMARY KEY,
    slot_id BIGINT NOT NULL REFERENCES stadium_slots(id),
    team_id BIGINT NOT NULL,
    booked_by BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    booked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_booking_status CHECK (status IN ('CONFIRMED', 'CANCELLED', 'COMPLETED'))
);

-- 구장 예약 인덱스
CREATE INDEX idx_stadium_bookings_slot ON stadium_bookings(slot_id);
CREATE INDEX idx_stadium_bookings_team ON stadium_bookings(team_id);
CREATE INDEX idx_stadium_bookings_status ON stadium_bookings(status);
