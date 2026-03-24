-- V048: 부상 퇴장 사유 컬럼 추가 (#385)
-- game_players 테이블에 ejection_reason 컬럼을 추가합니다.
-- INJURY: 부상, EJECTION_BY_UMPIRE: 심판 퇴장, OTHER: 기타

ALTER TABLE game_players
    ADD COLUMN ejection_reason VARCHAR(30) DEFAULT NULL;

COMMENT ON COLUMN game_players.ejection_reason IS '퇴장 사유 (INJURY, EJECTION_BY_UMPIRE, OTHER). NULL이면 일반 교체.';
