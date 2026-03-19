# Authorization Rules (인가 규칙)

## 절대 규칙: mutating 엔드포인트에 @PreAuthorize 필수

Controller에 POST/PUT/PATCH/DELETE 엔드포인트를 작성하거나 수정할 때:

1. **반드시 `@PreAuthorize`를 추가하라** — 없으면 IDOR 취약점이다
2. **identity 파라미터는 `@AuthenticationPrincipal`에서 도출하라** — `@RequestParam`이나 `@RequestBody`에서 teamId/playerId/scorerId/userId/appealerId를 받으면 안 된다
3. **잠금 소유자 검증을 추가하라** — scorer 엔드포인트에서 경기 잠금 소유자가 맞는지 확인

## 위반 예시 (이렇게 작성하면 안 됨)

```kotlin
// ❌ REJECT: @PreAuthorize 없음
@DeleteMapping("/{id}")
fun cancelBooking(@PathVariable id: Long): ApiResponse<BookingResponse>

// ❌ REJECT: scorerId를 클라이언트 입력으로 받음
@PostMapping("/{gameId}/lock")
fun lockGame(@PathVariable gameId: Long, @RequestParam scorerId: Long)

// ❌ REJECT: teamId를 RequestBody에서 받음
@PostMapping
fun createRequest(@RequestBody request: CreateRequest) // request.teamId를 검증 없이 사용
```

## 올바른 예시 (반드시 이렇게 작성)

```kotlin
// ✅ PASS: @PreAuthorize + @AuthenticationPrincipal
@PreAuthorize("@teamSecurity.isOwnerOrManager(#request.teamId, authentication.principal)")
@DeleteMapping("/{id}")
fun cancelBooking(
    @PathVariable id: Long,
    @AuthenticationPrincipal userId: Long,
): ApiResponse<BookingResponse>

// ✅ PASS: scorerId를 principal에서 도출
@PostMapping("/{gameId}/lock")
fun lockGame(
    @PathVariable gameId: Long,
    @AuthenticationPrincipal scorerId: Long,
)
```

## 예외

- GET 엔드포인트 (공개 조회 API)는 면제
- `/auth/login`, `/auth/register` 등 인증 자체를 수행하는 엔드포인트는 면제
- 클래스 레벨 @PreAuthorize가 적용된 Controller는 개별 메서드에 없어도 허용
