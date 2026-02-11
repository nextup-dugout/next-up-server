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

### 1. Build Verification
```bash
./gradlew clean build
```

### 2. Code Quality
```bash
# ktlint (active)
./gradlew ktlintCheck

# detekt (현재 비활성화 - Kotlin 2.1.x 미지원)
# ./gradlew detekt
```

### 3. Coverage Check
```bash
./gradlew jacocoTestReport
# Target: 80%+
```

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
| 커버리지 80% 미달 | HIGH |
| detekt bugs 발견 | HIGH |
| ktlint 에러 | MEDIUM |

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
- [x] detekt bugs: 0 issues

### 보안
- [x] Zero Entity Leak: PASS
- [x] 시크릿 노출: PASS

### 컨벤션
- [x] ApiResponse: PASS
- [x] CustomException: PASS
- [x] 의존성 방향: PASS
```

