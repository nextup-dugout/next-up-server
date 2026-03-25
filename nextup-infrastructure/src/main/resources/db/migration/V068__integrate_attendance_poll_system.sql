-- ============================================
-- Migration: V068 — 출석 투표 시스템 통합
-- ============================================
-- 1. attendance_polls에 category, game_id 컬럼 추가
-- 2. attendance_votes 테이블 DROP (GameParticipation 제거)
-- 3. 인덱스 추가

-- 1. attendance_polls에 이벤트 카테고리 컬럼 추가
ALTER TABLE attendance_polls
    ADD COLUMN IF NOT EXISTS category VARCHAR(20) NOT NULL DEFAULT 'OTHER';

-- 2. attendance_polls에 game_id 컬럼 추가 (GAME 카테고리일 때 사용)
ALTER TABLE attendance_polls
    ADD COLUMN IF NOT EXISTS game_id BIGINT;

-- 3. 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_ap_category ON attendance_polls(category);
CREATE INDEX IF NOT EXISTS idx_ap_game_id ON attendance_polls(game_id);

-- 4. attendance_votes 테이블 DROP (GameParticipation 제거)
-- 이 테이블은 경기별 출석 투표 전용이었으며, 이제 attendance_polls + poll_votes로 통합됩니다.
DROP TABLE IF EXISTS attendance_votes;

-- Rollback
-- ALTER TABLE attendance_polls DROP COLUMN IF EXISTS category;
-- ALTER TABLE attendance_polls DROP COLUMN IF EXISTS game_id;
-- DROP INDEX IF EXISTS idx_ap_category;
-- DROP INDEX IF EXISTS idx_ap_game_id;
