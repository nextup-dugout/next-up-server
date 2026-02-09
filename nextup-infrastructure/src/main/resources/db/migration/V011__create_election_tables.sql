-- 선거 테이블
CREATE TABLE elections
(
    id            BIGSERIAL PRIMARY KEY,
    team_id       BIGINT                   NOT NULL,
    title         VARCHAR(200)             NOT NULL,
    description   VARCHAR(1000),
    election_type VARCHAR(50)              NOT NULL,
    start_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    status        VARCHAR(50)              NOT NULL DEFAULT 'SCHEDULED',
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_elections_team ON elections (team_id);
CREATE INDEX idx_elections_status ON elections (status);
CREATE INDEX idx_elections_start_at ON elections (start_at);
CREATE INDEX idx_elections_end_at ON elections (end_at);

COMMENT ON TABLE elections IS '팀 내 선거/투표';
COMMENT ON COLUMN elections.id IS '선거 ID';
COMMENT ON COLUMN elections.team_id IS '팀 ID';
COMMENT ON COLUMN elections.title IS '선거 제목';
COMMENT ON COLUMN elections.description IS '선거 설명';
COMMENT ON COLUMN elections.election_type IS '선거 유형 (OWNER_ELECTION, CAPTAIN_ELECTION, GENERAL)';
COMMENT ON COLUMN elections.start_at IS '시작 시간';
COMMENT ON COLUMN elections.end_at IS '종료 시간';
COMMENT ON COLUMN elections.status IS '선거 상태 (SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED)';

-- 후보자 테이블
CREATE TABLE candidates
(
    id          BIGSERIAL PRIMARY KEY,
    election_id BIGINT                   NOT NULL,
    member_id   BIGINT                   NOT NULL,
    member_name VARCHAR(100)             NOT NULL,
    statement   VARCHAR(1000),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_candidates_election_member UNIQUE (election_id, member_id)
);

CREATE INDEX idx_candidates_election ON candidates (election_id);
CREATE INDEX idx_candidates_member ON candidates (member_id);

COMMENT ON TABLE candidates IS '선거 후보자';
COMMENT ON COLUMN candidates.id IS '후보자 ID';
COMMENT ON COLUMN candidates.election_id IS '선거 ID';
COMMENT ON COLUMN candidates.member_id IS '회원 ID';
COMMENT ON COLUMN candidates.member_name IS '회원 이름';
COMMENT ON COLUMN candidates.statement IS '공약/소견';

-- 투표 테이블
CREATE TABLE election_votes
(
    id           BIGSERIAL PRIMARY KEY,
    election_id  BIGINT                   NOT NULL,
    voter_id     BIGINT                   NOT NULL,
    candidate_id BIGINT                   NOT NULL,
    voted_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_election_votes_election_voter UNIQUE (election_id, voter_id)
);

CREATE INDEX idx_election_votes_election ON election_votes (election_id);
CREATE INDEX idx_election_votes_candidate ON election_votes (candidate_id);
CREATE INDEX idx_election_votes_voter ON election_votes (voter_id);

COMMENT ON TABLE election_votes IS '선거 투표 기록';
COMMENT ON COLUMN election_votes.id IS '투표 ID';
COMMENT ON COLUMN election_votes.election_id IS '선거 ID';
COMMENT ON COLUMN election_votes.voter_id IS '투표자 ID';
COMMENT ON COLUMN election_votes.candidate_id IS '후보자 ID';
COMMENT ON COLUMN election_votes.voted_at IS '투표 시간';
