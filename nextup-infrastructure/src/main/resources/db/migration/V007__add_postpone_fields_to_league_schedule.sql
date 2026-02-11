-- 우천 순연 필드 추가
ALTER TABLE league_schedules ADD COLUMN IF NOT EXISTS postponed_reason VARCHAR(500);
ALTER TABLE league_schedules ADD COLUMN IF NOT EXISTS original_date DATE;
