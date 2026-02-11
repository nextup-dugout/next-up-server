---
name: db-manager
description: |
  PostgreSQL/PostGIS 쿼리 검증 및 Flyway 마이그레이션 DDL 자동 생성을 수행하는 실행 스킬.
  GIS 쿼리 유효성 검사와 스키마 변경 관리를 담당한다.
disable-model-invocation: true
context: fork
allowed-tools: Read, Bash, Glob, Grep
---

# DB-Manager Skill - 데이터베이스 관리 스킬

## 개요

이 스킬은 PostgreSQL/PostGIS 데이터베이스 관련 물리적 조작을 수행합니다. 판단(Agent)과 실행(Skill)의 분리 원칙에 따라, 이 스킬은 **실행만** 담당하며 스키마 설계 결정은 `architect` 에이전트가 수행합니다.

## 사전 조건

- PostgreSQL 클라이언트 (`psql`) 설치
- PostGIS 확장 설치 (위치 기반 기능 사용 시)
- 데이터베이스 연결 정보 설정

## 제공 스크립트

### 1. check_postgis.py

PostGIS 쿼리의 유효성을 검증합니다.

```python
#!/usr/bin/env python3
# .claude/skills/db-manager/scripts/check_postgis.py
# PostGIS 쿼리 검증 스크립트

import re
import sys
import json
from typing import List, Dict, Any


# 지원되는 PostGIS 함수 목록
SUPPORTED_POSTGIS_FUNCTIONS = {
    # 기하학 생성
    'ST_Point', 'ST_MakePoint', 'ST_GeomFromText', 'ST_GeomFromGeoJSON',
    'ST_GeogFromText', 'ST_MakePolygon', 'ST_MakeLine',

    # 측정
    'ST_Distance', 'ST_DWithin', 'ST_Length', 'ST_Area', 'ST_Perimeter',

    # 관계
    'ST_Contains', 'ST_Within', 'ST_Intersects', 'ST_Overlaps',
    'ST_Touches', 'ST_Crosses', 'ST_Disjoint',

    # 변환
    'ST_Transform', 'ST_SetSRID', 'ST_AsText', 'ST_AsGeoJSON',
    'ST_X', 'ST_Y', 'ST_Centroid', 'ST_Buffer',

    # 집계
    'ST_Union', 'ST_Collect', 'ST_ConvexHull'
}

# SRID 유효성 (한국 좌표계)
VALID_SRIDS = {
    4326,   # WGS 84 (GPS)
    5186,   # Korea 2000 / Central Belt
    5187,   # Korea 2000 / East Belt
    5188,   # Korea 2000 / East Sea Belt
    5179,   # Korea 2000 / Unified CS
}


def validate_postgis_query(query: str) -> Dict[str, Any]:
    """PostGIS 쿼리 유효성 검증"""
    results = {
        'valid': True,
        'warnings': [],
        'errors': [],
        'detected_functions': [],
        'detected_srids': []
    }

    # PostGIS 함수 탐지
    function_pattern = r'(ST_\w+)\s*\('
    found_functions = re.findall(function_pattern, query, re.IGNORECASE)

    for func in found_functions:
        func_upper = func.upper()
        results['detected_functions'].append(func)

        if func_upper not in [f.upper() for f in SUPPORTED_POSTGIS_FUNCTIONS]:
            results['warnings'].append(f"Unknown PostGIS function: {func}")

    # SRID 탐지
    srid_pattern = r'(?:SRID\s*=\s*|ST_SetSRID\s*\([^,]+,\s*|ST_Transform\s*\([^,]+,\s*)(\d+)'
    found_srids = re.findall(srid_pattern, query, re.IGNORECASE)

    for srid_str in found_srids:
        srid = int(srid_str)
        results['detected_srids'].append(srid)

        if srid not in VALID_SRIDS:
            results['warnings'].append(f"Non-standard SRID detected: {srid}")

    # 성능 관련 경고
    if 'ST_Distance' in query.upper() and 'ST_DWithin' not in query.upper():
        results['warnings'].append(
            "Consider using ST_DWithin for distance filtering (better performance with spatial index)"
        )

    if 'ST_Transform' in query.upper():
        count = query.upper().count('ST_TRANSFORM')
        if count > 1:
            results['warnings'].append(
                f"Multiple ST_Transform calls ({count}) detected. Consider pre-transforming data."
            )

    # Geography vs Geometry 권장사항
    if 'geography' not in query.lower() and any(
        f in query.upper() for f in ['ST_DISTANCE', 'ST_DWITHIN']
    ):
        if '4326' in str(results['detected_srids']):
            results['warnings'].append(
                "Using SRID 4326 with geometry. Consider using geography type for accurate distance calculations."
            )

    # 인덱스 활용 가능성
    if 'WHERE' in query.upper() and any(
        f in query.upper() for f in ['ST_CONTAINS', 'ST_WITHIN', 'ST_INTERSECTS']
    ):
        results['warnings'].append(
            "Ensure spatial index (GiST) exists on geometry columns for optimal performance."
        )

    return results


def main():
    if len(sys.argv) < 2:
        print("Usage: check_postgis.py <query_or_file>")
        print("       check_postgis.py --file <sql_file>")
        sys.exit(1)

    if sys.argv[1] == '--file':
        with open(sys.argv[2], 'r') as f:
            query = f.read()
    else:
        query = ' '.join(sys.argv[1:])

    print("Validating PostGIS Query...")
    print(f"Query: {query[:100]}..." if len(query) > 100 else f"Query: {query}")
    print()

    result = validate_postgis_query(query)

    print(f"Validation Result: {'VALID' if result['valid'] else 'INVALID'}")

    if result['detected_functions']:
        print(f"\nDetected Functions: {', '.join(set(result['detected_functions']))}")

    if result['detected_srids']:
        print(f"Detected SRIDs: {', '.join(map(str, set(result['detected_srids'])))}")

    if result['warnings']:
        print(f"\nWarnings ({len(result['warnings'])}):")
        for i, warning in enumerate(result['warnings'], 1):
            print(f"  {i}. {warning}")

    if result['errors']:
        print(f"\nErrors ({len(result['errors'])}):")
        for i, error in enumerate(result['errors'], 1):
            print(f"  {i}. {error}")

    print("\n--- JSON Result ---")
    print(json.dumps(result, indent=2))

    sys.exit(0 if result['valid'] and not result['errors'] else 1)


if __name__ == '__main__':
    main()
```

