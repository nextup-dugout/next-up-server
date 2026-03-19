---
name: quality-metrics
description: |
  Gradle 빌드, JUnit 테스트, Jacoco 커버리지(80%+), ktlint 1.5.0 정적분석 통합 품질 관리.
  코드 품질 검증 및 CI/CD 파이프라인 연동을 담당한다.
user-invocable: false
allowed-tools: Read, Bash, Glob, Grep
---

# Quality Metrics - Build, Test & Code Analysis

> Gradle 빌드, JUnit 테스트, Jacoco 커버리지, ktlint 통합 품질 관리

## 개요

이 스킬은 코드 품질 검증을 위한 빌드, 테스트, 정적 분석, 커버리지 측정을 통합 수행합니다.

> **detekt**: Kotlin 2.1.x 미지원으로 전체 비활성화 (`enabled = false`). CI에서 `./gradlew detekt || true`로 참고용 실행만.

## 품질 기준

| 항목 | 기준 | 도구 |
|------|------|------|
| 빌드 | 성공 필수 | `./gradlew build` |
| 테스트 | 전체 통과 필수 | JUnit 5 |
| 커버리지 | 80% 이상 (로컬) / 85% (PR) | Jacoco + Codecov |
| 코드 스타일 | 위반 0건 | ktlint 1.5.0 |

## 빌드 & 테스트

> **주의**: Gradle 빌드는 동시 실행 금지 — 파일 시스템 충돌 발생.

### 전체 빌드 실행
```bash
./gradlew clean build --no-daemon --max-workers=2
```

### 테스트만 실행
```bash
./gradlew test --no-daemon --max-workers=2
```

### 특정 모듈 테스트
```bash
./gradlew :nextup-core:test --no-daemon --max-workers=2
./gradlew :nextup-infrastructure:test --no-daemon --max-workers=2
./gradlew :nextup-api:test --no-daemon --max-workers=2
```

### 경쟁 프로세스 정리
```bash
pkill -9 -f "GradleDaemon"
```

## Jacoco 커버리지

### 커버리지 리포트 생성
```bash
./gradlew test jacocoTestReport --no-daemon --max-workers=2
```

### 통합 리포트 (전체 모듈)
```bash
./gradlew jacocoRootReport --no-daemon --max-workers=2
```

### 리포트 위치
```
build/reports/jacoco/test/html/index.html       # HTML 리포트
build/reports/jacoco/test/jacocoTestReport.xml   # Codecov용 XML
```

### 커버리지 제외 대상
- `**/config/**`
- `**/dto/**`
- `**/exception/**`
- `**/mapper/**`
- `**/HealthController*`
- `**/domain/event/**`

### 커버리지 다층 기준

| 기준 | 타겟 | 용도 |
|------|------|------|
| **Jacoco minimum** | **80%** | 로컬 빌드 최소 기준 (`build.gradle.kts`) |
| **Codecov project/patch** | **85%** | CI/CD PR 머지 기준 (`codecov.yml`) |
| **Reviewer VETO** | **85%** | Reviewer 에이전트 자동 REJECT 기준 |

> Jacoco 80%는 개발 중 최소 기준이며, PR 머지 시에는 Codecov 85%를 통과해야 합니다.

## ktlint (코드 스타일)

### 검사 실행
```bash
./gradlew ktlintCheck
```

### 자동 수정
```bash
./gradlew ktlintFormat
```

### 주요 규칙
- 들여쓰기: 4 spaces
- 최대 줄 길이: 120자
- import 정렬: 알파벳순
- trailing comma 비활성화 (`.editorconfig`에서 disabled)
- multiline 표현식: 반드시 새 줄에서 시작
- body expression: 간단한 경우 같은 줄 허용

## 통합 품질 검사 명령어

### 전체 품질 검사 (권장)
```bash
./gradlew clean build ktlintCheck jacocoTestReport --no-daemon --max-workers=2
```

### 빠른 검사 (테스트 제외)
```bash
./gradlew ktlintCheck --no-build-cache
```

## 품질 검사 체크리스트

### PR 제출 전 필수 확인
- [ ] `./gradlew build --no-daemon --max-workers=2` 성공
- [ ] 모든 테스트 통과
- [ ] `./gradlew jacocoTestReport` — 커버리지 80% 이상 (Codecov 85%)
- [ ] `./gradlew ktlintCheck` — 위반 0건

### 실패 시 대응

| 실패 유형 | 대응 |
|----------|------|
| 빌드 실패 | 컴파일 에러 수정 |
| 테스트 실패 | 테스트 또는 구현 수정 |
| 커버리지 미달 | 테스트 추가 |
| ktlint 위반 | `./gradlew ktlintFormat` 실행 |

## Gradle 설정 참조

### Jacoco 설정 (build.gradle.kts)
```kotlin
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
```

### ktlint 설정
```kotlin
ktlint {
    version = "1.5.0"
    android = false
    ignoreFailures = false
}
```

## Agent 협업

이 Skill을 활용하는 Agent:
- **reviewer**: 빌드/테스트/커버리지 검증 후 VETO 판단
- **implementer**: 구현 완료 후 품질 검사 실행
- **devops**: CI/CD 파이프라인에서 자동 실행
