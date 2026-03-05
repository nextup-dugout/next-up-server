-- V022: Stats 테이블 생성
-- SeasonBattingStats, SeasonPitchingStats, CareerBattingStats, CareerPitchingStats

-- 시즌 타격 통계
CREATE TABLE season_batting_stats (
    id                          BIGSERIAL       NOT NULL,
    player_id                   BIGINT          NOT NULL,
    year                        INTEGER         NOT NULL,
    games_played                INTEGER         NOT NULL DEFAULT 0,
    plate_appearances           INTEGER         NOT NULL DEFAULT 0,
    at_bats                     INTEGER         NOT NULL DEFAULT 0,
    hits                        INTEGER         NOT NULL DEFAULT 0,
    doubles                     INTEGER         NOT NULL DEFAULT 0,
    triples                     INTEGER         NOT NULL DEFAULT 0,
    home_runs                   INTEGER         NOT NULL DEFAULT 0,
    runs                        INTEGER         NOT NULL DEFAULT 0,
    runs_batted_in              INTEGER         NOT NULL DEFAULT 0,
    walks                       INTEGER         NOT NULL DEFAULT 0,
    intentional_walks           INTEGER         NOT NULL DEFAULT 0,
    hit_by_pitch                INTEGER         NOT NULL DEFAULT 0,
    strikeouts                  INTEGER         NOT NULL DEFAULT 0,
    sacrifice_bunts             INTEGER         NOT NULL DEFAULT 0,
    sacrifice_flies             INTEGER         NOT NULL DEFAULT 0,
    stolen_bases                INTEGER         NOT NULL DEFAULT 0,
    caught_stealing             INTEGER         NOT NULL DEFAULT 0,
    grounded_into_double_plays  INTEGER         NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP       NOT NULL,
    updated_at                  TIMESTAMP       NOT NULL,
    CONSTRAINT pk_season_batting_stats PRIMARY KEY (id),
    CONSTRAINT fk_season_batting_stats_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT uk_season_batting_stats_player_year UNIQUE (player_id, year)
);

CREATE INDEX idx_season_batting_stats_player ON season_batting_stats (player_id);
CREATE INDEX idx_season_batting_stats_year ON season_batting_stats (year);
CREATE INDEX idx_season_batting_stats_games ON season_batting_stats (games_played);

-- 시즌 투수 통계
CREATE TABLE season_pitching_stats (
    id                      BIGSERIAL       NOT NULL,
    player_id               BIGINT          NOT NULL,
    year                    INTEGER         NOT NULL,
    games_played            INTEGER         NOT NULL DEFAULT 0,
    games_started           INTEGER         NOT NULL DEFAULT 0,
    innings_pitched_outs    INTEGER         NOT NULL DEFAULT 0,
    wins                    INTEGER         NOT NULL DEFAULT 0,
    losses                  INTEGER         NOT NULL DEFAULT 0,
    saves                   INTEGER         NOT NULL DEFAULT 0,
    holds                   INTEGER         NOT NULL DEFAULT 0,
    blown_saves             INTEGER         NOT NULL DEFAULT 0,
    earned_runs             INTEGER         NOT NULL DEFAULT 0,
    runs_allowed            INTEGER         NOT NULL DEFAULT 0,
    hits_allowed            INTEGER         NOT NULL DEFAULT 0,
    walks_allowed           INTEGER         NOT NULL DEFAULT 0,
    strikeouts              INTEGER         NOT NULL DEFAULT 0,
    home_runs_allowed       INTEGER         NOT NULL DEFAULT 0,
    hit_batsmen             INTEGER         NOT NULL DEFAULT 0,
    wild_pitches            INTEGER         NOT NULL DEFAULT 0,
    balks                   INTEGER         NOT NULL DEFAULT 0,
    batters_faced           INTEGER         NOT NULL DEFAULT 0,
    pitches_thrown          INTEGER,
    strikes_thrown          INTEGER,
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
    CONSTRAINT pk_season_pitching_stats PRIMARY KEY (id),
    CONSTRAINT fk_season_pitching_stats_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT uk_season_pitching_stats_player_year UNIQUE (player_id, year)
);

