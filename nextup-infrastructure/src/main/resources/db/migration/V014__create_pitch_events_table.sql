-- 투구 이벤트 테이블 생성
CREATE TABLE pitch_events
(
    id              BIGSERIAL PRIMARY KEY,
    game_id         BIGINT       NOT NULL,
    pitcher_id      BIGINT       NOT NULL,
    batter_id       BIGINT       NOT NULL,
    inning          INTEGER      NOT NULL CHECK (inning >= 1),
    is_top_inning   BOOLEAN      NOT NULL,
    pitch_number    INTEGER      NOT NULL CHECK (pitch_number >= 1),
    result          VARCHAR(20)  NOT NULL,
    ball_count      INTEGER      NOT NULL CHECK (ball_count >= 0 AND ball_count <= 4),
    strike_count    INTEGER      NOT NULL CHECK (strike_count >= 0 AND strike_count <= 3),
    description     VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pitch_events_game FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_events_pitcher FOREIGN KEY (pitcher_id) REFERENCES game_players (id) ON DELETE CASCADE,
    CONSTRAINT fk_pitch_events_batter FOREIGN KEY (batter_id) REFERENCES game_players (id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_pitch_events_game ON pitch_events (game_id);
CREATE INDEX idx_pitch_events_game_inning ON pitch_events (game_id, inning, is_top_inning);
CREATE INDEX idx_pitch_events_pitcher ON pitch_events (pitcher_id);
CREATE INDEX idx_pitch_events_batter ON pitch_events (batter_id);
CREATE INDEX idx_pitch_events_game_pitch_number ON pitch_events (game_id, pitch_number);

-- 코멘트 추가
COMMENT ON TABLE pitch_events IS '투구 이벤트 테이블 - 경기 중 발생하는 개별 투구를 기록';
COMMENT ON COLUMN pitch_events.id IS '투구 이벤트 ID';
COMMENT ON COLUMN pitch_events.game_id IS '경기 ID';
COMMENT ON COLUMN pitch_events.pitcher_id IS '투수 ID (game_players)';
COMMENT ON COLUMN pitch_events.batter_id IS '타자 ID (game_players)';
COMMENT ON COLUMN pitch_events.inning IS '이닝';
COMMENT ON COLUMN pitch_events.is_top_inning IS '초/말 여부 (true: 초, false: 말)';
COMMENT ON COLUMN pitch_events.pitch_number IS '투구 번호 (경기 내 순서)';
COMMENT ON COLUMN pitch_events.result IS '투구 결과 (BALL, STRIKE, FOUL, SWING_MISS, IN_PLAY)';
COMMENT ON COLUMN pitch_events.ball_count IS '투구 후 볼카운트 (0-4)';
COMMENT ON COLUMN pitch_events.strike_count IS '투구 후 스트라이크 카운트 (0-3)';
COMMENT ON COLUMN pitch_events.description IS '투구 설명 (선택)';
