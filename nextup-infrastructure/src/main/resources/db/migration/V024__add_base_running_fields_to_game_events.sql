-- 주루 플레이 상세 기록 컬럼 추가 (game_events 테이블)
ALTER TABLE game_events
    ADD COLUMN runner_player_id BIGINT REFERENCES game_players(id),
    ADD COLUMN from_base        VARCHAR(10),
    ADD COLUMN to_base          VARCHAR(10),
    ADD COLUMN base_running_result VARCHAR(30);

COMMENT ON COLUMN game_events.runner_player_id  IS '주루 플레이 주자 GamePlayer ID';
COMMENT ON COLUMN game_events.from_base          IS '출발 베이스 (FIRST/SECOND/THIRD/HOME)';
COMMENT ON COLUMN game_events.to_base            IS '도착 베이스 (FIRST/SECOND/THIRD/HOME)';
COMMENT ON COLUMN game_events.base_running_result IS '주루 플레이 결과 (STOLEN_BASE/CAUGHT_STEALING/PICKED_OFF 등)';
