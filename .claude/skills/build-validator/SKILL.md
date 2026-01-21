---
name: build-validator
description: |
  Gradle 빌드 및 JUnit 테스트 결과를 자동 분석하는 실행 스킬.
  ./gradlew build 실행 후 결과를 분석하여 판단 에이전트에게 전달한다.
---

# Build-Validator Skill - 빌드 검증 스킬

## 개요

이 스킬은 프로젝트의 빌드 및 테스트 상태를 물리적으로 검증합니다. 판단(Agent)과 실행(Skill)의 분리 원칙에 따라, 이 스킬은 **실행만** 담당하며 결과 해석은 호출한 에이전트가 수행합니다.

## 호출 조건

- 커밋 전 빌드 검증 필요 시
- PR 생성 전 최종 빌드 확인 시
- `reviewer`의 검수 프로세스 중 빌드 상태 확인 시

## 제공 스크립트

### 1. validate_build.sh

Gradle 빌드를 실행하고 결과를 반환합니다.

```bash
#!/bin/bash
# .claude/skills/build-validator/scripts/validate_build.sh

set -e

PROJECT_ROOT="${1:-.}"
OUTPUT_FILE="${2:-build-result.json}"

cd "$PROJECT_ROOT"

echo "🔨 Starting Gradle build..."

# 빌드 실행 및 결과 캡처
BUILD_OUTPUT=$(./gradlew build 2>&1) || BUILD_RESULT=$?
BUILD_RESULT=${BUILD_RESULT:-0}

# 테스트 결과 파싱
TEST_RESULTS=""
if [ -d "build/test-results" ]; then
    TEST_RESULTS=$(find . -name "TEST-*.xml" -exec cat {} \; 2>/dev/null || echo "")
fi

# JSON 결과 생성
cat > "$OUTPUT_FILE" << EOF
{
  "success": $([ $BUILD_RESULT -eq 0 ] && echo "true" || echo "false"),
  "exit_code": $BUILD_RESULT,
  "timestamp": "$(date -u +"%Y-%m-%dT%H:%M:%SZ")",
  "output_summary": "$(echo "$BUILD_OUTPUT" | tail -20 | sed 's/"/\\"/g' | tr '\n' ' ')"
}
EOF

echo "📄 Build result saved to: $OUTPUT_FILE"

exit $BUILD_RESULT
```

### 2. test_analyzer.py

테스트 결과를 분석하여 상세 리포트를 생성합니다.

