-- V035: bracket_entries 테이블에 game_id 컬럼 추가
-- BracketEntry → Game 연결을 위한 FK 컬럼

ALTER TABLE bracket_entries
    ADD COLUMN game_id BIGINT NULL,
    ADD CONSTRAINT fk_bracket_entries_game
        FOREIGN KEY (game_id) REFERENCES games (id) ON DELETE SET NULL;

CREATE INDEX idx_bracket_entries_game ON bracket_entries (game_id);
