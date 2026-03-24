-- M-16: 팀 일정 관리 테이블
CREATE TABLE team_schedules (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL REFERENCES teams(id),
    title VARCHAR(200) NOT NULL,
    description TEXT,
    schedule_type VARCHAR(30) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP,
    location VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_team_schedules_team_id ON team_schedules(team_id);
CREATE INDEX idx_team_schedules_start_at ON team_schedules(start_at);
CREATE INDEX idx_team_schedules_type ON team_schedules(schedule_type);