```python
#!/usr/bin/env python3
# .claude/skills/build-validator/scripts/test_analyzer.py

import xml.etree.ElementTree as ET
import json
import sys
from pathlib import Path
from datetime import datetime

def parse_junit_xml(xml_path: Path) -> dict:
    """JUnit XML 파일을 파싱하여 결과 반환"""
    tree = ET.parse(xml_path)
    root = tree.getroot()

    # testsuite 또는 testsuites 루트 처리
    if root.tag == 'testsuites':
        suites = root.findall('testsuite')
    else:
        suites = [root]

    results = {
        'total': 0,
        'passed': 0,
        'failed': 0,
        'skipped': 0,
        'errors': 0,
        'failures': []
    }

    for suite in suites:
        tests = int(suite.get('tests', 0))
        failures = int(suite.get('failures', 0))
        errors = int(suite.get('errors', 0))
        skipped = int(suite.get('skipped', 0))

        results['total'] += tests
        results['failed'] += failures + errors
        results['skipped'] += skipped
        results['errors'] += errors

        # 실패 케이스 상세 정보
        for testcase in suite.findall('testcase'):
            failure = testcase.find('failure')
            error = testcase.find('error')

            if failure is not None or error is not None:
                fail_info = failure if failure is not None else error
                results['failures'].append({
                    'class': testcase.get('classname'),
                    'method': testcase.get('name'),
                    'message': fail_info.get('message', ''),
                    'type': fail_info.get('type', '')
                })

    results['passed'] = results['total'] - results['failed'] - results['skipped']
    return results


def analyze_test_results(project_root: str) -> dict:
    """프로젝트의 모든 테스트 결과 분석"""
    root_path = Path(project_root)

    # 모든 모듈의 테스트 결과 수집
    all_results = {
        'timestamp': datetime.utcnow().isoformat() + 'Z',
        'modules': {},
        'summary': {
            'total': 0,
            'passed': 0,
            'failed': 0,
            'skipped': 0,
            'pass_rate': 0.0
        },
        'all_failures': []
    }

    # 각 모듈의 테스트 결과 검색
    test_result_dirs = list(root_path.glob('**/build/test-results/test'))

    for test_dir in test_result_dirs:
        module_name = test_dir.parts[-4]  # build 상위 디렉토리명
        module_results = {
            'total': 0,
            'passed': 0,
            'failed': 0,
            'skipped': 0,
            'failures': []
        }

        for xml_file in test_dir.glob('TEST-*.xml'):
            result = parse_junit_xml(xml_file)
            module_results['total'] += result['total']
            module_results['passed'] += result['passed']
            module_results['failed'] += result['failed']
            module_results['skipped'] += result['skipped']
            module_results['failures'].extend(result['failures'])

        all_results['modules'][module_name] = module_results

        # 전체 집계
        all_results['summary']['total'] += module_results['total']
        all_results['summary']['passed'] += module_results['passed']
        all_results['summary']['failed'] += module_results['failed']
        all_results['summary']['skipped'] += module_results['skipped']
        all_results['all_failures'].extend(module_results['failures'])

    # 통과율 계산
    if all_results['summary']['total'] > 0:
        all_results['summary']['pass_rate'] = round(
            all_results['summary']['passed'] / all_results['summary']['total'] * 100, 2
        )

    return all_results


def main():
    project_root = sys.argv[1] if len(sys.argv) > 1 else '.'
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'test-analysis.json'

    results = analyze_test_results(project_root)

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    # 콘솔 요약 출력
    summary = results['summary']
    print(f"\n📊 Test Analysis Summary")
    print(f"{'='*40}")
    print(f"Total:   {summary['total']}")
    print(f"Passed:  {summary['passed']} ✅")
    print(f"Failed:  {summary['failed']} {'❌' if summary['failed'] > 0 else ''}")
    print(f"Skipped: {summary['skipped']}")
    print(f"Pass Rate: {summary['pass_rate']}%")

    if results['all_failures']:
        print(f"\n❌ Failed Tests:")
        for fail in results['all_failures'][:5]:  # 최대 5개만 표시
            print(f"  - {fail['class']}.{fail['method']}")
            print(f"    {fail['message'][:100]}...")

    # 실패 시 exit code 1
    sys.exit(0 if summary['failed'] == 0 else 1)


if __name__ == '__main__':
    main()
```

## 사용 예시

### 에이전트에서 호출

```bash
# 빌드 검증
bash .claude/skills/build-validator/scripts/validate_build.sh . outputs/build/build-result.json

# 테스트 분석
python3 .claude/skills/build-validator/scripts/test_analyzer.py . outputs/build/test-summary.json
```

## 출력 형식

### build-result.json
```json
{
  "success": true,
  "exit_code": 0,
  "timestamp": "2024-01-01T00:00:00Z",
  "output_summary": "BUILD SUCCESSFUL in 30s"
}
```

### test-analysis.json
```json
{
  "timestamp": "2024-01-01T00:00:00Z",
  "modules": {
    "nextup-core": {
      "total": 50,
      "passed": 48,
      "failed": 2,
      "skipped": 0,
      "failures": [...]
    }
  },
  "summary": {
    "total": 150,
    "passed": 148,
    "failed": 2,
    "skipped": 0,
    "pass_rate": 98.67
  }
}
```

## 호출 에이전트

- `reviewer`: 검수 프로세스 중 빌드 상태 확인
- `github-manager`: 커밋 전 빌드 검증
- `scenario-tester`: 테스트 실행 후 결과 분석
- `risk-manager`: 실패 원인 분석 시 로그 확인
