---
name: verify-custom-exception
description: 예외를 throw하는 코드를 작성할 때 반드시 이 스킬을 참조하라. IllegalArgumentException, IllegalStateException 등 표준 Java 예외를 사용하면 안 되며, 반드시 BusinessException 계열 커스텀 예외를 사용해야 한다. throw 키워드를 작성하거나, catch 블록을 수정하거나, 새로운 예외 클래스를 만들 때 즉시 트리거하라.
---

# CustomException 사용 검증

## Purpose

1. `IllegalStateException`, `IllegalArgumentException` 등 표준 Java 예외가 프로덕션 코드에서 throw되지 않는지 검증
2. 모든 비즈니스 예외가 `BusinessException` 계열을 사용하는지 확인
3. 적절한 예외 코드와 메시지가 포함되어 있는지 검증

## When to Run

- 예외 처리 로직 추가/수정 후
- 새로운 Service 또는 Controller 구현 후
- PR 전 통합 검증 시

## Related Files

| File | Purpose |
|------|---------|
| `nextup-common/src/main/kotlin/com/nextup/common/exception/BusinessException.kt` | 기본 예외 계층 |
| `nextup-common/src/main/kotlin/com/nextup/common/exception/*.kt` | 도메인별 커스텀 예외 |
| `nextup-api/src/main/kotlin/**/*.kt` | API 모듈 소스 |
| `nextup-backoffice/src/main/kotlin/**/*.kt` | 백오피스 모듈 소스 |
| `nextup-scorer/src/main/kotlin/**/*.kt` | 스코어러 모듈 소스 |
| `nextup-infrastructure/src/main/kotlin/**/*.kt` | Infra 모듈 소스 |
| `nextup-core/src/main/kotlin/**/*.kt` | Core 모듈 소스 |

## Workflow

### Step 1: 표준 Java 예외 직접 throw 탐지

프로덕션 코드에서 표준 Java 예외를 직접 throw하는 곳을 찾습니다.

**도구:** Grep

**패턴:**
```
throw (IllegalStateException|IllegalArgumentException|RuntimeException|NullPointerException|UnsupportedOperationException|IndexOutOfBoundsException)\(
```

**대상:** `*/src/main/kotlin/**/*.kt`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** 1건 이상 매칭 시 커스텀 예외 미사용 (🟠 REJECT)

**수정 방법:**
```kotlin
// FAIL
throw IllegalStateException("You are not a member of this team")

// PASS - 적절한 커스텀 예외 사용
throw TeamMemberNotFoundException(teamId)
// 또는
throw InvalidStateException("TEAM_MEMBER_001", "You are not a member of this team")
```

### Step 2: 예외 계층 구조 확인

사용 가능한 커스텀 예외 계층을 확인합니다.

**도구:** Grep

**패턴:**
```
^(open )?class [A-Za-z]*Exception
```

**대상:** `nextup-common/src/main/kotlin/com/nextup/common/exception/**/*.kt`

기본 계층:
- `BusinessException(code, message)` — 기본
- `NotFoundException(code, message)` — 404
- `InvalidStateException(code, message)` — 잘못된 상태
- `InvalidInputException(code, message)` — 잘못된 입력
- `ForbiddenException(code, message)` — 권한 부족

### Step 3: require/check 함수 사용 탐지

Kotlin의 `require()`, `check()` 함수도 내부적으로 `IllegalArgumentException`, `IllegalStateException`을 throw합니다.

**도구:** Grep

**패턴:**
```
\b(require|check)\s*\(
```

**대상:** `*/src/main/kotlin/**/*.kt` (테스트 제외)

**PASS 기준:** 프로덕션 코드에서 사용하지 않거나, init 블록의 불변조건 검사에만 사용
**FAIL 기준:** Service/Controller에서 비즈니스 로직 검증에 사용 (커스텀 예외로 교체 권장)

**참고:** Entity init 블록에서 `require()`를 사용하는 것은 허용 (도메인 불변조건 보호)

## Output Format

```markdown
| # | 파일 | 라인 | 예외 타입 | 권장 대체 | 심각도 |
|---|------|------|-----------|-----------|--------|
| 1 | `Controller.kt:120` | IllegalStateException | TeamMemberNotFoundException | 🟠 REJECT |
```

## Exceptions

1. **Entity/data class init 블록의 require/check** — 도메인 불변조건을 보호하기 위한 `require()`, `check()`는 허용 (Entity 생성 시 빠른 실패, `PageCommand` 등 Value Object/data class도 포함)
2. **테스트 코드** — `src/test/` 내 파일은 테스트 편의를 위해 표준 예외를 사용할 수 있음
3. **프레임워크 어댑터 코드** — Spring Security 등 프레임워크가 요구하는 특정 예외 타입은 허용
4. **companion object의 팩토리 메서드** — Entity의 `create()` 등에서 입력 검증에 `require()`를 사용하는 것은 허용
5. **Core common 유틸 클래스** — `PageCommand`, `PageResult` 등 Core 공통 타입의 init 블록에서 `require()`를 사용하는 것은 허용 (입력 범위 검증)
