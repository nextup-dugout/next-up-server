-- =============================================================================
-- V061: ON DELETE CASCADE → RESTRICT 전환 + FK 누락 8건 추가
-- =============================================================================
-- 야구 기록 서비스에서 선수/팀/경기 삭제 시 시즌/통산 통계가 연쇄 삭제되는 치명적 버그 수정.
-- 독립적 비즈니스 의미를 가진 레코드의 ON DELETE CASCADE를 RESTRICT로 전환하고,
-- 누락된 FK 제약조건 8건을 추가한다.
--
-- 제외 대상 (CASCADE 유지):
--   - ElementCollection 조인 테이블 (user_roles, mercenary_request_positions,
--     mercenary_application_positions) — 부모 삭제 시 값 타입 자식도 삭제가 올바름
--
-- Refs #481
-- =============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- Part 1: ON DELETE CASCADE → RESTRICT 전환
-- ─────────────────────────────────────────────────────────────────────────────

-- === V001: team_members ===
ALTER TABLE team_members DROP CONSTRAINT IF EXISTS fk_tm_team;
ALTER TABLE team_members ADD CONSTRAINT fk_tm_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

ALTER TABLE team_members DROP CONSTRAINT IF EXISTS fk_tm_user;
ALTER TABLE team_members ADD CONSTRAINT fk_tm_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE team_members DROP CONSTRAINT IF EXISTS fk_tm_player;
ALTER TABLE team_members ADD CONSTRAINT fk_tm_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;

-- === V001: team_join_requests ===
ALTER TABLE team_join_requests DROP CONSTRAINT IF EXISTS fk_tjr_team;
ALTER TABLE team_join_requests ADD CONSTRAINT fk_tjr_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

ALTER TABLE team_join_requests DROP CONSTRAINT IF EXISTS fk_tjr_user;
ALTER TABLE team_join_requests ADD CONSTRAINT fk_tjr_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE team_join_requests DROP CONSTRAINT IF EXISTS fk_tjr_player;
ALTER TABLE team_join_requests ADD CONSTRAINT fk_tjr_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;

-- === V001: team_blacklist ===
ALTER TABLE team_blacklist DROP CONSTRAINT IF EXISTS fk_tbl_team;
ALTER TABLE team_blacklist ADD CONSTRAINT fk_tbl_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

ALTER TABLE team_blacklist DROP CONSTRAINT IF EXISTS fk_tbl_user;
ALTER TABLE team_blacklist ADD CONSTRAINT fk_tbl_user
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE team_blacklist DROP CONSTRAINT IF EXISTS fk_tbl_player;
ALTER TABLE team_blacklist ADD CONSTRAINT fk_tbl_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;

ALTER TABLE team_blacklist DROP CONSTRAINT IF EXISTS fk_tbl_registered_by;
ALTER TABLE team_blacklist ADD CONSTRAINT fk_tbl_registered_by
    FOREIGN KEY (registered_by) REFERENCES users(id) ON DELETE RESTRICT;

-- === V001: availability_votes ===
ALTER TABLE availability_votes DROP CONSTRAINT IF EXISTS fk_av_game;
ALTER TABLE availability_votes ADD CONSTRAINT fk_av_game
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE RESTRICT;

ALTER TABLE availability_votes DROP CONSTRAINT IF EXISTS fk_av_member;
ALTER TABLE availability_votes ADD CONSTRAINT fk_av_member
    FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE RESTRICT;

-- === V009: team_recruitments (inline FK → named) ===
ALTER TABLE team_recruitments DROP CONSTRAINT IF EXISTS team_recruitments_team_id_fkey;
ALTER TABLE team_recruitments ADD CONSTRAINT fk_team_recruitments_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

-- === V010: match_requests ===
ALTER TABLE match_requests DROP CONSTRAINT IF EXISTS fk_match_requests_team;
ALTER TABLE match_requests ADD CONSTRAINT fk_match_requests_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

-- === V010: match_responses ===
ALTER TABLE match_responses DROP CONSTRAINT IF EXISTS fk_match_responses_match_request;
ALTER TABLE match_responses ADD CONSTRAINT fk_match_responses_match_request
    FOREIGN KEY (match_request_id) REFERENCES match_requests(id) ON DELETE RESTRICT;

ALTER TABLE match_responses DROP CONSTRAINT IF EXISTS fk_match_responses_respond_team;
ALTER TABLE match_responses ADD CONSTRAINT fk_match_responses_respond_team
    FOREIGN KEY (respond_team_id) REFERENCES teams(id) ON DELETE RESTRICT;

-- === V014: pitch_events ===
ALTER TABLE pitch_events DROP CONSTRAINT IF EXISTS fk_pitch_events_game;
ALTER TABLE pitch_events ADD CONSTRAINT fk_pitch_events_game
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE RESTRICT;

ALTER TABLE pitch_events DROP CONSTRAINT IF EXISTS fk_pitch_events_pitcher;
ALTER TABLE pitch_events ADD CONSTRAINT fk_pitch_events_pitcher
    FOREIGN KEY (pitcher_id) REFERENCES game_players(id) ON DELETE RESTRICT;

ALTER TABLE pitch_events DROP CONSTRAINT IF EXISTS fk_pitch_events_batter;
ALTER TABLE pitch_events ADD CONSTRAINT fk_pitch_events_batter
    FOREIGN KEY (batter_id) REFERENCES game_players(id) ON DELETE RESTRICT;

