-- 알림 테이블
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    data TEXT,
    read_at TIMESTAMP,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 알림 인덱스
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_user_type ON notifications(user_id, type);
CREATE INDEX idx_notifications_sent_at ON notifications(sent_at);

-- 알림 테이블 코멘트
COMMENT ON TABLE notifications IS '푸시 알림';
COMMENT ON COLUMN notifications.user_id IS '사용자 ID';
COMMENT ON COLUMN notifications.type IS '알림 타입 (GAME_START, TEAM_NOTICE, ATTENDANCE_NUDGE, RECORD_ALERT)';
COMMENT ON COLUMN notifications.title IS '알림 제목';
COMMENT ON COLUMN notifications.body IS '알림 내용';
COMMENT ON COLUMN notifications.data IS '추가 데이터 (JSON)';
COMMENT ON COLUMN notifications.read_at IS '읽은 시각';
COMMENT ON COLUMN notifications.sent_at IS '전송 시각';

-- 디바이스 토큰 테이블
CREATE TABLE device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 디바이스 토큰 인덱스
CREATE INDEX idx_device_tokens_user_id ON device_tokens(user_id);
CREATE UNIQUE INDEX idx_device_tokens_token ON device_tokens(token);
CREATE INDEX idx_device_tokens_user_platform ON device_tokens(user_id, platform);

-- 디바이스 토큰 테이블 코멘트
COMMENT ON TABLE device_tokens IS 'FCM 디바이스 토큰';
COMMENT ON COLUMN device_tokens.user_id IS '사용자 ID';
COMMENT ON COLUMN device_tokens.token IS 'FCM 토큰';
COMMENT ON COLUMN device_tokens.platform IS '플랫폼 (IOS, ANDROID, WEB)';

-- 알림 설정 테이블
CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 알림 설정 인덱스
CREATE INDEX idx_notification_preferences_user_id ON notification_preferences(user_id);
CREATE UNIQUE INDEX idx_notification_preferences_user_type ON notification_preferences(user_id, type);

-- 알림 설정 테이블 코멘트
COMMENT ON TABLE notification_preferences IS '알림 수신 설정';
COMMENT ON COLUMN notification_preferences.user_id IS '사용자 ID';
COMMENT ON COLUMN notification_preferences.type IS '알림 타입';
COMMENT ON COLUMN notification_preferences.enabled IS '수신 활성화 여부';
