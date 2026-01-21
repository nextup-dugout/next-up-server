#!/bin/bash
# .claude/skills/git-toolkit/scripts/gh_issue_creator.sh
# GitHub 이슈 생성 래퍼 스크립트

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 사용법
usage() {
    echo "Usage: $0 <title> [body] [labels] [assignee]"
    echo ""
    echo "Arguments:"
    echo "  title     - Issue title (required)"
    echo "  body      - Issue body (optional)"
    echo "  labels    - Comma-separated labels (optional)"
    echo "  assignee  - Assignee username (optional, use @me for self)"
    echo ""
    echo "Example:"
    echo "  $0 'feat: Add player registration' 'Description here' 'enhancement,priority:high' '@me'"
    exit 1
}

# 파라미터 검증
if [ -z "$1" ]; then
    echo -e "${RED}Error: Title is required${NC}"
    usage
fi

TITLE="$1"
BODY="${2:-}"
LABELS="${3:-}"
ASSIGNEE="${4:-}"

echo -e "${YELLOW}📝 Creating GitHub Issue...${NC}"
echo "  Title: $TITLE"

# gh CLI 설치 확인
if ! command -v gh &> /dev/null; then
    echo -e "${RED}Error: GitHub CLI (gh) is not installed${NC}"
    echo "Please install it: https://cli.github.com/"
    exit 1
fi

# gh 인증 확인
if ! gh auth status &> /dev/null; then
    echo -e "${RED}Error: Not authenticated with GitHub CLI${NC}"
    echo "Please run: gh auth login"
    exit 1
fi

# 이슈 생성 명령어 구성
ARGS=("issue" "create" "--title" "$TITLE")

if [ -n "$BODY" ]; then
    ARGS+=("--body" "$BODY")
fi

if [ -n "$LABELS" ]; then
    # 쉼표로 구분된 레이블 처리
    IFS=',' read -ra LABEL_ARRAY <<< "$LABELS"
    for label in "${LABEL_ARRAY[@]}"; do
        label=$(echo "$label" | xargs)  # trim whitespace
        ARGS+=("--label" "$label")
    done
fi

if [ -n "$ASSIGNEE" ]; then
    ARGS+=("--assignee" "$ASSIGNEE")
fi

# 실행
echo -e "${YELLOW}Executing: gh ${ARGS[*]}${NC}"
ISSUE_URL=$(gh "${ARGS[@]}" 2>&1)

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Failed to create issue${NC}"
    echo "$ISSUE_URL"
    exit 1
fi

echo -e "${GREEN}✅ Issue created successfully!${NC}"
echo "  URL: $ISSUE_URL"

# 이슈 번호 추출
ISSUE_NUMBER=$(echo "$ISSUE_URL" | grep -oE '[0-9]+$' || echo "unknown")
echo "  Number: #$ISSUE_NUMBER"

# JSON 결과 출력
echo ""
echo "--- JSON Result ---"
cat << EOF
{
  "success": true,
  "issue_number": "$ISSUE_NUMBER",
  "issue_url": "$ISSUE_URL",
  "title": "$TITLE",
  "labels": "$LABELS",
  "assignee": "$ASSIGNEE"
}
EOF
