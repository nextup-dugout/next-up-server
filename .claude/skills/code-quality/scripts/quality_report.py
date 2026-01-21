#!/usr/bin/env python3
# .claude/skills/code-quality/scripts/quality_report.py
# 코드 품질 통합 리포트 생성 스크립트

import json
import sys
import subprocess
from pathlib import Path
from datetime import datetime
from typing import Dict, Any, List


def run_command(cmd: List[str]) -> tuple[int, str]:
    """명령어 실행 및 결과 반환"""
    try:
        result = subprocess.run(cmd, capture_output=True, text=True, timeout=300)
        return result.returncode, result.stdout + result.stderr
    except subprocess.TimeoutExpired:
        return 1, "Command timed out"
    except Exception as e:
        return 1, str(e)


def load_json_report(path: Path) -> Dict[str, Any]:
    """JSON 리포트 로드"""
    if path.exists():
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    return {}


def run_ktlint(project_root: Path, output_dir: Path) -> Dict[str, Any]:
    """ktlint 실행"""
    print("🔍 Running ktlint...")
    script_path = project_root / ".claude/skills/code-quality/scripts/run_ktlint.sh"

    if script_path.exists():
        code, output = run_command(["bash", str(script_path), "", str(output_dir)])
    else:
        code, output = run_command(["./gradlew", "ktlintCheck", "--no-daemon"])

    report_path = output_dir / "ktlint-report.json"
    return load_json_report(report_path) if report_path.exists() else {
        "success": code == 0,
        "exit_code": code,
        "summary": {"errors": 0, "warnings": 0, "total": 0}
    }


def run_detekt(project_root: Path, output_dir: Path) -> Dict[str, Any]:
    """detekt 실행"""
    print("🔬 Running detekt...")
    script_path = project_root / ".claude/skills/code-quality/scripts/run_detekt.sh"

    if script_path.exists():
        code, output = run_command(["bash", str(script_path), "", str(output_dir)])
    else:
        code, output = run_command(["./gradlew", "detekt", "--no-daemon"])

    report_path = output_dir / "detekt-report.json"
    return load_json_report(report_path) if report_path.exists() else {
        "success": code == 0,
        "exit_code": code,
        "summary": {"total": 0}
    }


def evaluate_quality(ktlint: Dict, detekt: Dict) -> Dict[str, Any]:
    """품질 평가 및 VETO 판정"""
    veto_reasons = []
    warnings = []

    # ktlint 평가
    ktlint_summary = ktlint.get("summary", {})
    ktlint_errors = ktlint_summary.get("errors", 0)
    ktlint_warnings = ktlint_summary.get("warnings", 0)

    if ktlint_errors > 0:
        veto_reasons.append(f"ktlint errors: {ktlint_errors}")
    if ktlint_warnings >= 10:
        veto_reasons.append(f"ktlint warnings exceed threshold: {ktlint_warnings} >= 10")
    elif ktlint_warnings > 0:
        warnings.append(f"ktlint warnings: {ktlint_warnings}")

    # detekt 평가
    detekt_summary = detekt.get("summary", {})
    potential_bugs = detekt_summary.get("potential_bugs", 0)
    complexity = detekt_summary.get("complexity", 0)
    code_smell = detekt_summary.get("code_smell", 0)

    if potential_bugs > 0:
        veto_reasons.append(f"Potential bugs detected: {potential_bugs}")
    if complexity > 10:
        veto_reasons.append(f"Complexity issues exceed threshold: {complexity} > 10")
    elif complexity > 5:
        warnings.append(f"Complexity issues: {complexity}")
    if code_smell > 5:
        veto_reasons.append(f"Code smell issues exceed threshold: {code_smell} > 5")
    elif code_smell > 0:
        warnings.append(f"Code smell issues: {code_smell}")

    return {
        "pass": len(veto_reasons) == 0,
        "veto_recommended": len(veto_reasons) > 0,
        "veto_reasons": veto_reasons,
        "warnings": warnings
    }


def generate_report(project_root: str, output_dir: str) -> Dict[str, Any]:
    """통합 품질 리포트 생성"""
    project_path = Path(project_root)
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)

    # 각 도구 실행
    ktlint_result = run_ktlint(project_path, output_path)
    detekt_result = run_detekt(project_path, output_path)

    # 품질 평가
    evaluation = evaluate_quality(ktlint_result, detekt_result)

    # 통합 리포트 생성
    report = {
        "timestamp": datetime.utcnow().isoformat() + "Z",
        "project_root": str(project_path.absolute()),
        "tools": {
            "ktlint": ktlint_result,
            "detekt": detekt_result
        },
        "summary": {
            "ktlint_issues": ktlint_result.get("summary", {}).get("total", 0),
            "detekt_issues": detekt_result.get("summary", {}).get("total", 0),
            "total_issues": (
                ktlint_result.get("summary", {}).get("total", 0) +
                detekt_result.get("summary", {}).get("total", 0)
            )
        },
        "evaluation": evaluation
    }

    return report


def print_report(report: Dict[str, Any]) -> None:
    """리포트 출력"""
    print()
    print("=" * 60)
    print("📊 NEXT-UP Code Quality Report")
    print("=" * 60)
    print(f"Timestamp: {report['timestamp']}")

    print("\n📈 Summary:")
    summary = report["summary"]
    print(f"   ktlint issues:  {summary['ktlint_issues']}")
    print(f"   detekt issues:  {summary['detekt_issues']}")
    print(f"   ─────────────────")
    print(f"   Total issues:   {summary['total_issues']}")

    evaluation = report["evaluation"]

    if evaluation["warnings"]:
        print("\n⚠️ Warnings:")
        for warning in evaluation["warnings"]:
            print(f"   • {warning}")

    if evaluation["veto_reasons"]:
        print("\n❌ VETO Reasons:")
        for reason in evaluation["veto_reasons"]:
            print(f"   • {reason}")

    print("\n" + "=" * 60)
    if evaluation["pass"]:
        print("✅ QUALITY GATE: PASSED")
    else:
        print("🚫 QUALITY GATE: FAILED - REVIEWER VETO RECOMMENDED")
    print("=" * 60)


def main():
    project_root = sys.argv[1] if len(sys.argv) > 1 else "."
    output_dir = sys.argv[2] if len(sys.argv) > 2 else "outputs/quality"

    print(f"🚀 Starting code quality analysis...")
    print(f"   Project: {project_root}")
    print(f"   Output:  {output_dir}")

    report = generate_report(project_root, output_dir)

    # JSON 저장
    output_path = Path(output_dir)
    report_file = output_path / "quality-report.json"
    with open(report_file, 'w', encoding='utf-8') as f:
        json.dump(report, f, indent=2, ensure_ascii=False)

    print(f"\n📄 Report saved to: {report_file}")

    # 콘솔 출력
    print_report(report)

    # 종료 코드
    sys.exit(0 if report["evaluation"]["pass"] else 1)


if __name__ == "__main__":
    main()
