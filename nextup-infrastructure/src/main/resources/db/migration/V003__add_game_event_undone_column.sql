-- V003: game_events 테이블에 undone 컬럼 추가 (기록원 Undo 시스템)
ALTER TABLE game_events ADD COLUMN undone BOOLEAN NOT NULL DEFAULT FALSE;

-- undone=false인 이벤트만 빠르게 조회하기 위한 인덱스
CREATE INDEX idx_game_events_game_undone ON game_events (game_id, undone, event_timestamp DESC);
