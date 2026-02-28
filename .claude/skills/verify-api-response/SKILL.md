---
name: verify-api-response
description: 모든 Controller가 ApiResponse로 감싸서 반환하는지 검증합니다. Controller 추가/수정 후 사용.
---

# ApiResponse 사용 검증

## Purpose

1. 모든 Controller가 `ApiResponse` 래퍼를 사용하여 응답을 반환하는지 검증
2. 일관된 API 응답 형식이 유지되는지 확인
3. 새로 추가된 Endpoint가 규칙을 준수하는지 검증

## When to Run

- 새로운 Controller 또는 Endpoint 추가 후
- Controller 반환 타입 변경 후
- PR 전 통합 검증 시

## Related Files

| File | Purpose |
|------|---------|
| `nextup-common/src/main/kotlin/com/nextup/common/dto/ApiResponse.kt` | ApiResponse 정의 |
| `nextup-api/src/main/kotlin/com/nextup/api/controller/**/*.kt` | API 컨트롤러 |
| `nextup-backoffice/src/main/kotlin/com/nextup/backoffice/controller/**/*.kt` | 백오피스 컨트롤러 |
| `nextup-scorer/src/main/kotlin/com/nextup/scorer/controller/**/*.kt` | 스코어러 컨트롤러 |

## Workflow

### Step 1: ApiResponse 미사용 Controller 탐지

Controller 파일 중 `ApiResponse`를 import하지 않는 파일을 찾습니다.

**도구:** Grep (files_with_matches 모드로 모든 Controller 파일 찾기) + Grep (ApiResponse import가 없는 파일 탐지)

**방법:**
1. 모든 Controller 파일 목록을 Glob으로 수집: `**/controller/**/*Controller*.kt` (`src/main/kotlin` 하위)
2. 각 파일에서 `ApiResponse` 문자열이 존재하는지 확인

**패턴:**
```
ApiResponse
```

**대상:** `**/src/main/kotlin/**/controller/**/*Controller*.kt`

**PASS 기준:** 모든 Controller 파일에 `ApiResponse` 사용 (HealthController 제외)
**FAIL 기준:** `ApiResponse`가 없는 Controller 파일 발견 (🟠 REJECT)

**수정 방법:**
```kotlin
// FAIL
@GetMapping
fun getItems(): List<ItemResponse> = service.getItems()

// PASS
@GetMapping
fun getItems(): ApiResponse<List<ItemResponse>> =
    ApiResponse.success(service.getItems())
```

### Step 2: ResponseEntity 직접 사용 검사

`ApiResponse` 대신 `ResponseEntity`를 직접 반환하는 Controller를 찾습니다.

**도구:** Grep

**패턴:**
```
ResponseEntity<(?!HealthResponse)
```

**대상:** `**/src/main/kotlin/**/controller/**/*Controller*.kt`

**PASS 기준:** `ResponseEntity` 직접 사용 0건 (HealthController 제외)
**FAIL 기준:** `ResponseEntity`를 직접 반환하는 endpoint 발견

**참고:** `ApiResponse`가 내부적으로 `ResponseEntity`를 사용하는 것은 정상

## Output Format

```markdown
| # | 파일 | 문제 | 심각도 |
|---|------|------|--------|
| 1 | `SomeController.kt` | ApiResponse 미사용 | 🟠 REJECT |
```

## Exceptions

1. **HealthController** — 헬스체크 엔드포인트는 `ResponseEntity<HealthResponse>`를 직접 반환해도 허용 (인프라 모니터링용)
2. **WebSocket 핸들러** — WebSocket 메시지 핸들러는 HTTP 응답이 아니므로 ApiResponse 불필요
3. **파일 다운로드 엔드포인트** — 바이너리 응답(파일 다운로드)은 `ResponseEntity<ByteArray>` 등을 사용할 수 있음
4. **SSE (Server-Sent Events)** — `SseEmitter` 반환은 스트리밍이므로 ApiResponse 불필요
