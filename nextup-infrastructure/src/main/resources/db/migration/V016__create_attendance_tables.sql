-- V016: 출석 투표 및 활동 점수 테이블 생성

-- 출석 투표 테이블
CREATE TABLE attendance_polls
(
    id         BIGSERIAL PRIMARY KEY,
    team_id    BIGINT                   NOT NULL,
    title      VARCHAR(200)             NOT NULL,
    event_date TIMESTAMP                NOT NULL,
    deadline   TIMESTAMP                NOT NULL,
    status     VARCHAR(20)              NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_attendance_polls_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT chk_attendance_polls_status CHECK (status IN ('OPEN', 'CLOSED'))
);

-- 출석 투표 인덱스
CREATE INDEX idx_ap_team_id ON attendance_polls (team_id);
CREATE INDEX idx_ap_status ON attendance_polls (status);
CREATE INDEX idx_ap_event_date ON attendance_polls (event_date);

-- 출석 투표 응답 테이블
CREATE TABLE poll_votes
(
    id         BIGSERIAL PRIMARY KEY,
    poll_id    BIGINT                   NOT NULL,
    player_id  BIGINT                   NOT NULL,
    vote_type  VARCHAR(20)              NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_poll_votes_poll FOREIGN KEY (poll_id) REFERENCES attendance_polls (id) ON DELETE CASCADE,
    CONSTRAINT fk_poll_votes_player FOREIGN KEY (player_id) REFERENCES players (id) ON DELETE CASCADE,
    CONSTRAINT uk_pv_poll_player UNIQUE (poll_id, player_id),
    CONSTRAINT chk_poll_votes_type CHECK (vote_type IN ('ATTEND', 'ABSENT', 'UNDECIDED'))
);

-- 출석 투표 응답 인덱스
CREATE INDEX idx_pv_poll_id ON poll_votes (poll_id);
CREATE INDEX idx_pv_player_id ON poll_votes (player_id);
CREATE INDEX idx_pv_vote_type ON poll_votes (vote_type);

-- 활동 점수 테이블
CREATE TABLE activity_scores
(
    id                        BIGSERIAL PRIMARY KEY,
    team_id                   BIGINT                   NOT NULL,
    member_id                 BIGINT                   NOT NULL,
    game_participation_rate   DECIMAL(5, 2)            NOT NULL DEFAULT 0.00,
    practice_attendance_rate  DECIMAL(5, 2)            NOT NULL DEFAULT 0.00,
    contribution_score        DECIMAL(5, 2)            NOT NULL DEFAULT 0.00,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at                TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_activity_scores_team FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_scores_member FOREIGN KEY (member_id) REFERENCES team_members (id) ON DELETE CASCADE,
    CONSTRAINT uk_as_team_member UNIQUE (team_id, member_id),
    CONSTRAINT chk_activity_scores_game_rate CHECK (game_participation_rate >= 0.00 AND game_participation_rate <= 100.00),
    CONSTRAINT chk_activity_scores_practice_rate CHECK (practice_attendance_rate >= 0.00 AND practice_attendance_rate <= 100.00),
    CONSTRAINT chk_activity_scores_contribution CHECK (contribution_score >= 0.00 AND contribution_score <= 100.00)
);

-- 활동 점수 인덱스
CREATE INDEX idx_as_team_id ON activity_scores (team_id);
CREATE INDEX idx_as_member_id ON activity_scores (member_id);

-- 코멘트 추가
COMMENT ON TABLE attendance_polls IS '팀 이벤트 출석 투표';
COMMENT ON TABLE poll_votes IS '출석 투표 응답';
COMMENT ON TABLE activity_scores IS '팀원 활동 점수';

COMMENT ON COLUMN attendance_polls.status IS '투표 상태: OPEN(진행중), CLOSED(마감)';
COMMENT ON COLUMN poll_votes.vote_type IS '투표 유형: ATTEND(참석), ABSENT(불참), UNDECIDED(미정)';
COMMENT ON COLUMN activity_scores.game_participation_rate IS '경기 참여율 (0~100)';
COMMENT ON COLUMN activity_scores.practice_attendance_rate IS '연습 참석률 (0~100)';
COMMENT ON COLUMN activity_scores.contribution_score IS '기여도 점수 (0~100)';
