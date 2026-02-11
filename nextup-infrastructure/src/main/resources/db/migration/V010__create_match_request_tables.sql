-- 매칭 요청 테이블
CREATE TABLE match_requests (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    preferred_date DATE NOT NULL,
    preferred_time VARCHAR(50),
    preferred_location VARCHAR(500),
    message TEXT,
    skill_level VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_requests_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE
);

-- 매칭 응답 테이블
CREATE TABLE match_responses (
    id BIGSERIAL PRIMARY KEY,
    match_request_id BIGINT NOT NULL,
    respond_team_id BIGINT NOT NULL,
    message TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_match_responses_match_request FOREIGN KEY (match_request_id) REFERENCES match_requests(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_responses_respond_team FOREIGN KEY (respond_team_id) REFERENCES teams(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_match_requests_team_id ON match_requests(team_id);
CREATE INDEX idx_match_requests_status ON match_requests(status);
CREATE INDEX idx_match_requests_preferred_date ON match_requests(preferred_date);
CREATE INDEX idx_match_requests_skill_level ON match_requests(skill_level);

CREATE INDEX idx_match_responses_match_request_id ON match_responses(match_request_id);
CREATE INDEX idx_match_responses_respond_team_id ON match_responses(respond_team_id);
CREATE INDEX idx_match_responses_status ON match_responses(status);
