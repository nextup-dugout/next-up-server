#!/bin/bash
# .claude/skills/code-quality/scripts/run_ktlint.sh
# ktlint 코드 스타일 검사 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 파라미터
MODULE="${1:-}"
OUTPUT_DIR="${2:-outputs/quality}"
PROJECT_ROOT="$(pwd)"

echo -e "${BLUE}🔍 Running ktlint code style check...${NC}"
echo "   Project: $PROJECT_ROOT"
[ -n "$MODULE" ] && echo "   Module: $MODULE"

# 출력 디렉토리 생성
mkdir -p "$OUTPUT_DIR"

# Gradle wrapper 확인
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Error: gradlew not found in current directory${NC}"
    exit 1
fi

# ktlint 실행
echo -e "${YELLOW}Executing ktlint...${NC}"

if [ -n "$MODULE" ]; then
    # 특정 모듈만 검사
    KTLINT_CMD="./gradlew :$MODULE:ktlintCheck --no-daemon"
else
    # 전체 프로젝트 검사
    KTLINT_CMD="./gradlew ktlintCheck --no-daemon"
fi

# 실행 및 결과 캡처
KTLINT_OUTPUT=$($KTLINT_CMD 2>&1) || KTLINT_RESULT=$?
KTLINT_RESULT=${KTLINT_RESULT:-0}

# 결과 파일 저장
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
REPORT_FILE="$OUTPUT_DIR/ktlint-report.json"

# JSON 리포트 수집 (각 모듈의 ktlint 리포트)
echo "{" > "$REPORT_FILE"
echo "  \"timestamp\": \"$TIMESTAMP\"," >> "$REPORT_FILE"
echo "  \"tool\": \"ktlint\"," >> "$REPORT_FILE"
echo "  \"success\": $([ $KTLINT_RESULT -eq 0 ] && echo "true" || echo "false")," >> "$REPORT_FILE"
echo "  \"exit_code\": $KTLINT_RESULT," >> "$REPORT_FILE"

# 이슈 카운트
ERROR_COUNT=$(echo "$KTLINT_OUTPUT" | grep -c "error" || echo "0")
WARNING_COUNT=$(echo "$KTLINT_OUTPUT" | grep -c "warning" || echo "0")

echo "  \"summary\": {" >> "$REPORT_FILE"
echo "    \"errors\": $ERROR_COUNT," >> "$REPORT_FILE"
echo "    \"warnings\": $WARNING_COUNT," >> "$REPORT_FILE"
echo "    \"total\": $((ERROR_COUNT + WARNING_COUNT))" >> "$REPORT_FILE"
echo "  }," >> "$REPORT_FILE"

# 출력 요약 저장
OUTPUT_SUMMARY=$(echo "$KTLINT_OUTPUT" | tail -30 | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | tr '\n' ' ' | cut -c1-2000)
echo "  \"output_summary\": \"$OUTPUT_SUMMARY\"" >> "$REPORT_FILE"
echo "}" >> "$REPORT_FILE"

# 콘솔 출력
echo ""
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}  ktlint Report${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"

if [ $KTLINT_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ ktlint check PASSED${NC}"
    echo "   No style violations found."
else
    echo -e "${RED}❌ ktlint check FAILED${NC}"
    echo "   Errors: $ERROR_COUNT"
    echo "   Warnings: $WARNING_COUNT"
    echo ""
    echo -e "${YELLOW}Issues found:${NC}"
    echo "$KTLINT_OUTPUT" | grep -E "(error|warning)" | head -20

    if [ $ERROR_COUNT -gt 20 ]; then
        echo "   ... and more (see full report)"
    fi
fi

echo ""
echo -e "${BLUE}Report saved to: $REPORT_FILE${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"

# 자동 수정 힌트
if [ $KTLINT_RESULT -ne 0 ]; then
    echo ""
    echo -e "${YELLOW}💡 To auto-fix some issues, run:${NC}"
    if [ -n "$MODULE" ]; then
        echo "   ./gradlew :$MODULE:ktlintFormat"
    else
        echo "   ./gradlew ktlintFormat"
    fi
fi

exit $KTLINT_RESULT
