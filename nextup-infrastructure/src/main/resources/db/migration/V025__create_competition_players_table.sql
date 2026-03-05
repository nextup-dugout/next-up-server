-- V018: 대회 등록 선수 테이블 생성 (Issue #133: 리그 등록 선수 검증)

CREATE TABLE competition_players
(
    id             BIGSERIAL PRIMARY KEY,
    competition_id BIGINT                   NOT NULL,
    team_id        BIGINT                   NOT NULL,
    player_id      BIGINT                   NOT NULL,
    status         VARCHAR(20)              NOT NULL DEFAULT 'ACTIVE',
    registered_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_competition_players_competition FOREIGN KEY (competition_id) REFERENCES competitions (id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_players_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_competition_players_player FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE,
    CONSTRAINT uk_competition_players_competition_player UNIQUE (competition_id, player_id),
    CONSTRAINT chk_competition_players_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'WITHDRAWN'))
);

-- 인덱스
CREATE INDEX idx_competition_players_competition ON competition_players (competition_id);
CREATE INDEX idx_competition_players_team ON competition_players (team_id);
CREATE INDEX idx_competition_players_player ON competition_players (player_id);
CREATE INDEX idx_competition_players_status ON competition_players (status);

-- 코멘트
COMMENT ON TABLE competition_players IS '대회 등록 선수 (부정선수 체크용)';
COMMENT ON COLUMN competition_players.status IS '선수 상태: ACTIVE(활성), SUSPENDED(출전 정지), WITHDRAWN(등록 취소)';
COMMENT ON COLUMN competition_players.registered_at IS '대회 등록 일시';
