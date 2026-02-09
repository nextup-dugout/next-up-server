CREATE TABLE bracket_entries (
    id BIGSERIAL PRIMARY KEY,
    competition_id BIGINT NOT NULL REFERENCES competitions(id),
    round_number INT NOT NULL,
    match_number INT NOT NULL,
    team1_id BIGINT REFERENCES teams(id),
    team2_id BIGINT REFERENCES teams(id),
    winner_id BIGINT REFERENCES teams(id),
    bracket_type VARCHAR(20) NOT NULL DEFAULT 'WINNERS',
    seed1 INT,
    seed2 INT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_bracket_entries_competition ON bracket_entries(competition_id);
