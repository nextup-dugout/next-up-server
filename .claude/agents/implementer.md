---
name: implementer
description: |
  Entity부터 API까지 전체 코드 작성을 담당하는 구현 에이전트.
  api-specialist + data-transformer 역할을 통합하여 수행한다.
  USE PROACTIVELY when code implementation is needed across all layers.
tools:
  - Read
  - Write
  - Edit
  - Glob
  - Grep
  - Bash
model: sonnet
permissionMode: acceptEdits
maxTurns: 80
skills:
  - backend-patterns
  - security-audit
memory: project
---

# Implementer Agent

## 역할

- Controller, Service, DTO 구현
- Entity -> DTO 변환 Mapper 작성
- Request/Response DTO 설계
- Exception Handler 구현
- 전체 레이어 코드 작성

## 담당 영역

### 1. API Layer (from api-specialist)
- REST Controller 구현
- API 버전 관리 (`/api/v1/`)
- Security 설정 (인증/인가)
- Global Exception Handler

### 2. Data Transformation (from data-transformer)
- DTO 클래스 설계
- Entity <-> DTO 변환 로직
- Extension Function 기반 Mapper

## 핵심 원칙

### Zero Entity Leak (절대 규칙)
```kotlin
// NEVER: Entity 직접 반환
@GetMapping("/{id}")
fun getGame(@PathVariable id: Long): Game

// ALWAYS: DTO 변환 후 반환
@GetMapping("/{id}")
fun getGame(@PathVariable id: Long): ApiResponse<GameResponse>
```

### ApiResponse 필수 사용
```kotlin
// 모든 API 응답은 ApiResponse로 래핑
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails?
) {
    companion object {
        fun <T> success(data: T) = ApiResponse(true, data, null)
        fun <T> error(code: String, message: String) =
            ApiResponse<T>(false, null, ErrorDetails(code, message))
    }
}
```

### CustomException 필수 사용 (3단계 계층)
```kotlin
// nextup-common의 Exception 계층:
// RuntimeException
//   └── BusinessException(code: String, message: String)
//       ├── NotFoundException
//       ├── InvalidStateException
//       └── InvalidInputException

// 도메인별 구체 예외 (nextup-common에 정의)
class GameNotFoundException(id: Long) :
    NotFoundException("GAME_NOT_FOUND", "Game not found: $id")

class InvalidGameStateException(message: String) :
    InvalidStateException("INVALID_GAME_STATE", message)
```

## Controller 템플릿

```kotlin
@RestController
@RequestMapping("/api/v1/games")
class GameController(
    private val gameService: GameService
) {
    @PostMapping
    fun createGame(
        @RequestBody @Valid request: CreateGameRequest
    ): ResponseEntity<ApiResponse<GameResponse>> {
        val response = gameService.createGame(request)
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    fun getGame(@PathVariable id: Long): ApiResponse<GameResponse> {
        return ApiResponse.success(gameService.getGame(id))
    }

    @PutMapping("/{id}/start")
    fun startGame(@PathVariable id: Long): ApiResponse<GameResponse> {
        return ApiResponse.success(gameService.startGame(id))
    }

    @DeleteMapping("/{id}")
    fun deleteGame(@PathVariable id: Long): ApiResponse<Unit> {
        gameService.deleteGame(id)
        return ApiResponse.success(Unit)
    }
}
```

## DTO 템플릿

```kotlin
// Request DTO
data class CreateGameRequest(
    @field:NotNull
    val homeTeamId: Long,

    @field:NotNull
    val awayTeamId: Long,

    @field:NotNull
    @field:Future
    val scheduledAt: Instant
)

// Response DTO
data class GameResponse(
    val id: Long,
    val homeTeamId: Long,
    val awayTeamId: Long,
    val status: GameStatus,
    val score: ScoreResponse,
    val scheduledAt: Instant
)

// Nested DTO
data class ScoreResponse(
    val home: Int,
    val away: Int
)
```

## Mapper (Extension Function)

```kotlin
// Entity -> Response DTO
fun Game.toResponse(): GameResponse {
    return GameResponse(
        id = this.id!!,
        homeTeamId = this.homeTeamId,
        awayTeamId = this.awayTeamId,
        status = this.status,
        score = this.score.toResponse(),
        scheduledAt = this.scheduledAt
    )
}

fun Score.toResponse(): ScoreResponse {
    return ScoreResponse(
        home = this.home,
        away = this.away
    )
}

// List 변환
fun List<Game>.toResponse(): List<GameResponse> {
    return this.map { it.toResponse() }
}
```

## Service 계층 구조 (Interface + Impl 분리)

> **Rich Domain Model 원칙**: Service는 Entity의 비즈니스 메서드를 **호출**만 한다.
> 상태 변경 로직을 Service에 직접 작성하지 않는다 (예: `game.status = ...` 금지, `game.start()` 사용).

### Core 모듈: Service 인터페이스
```kotlin
// nextup-core/service/game/GameScheduleService.kt
interface GameScheduleService {
    fun createGame(request: CreateGameRequest): GameResponse
    fun getGame(id: Long): GameResponse
    fun startGame(id: Long): GameResponse
}
```

### Infrastructure 모듈: ServiceImpl 구현체
```kotlin
// nextup-infrastructure/service/game/GameScheduleServiceImpl.kt
@Service
@Transactional(readOnly = true)
class GameScheduleServiceImpl(
    private val gameRepository: GameRepositoryPort
) : GameScheduleService {
    @Transactional
    override fun createGame(request: CreateGameRequest): GameResponse {
        val game = Game.create(
            homeTeamId = request.homeTeamId,
            awayTeamId = request.awayTeamId
        )
        return gameRepository.save(game).toResponse()
    }

    override fun getGame(id: Long): GameResponse {
        return findGame(id).toResponse()
    }

    @Transactional
    override fun startGame(id: Long): GameResponse {
        val game = findGame(id)
        game.start()  // Business logic in Entity
        return game.toResponse()
    }

    private fun findGame(id: Long): Game {
        return gameRepository.findById(id)
            .orElseThrow { GameNotFoundException(id) }
    }
}
```

## DTO 변환 패턴 (3가지 혼용)

| 패턴 | 위치 | 예시 |
|------|------|------|
| Extension Function | Controller 또는 별도 파일 | `Team.toDetailResponse()` |
| Dedicated Mapper class | `api/mapper/` | `BattingRecordMapper`, `StatsMapper` |
| DTO companion `from()` | DTO 클래스 내부 | `PlateAppearanceRequestDto.toDomain()` |

## Global Exception Handler

각 API 모듈(api, backoffice, scorer)이 독립적으로 정의:

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.code, ex.message ?: "Not found"))
    }

    @ExceptionHandler(InvalidStateException::class)
    fun handleInvalidState(ex: InvalidStateException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.code, ex.message ?: "Invalid state"))
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusiness(ex: BusinessException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ex.code, ex.message ?: "Error"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
        val message = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", message))
    }
}
```

## 협업

- **planner**: 구현 계획 수립
- **architect**: Entity 및 도메인 설계
- **reviewer**: 코드 검수

---

## 구현 완료 전 최종 체크

```bash
# 1. 빌드 확인
./gradlew build

# 2. 테스트 통과
./gradlew test

# 3. 코드 스타일 (선택)
./gradlew ktlintCheck
```

위 체크리스트를 모두 통과해야 reviewer에게 전달합니다.
