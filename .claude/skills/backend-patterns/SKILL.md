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

// Usage
val response = player.toResponse()
```

## JPA Entity Patterns

### Rich Domain Model
```kotlin
@Entity
@Table(name = "games")
class Game private constructor(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val homeTeamId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GameStatus = GameStatus.SCHEDULED
) {
    companion object {
        fun create(homeTeamId: Long, awayTeamId: Long): Game {
            // Business logic here
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

## Service Layer Patterns

### Transaction Management
```kotlin
@Service
@Transactional(readOnly = true)
class GameService(
    private val gameRepository: GameRepository
) {
    @Transactional  // Write operation
    fun createGame(request: CreateGameRequest): GameResponse {
        val game = Game.create(
            homeTeamId = request.homeTeamId,
            awayTeamId = request.awayTeamId
        )
        return gameRepository.save(game).toResponse()
    }

    // Read operation (no @Transactional override needed)
    fun getGame(id: Long): GameResponse {
        val game = gameRepository.findById(id)
            .orElseThrow { GameNotFoundException(id) }
        return game.toResponse()
    }
}
```

### Custom Exceptions
```kotlin
// Domain exception
class GameNotFoundException(id: Long) : RuntimeException("Game not found: $id")

class InvalidGameStateException(message: String) : IllegalStateException(message)
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

    @PutMapping("/{id}/start")
    fun startGame(@PathVariable id: Long): ApiResponse<GameResponse> {
        return ApiResponse.success(gameService.startGame(id))
    }
}
```

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

### Global Exception Handler
```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(GameNotFoundException::class)
    fun handleNotFound(ex: GameNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("GAME_NOT_FOUND", ex.message ?: "Game not found"))
    }

    @ExceptionHandler(InvalidGameStateException::class)
    fun handleInvalidState(ex: InvalidGameStateException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("INVALID_GAME_STATE", ex.message ?: "Invalid game state"))
    }
}
```

## Repository Patterns

### Basic Repository
```kotlin
interface GameRepository : JpaRepository<Game, Long>
```

### QueryDSL for Complex Queries
```kotlin
interface GameRepositoryCustom {
    fun findScheduledGames(teamId: Long): List<Game>
}

@Repository
class GameRepositoryImpl(
    private val queryFactory: JPAQueryFactory
) : GameRepositoryCustom {
    override fun findScheduledGames(teamId: Long): List<Game> {
        val game = QGame.game
        return queryFactory
            .selectFrom(game)
            .where(
                game.status.eq(GameStatus.SCHEDULED)
                    .and(
                        game.homeTeamId.eq(teamId)
                            .or(game.awayTeamId.eq(teamId))
                    )
            )
            .orderBy(game.scheduledDate.asc())
            .fetch()
    }
}
```

## Configuration Patterns

### Properties Binding
```kotlin
@ConfigurationProperties(prefix = "app.baseball")
@ConstructorBinding
data class BaseballProperties(
    val maxInnings: Int = 9,
    val extraInningsEnabled: Boolean = true,
    val mercyRuleEnabled: Boolean = true,
    val mercyRunDiff: Int = 10
)
```

## Testing Patterns

### Unit Test (Entity Business Logic)
```kotlin
@Test
fun `should cancel game when status is SCHEDULED`() {
    // given
    val game = Game.create(homeTeamId = 1L, awayTeamId = 2L)

    // when
    game.cancel("비 때문에 취소")

    // then
    assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
}

@Test
fun `should throw exception when canceling already started game`() {
    // given
    val game = Game.create(homeTeamId = 1L, awayTeamId = 2L)
    game.start()

    // when & then
    assertThrows<IllegalStateException> {
        game.cancel("취소 불가")
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
        // given
        val game1 = Game.create(homeTeamId = 1L, awayTeamId = 2L)
        val game2 = Game.create(homeTeamId = 1L, awayTeamId = 3L).apply { start() }
        gameRepository.saveAll(listOf(game1, game2))

        // when
        val result = gameRepository.findScheduledGames(1L)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(GameStatus.SCHEDULED)
    }
}
```

## Best Practices Checklist

### Entity Design
- [ ] Use `private constructor` + `companion object.create()`
- [ ] Business logic in Entity, not in Service
- [ ] Value Objects for complex values
- [ ] Enum types with `@Enumerated(EnumType.STRING)`

### Service Layer
- [ ] Class-level `@Transactional(readOnly = true)`
- [ ] Method-level `@Transactional` for write operations
- [ ] Custom exceptions for domain errors
- [ ] Extension functions for Entity → DTO conversion

### Controller Layer
- [ ] Zero Entity Leak (NEVER return Entity)
- [ ] ApiResponse wrapper for all responses
- [ ] `@Valid` for request validation
- [ ] Global exception handler for consistency

### Repository Layer
- [ ] Extend `JpaRepository` for basic CRUD
- [ ] QueryDSL for complex queries
- [ ] No business logic in Repository

## Anti-Patterns to Avoid

```kotlin
// ❌ AVOID: Anemic Domain Model
class Game {
    var status: GameStatus = GameStatus.SCHEDULED
}

// Service with business logic
fun startGame(id: Long) {
    val game = findGame(id)
    game.status = GameStatus.IN_PROGRESS  // ❌ Logic in Service
}

// ✅ PREFER: Rich Domain Model
class Game {
    fun start() {
        require(status == GameStatus.SCHEDULED)
        status = GameStatus.IN_PROGRESS
    }
}
```

```kotlin
// ❌ AVOID: Entity exposure
@GetMapping("/{id}")
fun getGame(@PathVariable id: Long): Game  // ❌ Zero Entity Leak violation

// ✅ PREFER: DTO response
@GetMapping("/{id}")
fun getGame(@PathVariable id: Long): ApiResponse<GameResponse>  // ✅ DTO
```

## Agent 협업

이 Skill을 활용하는 Agent:
- **architect**: Entity 설계 및 비즈니스 로직 구현
- **implementer**: Controller/Service/DTO 구현
- **reviewer**: 테스트 코드 작성 및 코드 품질 검증 시 컨벤션 준수 확인
