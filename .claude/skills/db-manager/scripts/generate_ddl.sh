#!/bin/bash
# .claude/skills/db-manager/scripts/generate_ddl.sh
# Flyway 마이그레이션 DDL 생성 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 사용법
usage() {
    echo "Usage: $0 <migration_name> [migration_dir] [--type <type>]"
    echo ""
    echo "Arguments:"
    echo "  migration_name  - Name of the migration (e.g., create_player_table)"
    echo "  migration_dir   - Migration directory (default: src/main/resources/db/migration)"
    echo ""
    echo "Options:"
    echo "  --type <type>   - Migration type: table, index, alter, data (default: table)"
    echo ""
    echo "Examples:"
    echo "  $0 create_player_table"
    echo "  $0 add_location_index --type index"
    echo "  $0 add_team_column_to_player --type alter"
    exit 1
}

# 파라미터 파싱
MIGRATION_NAME=""
MIGRATION_DIR="src/main/resources/db/migration"
MIGRATION_TYPE="table"

while [[ $# -gt 0 ]]; do
    case $1 in
        --type)
            MIGRATION_TYPE="$2"
            shift 2
            ;;
        --help|-h)
            usage
            ;;
        *)
            if [ -z "$MIGRATION_NAME" ]; then
                MIGRATION_NAME="$1"
            else
                MIGRATION_DIR="$1"
            fi
            shift
            ;;
    esac
done

if [ -z "$MIGRATION_NAME" ]; then
    echo -e "${RED}Error: Migration name is required${NC}"
    usage
fi

# 버전 번호 생성 (현재 타임스탬프)
VERSION=$(date +%Y%m%d%H%M%S)
TODAY=$(date +%Y-%m-%d)

# 파일명 생성
FILENAME="V${VERSION}__${MIGRATION_NAME}.sql"
FILEPATH="${MIGRATION_DIR}/${FILENAME}"

echo -e "${YELLOW}📝 Generating Flyway migration...${NC}"
echo "  Name: $MIGRATION_NAME"
echo "  Type: $MIGRATION_TYPE"
echo "  File: $FILEPATH"

# 디렉토리 생성
mkdir -p "$MIGRATION_DIR"

# 타입별 템플릿 생성
case $MIGRATION_TYPE in
    table)
        cat > "$FILEPATH" << EOF
-- ============================================
-- Migration: ${MIGRATION_NAME}
-- Version: V${VERSION}
-- Author: NEXT-UP Auto Generator
-- Date: ${TODAY}
-- Type: CREATE TABLE
-- ============================================

-- Description:
-- [테이블 생성 목적 및 설명 작성]

-- ============================================
-- Prerequisites
-- ============================================
-- CREATE EXTENSION IF NOT EXISTS postgis;

-- ============================================
-- DDL: Create Table
-- ============================================

CREATE TABLE IF NOT EXISTS [table_name] (
    -- Primary Key
    id BIGSERIAL PRIMARY KEY,

    -- Business Columns
    name VARCHAR(255) NOT NULL,
    -- [추가 컬럼 정의]

    -- Foreign Keys
    -- team_id BIGINT REFERENCES teams(id),

    -- PostGIS Geometry Column (필요 시)
    -- location GEOMETRY(Point, 4326),

    -- Audit Columns (BaseTimeEntity)
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================
-- Indexes
-- ============================================

-- Unique Index
-- CREATE UNIQUE INDEX IF NOT EXISTS idx_[table]_[column]
--     ON [table_name] ([column]);

-- Spatial Index (PostGIS)
-- CREATE INDEX IF NOT EXISTS idx_[table]_location
--     ON [table_name] USING GIST (location);

-- ============================================
-- Comments
-- ============================================

COMMENT ON TABLE [table_name] IS '[테이블 설명]';
COMMENT ON COLUMN [table_name].id IS '기본 키';
-- COMMENT ON COLUMN [table_name].[column] IS '[컬럼 설명]';

-- ============================================
-- Rollback Script (주석으로 보관)
-- ============================================
-- DROP TABLE IF EXISTS [table_name] CASCADE;

EOF
        ;;

    index)
        cat > "$FILEPATH" << EOF
-- ============================================
-- Migration: ${MIGRATION_NAME}
-- Version: V${VERSION}
-- Author: NEXT-UP Auto Generator
-- Date: ${TODAY}
-- Type: CREATE INDEX
-- ============================================

