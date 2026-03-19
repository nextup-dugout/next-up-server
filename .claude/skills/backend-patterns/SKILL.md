---
name: backend-patterns
description: |
  Kotlin/Spring Boot 컨벤션 및 패턴 가이드. Entity 설계(Rich Domain Model),
  Controller/Service 패턴, JPA 매핑, DTO 변환 규칙을 제공한다.
user-invocable: false
allowed-tools: Read, Glob, Grep
---

# Backend Patterns - Kotlin/Spring Boot

> Kotlin + Spring Boot + JPA 기반 백엔드 개발 컨벤션 및 패턴

## 기술 스택

- **Language**: Kotlin 2.1.10 (JDK 21)
- **Framework**: Spring Boot 3.4.1
- **ORM**: Spring Data JPA + Hibernate 6
- **Database**: PostgreSQL + PostGIS
- **Build**: Gradle 8.12 (Kotlin DSL)

## Architecture

이 프로젝트는 **Hexagonal Architecture (Ports & Adapters)** 기반입니다.
- **Port** (core): Service 인터페이스, RepositoryPort 인터페이스
- **Adapter** (infrastructure): ServiceImpl, JpaRepository, External API Client
- **Inbound Adapter** (api/backoffice/scorer): Controller

## Kotlin Code Conventions

### Immutability First
```kotlin
// PREFER: val (immutable)
val player = Player.create(name = "홍길동")

// AVOID: var (mutable)
var player = Player.create(name = "홍길동")
player = anotherPlayer  // ❌
```

### Data Class for DTOs
```kotlin
// ✅ APPROVED: Data class for simple DTOs
data class PlayerResponse(
    val id: Long,
    val name: String,
    val position: Position
)

// ❌ AVOID: Regular class for DTOs
class PlayerResponse(
    val id: Long,
    val name: String
)
```

### Null Safety
```kotlin
// ✅ APPROVED: Explicit nullability
fun findPlayer(id: Long): Player? {
    return playerRepository.findById(id).orElse(null)
}

// ✅ APPROVED: Elvis operator
val name = player?.name ?: "Unknown"

// ❌ AVOID: Force unwrap (!!)
val name = player!!.name  // Dangerous
```

### Extension Functions
```kotlin
// ✅ APPROVED: Extension for conversion
fun Player.toResponse(): PlayerResponse {
    return PlayerResponse(
        id = this.id!!,
        name = this.name,
        position = this.position
    )
}
```

## JPA Entity Patterns

### Rich Domain Model
```kotlin
@Entity
@Table(
    name = "games",
    indexes = [
        Index(name = "idx_game_status", columnList = "status"),
        Index(name = "idx_game_scheduled_at", columnList = "scheduled_at")
    ]
)
class Game private constructor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val homeTeamId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GameStatus = GameStatus.SCHEDULED,

    @Version
    var version: Long = 0
) : BaseTimeEntity() {
    companion object {
        fun create(homeTeamId: Long, awayTeamId: Long): Game {
            return Game(homeTeamId = homeTeamId)
        }
    }

    // Business methods (NOT in Service!)
    fun start() {
        require(status == GameStatus.SCHEDULED) { "Game already started" }
        status = GameStatus.IN_PROGRESS
    }

    fun cancel(reason: String) {
        require(status == GameStatus.SCHEDULED) { "Cannot cancel started game" }
        status = GameStatus.CANCELLED
    }
}
```

### BaseTimeEntity (공통 상속)
```kotlin
@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
        protected set
}
```

> **타임스탬프**: `Instant` 타입 사용 (UTC 기준), `LocalDateTime` 사용 금지

### JPA 컬렉션 패턴 (private backing field)
```kotlin
@OneToMany(mappedBy = "game", cascade = [CascadeType.ALL])
private val _gameTeams: MutableList<GameTeam> = mutableListOf()
val gameTeams: List<GameTeam> get() = _gameTeams.toList()

fun addTeam(team: GameTeam) {
    _gameTeams.add(team)
}
```

