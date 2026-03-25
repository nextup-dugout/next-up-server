-- ============================================
-- Migration: V066__drop_mercenary_tables
-- Drop legacy mercenary system tables
-- ============================================

DROP TABLE IF EXISTS mercenary_participations;
DROP TABLE IF EXISTS mercenary_applications;
DROP TABLE IF EXISTS mercenary_request_positions;
DROP TABLE IF EXISTS mercenary_requests;

-- Remove max_mercenary_count column from game_rules (embedded in competitions)
ALTER TABLE competitions DROP COLUMN IF EXISTS max_mercenary_count;

-- Rollback
-- CREATE TABLE mercenary_requests (...);
-- CREATE TABLE mercenary_request_positions (...);
-- CREATE TABLE mercenary_applications (...);
-- CREATE TABLE mercenary_participations (...);
-- ALTER TABLE competitions ADD COLUMN max_mercenary_count INTEGER;
