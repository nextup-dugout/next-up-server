# /review - Code Quality & Security Review

코드 품질 및 보안 검증을 수행합니다.

## Review Process

### 1. Build Verification
```bash
./gradlew clean build
```

### 2. Code Quality
```bash
./gradlew ktlintCheck detekt
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
| 빌드 실패 | 🔴 CRITICAL |
| 테스트 실패 | 🔴 CRITICAL |
| 의존성 규칙 위반 | 🔴 CRITICAL |
| 보안 취약점 (CRITICAL/HIGH) | 🔴 CRITICAL |
| Zero Entity Leak 위반 | 🔴 CRITICAL |
| 커버리지 80% 미달 | 🟠 HIGH |
| detekt bugs 발견 | 🟠 HIGH |
| ktlint 에러 | 🟡 MEDIUM |

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

## 사용 예시

```
User: /review
Assistant:
코드 리뷰를 시작합니다...

1. 빌드 검증 중...
   ✅ BUILD SUCCESSFUL

2. 코드 품질 검사 중...
   ✅ ktlint: 0 errors
   ✅ detekt: 0 bugs

3. 커버리지 확인 중...
   ✅ Coverage: 82% (≥80%)

4. 보안 검사 중...
   ✅ Zero Entity Leak: PASS
   ✅ 시크릿 노출: PASS

5. 컨벤션 확인 중...
   ✅ ApiResponse: PASS
   ✅ CustomException: PASS

## 판정: ✅ APPROVED

PR 생성 가능합니다. /pr 명령어를 사용하세요.
```

## 활용 Skills

- `quality-metrics`: 빌드/테스트/정적분석
- `security-audit`: 보안 검사
- `domain-baseball`: 야구 규칙 검증

## 활용 Agents

- `reviewer`: 최종 검수 및 VETO 판정
