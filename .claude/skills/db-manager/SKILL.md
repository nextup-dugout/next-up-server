---
name: db-manager
description: |
  PostgreSQL/PostGIS 쿼리 검증 및 Flyway 마이그레이션 관리.
  GIS 쿼리 유효성 검사와 스키마 변경 관리를 담당한다.
disable-model-invocation: true
context: fork
allowed-tools: Read, Bash, Glob, Grep
---

# DB-Manager Skill - 데이터베이스 관리 스킬

## 개요

이 스킬은 PostgreSQL/PostGIS 데이터베이스 관련 작업을 수행합니다. 판단(Agent)과 실행(Skill)의 분리 원칙에 따라, 이 스킬은 **실행만** 담당하며 스키마 설계 결정은 `architect` 에이전트가 수행합니다.

## Flyway 마이그레이션

### 현재 상태

프로젝트는 **Flyway를 사용** 중이며, 현재 V042까지 마이그레이션이 존재합니다.

**마이그레이션 위치:** `nextup-infrastructure/src/main/resources/db/migration/`

### 마이그레이션 명명 규칙

```
V{번호}__{설명}.sql
```

- 번호: 3자리 zero-padded (예: `V001`, `V042`)
- 이중 언더스코어(`__`) 필수
- 설명: snake_case (예: `create_player_table`, `add_location_to_stadium`)

### 마이그레이션 생성 시 주의사항

- **기존 마이그레이션 수정 금지** — 이미 적용된 마이그레이션은 immutable
- **버전 충돌 방지** — 새 마이그레이션 생성 전 반드시 최신 버전 확인:
  ```bash
  ls nextup-infrastructure/src/main/resources/db/migration/ | tail -5
  ```
- **롤백 주석** — 마이그레이션 하단에 롤백 SQL을 주석으로 보관 권장

### 마이그레이션 DDL 템플릿

```sql
-- ============================================
-- Migration: V{번호}__{설명}
-- ============================================

CREATE TABLE IF NOT EXISTS example (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    location GEOMETRY(Point, 4326),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Spatial Index (PostGIS)
CREATE INDEX IF NOT EXISTS idx_example_location
    ON example USING GIST (location);

-- Rollback (주석 보관)
-- DROP TABLE IF EXISTS example;
```

## NEXT-UP 프로젝트 DB 규칙

### 테이블 명명 규칙
- 스네이크 케이스 사용: `player_stats`, `game_record`
- 복수형 사용: `players`, `teams`, `games`

### 컬럼 규칙
- PK: `id` (BIGSERIAL)
- 외래키: `[테이블명_단수]_id`
- 타임스탬프: `created_at`, `updated_at` (UTC)
- 위치 정보: `location` (GEOMETRY/GEOGRAPHY)

### PostGIS 규칙
- 기본 SRID: 4326 (WGS 84)
- 거리 계산 시 Geography 타입 권장
- 공간 인덱스(GiST) 필수 생성
- `ST_DWithin` 사용 권장 (ST_Distance 대비 인덱스 활용 가능)

### PostGIS 주요 함수

| 카테고리 | 함수 | 용도 |
|----------|------|------|
| 생성 | `ST_Point`, `ST_MakePoint`, `ST_SetSRID` | 포인트 생성 |
| 측정 | `ST_Distance`, `ST_DWithin` | 거리 계산/필터 |
| 관계 | `ST_Contains`, `ST_Within`, `ST_Intersects` | 공간 관계 |
| 변환 | `ST_Transform`, `ST_AsText`, `ST_AsGeoJSON` | 좌표계 변환 |

### SRID 참조 (한국 좌표계)

| SRID | 설명 |
|------|------|
| 4326 | WGS 84 (GPS) — **기본값** |
| 5186 | Korea 2000 / Central Belt |
| 5179 | Korea 2000 / Unified CS |

## 참조 문서

`references/postgis-spec.md` 파일 참조

## 호출 에이전트

- `architect`: Repository 구현 시 DB 스키마 검증, Entity 설계 시 DDL 생성 요청
- `reviewer`: 테스트용 DB 스키마 검증
