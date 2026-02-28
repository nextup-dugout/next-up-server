-- Add max_team_count column to leagues table
-- Allows configuring the maximum number of teams a league can accept
-- NULL means no limit
ALTER TABLE leagues
    ADD COLUMN IF NOT EXISTS max_team_count INTEGER;
