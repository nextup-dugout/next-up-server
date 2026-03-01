-- V022: Add inherited runner pitcher tracking columns to games table
-- Issue #183: Track which pitcher is responsible for each base runner
-- When a pitcher is replaced, runners left on base are "inherited" by the new pitcher,
-- but any runs scored by those runners are charged to the original pitcher.

ALTER TABLE games
    ADD COLUMN runner_on_first_pitcher_id  BIGINT NULL,
    ADD COLUMN runner_on_second_pitcher_id BIGINT NULL,
    ADD COLUMN runner_on_third_pitcher_id  BIGINT NULL;

COMMENT ON COLUMN games.runner_on_first_pitcher_id  IS '1루 주자 담당 투수 game_player_id (계승 주자 추적)';
COMMENT ON COLUMN games.runner_on_second_pitcher_id IS '2루 주자 담당 투수 game_player_id (계승 주자 추적)';
COMMENT ON COLUMN games.runner_on_third_pitcher_id  IS '3루 주자 담당 투수 game_player_id (계승 주자 추적)';
