-- stadium_slots 테이블에 낙관적 락 version 컬럼 추가
ALTER TABLE stadium_slots ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