-- Description:
-- [인덱스 생성 목적 설명]

-- ============================================
-- DDL: Create Index
-- ============================================

-- B-Tree Index (일반 조회)
-- CREATE INDEX IF NOT EXISTS idx_[table]_[column]
--     ON [table_name] ([column]);

-- Composite Index (복합 조회)
-- CREATE INDEX IF NOT EXISTS idx_[table]_[col1]_[col2]
--     ON [table_name] ([col1], [col2]);

-- Partial Index (조건부 인덱스)
-- CREATE INDEX IF NOT EXISTS idx_[table]_[column]_active
--     ON [table_name] ([column])
--     WHERE is_active = true;

-- Spatial Index (PostGIS)
-- CREATE INDEX IF NOT EXISTS idx_[table]_location
--     ON [table_name] USING GIST (location);

-- ============================================
-- Rollback Script
-- ============================================
-- DROP INDEX IF EXISTS idx_[table]_[column];

EOF
        ;;

    alter)
        cat > "$FILEPATH" << EOF
-- ============================================
-- Migration: ${MIGRATION_NAME}
-- Version: V${VERSION}
-- Author: NEXT-UP Auto Generator
-- Date: ${TODAY}
-- Type: ALTER TABLE
-- ============================================

-- Description:
-- [변경 목적 설명]

-- ============================================
-- DDL: Alter Table
-- ============================================

-- Add Column
-- ALTER TABLE [table_name]
--     ADD COLUMN [column_name] [data_type] [constraints];

-- Add Column with Default
-- ALTER TABLE [table_name]
--     ADD COLUMN [column_name] VARCHAR(255) DEFAULT '' NOT NULL;

-- Modify Column
-- ALTER TABLE [table_name]
--     ALTER COLUMN [column_name] TYPE [new_type];

-- Add Foreign Key
-- ALTER TABLE [table_name]
--     ADD CONSTRAINT fk_[table]_[ref_table]
--     FOREIGN KEY ([column]) REFERENCES [ref_table](id);

-- Add Constraint
-- ALTER TABLE [table_name]
--     ADD CONSTRAINT chk_[table]_[column]
--     CHECK ([condition]);

-- ============================================
-- Rollback Script
-- ============================================
-- ALTER TABLE [table_name] DROP COLUMN [column_name];
-- ALTER TABLE [table_name] DROP CONSTRAINT [constraint_name];

EOF
        ;;

    data)
        cat > "$FILEPATH" << EOF
-- ============================================
-- Migration: ${MIGRATION_NAME}
-- Version: V${VERSION}
-- Author: NEXT-UP Auto Generator
-- Date: ${TODAY}
-- Type: DATA MIGRATION
-- ============================================

-- Description:
-- [데이터 마이그레이션 목적 설명]

-- ============================================
-- Data Migration
-- ============================================

-- Insert Initial Data
-- INSERT INTO [table_name] ([columns])
-- VALUES
--     ([values1]),
--     ([values2]);

-- Update Existing Data
-- UPDATE [table_name]
-- SET [column] = [value]
-- WHERE [condition];

-- Migrate Data from Another Table
-- INSERT INTO [new_table] ([columns])
-- SELECT [columns]
-- FROM [old_table]
-- WHERE [condition];

-- ============================================
-- Verification
-- ============================================
-- SELECT COUNT(*) FROM [table_name] WHERE [condition];

-- ============================================
-- Rollback Script
-- ============================================
-- DELETE FROM [table_name] WHERE [condition];

EOF
        ;;

    *)
        echo -e "${RED}Error: Unknown migration type: $MIGRATION_TYPE${NC}"
        echo "Valid types: table, index, alter, data"
        exit 1
        ;;
esac

echo -e "${GREEN}✅ Migration file created successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Next steps:${NC}"
echo "  1. Edit the migration file: $FILEPATH"
echo "  2. Replace [placeholders] with actual values"
echo "  3. Run: ./gradlew flywayMigrate"
echo "  4. Verify: ./gradlew flywayInfo"
echo ""

# JSON 결과 출력
cat << EOF
--- JSON Result ---
{
  "success": true,
  "version": "$VERSION",
  "filename": "$FILENAME",
  "filepath": "$FILEPATH",
  "migration_name": "$MIGRATION_NAME",
  "migration_type": "$MIGRATION_TYPE",
  "date": "$TODAY"
}
EOF
