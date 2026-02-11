#!/bin/bash
# PostToolUse Bash hook: 커밋 메시지 Udacity Style 컨벤션 검증
# git commit 실행 후 커밋 메시지 형식을 검증한다.

# stdin에서 hook 데이터 읽기
INPUT=$(cat)

# tool_input.command에서 실행된 명령 추출
COMMAND=$(echo "$INPUT" | jq -r '.tool_input.command // ""' 2>/dev/null)

# git commit 명령이 아니면 무시
if ! echo "$COMMAND" | grep -qE 'git commit'; then
  exit 0
fi

# 가장 최근 커밋 메시지 가져오기
LAST_MSG=$(git log -1 --pretty=%s 2>/dev/null)
if [ -z "$LAST_MSG" ]; then
  exit 0
fi

# Udacity Style 커밋 타입 검증: type(scope): description
VALID_TYPES="feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert"
if ! echo "$LAST_MSG" | grep -qE "^($VALID_TYPES)(\(.+\))?: .+"; then
  echo "WARN: 커밋 메시지가 Udacity Style 컨벤션을 따르지 않습니다."
  echo "  현재: $LAST_MSG"
  echo "  형식: type(scope): description"
  echo "  타입: feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert"
  # WARN만 출력하고 차단하지는 않음 (PostToolUse이므로 이미 실행됨)
  exit 0
fi

exit 0
