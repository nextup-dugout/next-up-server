-- V058: Remove transfer logic from player_team_histories
-- Issue #474: 사회인 야구에 "이적" 개념 없음. 탈퇴 + 새 팀 가입으로 처리.

-- Step 1: Migrate existing TRANSFERRED records to INACTIVE
UPDATE player_team_histories
SET status = 'INACTIVE'
WHERE status = 'TRANSFERRED';

-- Step 2: Drop the contract_type column (no longer needed)
ALTER TABLE player_team_histories DROP COLUMN IF EXISTS contract_type;
