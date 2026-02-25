-- 비상대책위원회 모드 (긴급 선거) 관련 컬럼 추가
-- Election 엔티티에 긴급 선거 전용 필드 추가

ALTER TABLE elections
    ADD COLUMN triggered_by_member_id    BIGINT,
    ADD COLUMN acting_owner_member_id    BIGINT,
    ADD COLUMN regular_election_deadline TIMESTAMP WITH TIME ZONE,
    ADD COLUMN can_manage_lineup         BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN can_manage_schedule       BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN can_kick_member           BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN can_dissolve_team         BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN can_transfer_ownership    BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN elections.triggered_by_member_id IS '긴급 선거를 발동한 MANAGER 멤버 ID (EMERGENCY 타입 전용)';
COMMENT ON COLUMN elections.acting_owner_member_id IS '임시 구단주로 지정된 멤버 ID (EMERGENCY 타입 전용)';
COMMENT ON COLUMN elections.regular_election_deadline IS '정규 선거 마감 기한 - 긴급 선거 발동 후 14일 (EMERGENCY 타입 전용)';
COMMENT ON COLUMN elections.can_manage_lineup IS '임시 구단주 라인업 관리 권한';
COMMENT ON COLUMN elections.can_manage_schedule IS '임시 구단주 일정 관리 권한';
COMMENT ON COLUMN elections.can_kick_member IS '임시 구단주 멤버 강퇴 권한 (기본값: 불가)';
COMMENT ON COLUMN elections.can_dissolve_team IS '임시 구단주 팀 해산 권한 (기본값: 불가)';
COMMENT ON COLUMN elections.can_transfer_ownership IS '임시 구단주 소유권 이전 권한 (기본값: 불가)';
