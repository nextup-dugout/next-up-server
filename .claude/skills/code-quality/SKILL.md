---
name: code-quality
description: |
  Kotlin 코드 품질 검사를 수행하는 실행 스킬.
  ktlint(코드 스타일), detekt(정적 분석)를 실행하여 코드 품질을 검증한다.
---

# Code-Quality Skill - 코드 품질 검사 스킬

## 개요

이 스킬은 Kotlin 코드의 품질을 자동으로 검사합니다. 판단(Agent)과 실행(Skill)의 분리 원칙에 따라, 이 스킬은 **실행만** 담당하며 결과 해석 및 조치는 호출한 에이전트가 수행합니다.

## 호출 조건

- 커밋 전 코드 스타일 검증
- PR 생성 전 정적 분석 수행
- `reviewer`의 검수 프로세스 중 코드 품질 확인

## 제공 스크립트

### 1. run_ktlint.sh

ktlint를 실행하여 Kotlin 코드 스타일을 검사합니다.

### 2. run_detekt.sh

detekt를 실행하여 코드 스멜, 복잡도, 잠재적 버그를 탐지합니다.

### 3. quality_report.py

ktlint + detekt 결과를 통합 분석하여 리포트를 생성합니다.

## 검사 항목

### ktlint (코드 스타일)

| 규칙 | 설명 |
|------|------|
| `indent` | 들여쓰기 (4 spaces) |
| `max-line-length` | 최대 줄 길이 (120자) |
| `no-wildcard-imports` | 와일드카드 import 금지 |
| `no-unused-imports` | 미사용 import 제거 |
| `trailing-comma` | 후행 쉼표 규칙 |
| `spacing` | 공백 규칙 |

### detekt (정적 분석)

| 카테고리 | 검사 내용 |
|----------|-----------|
| **complexity** | 순환 복잡도, 함수 길이, 클래스 크기 |
| **code-smell** | 매직 넘버, 긴 파라미터 목록, 중복 코드 |
| **potential-bugs** | null 처리, 예외 처리, 리소스 누수 |
| **performance** | 불필요한 객체 생성, 비효율적 컬렉션 사용 |
| **style** | 네이밍 컨벤션, 주석 규칙 |

## 사용 예시

### 전체 검사 실행

```bash
# ktlint 검사
bash .claude/skills/code-quality/scripts/run_ktlint.sh

# detekt 검사
bash .claude/skills/code-quality/scripts/run_detekt.sh

# 통합 리포트 생성
python3 .claude/skills/code-quality/scripts/quality_report.py
```

### 특정 모듈만 검사

```bash
bash .claude/skills/code-quality/scripts/run_ktlint.sh nextup-core
bash .claude/skills/code-quality/scripts/run_detekt.sh nextup-api
```

## 출력 형식

### quality-report.json

```json
{
  "timestamp": "2025-01-21T10:00:00Z",
  "summary": {
    "ktlint": {
      "total_issues": 5,
      "errors": 2,
      "warnings": 3
    },
    "detekt": {
      "total_issues": 8,
      "complexity": 2,
      "code_smell": 4,
      "potential_bugs": 1,
      "style": 1
    }
  },
  "pass": false,
  "issues": [...]
}
```

## Gradle 설정 요구사항

### build.gradle.kts (루트)

```kotlin
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    ktlint {
        version.set("1.1.1")
        android.set(false)
        outputToConsole.set(true)
        reporters {
            reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.JSON)
        }
    }

    detekt {
        config.setFrom(files("${rootProject.projectDir}/config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
        allRules = false
    }
}
```

## VETO 기준 (reviewer 참조용)

| 위반 유형 | VETO 여부 |
|-----------|-----------|
| ktlint 에러 (error) | **REJECT** |
| ktlint 경고 (warning) 10개 이상 | **REJECT** |
| detekt complexity 위반 | **REJECT** |
| detekt potential-bugs | **REJECT** |
| detekt code-smell 5개 이상 | **REJECT** |

## 호출 에이전트

- `reviewer`: 검수 프로세스 중 코드 품질 확인
- `github-manager`: 커밋/PR 전 품질 게이트
- `modeler`, `logic-broker`, `api-specialist`: 개발 중 자가 검증
