---
name: verify-authorization
description: Controller에 POST/PUT/PATCH/DELETE 엔드포인트를 작성하거나 수정할 때 반드시 이 스킬을 참조하라. @PreAuthorize 없는 mutating 엔드포인트는 IDOR 취약점이다. teamId/playerId/scorerId를 @RequestParam이나 @RequestBody로 받는 코드를 작성하려 할 때 즉시 트리거하라. Controller 코드를 생성, 수정, 리뷰할 때 항상 사용.
---

# 인가(Authorization) 검증

## Purpose

1. mutating 엔드포인트(POST/PUT/PATCH/DELETE)에 `@PreAuthorize`가 적용되었는지 검증
2. identity-bearing 파라미터(teamId, playerId, scorerId 등)가 `@AuthenticationPrincipal`에서 도출되는지 검증
3. 클라이언트 입력을 서버 측 검증 없이 신뢰하는 IDOR 패턴을 탐지

## When to Run

- 새로운 Controller 또는 mutating 엔드포인트 추가 후
- 인가/인증 관련 코드 변경 후
- `@PreAuthorize` 또는 `@AuthenticationPrincipal` 관련 수정 후
- PR 전 통합 검증 시

## Related Files

| File | Purpose |
|------|---------|
| `nextup-api/src/main/kotlin/com/nextup/api/controller/**/*.kt` | API 컨트롤러 |
| `nextup-backoffice/src/main/kotlin/com/nextup/backoffice/controller/**/*.kt` | 백오피스 컨트롤러 |
| `nextup-scorer/src/main/kotlin/com/nextup/scorer/controller/**/*.kt` | 스코어러 컨트롤러 |
| `nextup-infrastructure/src/main/kotlin/com/nextup/infrastructure/security/**/*.kt` | 보안 설정 |

## Workflow

### Step 1: mutating 엔드포인트 중 @PreAuthorize 미적용 탐지

모든 Controller에서 `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping`이 있는 함수 중 `@PreAuthorize`가 없는 것을 탐지합니다.

**도구:** Grep (multiline)

**패턴:**
```
@(PostMapping|PutMapping|PatchMapping|DeleteMapping)
```

**대상:** `**/controller/**/*.kt`

각 매칭에 대해 해당 함수 위에 `@PreAuthorize`가 있는지 확인합니다. 없으면 FAIL.

**PASS 기준:** 모든 mutating 엔드포인트에 `@PreAuthorize` 적용
**FAIL 기준:** `@PreAuthorize` 없는 mutating 엔드포인트 존재

**수정 방법:**
```kotlin
// FAIL
@PostMapping
fun createRequest(@RequestBody request: CreateRequest): ApiResponse<Response> { ... }

// PASS
@PreAuthorize("@teamSecurity.isOwnerOrManager(#request.teamId, authentication.principal)")
@PostMapping
fun createRequest(
    @AuthenticationPrincipal userId: Long,
    @RequestBody request: CreateRequest,
): ApiResponse<Response> { ... }
```

### Step 2: identity 파라미터가 클라이언트 입력으로 전달되는 IDOR 패턴 탐지

Controller 메서드에서 `teamId`, `playerId`, `scorerId`, `userId`, `appealerId` 등 identity 파라미터를 `@RequestParam` 또는 `@RequestBody`에서 직접 받아 Service에 전달하는 패턴을 탐지합니다.

**도구:** Grep

**패턴:**
```
@RequestParam.*(teamId|playerId|scorerId|userId|appealerId|sellerId|buyerId)
```

**대상:** `**/controller/**/*.kt`

**PASS 기준:** identity 파라미터가 `@AuthenticationPrincipal`에서 도출
**FAIL 기준:** identity 파라미터를 `@RequestParam`/`@RequestBody`에서 직접 수신

**수정 방법:**
```kotlin
// FAIL — 클라이언트 입력 신뢰
@PostMapping("/{gameId}/lock")
fun lockGame(@PathVariable gameId: Long, @RequestParam scorerId: Long): ApiResponse<GameResponse>

// PASS — 서버에서 도출
@PostMapping("/{gameId}/lock")
fun lockGame(@PathVariable gameId: Long, @AuthenticationPrincipal scorerId: Long): ApiResponse<GameResponse>
```

### Step 3: 클래스 레벨 @PreAuthorize 확인

Controller 클래스에 `@PreAuthorize`가 있으면 해당 클래스의 모든 메서드에 적용됩니다. 이 경우 개별 메서드에 `@PreAuthorize`가 없어도 PASS로 처리합니다.

**도구:** Grep (multiline)

**패턴:**
```
@PreAuthorize.*\n.*class.*Controller
```

**대상:** `**/controller/**/*.kt`

클래스 레벨 `@PreAuthorize`가 있는 Controller는 Step 1의 FAIL 목록에서 제외합니다.

## Output Format

```markdown
| # | 파일 | 라인 | 문제 | 심각도 |
|---|------|------|------|--------|
| 1 | `MercenaryRequestController.kt:29` | @PreAuthorize 없는 POST | 🔴 CRITICAL (IDOR) |
| 2 | `GameScorerController.kt:136` | scorerId를 @RequestParam으로 수신 | 🔴 CRITICAL (IDOR) |
```

## Exceptions

1. **GET 엔드포인트** — `@GetMapping`은 읽기 전용이므로 `@PreAuthorize` 필수가 아님 (공개 조회 API)
2. **인증 관련 엔드포인트** — `/auth/login`, `/auth/register`, `/auth/refresh` 등 인증 자체를 수행하는 엔드포인트는 면제
3. **Swagger/Actuator 엔드포인트** — 시스템 관리용 엔드포인트는 SecurityConfig에서 별도 관리
4. **클래스 레벨 @PreAuthorize 적용 Controller** — 개별 메서드에 없어도 클래스 레벨에서 적용되면 PASS
5. **테스트 코드** — `src/test/` 내 파일은 면제
