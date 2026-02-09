-- 팀 모집 공고 테이블
CREATE TABLE team_recruitments
(
    id               BIGSERIAL PRIMARY KEY,
    team_id          BIGINT       NOT NULL REFERENCES teams (id) ON DELETE CASCADE,
    title            VARCHAR(200) NOT NULL,
    description      TEXT         NOT NULL,
    positions_needed VARCHAR(255) NOT NULL,
    age_range        VARCHAR(50),
    skill_level      VARCHAR(50),
    location         VARCHAR(255),
    deadline         DATE         NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_recruitment_status CHECK (status IN ('OPEN', 'CLOSED', 'EXPIRED'))
);

-- 인덱스 생성
CREATE INDEX idx_team_recruitments_team_id ON team_recruitments (team_id);
CREATE INDEX idx_team_recruitments_status ON team_recruitments (status);
CREATE INDEX idx_team_recruitments_deadline ON team_recruitments (deadline);
CREATE INDEX idx_team_recruitments_team_status ON team_recruitments (team_id, status);

-- 테이블 코멘트
COMMENT ON TABLE team_recruitments IS '팀 선수 모집 공고';
COMMENT ON COLUMN team_recruitments.id IS '모집 공고 ID';
COMMENT ON COLUMN team_recruitments.team_id IS '팀 ID';
COMMENT ON COLUMN team_recruitments.title IS '모집 공고 제목';
COMMENT ON COLUMN team_recruitments.description IS '모집 공고 상세 설명';
COMMENT ON COLUMN team_recruitments.positions_needed IS '모집 포지션 (쉼표로 구분)';
COMMENT ON COLUMN team_recruitments.age_range IS '나이 범위 (예: 20-35)';
COMMENT ON COLUMN team_recruitments.skill_level IS '실력 수준 (예: 초급, 중급, 고급)';
COMMENT ON COLUMN team_recruitments.location IS '활동 지역';
COMMENT ON COLUMN team_recruitments.deadline IS '모집 마감일';
COMMENT ON COLUMN team_recruitments.status IS '모집 상태 (OPEN, CLOSED, EXPIRED)';
COMMENT ON COLUMN team_recruitments.created_at IS '생성 일시';
COMMENT ON COLUMN team_recruitments.updated_at IS '수정 일시';
