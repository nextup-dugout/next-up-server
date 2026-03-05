CREATE TABLE audit_logs (
    id            BIGSERIAL    NOT NULL,
    admin_user_id BIGINT       NOT NULL,
    action        VARCHAR(100) NOT NULL,
    target_entity VARCHAR(100) NOT NULL,
    target_id     BIGINT,
    details       TEXT,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_audit_logs PRIMARY KEY (id)
);

CREATE INDEX idx_audit_log_admin_user ON audit_logs (admin_user_id);
CREATE INDEX idx_audit_log_target ON audit_logs (target_entity, target_id);
CREATE INDEX idx_audit_log_created_at ON audit_logs (created_at);
