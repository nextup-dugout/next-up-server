---
name: review
description: |
  코드 품질 및 보안 검증 수행. 빌드, 정적분석(ktlint), 커버리지(80%+), 보안 검사, 컨벤션 확인을 포함한다.
  VETO 조건 해당 시 자동 REJECT 판정을 내린다.
user-invocable: true
argument-hint: "[scope] e.g. all, security, coverage"
allowed-tools: Bash, Read, Glob, Grep
---

# /review - Code Quality & Security Review

코드 품질 및 보안 검증을 수행합니다.

## Arguments

`$ARGUMENTS`를 통해 특정 모듈만 검증할 수 있습니다.

| 사용법 | 설명 |
|--------|------|
| `/review` | 전체 프로젝트 검증 |
| `/review core` | Core 모듈만 검증 |
| `/review api` | API 모듈만 검증 |
| `/review backoffice` | Backoffice 모듈만 검증 |

## Review Process

> **주의**: Gradle 빌드는 동시 실행 금지. 항상 `--no-daemon --max-workers=2` 플래그 사용.

### 1. Build Verification
```bash
./gradlew clean build --no-daemon --max-workers=2
```

### 2. Code Quality
```bash
# ktlint 검사
./gradlew ktlintCheck

# 위반 시 자동 수정 후 재검사
./gradlew ktlintFormat
```

> detekt는 Kotlin 2.1.x 미지원으로 비활성화 상태 (`enabled = false`). CI에서 참고용으로만 실행.

### 3. Coverage Check
```bash
./gradlew jacocoTestReport --no-daemon --max-workers=2
# Jacoco: 80%+ (로컬), Codecov: 85%+ (PR 머지)
```

커버리지 제외 대상: `config/**`, `dto/**`, `exception/**`, `mapper/**`, `HealthController`, `domain/event/**`

### 4. Security Scan
- Zero Entity Leak 검증
- 시크릿 노출 검사
- OWASP Top 10 체크

### 5. Convention Check
- CLAUDE.md 의존성 규칙
- ApiResponse 사용 여부
- CustomException 사용 여부
- 커밋 메시지 형식

## VETO Conditions (자동 REJECT)

| 조건 | 심각도 |
|------|--------|
| 빌드 실패 | CRITICAL |
| 테스트 실패 | CRITICAL |
| 의존성 규칙 위반 | CRITICAL |
| 보안 취약점 (CRITICAL/HIGH) | CRITICAL |
| Zero Entity Leak 위반 | CRITICAL |
| 커버리지 미달 (Jacoco 80% 또는 Codecov 85%) | HIGH |
| ktlint 에러 | MEDIUM |
| detekt bugs 발견 | SKIP (비활성화) |

## Review Report Template

```markdown
# Review Report

## 판정: APPROVED / REJECTED

### 빌드
- [x] ./gradlew build: PASS
- [x] 테스트: 156/156 passed (100%)
- [x] 커버리지: 82%

### 코드 품질
- [x] ktlint: 0 errors

### 보안
- [x] Zero Entity Leak: PASS
- [x] 시크릿 노출: PASS

### 컨벤션
- [x] ApiResponse: PASS
- [x] CustomException: PASS
- [x] 의존성 방향: PASS
```
