-- #75: 몰수패 자동 처리 - 몰수 사유 필드 추가
ALTER TABLE games ADD COLUMN forfeit_reason VARCHAR(500);

-- #69: 플레이오프 라인 하이라이트 - 플레이오프 진출 팀 수 필드 추가
ALTER TABLE competitions ADD COLUMN playoff_teams INTEGER;
