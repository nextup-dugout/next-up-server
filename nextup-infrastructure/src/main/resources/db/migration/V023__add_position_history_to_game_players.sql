-- V023: game_players 테이블에 포지션 이력 컬럼 추가
-- 이슈 #229: 수비 이닝 이력 추적

ALTER TABLE game_players
    ADD COLUMN IF NOT EXISTS position_history TEXT;

COMMENT ON COLUMN game_players.position_history IS '포지션 변경 이력 (형식: "이닝:포지션명,이닝:포지션명", 예: "1:PITCHER,3:FIRST_BASE")';
