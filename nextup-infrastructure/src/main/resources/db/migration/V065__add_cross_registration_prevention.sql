-- V065: 크로스 등록 방지 — 동일 대회 다른 팀 이중 등록 방지 (Issue #486)
--
-- 기존 uk_competition_players_competition_player (competition_id, player_id) 제약은
-- WITHDRAWN 상태의 선수도 재등록을 차단합니다.
-- 부분 고유 인덱스(partial unique index)로 교체하여:
--   1. 활성(ACTIVE/SUSPENDED) 상태에서는 동일 대회 이중 등록 차단
--   2. WITHDRAWN 후 다른 팀으로 재등록 허용

-- 기존 unique constraint 삭제
ALTER TABLE competition_players
    DROP CONSTRAINT IF EXISTS uk_competition_players_competition_player;

-- 활성 상태 선수에 대해서만 적용되는 부분 고유 인덱스 추가
-- WITHDRAWN이 아닌 상태에서 동일 대회 + 동일 선수 조합을 차단
CREATE UNIQUE INDEX uk_competition_players_active_registration
    ON competition_players (competition_id, player_id)
    WHERE status <> 'WITHDRAWN';

COMMENT ON INDEX uk_competition_players_active_registration
    IS '크로스 등록 방지: 활성 상태에서 동일 대회 이중 등록 차단 (WITHDRAWN 후 재등록은 허용)';
