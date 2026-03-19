-- L-4: GameRules에 더블헤더 전용 이닝 수 컬럼 추가
ALTER TABLE competitions ADD COLUMN IF NOT EXISTS doubleheader_innings INTEGER NOT NULL DEFAULT 7;