CREATE INDEX idx_season_pitching_stats_player ON season_pitching_stats (player_id);
CREATE INDEX idx_season_pitching_stats_year ON season_pitching_stats (year);
CREATE INDEX idx_season_pitching_stats_games ON season_pitching_stats (games_played);

-- 통산 타격 통계
CREATE TABLE career_batting_stats (
    id                          BIGSERIAL       NOT NULL,
    player_id                   BIGINT          NOT NULL,
    seasons_played              INTEGER         NOT NULL DEFAULT 0,
    games_played                INTEGER         NOT NULL DEFAULT 0,
    plate_appearances           INTEGER         NOT NULL DEFAULT 0,
    at_bats                     INTEGER         NOT NULL DEFAULT 0,
    hits                        INTEGER         NOT NULL DEFAULT 0,
    doubles                     INTEGER         NOT NULL DEFAULT 0,
    triples                     INTEGER         NOT NULL DEFAULT 0,
    home_runs                   INTEGER         NOT NULL DEFAULT 0,
    runs                        INTEGER         NOT NULL DEFAULT 0,
    runs_batted_in              INTEGER         NOT NULL DEFAULT 0,
    walks                       INTEGER         NOT NULL DEFAULT 0,
    intentional_walks           INTEGER         NOT NULL DEFAULT 0,
    hit_by_pitch                INTEGER         NOT NULL DEFAULT 0,
    strikeouts                  INTEGER         NOT NULL DEFAULT 0,
    sacrifice_bunts             INTEGER         NOT NULL DEFAULT 0,
    sacrifice_flies             INTEGER         NOT NULL DEFAULT 0,
    stolen_bases                INTEGER         NOT NULL DEFAULT 0,
    caught_stealing             INTEGER         NOT NULL DEFAULT 0,
    grounded_into_double_plays  INTEGER         NOT NULL DEFAULT 0,
    created_at                  TIMESTAMP       NOT NULL,
    updated_at                  TIMESTAMP       NOT NULL,
    CONSTRAINT pk_career_batting_stats PRIMARY KEY (id),
    CONSTRAINT fk_career_batting_stats_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT uk_career_batting_stats_player UNIQUE (player_id)
);

CREATE INDEX idx_career_batting_stats_player ON career_batting_stats (player_id);

-- 통산 투수 통계
CREATE TABLE career_pitching_stats (
    id                      BIGSERIAL       NOT NULL,
    player_id               BIGINT          NOT NULL,
    seasons_played          INTEGER         NOT NULL DEFAULT 0,
    games_played            INTEGER         NOT NULL DEFAULT 0,
    games_started           INTEGER         NOT NULL DEFAULT 0,
    innings_pitched_outs    INTEGER         NOT NULL DEFAULT 0,
    wins                    INTEGER         NOT NULL DEFAULT 0,
    losses                  INTEGER         NOT NULL DEFAULT 0,
    saves                   INTEGER         NOT NULL DEFAULT 0,
    holds                   INTEGER         NOT NULL DEFAULT 0,
    blown_saves             INTEGER         NOT NULL DEFAULT 0,
    earned_runs             INTEGER         NOT NULL DEFAULT 0,
    runs_allowed            INTEGER         NOT NULL DEFAULT 0,
    hits_allowed            INTEGER         NOT NULL DEFAULT 0,
    walks_allowed           INTEGER         NOT NULL DEFAULT 0,
    strikeouts              INTEGER         NOT NULL DEFAULT 0,
    home_runs_allowed       INTEGER         NOT NULL DEFAULT 0,
    hit_batsmen             INTEGER         NOT NULL DEFAULT 0,
    wild_pitches            INTEGER         NOT NULL DEFAULT 0,
    balks                   INTEGER         NOT NULL DEFAULT 0,
    batters_faced           INTEGER         NOT NULL DEFAULT 0,
    pitches_thrown          INTEGER,
    strikes_thrown          INTEGER,
    created_at              TIMESTAMP       NOT NULL,
    updated_at              TIMESTAMP       NOT NULL,
    CONSTRAINT pk_career_pitching_stats PRIMARY KEY (id),
    CONSTRAINT fk_career_pitching_stats_player FOREIGN KEY (player_id) REFERENCES players (id),
    CONSTRAINT uk_career_pitching_stats_player UNIQUE (player_id)
);

CREATE INDEX idx_career_pitching_stats_player ON career_pitching_stats (player_id);
