-- V024: 수비 기록 테이블 생성
CREATE TABLE fielding_records (
    id                BIGSERIAL PRIMARY KEY,
    game_player_id    BIGINT       NOT NULL,
    put_outs          INTEGER      NOT NULL DEFAULT 0,
    assists           INTEGER      NOT NULL DEFAULT 0,
    errors            INTEGER      NOT NULL DEFAULT 0,
    double_plays      INTEGER      NOT NULL DEFAULT 0,
    passed_balls      INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_fielding_records_game_player
        FOREIGN KEY (game_player_id) REFERENCES game_players(id) ON DELETE CASCADE
);

CREATE INDEX idx_fielding_records_game_player ON fielding_records(game_player_id);
