-- V062: 시간 제한 도달 플래그 추가
-- 야구 규칙에 따라 시간 제한 도달 시 즉시 종료가 아닌 이닝 전환 시점에서 종료 판단
ALTER TABLE games ADD COLUMN time_limit_reached BOOLEAN NOT NULL DEFAULT FALSE;
