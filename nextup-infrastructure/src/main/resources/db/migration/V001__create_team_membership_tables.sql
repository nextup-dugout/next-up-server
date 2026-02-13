-- V001: Create team membership tables
-- Description: 팀 멤버십 관리 시스템 (TeamMember, TeamJoinRequest, TeamBlacklist, AttendanceVote)

-- ===========================================================================
-- 1. team_members 테이블
-- ===========================================================================
CREATE TABLE team_members (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    uniform_number INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP NOT NULL,
    left_at TIMESTAMP,
    memo VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_tm_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tm_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,

    -- Unique Constraints
    CONSTRAINT uk_tm_team_user UNIQUE (team_id, user_id)
);

-- Partial Unique Index: 활성 멤버만 등번호 유니크 제약
CREATE UNIQUE INDEX uk_tm_team_uniform_active
    ON team_members(team_id, uniform_number)
    WHERE status = 'ACTIVE';

-- Indexes for performance
CREATE INDEX idx_tm_team_id ON team_members(team_id);
CREATE INDEX idx_tm_user_id ON team_members(user_id);
CREATE INDEX idx_tm_player_id ON team_members(player_id);
CREATE INDEX idx_tm_team_status ON team_members(team_id, status);
CREATE INDEX idx_tm_uniform ON team_members(team_id, uniform_number);

COMMENT ON TABLE team_members IS '팀 멤버 - 팀 내 회원의 역할, 등번호, 상태를 관리';
COMMENT ON COLUMN team_members.role IS 'OWNER(감독), MANAGER(운영진), MEMBER(일반)';
COMMENT ON COLUMN team_members.status IS 'ACTIVE(활동중), SUSPENDED(정지), LEFT(탈퇴), KICKED(강퇴)';
COMMENT ON COLUMN team_members.uniform_number IS '팀 내 등번호 (1~99, ACTIVE 상태에서만 유니크)';

-- ===========================================================================
-- 2. team_join_requests 테이블
-- ===========================================================================
CREATE TABLE team_join_requests (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    desired_uniform_number INT NOT NULL,
    request_message VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    processed_by BIGINT,
    response_message VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_tjr_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_tjr_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tjr_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,
    CONSTRAINT fk_tjr_processed_by FOREIGN KEY (processed_by) REFERENCES users(id) ON DELETE SET NULL,

    -- Constraints
    CONSTRAINT chk_tjr_uniform_range CHECK (desired_uniform_number BETWEEN 1 AND 99)
);

-- Indexes for performance
CREATE INDEX idx_tjr_team_id ON team_join_requests(team_id);
CREATE INDEX idx_tjr_user_id ON team_join_requests(user_id);
CREATE INDEX idx_tjr_status ON team_join_requests(status);
CREATE INDEX idx_tjr_team_status ON team_join_requests(team_id, status);

COMMENT ON TABLE team_join_requests IS '팀 가입 신청 - 승인/거부 프로세스 관리';
COMMENT ON COLUMN team_join_requests.status IS 'PENDING(대기), APPROVED(승인), REJECTED(거부)';
COMMENT ON COLUMN team_join_requests.desired_uniform_number IS '희망 등번호 (1~99)';

-- ===========================================================================
-- 3. team_blacklists 테이블
-- ===========================================================================
CREATE TABLE team_blacklists (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    registered_by BIGINT NOT NULL,
    registered_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_tbl_team FOREIGN KEY (team_id) REFERENCES teams(id) ON DELETE CASCADE,
    CONSTRAINT fk_tbl_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tbl_player FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE CASCADE,
    CONSTRAINT fk_tbl_registered_by FOREIGN KEY (registered_by) REFERENCES users(id) ON DELETE CASCADE,

    -- Unique Constraints
    CONSTRAINT uk_tbl_team_user UNIQUE (team_id, user_id)
);

-- Indexes for performance
CREATE INDEX idx_tbl_team_id ON team_blacklists(team_id);
CREATE INDEX idx_tbl_user_id ON team_blacklists(user_id);
CREATE INDEX idx_tbl_expires_at ON team_blacklists(expires_at) WHERE expires_at IS NOT NULL;

COMMENT ON TABLE team_blacklists IS '팀 블랙리스트 - 재가입 방지';
COMMENT ON COLUMN team_blacklists.expires_at IS 'NULL이면 영구 블랙리스트, 값이 있으면 기한부';

-- ===========================================================================
-- 4. attendance_votes 테이블
-- ===========================================================================
CREATE TABLE attendance_votes (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'UNDECIDED',
    reason VARCHAR(500),
    responded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign Keys
    CONSTRAINT fk_av_game FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    CONSTRAINT fk_av_member FOREIGN KEY (member_id) REFERENCES team_members(id) ON DELETE CASCADE,

    -- Unique Constraints
    CONSTRAINT uk_av_game_member UNIQUE (game_id, member_id)
);

-- Indexes for performance
CREATE INDEX idx_av_game_id ON attendance_votes(game_id);
CREATE INDEX idx_av_member_id ON attendance_votes(member_id);
CREATE INDEX idx_av_game_status ON attendance_votes(game_id, status);

COMMENT ON TABLE attendance_votes IS '출석 투표 - 경기별 참석 의사 관리';
COMMENT ON COLUMN attendance_votes.status IS 'ATTENDING(참석), ABSENT(불참), UNDECIDED(미정)';
COMMENT ON COLUMN attendance_votes.responded_at IS '투표 응답 시각 (NULL이면 미투표)';
