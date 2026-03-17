-- #349: 모집 공고 지원(Apply) 흐름 구현
CREATE TABLE recruitment_applications (
    id                  BIGSERIAL       PRIMARY KEY,
    recruitment_id      BIGINT          NOT NULL REFERENCES team_recruitments(id),
    applicant_id        BIGINT          NOT NULL,
    message             VARCHAR(500)    NOT NULL,
    preferred_positions VARCHAR(255)    NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    applied_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    processed_at        TIMESTAMPTZ,
    processed_by        BIGINT,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_ra_recruitment_applicant_active UNIQUE (recruitment_id, applicant_id)
);

CREATE INDEX idx_ra_recruitment_id ON recruitment_applications(recruitment_id);
CREATE INDEX idx_ra_applicant_id ON recruitment_applications(applicant_id);
CREATE INDEX idx_ra_status ON recruitment_applications(status);
CREATE INDEX idx_ra_recruitment_applicant ON recruitment_applications(recruitment_id, applicant_id);