### 2. generate_ddl.sh (향후 Flyway 도입 시 사용)

> **현재 상태**: 프로젝트는 Flyway/Liquibase 미사용 (ddl-auto: validate/create-drop).
> 이 스크립트는 향후 Flyway 도입 시 활용 가능.

Flyway 마이그레이션 DDL을 생성합니다.

```bash
#!/bin/bash
# .claude/skills/db-manager/scripts/generate_ddl.sh
# Flyway 마이그레이션 DDL 생성 스크립트

set -e

# 파라미터
MIGRATION_NAME="${1:?'Migration name is required (e.g., create_player_table)'}"
MIGRATION_DIR="${2:-src/main/resources/db/migration}"

# 버전 번호 생성 (현재 타임스탬프)
VERSION=$(date +%Y%m%d%H%M%S)

# 파일명 생성
FILENAME="V${VERSION}__${MIGRATION_NAME}.sql"
FILEPATH="${MIGRATION_DIR}/${FILENAME}"

echo "Generating Flyway migration..."
echo "  Name: $MIGRATION_NAME"
echo "  File: $FILEPATH"

# 디렉토리 생성
mkdir -p "$MIGRATION_DIR"

# 템플릿 생성
cat > "$FILEPATH" << 'EOF'
-- ============================================
-- Migration: ${MIGRATION_NAME}
-- Version: V${VERSION}
-- Author: NEXT-UP Auto Generator
-- Date: $(date +%Y-%m-%d)
-- ============================================

-- Description:
-- [마이그레이션 설명 작성]

-- ============================================
-- DDL Statements
-- ============================================

-- Example: Create table with PostGIS support
-- CREATE TABLE IF NOT EXISTS example (
--     id BIGSERIAL PRIMARY KEY,
--     name VARCHAR(255) NOT NULL,
--     location GEOMETRY(Point, 4326),
--     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
-- );

-- Example: Create spatial index
-- CREATE INDEX IF NOT EXISTS idx_example_location
--     ON example USING GIST (location);

-- ============================================
-- Data Migration (if needed)
-- ============================================

-- [데이터 마이그레이션 쿼리]

-- ============================================
-- Rollback Script (주석으로 보관)
-- ============================================
-- DROP TABLE IF EXISTS example;

EOF

# 변수 치환
sed -i '' "s/\${MIGRATION_NAME}/${MIGRATION_NAME}/g" "$FILEPATH" 2>/dev/null || \
sed -i "s/\${MIGRATION_NAME}/${MIGRATION_NAME}/g" "$FILEPATH"

sed -i '' "s/\${VERSION}/${VERSION}/g" "$FILEPATH" 2>/dev/null || \
sed -i "s/\${VERSION}/${VERSION}/g" "$FILEPATH"

echo "Migration file created: $FILEPATH"
echo ""
echo "Next steps:"
echo "  1. Edit the migration file with your DDL statements"
echo "  2. Run './gradlew flywayMigrate' to apply"
echo "  3. Verify with './gradlew flywayInfo'"
echo ""

# JSON 결과
cat << EOF
{
  "success": true,
  "version": "$VERSION",
  "filename": "$FILENAME",
  "filepath": "$FILEPATH",
  "migration_name": "$MIGRATION_NAME"
}
EOF
```

## 참조 문서

### PostGIS 주요 함수 가이드

`references/postgis-spec.md` 파일 참조

## 사용 예시

### PostGIS 쿼리 검증

```bash
# 직접 쿼리 입력
python3 .claude/skills/db-manager/scripts/check_postgis.py \
  "SELECT * FROM stadiums WHERE ST_DWithin(location, ST_SetSRID(ST_Point(127.0, 37.5), 4326), 1000)"

# SQL 파일 검증
python3 .claude/skills/db-manager/scripts/check_postgis.py --file query.sql
```

### Flyway 마이그레이션 생성 (향후 도입 시)

> 현재 ddl-auto 기반. Flyway 도입 후 사용 가능.

```bash
bash .claude/skills/db-manager/scripts/generate_ddl.sh create_player_table
bash .claude/skills/db-manager/scripts/generate_ddl.sh add_location_to_stadium
```

## NEXT-UP 프로젝트 DB 규칙

### 테이블 명명 규칙
- 스네이크 케이스 사용: `player_stats`, `game_record`
- 복수형 사용: `players`, `teams`, `games`

### 컬럼 규칙
- PK: `id` (BIGSERIAL)
- 외래키: `[테이블명]_id`
- 타임스탬프: `created_at`, `updated_at`
- 위치 정보: `location` (GEOMETRY/GEOGRAPHY)

### PostGIS 규칙
- 기본 SRID: 4326 (WGS 84)
- 거리 계산 시 Geography 타입 권장
- 공간 인덱스(GiST) 필수 생성

## 호출 에이전트

- `architect`: Repository 구현 시 DB 스키마 검증, Entity 설계 시 DDL 생성 요청
- `reviewer`: 테스트용 DB 스키마 검증
