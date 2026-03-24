-- ============================================
-- V000: Base Schema — 핵심 테이블 CREATE
-- ============================================
-- 이 마이그레이션은 프로젝트 초기부터 존재했어야 할 핵심 테이블들을 생성합니다.
-- V001 이후의 ALTER TABLE / CREATE TABLE 마이그레이션이 참조하는
-- 기초 테이블(associations, leagues, teams, users, players, games 등)이 대상입니다.
--
-- 기존 환경: Hibernate auto-ddl 또는 수동으로 이미 테이블이 존재할 수 있으므로
-- 모두 IF NOT EXISTS 를 사용합니다.

-- =====================
-- 1. associations (최상위 — FK 없음)
-- =====================
CREATE TABLE IF NOT EXISTS associations (
    id             BIGSERIAL    PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    abbreviation   VARCHAR(20),
    region         VARCHAR(50),
    description    VARCHAR(500),
    logo_url       VARCHAR(255),
    website_url    VARCHAR(255),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_associations_region    ON associations(region);
CREATE INDEX IF NOT EXISTS idx_associations_is_active ON associations(is_active);

-- =====================
-- 2. leagues (→ associations)
-- =====================
CREATE TABLE IF NOT EXISTS leagues (
    id              BIGSERIAL    PRIMARY KEY,
    association_id  BIGINT       NOT NULL REFERENCES associations(id),
    name            VARCHAR(100) NOT NULL,
    abbreviation    VARCHAR(20),
    founded_year    INTEGER      NOT NULL,
    division_level  INTEGER,
    description     VARCHAR(500),
    logo_url        VARCHAR(255),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    max_team_count  INTEGER,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_leagues_association ON leagues(association_id);
CREATE INDEX IF NOT EXISTS idx_leagues_is_active   ON leagues(is_active);

-- =====================
-- 3. teams (→ leagues)
-- =====================
CREATE TABLE IF NOT EXISTS teams (
    id              BIGSERIAL    PRIMARY KEY,
    league_id       BIGINT       NOT NULL REFERENCES leagues(id),
    name            VARCHAR(100) NOT NULL,
    abbreviation    VARCHAR(20),
    city            VARCHAR(100) NOT NULL,
    founded_year    INTEGER      NOT NULL,
    logo_url        VARCHAR(255),
    primary_color   VARCHAR(50),
    secondary_color VARCHAR(50),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_teams_league_id ON teams(league_id);
CREATE INDEX IF NOT EXISTS idx_teams_is_active ON teams(is_active);

-- =====================
-- 4. players (FK 없음)
-- =====================
CREATE TABLE IF NOT EXISTS players (
    id                BIGSERIAL    PRIMARY KEY,
    name              VARCHAR(50)  NOT NULL,
    birth_date        DATE,
    birth_place       VARCHAR(50),
    nationality       VARCHAR(50),
    height            INTEGER,
    weight            INTEGER,
    throwing_hand     VARCHAR(10),
    batting_hand      VARCHAR(10),
    primary_position  VARCHAR(30)  NOT NULL,
    debut_year        INTEGER,
    retirement_year   INTEGER,
    profile_image_url VARCHAR(255),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_players_name       ON players(name);
CREATE INDEX IF NOT EXISTS idx_players_birth_date ON players(birth_date);

-- =====================
-- 5. users (→ players, optional)
-- =====================
CREATE TABLE IF NOT EXISTS users (
    id                BIGSERIAL    PRIMARY KEY,
    email             VARCHAR(100) NOT NULL UNIQUE,
    password          VARCHAR(255),
    nickname          VARCHAR(50)  NOT NULL,
    profile_image_url VARCHAR(255),
    player_id         BIGINT       REFERENCES players(id),
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email  ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_player         ON users(player_id);

-- =====================
-- 6. user_roles (ElementCollection → users)
-- =====================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT      NOT NULL REFERENCES users(id),
    role    VARCHAR(30) NOT NULL,
    PRIMARY KEY (user_id, role)
);

-- =====================
-- 7. oauth_accounts (→ users)
-- =====================
CREATE TABLE IF NOT EXISTS oauth_accounts (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id),
    provider     VARCHAR(20)  NOT NULL,
    oauth_id     VARCHAR(100) NOT NULL,
    email        VARCHAR(100),
    connected_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_oauth_provider_id UNIQUE (provider, oauth_id)
);

CREATE INDEX IF NOT EXISTS idx_oauth_user     ON oauth_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_oauth_provider ON oauth_accounts(provider);

-- =====================
-- 8. refresh_tokens (→ users by user_id column)
-- =====================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    token      VARCHAR(500) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    is_revoked BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_refresh_token_user    ON refresh_tokens(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_refresh_token_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires ON refresh_tokens(expires_at);

-- =====================
-- 9. organization_admins (→ users, polymorphic org)
-- =====================
CREATE TABLE IF NOT EXISTS organization_admins (
    id                BIGSERIAL    PRIMARY KEY,
    user_id           BIGINT       NOT NULL REFERENCES users(id),
    organization_type VARCHAR(20)  NOT NULL,
    organization_id   BIGINT       NOT NULL,
    role              VARCHAR(20)  NOT NULL,
    assigned_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by       BIGINT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_organization_admin_user_org UNIQUE (user_id, organization_type, organization_id)
);

CREATE INDEX IF NOT EXISTS idx_org_admin_user   ON organization_admins(user_id);
CREATE INDEX IF NOT EXISTS idx_org_admin_org    ON organization_admins(organization_type, organization_id);
CREATE INDEX IF NOT EXISTS idx_org_admin_active ON organization_admins(is_active);

-- =====================
-- 10. player_careers (→ players)
-- =====================
CREATE TABLE IF NOT EXISTS player_careers (
    id          BIGSERIAL    PRIMARY KEY,
    player_id   BIGINT       NOT NULL REFERENCES players(id),
    career_type VARCHAR(20)  NOT NULL,
    organization VARCHAR(100) NOT NULL,
    start_date  DATE         NOT NULL,
    end_date    DATE,
    position    VARCHAR(50),
    description VARCHAR(500),
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_player_careers_player ON player_careers(player_id);
CREATE INDEX IF NOT EXISTS idx_player_careers_type   ON player_careers(career_type);
CREATE INDEX IF NOT EXISTS idx_player_careers_dates  ON player_careers(player_id, start_date, end_date);

-- =====================
-- 11. player_team_histories (→ players, teams)
-- =====================
CREATE TABLE IF NOT EXISTS player_team_histories (
    id             BIGSERIAL    PRIMARY KEY,
    player_id      BIGINT       NOT NULL REFERENCES players(id),
    team_id        BIGINT       NOT NULL REFERENCES teams(id),
    start_date     DATE         NOT NULL,
    end_date       DATE,
    uniform_number INTEGER,
    position       VARCHAR(30)  NOT NULL,
    contract_type  VARCHAR(20)  NOT NULL DEFAULT 'REGULAR',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pth_player_id     ON player_team_histories(player_id);
CREATE INDEX IF NOT EXISTS idx_pth_team_id       ON player_team_histories(team_id);
CREATE INDEX IF NOT EXISTS idx_pth_player_dates  ON player_team_histories(player_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_pth_team_dates    ON player_team_histories(team_id, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_pth_current       ON player_team_histories(player_id, end_date);
CREATE INDEX IF NOT EXISTS idx_pth_status        ON player_team_histories(status);
CREATE INDEX IF NOT EXISTS idx_pth_player_status ON player_team_histories(player_id, status);

-- =====================
-- 12. competitions (→ leagues, embeds GameRules)
-- =====================
CREATE TABLE IF NOT EXISTS competitions (
    id                            BIGSERIAL    PRIMARY KEY,
    league_id                     BIGINT       NOT NULL REFERENCES leagues(id),
    name                          VARCHAR(100) NOT NULL,
    year                          INTEGER      NOT NULL,
    season                        INTEGER      NOT NULL DEFAULT 1,
    type                          VARCHAR(20)  NOT NULL DEFAULT 'LEAGUE',
    start_date                    DATE         NOT NULL,
    end_date                      DATE,
    status                        VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    description                   VARCHAR(500),
    max_teams                     INTEGER,
    playoff_teams                 INTEGER,
    -- GameRules embedded fields
    default_innings               INTEGER      NOT NULL DEFAULT 9,
    mercy_rule_enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    mercy_run_difference          INTEGER,
    mercy_minimum_inning          INTEGER,
    max_extra_innings             INTEGER,
    tied_game_result              VARCHAR(20)  DEFAULT 'DRAW',
    tiebreaker_enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    forfeit_score                 INTEGER      NOT NULL DEFAULT 7,
    starter_win_qualification_outs INTEGER     NOT NULL DEFAULT 15,
    qualification_pa_multiplier   DOUBLE PRECISION NOT NULL DEFAULT 3.1,
    qualification_ip_multiplier   DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    time_limit_minutes            INTEGER,
    pitch_count_limit             INTEGER,
    pitch_count_warning_threshold INTEGER      NOT NULL DEFAULT 10,
    doubleheader_innings          INTEGER      NOT NULL DEFAULT 7,
    max_mercenary_count           INTEGER,
    standings_tiebreaker_order    VARCHAR(255) NOT NULL DEFAULT 'HEAD_TO_HEAD,RUN_DIFFERENTIAL,RUNS_SCORED',
    created_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_competitions_league      ON competitions(league_id);
CREATE INDEX IF NOT EXISTS idx_competitions_year_season  ON competitions(year, season);
CREATE INDEX IF NOT EXISTS idx_competitions_dates        ON competitions(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_competitions_status       ON competitions(status);

-- =====================
-- 13. games (→ competitions, embeds GameState)
-- =====================
CREATE TABLE IF NOT EXISTS games (
    id                          BIGSERIAL    PRIMARY KEY,
    competition_id              BIGINT       NOT NULL REFERENCES competitions(id),
    scheduled_at                TIMESTAMP    NOT NULL,
    location                    VARCHAR(100),
    field_name                  VARCHAR(100),
    game_number                 INTEGER,
    is_doubleheader             BOOLEAN      NOT NULL DEFAULT FALSE,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED',
    current_inning              INTEGER      DEFAULT 0,
    is_top_inning               BOOLEAN      DEFAULT TRUE,
    total_innings               INTEGER      DEFAULT 9,
    started_at                  TIMESTAMP,
    ended_at                    TIMESTAMP,
    note                        VARCHAR(500),
    forfeit_reason              VARCHAR(500),
    -- GameState embedded fields
    outs                        INTEGER      DEFAULT 0,
    balls                       INTEGER      DEFAULT 0,
    strikes                     INTEGER      DEFAULT 0,
    runner_on_first_id          BIGINT,
    runner_on_second_id         BIGINT,
    runner_on_third_id          BIGINT,
    home_batting_order          INTEGER      DEFAULT 1,
    away_batting_order          INTEGER      DEFAULT 1,
    current_pitcher_id          BIGINT,
    current_batter_id           BIGINT,
    runner_on_first_pitcher_id  BIGINT,
    runner_on_second_pitcher_id BIGINT,
    runner_on_third_pitcher_id  BIGINT,
    was_dh_released             BOOLEAN      NOT NULL DEFAULT FALSE,
    scorer_id                   BIGINT,
    locked_at                   TIMESTAMP,
    version                     BIGINT       NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_games_competition      ON games(competition_id);
CREATE INDEX IF NOT EXISTS idx_games_scheduled_at      ON games(scheduled_at);
CREATE INDEX IF NOT EXISTS idx_games_status             ON games(status);
CREATE INDEX IF NOT EXISTS idx_games_competition_date   ON games(competition_id, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_games_scorer_id          ON games(scorer_id) WHERE scorer_id IS NOT NULL;

-- =====================
-- 14. game_teams (→ games, teams)
-- =====================
CREATE TABLE IF NOT EXISTS game_teams (
    id             BIGSERIAL    PRIMARY KEY,
    game_id        BIGINT       NOT NULL REFERENCES games(id),
    team_id        BIGINT       NOT NULL REFERENCES teams(id),
    home_away      VARCHAR(10)  NOT NULL,
    total_score    INTEGER      NOT NULL DEFAULT 0,
    total_hits     INTEGER      NOT NULL DEFAULT 0,
    total_errors   INTEGER      NOT NULL DEFAULT 0,
    result         VARCHAR(10)  NOT NULL DEFAULT 'UNDECIDED',
    inning_scores  VARCHAR(100),
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_game_teams_game_home_away UNIQUE (game_id, home_away),
    CONSTRAINT uk_game_teams_game_team      UNIQUE (game_id, team_id)
);

CREATE INDEX IF NOT EXISTS idx_game_teams_game   ON game_teams(game_id);
CREATE INDEX IF NOT EXISTS idx_game_teams_team   ON game_teams(team_id);
CREATE INDEX IF NOT EXISTS idx_game_teams_result ON game_teams(result);

-- =====================
-- 15. game_players (→ game_teams, players)
-- =====================
CREATE TABLE IF NOT EXISTS game_players (
    id                   BIGSERIAL    PRIMARY KEY,
    game_team_id         BIGINT       NOT NULL REFERENCES game_teams(id),
    player_id            BIGINT       NOT NULL REFERENCES players(id),
    position             VARCHAR(30)  NOT NULL,
    batting_order        INTEGER,
    back_number          INTEGER,
    is_starter           BOOLEAN      NOT NULL DEFAULT TRUE,
    is_currently_playing BOOLEAN      NOT NULL DEFAULT TRUE,
    entry_inning         INTEGER,
    exit_inning          INTEGER,
    is_designated_hitter BOOLEAN      NOT NULL DEFAULT FALSE,
    pitcher_batting_order INTEGER,
    ejection_reason      VARCHAR(30),
    position_history     TEXT,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_game_players_game_team_player UNIQUE (game_team_id, player_id)
);

CREATE INDEX IF NOT EXISTS idx_game_players_game_team     ON game_players(game_team_id);
CREATE INDEX IF NOT EXISTS idx_game_players_player        ON game_players(player_id);
CREATE INDEX IF NOT EXISTS idx_game_players_batting_order  ON game_players(game_team_id, batting_order);
CREATE INDEX IF NOT EXISTS idx_game_players_position       ON game_players(game_team_id, position);

-- =====================
-- 16. game_events (→ games, game_players)
-- =====================
CREATE TABLE IF NOT EXISTS game_events (
    id                     BIGSERIAL    PRIMARY KEY,
    game_id                BIGINT       NOT NULL REFERENCES games(id),
    inning                 INTEGER      NOT NULL,
    is_top_inning          BOOLEAN      NOT NULL,
    out_count_before       INTEGER      NOT NULL,
    out_count_after        INTEGER      NOT NULL,
    event_type             VARCHAR(30)  NOT NULL,
    description            VARCHAR(500) NOT NULL,
    batter_id              BIGINT       REFERENCES game_players(id),
    pitcher_id             BIGINT       REFERENCES game_players(id),
    runners_before_json    TEXT,
    runners_after_json     TEXT,
    plate_appearance_result VARCHAR(30),
    runner_player_id       BIGINT       REFERENCES game_players(id),
    from_base              VARCHAR(10),
    to_base                VARCHAR(10),
    base_running_result    VARCHAR(30),
    runs_scored            INTEGER      NOT NULL DEFAULT 0,
    rbis                   INTEGER      NOT NULL DEFAULT 0,
    scoring_runner_ids     TEXT,
    event_timestamp        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    undone                 BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_game_events_game          ON game_events(game_id);
CREATE INDEX IF NOT EXISTS idx_game_events_game_inning   ON game_events(game_id, inning, is_top_inning);
CREATE INDEX IF NOT EXISTS idx_game_events_timestamp     ON game_events(event_timestamp);

-- =====================
-- 17. batting_records (→ game_players)
-- =====================
CREATE TABLE IF NOT EXISTS batting_records (
    id                       BIGSERIAL PRIMARY KEY,
    game_player_id           BIGINT    NOT NULL UNIQUE REFERENCES game_players(id),
    plate_appearances        INTEGER   NOT NULL DEFAULT 0,
    at_bats                  INTEGER   NOT NULL DEFAULT 0,
    hits                     INTEGER   NOT NULL DEFAULT 0,
    doubles                  INTEGER   NOT NULL DEFAULT 0,
    triples                  INTEGER   NOT NULL DEFAULT 0,
    home_runs                INTEGER   NOT NULL DEFAULT 0,
    runs                     INTEGER   NOT NULL DEFAULT 0,
    runs_batted_in           INTEGER   NOT NULL DEFAULT 0,
    walks                    INTEGER   NOT NULL DEFAULT 0,
    intentional_walks        INTEGER   NOT NULL DEFAULT 0,
    hit_by_pitch             INTEGER   NOT NULL DEFAULT 0,
    strikeouts               INTEGER   NOT NULL DEFAULT 0,
    sacrifice_bunts          INTEGER   NOT NULL DEFAULT 0,
    sacrifice_flies          INTEGER   NOT NULL DEFAULT 0,
    stolen_bases             INTEGER   NOT NULL DEFAULT 0,
    caught_stealing          INTEGER   NOT NULL DEFAULT 0,
    grounded_into_double_plays INTEGER NOT NULL DEFAULT 0,
    triple_plays             INTEGER   NOT NULL DEFAULT 0,
    batter_interferences     INTEGER   NOT NULL DEFAULT 0,
    runner_interferences     INTEGER   NOT NULL DEFAULT 0,
    created_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_batting_records_game_player ON batting_records(game_player_id);

-- =====================
-- 18. pitching_records (→ game_players)
-- =====================
CREATE TABLE IF NOT EXISTS pitching_records (
    id                      BIGSERIAL   PRIMARY KEY,
    game_player_id          BIGINT      NOT NULL UNIQUE REFERENCES game_players(id),
    innings_pitched_outs    INTEGER     NOT NULL DEFAULT 0,
    earned_runs             INTEGER     NOT NULL DEFAULT 0,
    runs_allowed            INTEGER     NOT NULL DEFAULT 0,
    hits_allowed            INTEGER     NOT NULL DEFAULT 0,
    walks_allowed           INTEGER     NOT NULL DEFAULT 0,
    strikeouts              INTEGER     NOT NULL DEFAULT 0,
    home_runs_allowed       INTEGER     NOT NULL DEFAULT 0,
    hit_batsmen             INTEGER     NOT NULL DEFAULT 0,
    wild_pitches            INTEGER     NOT NULL DEFAULT 0,
    balks                   INTEGER     NOT NULL DEFAULT 0,
    batters_faced           INTEGER     NOT NULL DEFAULT 0,
    decision                VARCHAR(20) NOT NULL DEFAULT 'NONE',
    is_starting_pitcher     BOOLEAN     NOT NULL DEFAULT FALSE,
    pitches_thrown           INTEGER,
    strikes_thrown           INTEGER,
    stolen_bases_allowed    INTEGER     NOT NULL DEFAULT 0,
    runners_caught_stealing INTEGER     NOT NULL DEFAULT 0,
    pickoffs                INTEGER     NOT NULL DEFAULT 0,
    created_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_pitching_records_game_player ON pitching_records(game_player_id);
CREATE INDEX IF NOT EXISTS idx_pitching_records_decision    ON pitching_records(decision);

-- =====================
-- 19. lineup_submissions (→ games, teams, users)
-- =====================
CREATE TABLE IF NOT EXISTS lineup_submissions (
    id                        BIGSERIAL    PRIMARY KEY,
    game_id                   BIGINT       NOT NULL REFERENCES games(id),
    team_id                   BIGINT       NOT NULL REFERENCES teams(id),
    submitted_by_id           BIGINT       NOT NULL REFERENCES users(id),
    status                    VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    submitted_at              TIMESTAMP,
    confirmed_at              TIMESTAMP,
    confirmed_by_id           BIGINT       REFERENCES users(id),
    rejection_reason          VARCHAR(500),
    rejected_by_id            BIGINT       REFERENCES users(id),
    exchange_pending_at       TIMESTAMP,
    exchange_rejection_reason VARCHAR(500),
    exchange_rejected_by_id   BIGINT       REFERENCES users(id),
    created_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lineup_submissions_game_team UNIQUE (game_id, team_id)
);

CREATE INDEX IF NOT EXISTS idx_lineup_submissions_game         ON lineup_submissions(game_id);
CREATE INDEX IF NOT EXISTS idx_lineup_submissions_team         ON lineup_submissions(team_id);
CREATE INDEX IF NOT EXISTS idx_lineup_submissions_status       ON lineup_submissions(status);
CREATE INDEX IF NOT EXISTS idx_lineup_submissions_submitted_by ON lineup_submissions(submitted_by_id);

-- =====================
-- 20. lineup_entries (→ lineup_submissions, players)
-- =====================
CREATE TABLE IF NOT EXISTS lineup_entries (
    id             BIGSERIAL    PRIMARY KEY,
    submission_id  BIGINT       NOT NULL REFERENCES lineup_submissions(id),
    player_id      BIGINT       NOT NULL REFERENCES players(id),
    position       VARCHAR(30)  NOT NULL,
    batting_order  INTEGER,
    back_number    INTEGER,
    is_starter     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_lineup_entries_submission_player        UNIQUE (submission_id, player_id),
    CONSTRAINT uk_lineup_entries_submission_batting_order UNIQUE (submission_id, batting_order)
);

CREATE INDEX IF NOT EXISTS idx_lineup_entries_submission    ON lineup_entries(submission_id);
CREATE INDEX IF NOT EXISTS idx_lineup_entries_player        ON lineup_entries(player_id);
CREATE INDEX IF NOT EXISTS idx_lineup_entries_batting_order ON lineup_entries(submission_id, batting_order);

-- Rollback (주석 보관)
-- DROP TABLE IF EXISTS lineup_entries;
-- DROP TABLE IF EXISTS lineup_submissions;
-- DROP TABLE IF EXISTS pitching_records;
-- DROP TABLE IF EXISTS batting_records;
-- DROP TABLE IF EXISTS game_events;
-- DROP TABLE IF EXISTS game_players;
-- DROP TABLE IF EXISTS game_teams;
-- DROP TABLE IF EXISTS games;
-- DROP TABLE IF EXISTS competitions;
-- DROP TABLE IF EXISTS player_team_histories;
-- DROP TABLE IF EXISTS player_careers;
-- DROP TABLE IF EXISTS organization_admins;
-- DROP TABLE IF EXISTS refresh_tokens;
-- DROP TABLE IF EXISTS oauth_accounts;
-- DROP TABLE IF EXISTS user_roles;
-- DROP TABLE IF EXISTS users;
-- DROP TABLE IF EXISTS players;
-- DROP TABLE IF EXISTS teams;
-- DROP TABLE IF EXISTS leagues;
-- DROP TABLE IF EXISTS associations;
