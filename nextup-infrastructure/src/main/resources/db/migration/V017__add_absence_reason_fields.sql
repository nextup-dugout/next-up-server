-- V017: 출석 투표 불참 사유 필드 추가 (Issue #118)

-- poll_votes 테이블에 불참 사유 필드 추가
ALTER TABLE poll_votes ADD COLUMN absence_reason VARCHAR(20);
ALTER TABLE poll_votes ADD COLUMN reason_detail VARCHAR(500);

-- attendance_votes 테이블: 기존 reason 컬럼을 reason_detail로 변경, absence_reason 추가
ALTER TABLE attendance_votes RENAME COLUMN reason TO reason_detail;
ALTER TABLE attendance_votes ADD COLUMN absence_reason VARCHAR(20);
