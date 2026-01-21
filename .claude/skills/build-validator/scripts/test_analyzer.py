#!/usr/bin/env python3
# .claude/skills/build-validator/scripts/test_analyzer.py
# JUnit 테스트 결과 분석 스크립트

import xml.etree.ElementTree as ET
import json
import sys
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Any


def parse_junit_xml(xml_path: Path) -> Dict[str, Any]:
    """JUnit XML 파일을 파싱하여 결과 반환"""
    try:
        tree = ET.parse(xml_path)
        root = tree.getroot()
    except ET.ParseError as e:
        print(f"⚠️ Failed to parse {xml_path}: {e}")
        return {'total': 0, 'passed': 0, 'failed': 0, 'skipped': 0, 'errors': 0, 'failures': []}

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

        # 실패 케이스 상세 정보 수집
        for testcase in suite.findall('testcase'):
            failure = testcase.find('failure')
            error = testcase.find('error')

            if failure is not None or error is not None:
                fail_info = failure if failure is not None else error
                results['failures'].append({
                    'class': testcase.get('classname', 'Unknown'),
                    'method': testcase.get('name', 'Unknown'),
                    'message': (fail_info.get('message', '') or '')[:500],
                    'type': fail_info.get('type', 'Unknown'),
                    'file': str(xml_path)
                })

    results['passed'] = results['total'] - results['failed'] - results['skipped']
    return results


def analyze_test_results(project_root: str) -> Dict[str, Any]:
    """프로젝트의 모든 테스트 결과 분석"""
    root_path = Path(project_root)

    all_results = {
        'timestamp': datetime.utcnow().isoformat() + 'Z',
        'project_root': str(root_path.absolute()),
        'modules': {},
        'summary': {
            'total': 0,
            'passed': 0,
            'failed': 0,
            'skipped': 0,
            'errors': 0,
            'pass_rate': 0.0
        },
        'all_failures': []
    }

    # 각 모듈의 테스트 결과 검색
    # Gradle 멀티모듈 구조: module/build/test-results/test/
    test_result_patterns = [
        '**/build/test-results/test',
        '**/build/test-results/testDebugUnitTest',  # Android
    ]

    test_dirs: List[Path] = []
    for pattern in test_result_patterns:
        test_dirs.extend(root_path.glob(pattern))

    if not test_dirs:
        print("⚠️ No test results found. Make sure to run './gradlew test' first.")
        return all_results

    for test_dir in test_dirs:
        # 모듈명 추출 (build 상위 디렉토리)
        try:
            build_idx = test_dir.parts.index('build')
            module_name = test_dir.parts[build_idx - 1] if build_idx > 0 else 'root'
        except ValueError:
            module_name = 'unknown'

        module_results = {
            'total': 0,
            'passed': 0,
            'failed': 0,
            'skipped': 0,
            'errors': 0,
            'failures': []
        }

        # XML 파일 파싱
        for xml_file in test_dir.glob('TEST-*.xml'):
            result = parse_junit_xml(xml_file)
            module_results['total'] += result['total']
            module_results['passed'] += result['passed']
            module_results['failed'] += result['failed']
            module_results['skipped'] += result['skipped']
            module_results['errors'] += result['errors']
            module_results['failures'].extend(result['failures'])

        if module_results['total'] > 0:
            all_results['modules'][module_name] = module_results

            # 전체 집계
            all_results['summary']['total'] += module_results['total']
            all_results['summary']['passed'] += module_results['passed']
            all_results['summary']['failed'] += module_results['failed']
            all_results['summary']['skipped'] += module_results['skipped']
            all_results['summary']['errors'] += module_results['errors']
            all_results['all_failures'].extend(module_results['failures'])

    # 통과율 계산
    if all_results['summary']['total'] > 0:
        all_results['summary']['pass_rate'] = round(
            all_results['summary']['passed'] / all_results['summary']['total'] * 100, 2
        )

    return all_results


def print_summary(results: Dict[str, Any]) -> None:
    """결과 요약 출력"""
    summary = results['summary']

    print(f"\n{'='*50}")
    print(f"📊 NEXT-UP Test Analysis Report")
    print(f"{'='*50}")
    print(f"Timestamp: {results['timestamp']}")
    print(f"\n📈 Summary:")
    print(f"  Total Tests:  {summary['total']}")
    print(f"  ✅ Passed:    {summary['passed']}")
    print(f"  ❌ Failed:    {summary['failed']}")
    print(f"  ⏭️ Skipped:   {summary['skipped']}")
    print(f"  🔥 Errors:    {summary['errors']}")
    print(f"  📊 Pass Rate: {summary['pass_rate']}%")

    if results['modules']:
        print(f"\n📦 Module Breakdown:")
        for module, data in results['modules'].items():
            status = "✅" if data['failed'] == 0 else "❌"
            print(f"  {status} {module}: {data['passed']}/{data['total']} passed")

    if results['all_failures']:
        print(f"\n❌ Failed Tests ({len(results['all_failures'])}):")
        for i, fail in enumerate(results['all_failures'][:10], 1):
            print(f"\n  {i}. {fail['class']}")
            print(f"     Method: {fail['method']}")
            print(f"     Type: {fail['type']}")
            if fail['message']:
                msg = fail['message'][:200].replace('\n', ' ')
                print(f"     Message: {msg}...")

        if len(results['all_failures']) > 10:
            print(f"\n  ... and {len(results['all_failures']) - 10} more failures")

    print(f"\n{'='*50}")

    # 최종 판정
    if summary['failed'] == 0 and summary['total'] > 0:
        print("🎉 ALL TESTS PASSED!")
    elif summary['total'] == 0:
        print("⚠️ NO TESTS FOUND")
    else:
        print(f"💥 {summary['failed']} TEST(S) FAILED - BUILD SHOULD BE REJECTED")


def main():
    project_root = sys.argv[1] if len(sys.argv) > 1 else '.'
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'test-analysis.json'

    print(f"🔍 Analyzing test results in: {project_root}")

    results = analyze_test_results(project_root)

    # 출력 디렉토리 생성
    output_path = Path(output_file)
    output_path.parent.mkdir(parents=True, exist_ok=True)

    # JSON 저장
    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(results, f, indent=2, ensure_ascii=False)

    print(f"📄 Analysis saved to: {output_file}")

    # 요약 출력
    print_summary(results)

    # 실패 시 exit code 1
    sys.exit(0 if results['summary']['failed'] == 0 else 1)


if __name__ == '__main__':
    main()
