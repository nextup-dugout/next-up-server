-- #472: 프로야구 오버엔지니어링 전량 삭제
-- Drop election, appeal, certificate tables (dependency order: children first)

DROP TABLE IF EXISTS election_votes;
DROP TABLE IF EXISTS candidates;
DROP TABLE IF EXISTS elections;
DROP TABLE IF EXISTS appeals;
DROP TABLE IF EXISTS certificates;
