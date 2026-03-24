-- 시즌 타이틀(개인상) 테이블
-- 시즌 종료 시 각 부문별 최고 성적을 거둔 선수에게 부여되는 타이틀을 저장합니다.
CREATE TABLE season_awards (
    id            BIGSERIAL    PRIMARY KEY,
    player_id     BIGINT       NOT NULL REFERENCES players(id),
    year          INTEGER      NOT NULL,
    title         VARCHAR(30)  NOT NULL,
    stat_value    NUMERIC(10, 3),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_season_awards_player ON season_awards(player_id);
CREATE INDEX idx_season_awards_year ON season_awards(year);
CREATE INDEX idx_season_awards_title ON season_awards(title);
CREATE INDEX idx_season_awards_year_title ON season_awards(year, title);
