-- ============================================
-- Migration: V056__unify_timestamp_to_timestamptz
-- Issue: #471 — TIMESTAMP → TIMESTAMPTZ 통일
-- ============================================
-- BaseTimeEntity는 Instant(UTC)를 사용하지만, V001~V010 및 일부 이후
-- 마이그레이션이 TIMESTAMP (TZ 없음)으로 컬럼을 생성했다.
-- JVM 타임존이 변경되면 데이터 왜곡이 발생하므로,
-- 모든 TIMESTAMP 컬럼을 TIMESTAMP WITH TIME ZONE으로 통일한다.
--
-- PostgreSQL에서 ALTER TYPE TIMESTAMP → TIMESTAMPTZ는
-- 기존 값을 session timezone 기준으로 해석하므로,
-- SET timezone = 'UTC'를 먼저 실행하여 데이터 왜곡을 방지한다.
-- ============================================

-- 세션 타임존을 UTC로 설정 (기존 TIMESTAMP 값이 UTC로 해석되도록)
SET timezone = 'UTC';

-- =====================
-- 1. associations
-- =====================
ALTER TABLE associations
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 2. leagues
-- =====================
ALTER TABLE leagues
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 3. teams
-- =====================
ALTER TABLE teams
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 4. players
-- =====================
ALTER TABLE players
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 5. users
-- =====================
ALTER TABLE users
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 6. oauth_accounts
-- =====================
ALTER TABLE oauth_accounts
    ALTER COLUMN connected_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at   TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at   TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 7. refresh_tokens
-- =====================
ALTER TABLE refresh_tokens
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 8. organization_admins
-- =====================
ALTER TABLE organization_admins
    ALTER COLUMN assigned_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at  TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 9. player_careers
-- =====================
ALTER TABLE player_careers
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 10. player_team_histories
-- =====================
ALTER TABLE player_team_histories
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 11. competitions
-- =====================
ALTER TABLE competitions
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 12. games
-- =====================
ALTER TABLE games
    ALTER COLUMN scheduled_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN started_at   TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN ended_at     TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN locked_at    TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at   TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at   TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 13. game_teams
-- =====================
ALTER TABLE game_teams
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 14. game_players
-- =====================
ALTER TABLE game_players
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 15. game_events
-- =====================
ALTER TABLE game_events
    ALTER COLUMN event_timestamp TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at      TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at      TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 16. batting_records
-- =====================
ALTER TABLE batting_records
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 17. pitching_records
-- =====================
ALTER TABLE pitching_records
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 18. lineup_submissions
-- =====================
ALTER TABLE lineup_submissions
    ALTER COLUMN submitted_at        TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN confirmed_at        TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN exchange_pending_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at          TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at          TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 19. lineup_entries
-- =====================
ALTER TABLE lineup_entries
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 20. team_members (V001)
-- =====================
ALTER TABLE team_members
    ALTER COLUMN joined_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN left_at    TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 21. team_join_requests (V001)
-- =====================
ALTER TABLE team_join_requests
    ALTER COLUMN requested_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN processed_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at   TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at   TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 22. team_blacklists (V001)
-- =====================
ALTER TABLE team_blacklists
    ALTER COLUMN registered_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN expires_at    TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at    TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at    TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 23. attendance_votes (V001)
-- =====================
ALTER TABLE attendance_votes
    ALTER COLUMN responded_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at   TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at   TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 24. league_schedules (V002)
-- =====================
ALTER TABLE league_schedules
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 25. disciplines (V005)
-- =====================
ALTER TABLE disciplines
    ALTER COLUMN issued_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 26. appeals (V006)
-- =====================
ALTER TABLE appeals
    ALTER COLUMN reviewed_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at  TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 27. notifications (V008)
-- =====================
ALTER TABLE notifications
    ALTER COLUMN read_at    TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN sent_at    TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 28. device_tokens (V008)
-- =====================
ALTER TABLE device_tokens
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 29. notification_preferences (V008)
-- =====================
ALTER TABLE notification_preferences
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 30. team_recruitments (V009)
-- =====================
ALTER TABLE team_recruitments
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 31. match_requests (V010)
-- =====================
ALTER TABLE match_requests
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 32. match_responses (V010)
-- =====================
ALTER TABLE match_responses
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 33. stadiums (V012)
-- =====================
ALTER TABLE stadiums
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 34. stadium_slots (V012)
-- =====================
ALTER TABLE stadium_slots
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 35. stadium_bookings (V012)
-- =====================
ALTER TABLE stadium_bookings
    ALTER COLUMN booked_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 36. pitch_events (V014)
-- =====================
ALTER TABLE pitch_events
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 37. certificates (V015)
-- =====================
ALTER TABLE certificates
    ALTER COLUMN issued_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN expires_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 38. attendance_polls (V016) — event_date, deadline만 (created_at/updated_at은 이미 TIMESTAMPTZ)
-- =====================
ALTER TABLE attendance_polls
    ALTER COLUMN event_date TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN deadline   TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 39. booking_transfers (V030)
-- =====================
ALTER TABLE booking_transfers
    ALTER COLUMN expires_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN accepted_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at  TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 40. season_batting_stats (V034)
-- =====================
ALTER TABLE season_batting_stats
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 41. season_pitching_stats (V034)
-- =====================
ALTER TABLE season_pitching_stats
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 42. career_batting_stats (V034)
-- =====================
ALTER TABLE career_batting_stats
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 43. career_pitching_stats (V034)
-- =====================
ALTER TABLE career_pitching_stats
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 44. team_schedules (V041)
-- =====================
ALTER TABLE team_schedules
    ALTER COLUMN start_at   TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN end_at     TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at TYPE TIMESTAMP WITH TIME ZONE;

-- =====================
-- 45. correction_requests (V042)
-- =====================
ALTER TABLE correction_requests
    ALTER COLUMN reviewed_at TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN created_at  TYPE TIMESTAMP WITH TIME ZONE,
    ALTER COLUMN updated_at  TYPE TIMESTAMP WITH TIME ZONE;

-- Rollback (주석 보관)
-- SET timezone = 'UTC';
-- ALTER TABLE associations ALTER COLUMN created_at TYPE TIMESTAMP, ALTER COLUMN updated_at TYPE TIMESTAMP;
-- (모든 테이블에 대해 역방향 ALTER 수행)
