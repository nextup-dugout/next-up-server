-- #268: 더블헤더 관리 개념 도입
ALTER TABLE games ADD COLUMN is_doubleheader BOOLEAN NOT NULL DEFAULT FALSE;
