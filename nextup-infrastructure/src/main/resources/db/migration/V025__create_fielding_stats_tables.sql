-- V025: 시즌/통산 수비 통계 테이블 생성

-- 시즌 수비 통계
CREATE TABLE season_fielding_stats (
    id           BIGSERIAL PRIMARY KEY,
    player_id    BIGINT  NOT NULL,
    year         INTEGER NOT NULL,
    games_played INTEGER NOT NULL DEFAULT 0,
    put_outs     INTEGER NOT NULL DEFAULT 0,
    assists      INTEGER NOT NULL DEFAULT 0,
    errors       INTEGER NOT NULL DEFAULT 0,
    double_plays INTEGER NOT NULL DEFAULT 0,
    passed_balls INTEGER NOT NULL DEFAULT 0,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_season_fielding_stats_player
        FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,
    CONSTRAINT uk_season_fielding_stats_player_year
        UNIQUE (player_id, year)
);

CREATE INDEX idx_season_fielding_stats_player ON season_fielding_stats(player_id);
CREATE INDEX idx_season_fielding_stats_year   ON season_fielding_stats(year);

-- 통산 수비 통계
CREATE TABLE career_fielding_stats (
    id              BIGSERIAL PRIMARY KEY,
    player_id       BIGINT  NOT NULL UNIQUE,
    seasons_played  INTEGER NOT NULL DEFAULT 0,
    games_played    INTEGER NOT NULL DEFAULT 0,
    put_outs        INTEGER NOT NULL DEFAULT 0,
    assists         INTEGER NOT NULL DEFAULT 0,
    errors          INTEGER NOT NULL DEFAULT 0,
    double_plays    INTEGER NOT NULL DEFAULT 0,
    passed_balls    INTEGER NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_career_fielding_stats_player
        FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE
);

CREATE INDEX idx_career_fielding_stats_player ON career_fielding_stats(player_id);
