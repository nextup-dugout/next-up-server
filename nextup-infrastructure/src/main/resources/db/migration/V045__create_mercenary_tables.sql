-- 용병 요청 테이블
CREATE TABLE mercenary_requests (
    id BIGSERIAL PRIMARY KEY,
    requesting_team_id BIGINT NOT NULL,
    game_id BIGINT NOT NULL,
    max_count INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    deadline TIMESTAMP WITH TIME ZONE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mr_team FOREIGN KEY (requesting_team_id) REFERENCES teams(id),
    CONSTRAINT fk_mr_game FOREIGN KEY (game_id) REFERENCES games(id)
);

CREATE INDEX idx_mr_requesting_team_id ON mercenary_requests(requesting_team_id);
CREATE INDEX idx_mr_game_id ON mercenary_requests(game_id);
CREATE INDEX idx_mr_status ON mercenary_requests(status);
CREATE INDEX idx_mr_deadline ON mercenary_requests(deadline);

-- 용병 요청 포지션 (ElementCollection)
CREATE TABLE mercenary_request_positions (
    mercenary_request_id BIGINT NOT NULL,
    position VARCHAR(30) NOT NULL,
    CONSTRAINT fk_mrp_request FOREIGN KEY (mercenary_request_id)
        REFERENCES mercenary_requests(id) ON DELETE CASCADE,
    PRIMARY KEY (mercenary_request_id, position)
);

-- 용병 지원 테이블
CREATE TABLE mercenary_applications (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ma_request FOREIGN KEY (request_id) REFERENCES mercenary_requests(id),
    CONSTRAINT fk_ma_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT uk_ma_request_player UNIQUE (request_id, player_id)
);

CREATE INDEX idx_ma_request_id ON mercenary_applications(request_id);
CREATE INDEX idx_ma_player_id ON mercenary_applications(player_id);
CREATE INDEX idx_ma_status ON mercenary_applications(status);

-- 용병 지원 선호 포지션 (ElementCollection)
CREATE TABLE mercenary_application_positions (
    mercenary_application_id BIGINT NOT NULL,
    position VARCHAR(30) NOT NULL,
    CONSTRAINT fk_map_application FOREIGN KEY (mercenary_application_id)
        REFERENCES mercenary_applications(id) ON DELETE CASCADE,
    PRIMARY KEY (mercenary_application_id, position)
);

-- 용병 참가 테이블
CREATE TABLE mercenary_participations (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_mp_game FOREIGN KEY (game_id) REFERENCES games(id),
    CONSTRAINT fk_mp_player FOREIGN KEY (player_id) REFERENCES players(id),
    CONSTRAINT fk_mp_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT uk_mp_game_player UNIQUE (game_id, player_id)
);

CREATE INDEX idx_mp_game_id ON mercenary_participations(game_id);
CREATE INDEX idx_mp_player_id ON mercenary_participations(player_id);
CREATE INDEX idx_mp_team_id ON mercenary_participations(team_id);