### Optimistic Locking
```kotlin
@Version
var version: Long = 0
```

### Value Objects
```kotlin
@Embeddable
data class Score(
    @Column(name = "home_score", nullable = false)
    val home: Int = 0,

    @Column(name = "away_score", nullable = false)
    val away: Int = 0
) {
    init {
        require(home >= 0) { "Home score must be non-negative" }
        require(away >= 0) { "Away score must be non-negative" }
    }

    fun isHomeWinning(): Boolean = home > away
}
```

### Enum Types
```kotlin
@Enumerated(EnumType.STRING)  // ✅ Always use STRING
@Column(nullable = false)
var status: GameStatus
```

## Service Layer Patterns (Interface + Impl 분리)

### Core 모듈: Service 인터페이스
```kotlin
// nextup-core/service/game/GameScheduleService.kt
interface GameScheduleService {
    fun createGame(homeTeamId: Long, awayTeamId: Long): Game
    fun getGame(id: Long): Game
}
```

### Infrastructure 모듈: ServiceImpl 구현체
```kotlin
// nextup-infrastructure/service/game/GameScheduleServiceImpl.kt
@Service
@Transactional(readOnly = true)
class GameScheduleServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher
) : GameScheduleService {
    @Transactional  // Write operation
    override fun createGame(homeTeamId: Long, awayTeamId: Long): Game {
        val game = Game.create(
            homeTeamId = homeTeamId,
            awayTeamId = awayTeamId
        )
        val saved = gameRepository.save(game)
        eventPublisher.publishEvent(GameCreatedEvent(saved.id!!))
        return saved
    }

    override fun getGame(id: Long): Game {
        return gameRepository.findById(id)
            .orElseThrow { GameNotFoundException(id) }
    }
}
```

### Custom Exceptions (3단계 계층)
```kotlin
// nextup-common의 Exception 계층:
// RuntimeException
//   └── BusinessException(code: String, message: String)
//       ├── NotFoundException
//       ├── InvalidStateException
//       ├── InvalidInputException
//       └── ForbiddenException

// 도메인별 구체 예외 (nextup-common에 정의)
class GameNotFoundException(id: Long) :
    NotFoundException("GAME_NOT_FOUND", "Game not found: $id")

class InvalidGameStateException(message: String) :
    InvalidStateException("INVALID_GAME_STATE", message)
```

## Controller Patterns

### REST API Structure
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
}
```

### URL 프리픽스 규칙

| 모듈 | 프리픽스 |
|------|----------|
| `nextup-api` | `/api/v1/` |
| `nextup-backoffice` | `/admin/` |
| `nextup-scorer` | `/scorer/` |

### ApiResponse Wrapper (MANDATORY)
```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorDetails?
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data, error = null)
        }

        fun <T> error(code: String, message: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                data = null,
                error = ErrorDetails(code, message)
            )
        }
    }
}
```

### Global Exception Handler (각 API 모듈에 독립 정의)
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(ex: NotFoundException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.code, ex.message ?: "Not found"))
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

## Repository Patterns (2가지 전략 혼용)

### 전략 1: Direct JPA + Port (대부분의 repository)
```kotlin
// Core 모듈: Port 인터페이스
interface GameRepositoryPort {
    fun save(game: Game): Game
    fun findById(id: Long): Optional<Game>
}

// Infrastructure 모듈: JPA가 직접 Port 구현
interface GameRepository : JpaRepository<Game, Long>, GameRepositoryPort
```

### 전략 2: Adapter wrapping (복잡한 쿼리가 필요한 repository)
```kotlin
// Infrastructure 모듈: 내부 JPA Repository
interface BracketEntryJpaRepository : JpaRepository<BracketEntry, Long> {
    fun findByCompetitionId(competitionId: Long): List<BracketEntry>
}