-- === V015: certificates ===
ALTER TABLE certificates DROP CONSTRAINT IF EXISTS fk_cert_player;
ALTER TABLE certificates ADD CONSTRAINT fk_cert_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;

-- === V016: attendance_polls ===
ALTER TABLE attendance_polls DROP CONSTRAINT IF EXISTS fk_attendance_polls_team;
ALTER TABLE attendance_polls ADD CONSTRAINT fk_attendance_polls_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

-- === V016: poll_votes ===
ALTER TABLE poll_votes DROP CONSTRAINT IF EXISTS fk_poll_votes_poll;
ALTER TABLE poll_votes ADD CONSTRAINT fk_poll_votes_poll
    FOREIGN KEY (poll_id) REFERENCES attendance_polls(id) ON DELETE RESTRICT;

ALTER TABLE poll_votes DROP CONSTRAINT IF EXISTS fk_poll_votes_player;
ALTER TABLE poll_votes ADD CONSTRAINT fk_poll_votes_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;

-- === V016: activity_scores ===
ALTER TABLE activity_scores DROP CONSTRAINT IF EXISTS fk_activity_scores_team;
ALTER TABLE activity_scores ADD CONSTRAINT fk_activity_scores_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

ALTER TABLE activity_scores DROP CONSTRAINT IF EXISTS fk_activity_scores_member;
ALTER TABLE activity_scores ADD CONSTRAINT fk_activity_scores_member
    FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE RESTRICT;

-- === V025: competition_players ===
ALTER TABLE competition_players DROP CONSTRAINT IF EXISTS fk_competition_players_competition;
ALTER TABLE competition_players ADD CONSTRAINT fk_competition_players_competition
    FOREIGN KEY (competition_id) REFERENCES competitions(id) ON DELETE RESTRICT;

ALTER TABLE competition_players DROP CONSTRAINT IF EXISTS fk_competition_players_team;
ALTER TABLE competition_players ADD CONSTRAINT fk_competition_players_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

ALTER TABLE competition_players DROP CONSTRAINT IF EXISTS fk_competition_players_player;
ALTER TABLE competition_players ADD CONSTRAINT fk_competition_players_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;

-- === V028: fielding_records ===
ALTER TABLE fielding_records DROP CONSTRAINT IF EXISTS fk_fielding_records_game_player;
ALTER TABLE fielding_records ADD CONSTRAINT fk_fielding_records_game_player
    FOREIGN KEY (game_player_id) REFERENCES game_players(id) ON DELETE RESTRICT;

-- === V031: season_fielding_stats ===
ALTER TABLE season_fielding_stats DROP CONSTRAINT IF EXISTS fk_season_fielding_stats_player;
ALTER TABLE season_fielding_stats ADD CONSTRAINT fk_season_fielding_stats_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;

-- === V031: career_fielding_stats ===
ALTER TABLE career_fielding_stats DROP CONSTRAINT IF EXISTS fk_career_fielding_stats_player;
ALTER TABLE career_fielding_stats ADD CONSTRAINT fk_career_fielding_stats_player
    FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT;


-- ─────────────────────────────────────────────────────────────────────────────
-- Part 2: 누락된 FK 제약조건 추가
-- ─────────────────────────────────────────────────────────────────────────────

-- 1. correction_requests.game_id → games(id)
ALTER TABLE correction_requests
    ADD CONSTRAINT fk_correction_requests_game
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE RESTRICT;

-- 2. correction_requests.requester_user_id → users(id)
ALTER TABLE correction_requests
    ADD CONSTRAINT fk_correction_requests_requester
    FOREIGN KEY (requester_user_id) REFERENCES users(id) ON DELETE RESTRICT;

-- 3. correction_requests.reviewer_user_id → users(id) (nullable)
ALTER TABLE correction_requests
    ADD CONSTRAINT fk_correction_requests_reviewer
    FOREIGN KEY (reviewer_user_id) REFERENCES users(id) ON DELETE RESTRICT;

-- 4. correction_requests.target_record_id — polymorphic (CHECK 제약)
--    correction_type에 따라 다른 테이블을 참조하므로 FK 대신 CHECK 제약으로 보호
ALTER TABLE correction_requests
    ADD CONSTRAINT chk_correction_requests_target_record_id
    CHECK (target_record_id > 0);

-- 5. games.scorer_id → users(id) (nullable)
ALTER TABLE games
    ADD CONSTRAINT fk_games_scorer
    FOREIGN KEY (scorer_id) REFERENCES users(id) ON DELETE RESTRICT;

-- 6. recruitment_applications.applicant_id → players(id)
ALTER TABLE recruitment_applications
    ADD CONSTRAINT fk_recruitment_applications_applicant
    FOREIGN KEY (applicant_id) REFERENCES players(id) ON DELETE RESTRICT;

-- 7. season_batting_stats.team_id → teams(id) (nullable)
ALTER TABLE season_batting_stats
    ADD CONSTRAINT fk_season_batting_stats_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

-- 8. season_pitching_stats.team_id → teams(id) (nullable)
ALTER TABLE season_pitching_stats
    ADD CONSTRAINT fk_season_pitching_stats_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;

-- 9. season_fielding_stats.team_id → teams(id) (nullable, 이슈 본문 2건 + 추가 발견 1건)
ALTER TABLE season_fielding_stats
    ADD CONSTRAINT fk_season_fielding_stats_team
    FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE RESTRICT;
