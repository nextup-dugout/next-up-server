#!/bin/bash
# .claude/skills/code-quality/scripts/run_detekt.sh
# detekt 정적 분석 스크립트

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

echo -e "${BLUE}🔬 Running detekt static analysis...${NC}"
echo "   Project: $PROJECT_ROOT"
[ -n "$MODULE" ] && echo "   Module: $MODULE"

# 출력 디렉토리 생성
mkdir -p "$OUTPUT_DIR"

# Gradle wrapper 확인
if [ ! -f "./gradlew" ]; then
    echo -e "${RED}Error: gradlew not found in current directory${NC}"
    exit 1
fi

# detekt 실행
echo -e "${YELLOW}Executing detekt...${NC}"

if [ -n "$MODULE" ]; then
    DETEKT_CMD="./gradlew :$MODULE:detekt --no-daemon"
else
    DETEKT_CMD="./gradlew detekt --no-daemon"
fi

# 실행 및 결과 캡처
DETEKT_OUTPUT=$($DETEKT_CMD 2>&1) || DETEKT_RESULT=$?
DETEKT_RESULT=${DETEKT_RESULT:-0}

# 결과 파일 저장
TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
REPORT_FILE="$OUTPUT_DIR/detekt-report.json"

# 카테고리별 이슈 카운트
COMPLEXITY_COUNT=$(echo "$DETEKT_OUTPUT" | grep -ci "complexity" || echo "0")
CODE_SMELL_COUNT=$(echo "$DETEKT_OUTPUT" | grep -ci "code-smell\|codesmell" || echo "0")
POTENTIAL_BUGS_COUNT=$(echo "$DETEKT_OUTPUT" | grep -ci "potential-bugs\|potentialbugs" || echo "0")
PERFORMANCE_COUNT=$(echo "$DETEKT_OUTPUT" | grep -ci "performance" || echo "0")
STYLE_COUNT=$(echo "$DETEKT_OUTPUT" | grep -ci "style" || echo "0")

TOTAL_COUNT=$((COMPLEXITY_COUNT + CODE_SMELL_COUNT + POTENTIAL_BUGS_COUNT + PERFORMANCE_COUNT + STYLE_COUNT))

# JSON 리포트 생성
cat > "$REPORT_FILE" << EOF
{
  "timestamp": "$TIMESTAMP",
  "tool": "detekt",
  "success": $([ $DETEKT_RESULT -eq 0 ] && echo "true" || echo "false"),
  "exit_code": $DETEKT_RESULT,
  "summary": {
    "total": $TOTAL_COUNT,
    "complexity": $COMPLEXITY_COUNT,
    "code_smell": $CODE_SMELL_COUNT,
    "potential_bugs": $POTENTIAL_BUGS_COUNT,
    "performance": $PERFORMANCE_COUNT,
    "style": $STYLE_COUNT
  },
  "thresholds": {
    "max_complexity": 10,
    "max_code_smell": 5,
    "max_potential_bugs": 0,
    "max_total": 20
  }
}
EOF

# 콘솔 출력
echo ""
echo -e "${BLUE}═══════════════════════════════════════════${NC}"
echo -e "${BLUE}  detekt Static Analysis Report${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"

if [ $DETEKT_RESULT -eq 0 ]; then
    echo -e "${GREEN}✅ detekt analysis PASSED${NC}"
else
    echo -e "${RED}❌ detekt analysis FAILED${NC}"
fi

echo ""
echo "📊 Issue Breakdown:"
echo "   ├── Complexity:     $COMPLEXITY_COUNT"
echo "   ├── Code Smell:     $CODE_SMELL_COUNT"
echo "   ├── Potential Bugs: $POTENTIAL_BUGS_COUNT"
echo "   ├── Performance:    $PERFORMANCE_COUNT"
echo "   └── Style:          $STYLE_COUNT"
echo "   ────────────────────"
echo "   Total:              $TOTAL_COUNT"

# VETO 기준 체크
echo ""
echo -e "${YELLOW}⚠️ VETO Criteria Check:${NC}"

VETO_TRIGGERED=false

if [ $POTENTIAL_BUGS_COUNT -gt 0 ]; then
    echo -e "   ${RED}✗ Potential bugs detected ($POTENTIAL_BUGS_COUNT) - VETO${NC}"
    VETO_TRIGGERED=true
else
    echo -e "   ${GREEN}✓ No potential bugs${NC}"
fi

if [ $COMPLEXITY_COUNT -gt 10 ]; then
    echo -e "   ${RED}✗ Complexity issues exceed threshold ($COMPLEXITY_COUNT > 10) - VETO${NC}"
    VETO_TRIGGERED=true
else
    echo -e "   ${GREEN}✓ Complexity within limits ($COMPLEXITY_COUNT ≤ 10)${NC}"
fi

if [ $CODE_SMELL_COUNT -gt 5 ]; then
    echo -e "   ${RED}✗ Code smell issues exceed threshold ($CODE_SMELL_COUNT > 5) - VETO${NC}"
    VETO_TRIGGERED=true
else
    echo -e "   ${GREEN}✓ Code smell within limits ($CODE_SMELL_COUNT ≤ 5)${NC}"
fi

echo ""
if [ "$VETO_TRIGGERED" = true ]; then
    echo -e "${RED}🚫 REVIEWER VETO RECOMMENDED${NC}"
else
    echo -e "${GREEN}✅ Quality gate PASSED${NC}"
fi

echo ""
echo -e "${BLUE}Report saved to: $REPORT_FILE${NC}"
echo -e "${BLUE}═══════════════════════════════════════════${NC}"

# detekt HTML 리포트 위치 안내
echo ""
echo -e "${YELLOW}💡 Detailed HTML reports available at:${NC}"
if [ -n "$MODULE" ]; then
    echo "   $MODULE/build/reports/detekt/detekt.html"
else
    echo "   */build/reports/detekt/detekt.html"
fi

exit $DETEKT_RESULT