// Adapter가 Port를 구현하며 JPA를 래핑
@Repository
class BracketEntryRepositoryAdapter(
    private val jpaRepository: BracketEntryJpaRepository
) : BracketEntryRepositoryPort {
    override fun findByCompetitionId(competitionId: Long): List<BracketEntry> {
        return jpaRepository.findByCompetitionId(competitionId)
    }
}
```

### 전략 선택 기준
| 조건 | 전략 | 위치 |
|------|------|------|
| 기본 CRUD + 단순 쿼리 메서드 | Direct JPA + Port | `infrastructure/repository/` |
| 복잡한 조합/변환 로직 | Adapter wrapping | `infrastructure/persistence/{domain}/` |

## DTO 변환 패턴

| 패턴 | 위치 | 예시 |
|------|------|------|
| Extension Function (Controller 내부) | Controller 파일 | `Team.toDetailResponse()` |
| Dedicated Mapper class | `api/mapper/` | `BattingRecordMapper`, `StatsMapper` |
| DTO companion `from()` / `toDomain()` | DTO 클래스 내부 | `PlateAppearanceRequestDto.toDomain()` |

### DTO 명명 규칙

| 타입 | 명명 | 예시 |
|------|------|------|
| Request (API) | `*ApiRequest` | `CreateAppealApiRequest` |
| Request (내부) | `*Request` | `LoginRequest` |
| Response | `*Response` | `TeamDetailResponse`, `PlayerResponse` |
| 페이지네이션 | `PagedResponse<T>` | `PagedResponse.from(page)` |

## JSON & Application 컨벤션

```yaml
# 모든 API 모듈 공통 설정
spring:
  jackson:
    property-naming-strategy: SNAKE_CASE   # 필수
    default-property-inclusion: non_null   # null 필드 제외
  jpa:
    open-in-view: false                    # 필수
    properties:
      hibernate.default_batch_fetch_size: 100
```

## Testing Patterns

### Unit Test (Entity Business Logic)
```kotlin
@DisplayName("Game 엔티티 테스트")
class GameTest {
    @Nested
    @DisplayName("cancel()")
    inner class Cancel {
        @Test
        @DisplayName("SCHEDULED 상태에서 취소 성공")
        fun cancelScheduledGame() {
            // given
            val game = Game.create(homeTeamId = 1L, awayTeamId = 2L)

            // when
            game.cancel("비 때문에 취소")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
        }
    }
}
```

### Integration Test (Repository)
```kotlin
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameRepositoryTest {
    @Autowired
    private lateinit var gameRepository: GameRepository

    @Test
    @Transactional
    fun `should find scheduled games by team`() {
        // given-when-then
    }
}
```

## Best Practices Checklist

### Entity Design
- [ ] Use `private constructor` + `companion object.create()`
- [ ] Business logic in Entity, not in Service
- [ ] Extend `BaseTimeEntity()` (Instant, UTC)
- [ ] `@Version` for optimistic locking where needed
- [ ] Private backing field for JPA collections
- [ ] `@Table(indexes = [...])` for query optimization

### Service Layer
- [ ] Class-level `@Transactional(readOnly = true)`
- [ ] Method-level `@Transactional` for write operations
- [ ] Custom exceptions for domain errors
- [ ] `ApplicationEventPublisher` for domain events

### Controller Layer
- [ ] Zero Entity Leak (NEVER return Entity)
- [ ] ApiResponse wrapper for all responses
- [ ] `@Valid` for request validation
- [ ] Correct URL prefix per module

### Repository Layer
- [ ] Direct JPA+Port for basic CRUD
- [ ] Adapter wrapping for complex queries
- [ ] No business logic in Repository

## Agent 협업

이 Skill을 활용하는 Agent:
- **architect**: Entity 설계 및 비즈니스 로직 구현
- **implementer**: Controller/Service/DTO 구현
- **reviewer**: 코드 품질 검증 시 컨벤션 준수 확인
