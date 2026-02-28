---
name: verify-url-prefix
description: 모듈별 URL 프리픽스 통일을 검증합니다. Controller 추가/수정 후 사용.
---

# URL 프리픽스 통일 검증

## Purpose

1. `nextup-api` 모듈의 모든 Controller가 `/api/v1/` 프리픽스를 사용하는지 검증
2. `nextup-backoffice` 모듈의 모든 Controller가 `/api/backoffice/` 프리픽스를 사용하는지 검증
3. `nextup-scorer` 모듈의 모든 Controller가 `/api/scorer/` 프리픽스를 사용하는지 검증
4. 모듈 간 URL 프리픽스 규칙의 일관성 유지

## When to Run

- 새로운 Controller 추가 후
- `@RequestMapping` 경로 변경 후
- PR 전 통합 검증 시

## Related Files

| File | Purpose |
|------|---------|
| `nextup-api/src/main/kotlin/com/nextup/api/controller/**/*.kt` | API 컨트롤러 |
| `nextup-backoffice/src/main/kotlin/com/nextup/backoffice/controller/**/*.kt` | 백오피스 컨트롤러 |
| `nextup-scorer/src/main/kotlin/com/nextup/scorer/controller/**/*.kt` | 스코어러 컨트롤러 |

## Workflow

### Step 1: nextup-api URL 프리픽스 검사

API 모듈의 모든 `@RequestMapping`이 `/api/v1/`로 시작하는지 검사합니다.

**도구:** Grep

**패턴:**
```
@RequestMapping\("(?!/api/v1)
```

**대상:** `nextup-api/src/main/kotlin/**/controller/**/*.kt`

**PASS 기준:** 매칭 결과 0건 (모든 API가 `/api/v1/`로 시작)
**FAIL 기준:** `/api/v1/`로 시작하지 않는 `@RequestMapping` 발견 (🟠 REJECT)

**수정 방법:**
```kotlin
// FAIL
@RequestMapping("/teams")

// PASS
@RequestMapping("/api/v1/teams")
```

### Step 2: nextup-backoffice URL 프리픽스 검사

백오피스 모듈의 모든 `@RequestMapping`이 `/api/backoffice/`로 시작하는지 검사합니다.

**도구:** Grep

**패턴:**
```
@RequestMapping\("(?!/api/backoffice)
```

**대상:** `nextup-backoffice/src/main/kotlin/**/controller/**/*.kt`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** `/api/backoffice/`로 시작하지 않는 `@RequestMapping` 발견 (🟠 REJECT)

### Step 3: nextup-scorer URL 프리픽스 검사

스코어러 모듈의 모든 `@RequestMapping`이 `/api/scorer/`로 시작하는지 검사합니다.

**도구:** Grep

**패턴:**
```
@RequestMapping\("(?!/api/scorer)
```

**대상:** `nextup-scorer/src/main/kotlin/**/controller/**/*.kt`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** `/api/scorer/`로 시작하지 않는 `@RequestMapping` 발견 (🟠 REJECT)

### Step 4: 개별 메서드 레벨 경로 검사

`@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping`에 전체 경로가 지정된 경우 프리픽스 규칙을 위반하는지 검사합니다.

**도구:** Grep

**패턴:**
```
@(Get|Post|Put|Delete|Patch)Mapping\("/(api|backoffice|scorer)
```

**대상:** `*/src/main/kotlin/**/controller/**/*.kt`

**PASS 기준:** 메서드 레벨에서 절대 경로를 사용하지 않음 (클래스 레벨 `@RequestMapping`에 위임)
**FAIL 기준:** 메서드 레벨에서 절대 경로 사용 시 프리픽스 불일치 가능

## Output Format

```markdown
| # | 모듈 | 파일 | 현재 프리픽스 | 기대 프리픽스 | 심각도 |
|---|------|------|--------------|--------------|--------|
| - | 위반 없음 | - | - | - | ✅ PASS |
```

## Exceptions

1. **HealthController** — 헬스체크 경로(`/health`, `/actuator/health`)는 프리픽스 규칙에서 면제
2. **WebSocket 엔드포인트** — WebSocket 핸드쉐이크 경로(`/ws/`)는 별도 규칙을 따를 수 있음
3. **Actuator 엔드포인트** — Spring Actuator(`/actuator/`)는 프레임워크 관리 경로이므로 면제
4. **OAuth 콜백** — 외부 서비스의 콜백 URL은 해당 서비스가 요구하는 경로를 따라야 할 수 있음
