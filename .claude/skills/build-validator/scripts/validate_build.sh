#!/bin/bash
# .claude/skills/build-validator/scripts/validate_build.sh
# Gradle 빌드 실행 및 결과 반환

set -e

PROJECT_ROOT="${1:-.}"
OUTPUT_FILE="${2:-build-result.json}"

cd "$PROJECT_ROOT"

echo "🔨 Starting Gradle build..."
echo "📁 Project root: $(pwd)"

# 빌드 실행 및 결과 캡처
BUILD_OUTPUT=$(./gradlew build --no-daemon 2>&1) || BUILD_RESULT=$?
BUILD_RESULT=${BUILD_RESULT:-0}

# 빌드 결과 요약
if [ $BUILD_RESULT -eq 0 ]; then
    STATUS="SUCCESS"
    echo "✅ Build completed successfully"
else
    STATUS="FAILED"
    echo "❌ Build failed with exit code: $BUILD_RESULT"
fi

# 출력 디렉토리 생성
OUTPUT_DIR=$(dirname "$OUTPUT_FILE")
mkdir -p "$OUTPUT_DIR"

# JSON 결과 생성 (특수문자 이스케이프 처리)
OUTPUT_SUMMARY=$(echo "$BUILD_OUTPUT" | tail -30 | sed 's/\\/\\\\/g' | sed 's/"/\\"/g' | sed 's/	/ /g' | tr '\n' ' ' | cut -c1-1000)

cat > "$OUTPUT_FILE" << EOF
{
  "success": $([ $BUILD_RESULT -eq 0 ] && echo "true" || echo "false"),
  "status": "$STATUS",
  "exit_code": $BUILD_RESULT,
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "project_root": "$PROJECT_ROOT",
  "output_summary": "$OUTPUT_SUMMARY"
}
EOF

echo "📄 Build result saved to: $OUTPUT_FILE"

# 테스트 결과 요약 (있는 경우)
if [ -d "build/reports/tests" ]; then
    echo "📊 Test reports available at: build/reports/tests/"
fi

exit $BUILD_RESULT
