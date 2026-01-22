# Backend-Patterns Skill - Kotlin/Spring Boot 컨벤션

> **재사용 가능한 백엔드 기술 지식**
> Kotlin, Spring Boot 3.x, JPA 최적화 패턴 제공

---

## 📚 Skill 개요

### 목적
- Kotlin + Spring Boot 3.x 최적화 패턴 제공
- JPA Entity, Service, Controller, DTO 코딩 컨벤션
- 멀티모듈 프로젝트 구조화 가이드

### 사용 시나리오
- ✅ nextup-core Entity 작성 시
- ✅ nextup-api Controller 작성 시
- ✅ nextup-infrastructure Repository 작성 시

---

## 🏗️ Multi-Module Architecture

### Module Dependency
```
nextup-api → nextup-infrastructure → nextup-core → nextup-common
```

### Package Convention
```kotlin
// nextup-core (비즈니스 로직)
com.nextup.core.
├── entity/         # Rich Domain Model
├── service/        # 도메인 서비스
├── port/           # Hexagonal - Port (interface)
└── vo/             # Value Object

// nextup-infrastructure (어댑터)
com.nextup.infrastructure.
├── repository/     # JPA Repository
├── adapter/        # Hexagonal - Adapter (구현)
└── config/         # Infrastructure 설정

// nextup-api (API 계층)
com.nextup.api.
├── controller/     # REST Controller
├── dto/            # Request/Response DTO
├── exception/      # Global Exception Handler
└── config/         # Security, CORS 설정
```

---

## 🎯 Entity 패턴 (nextup-core)

### 1. Rich Domain Model

```kotlin
// ❌ 빈약한 도메인 모델 (Anemic)
@Entity
class Player(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    var name: String,
    var position: Position
)
// 비즈니스 로직이 Service에만 있음 → 안티패턴

// ✅ Rich Domain Model
@Entity
class Player(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    name: String,
    position: Position,
    battingOrder: Int? = null
) {
    var name: String = name
        private set  // setter 캡슐화

    var position: Position = position
        private set

    var battingOrder: Int? = battingOrder
        private set

    // 비즈니스 로직은 Entity 내부에
    fun changeBattingOrder(newOrder: Int): Player {
        require(newOrder in 1..9) { "타순은 1-9 사이여야 합니다" }
        return this.copy(battingOrder = newOrder)
    }

    fun moveTo(newPosition: Position): Player {
        // 포지션 변경 비즈니스 룰
        return this.copy(position = newPosition)
    }
}
```

### 2. JPA Conventions

```kotlin
// ID 생성 전략
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
val id: Long = 0

// Nullable 처리
var battingOrder: Int? = null  // Kotlin nullable

// Enum 타입
@Enumerated(EnumType.STRING)  // ORDINAL 금지!
var position: Position

// 날짜/시간
@CreatedDate
@Column(nullable = false, updatable = false)
lateinit var createdAt: LocalDateTime

@LastModifiedDate
@Column(nullable = false)
lateinit var updatedAt: LocalDateTime

// 연관관계
@ManyToOne(fetch = FetchType.LAZY)  // EAGER 금지!
@JoinColumn(name = "team_id")
var team: Team? = null
```

### 3. Entity Lifecycle

```kotlin
@Entity
@EntityListeners(AuditingEntityListener::class)  // Auditing
class Player(
    // ...
) {
    @PrePersist
    fun prePersist() {
        // 저장 전 검증
    }

    @PreUpdate
    fun preUpdate() {
        // 수정 전 검증
    }
}
```

---

## 🔄 Service 패턴 (nextup-core)

### 1. Service Layer

```kotlin
@Service
@Transactional(readOnly = true)  // 기본 readOnly
class PlayerService(
    private val playerPort: PlayerPort  // Port 사용 (interface)
) {
    @Transactional  // 쓰기 작업만 readOnly = false
    fun create(command: CreatePlayerCommand): Player {
        val player = Player(
            name = command.name,
            position = command.position
        )
        return playerPort.save(player)
    }

    fun findById(id: Long): Player? {
        return playerPort.findById(id)
    }

    @Transactional
    fun changeBattingOrder(id: Long, newOrder: Int): Player {
        val player = playerPort.findById(id)
            ?: throw PlayerNotFoundException(id)
        val updated = player.changeBattingOrder(newOrder)  // Entity 메서드 호출
        return playerPort.save(updated)
    }
}
```

### 2. Port (Hexagonal)

```kotlin
// nextup-core/port/PlayerPort.kt
interface PlayerPort {
    fun save(player: Player): Player
    fun findById(id: Long): Player?
    fun findAll(): List<Player>
    fun delete(id: Long)
}

// nextup-infrastructure/adapter/PlayerAdapter.kt
@Component
class PlayerAdapter(
    private val playerRepository: PlayerRepository
) : PlayerPort {
    override fun save(player: Player): Player {
        return playerRepository.save(player)
    }

    override fun findById(id: Long): Player? {
        return playerRepository.findById(id).orElse(null)
    }
}
```

---

## 🌐 Controller 패턴 (nextup-api)

### 1. Thin Controller

```kotlin
@RestController
@RequestMapping("/api/v1/players")
class PlayerController(
    private val playerService: PlayerService
) {
    @GetMapping("/{id}")
    fun getPlayer(@PathVariable id: Long): ApiResponse<PlayerResponse> {
        val player = playerService.findById(id)
            ?: throw PlayerNotFoundException(id)
        return ApiResponse.success(player.toResponse())
    }

    @PostMapping
    fun createPlayer(
        @Valid @RequestBody request: CreatePlayerRequest
    ): ApiResponse<PlayerResponse> {
        val player = playerService.create(request.toCommand())
        return ApiResponse.success(player.toResponse())
    }

    @PatchMapping("/{id}/batting-order")
    fun changeBattingOrder(
        @PathVariable id: Long,
        @Valid @RequestBody request: ChangeBattingOrderRequest
    ): ApiResponse<PlayerResponse> {
        val player = playerService.changeBattingOrder(id, request.battingOrder)
        return ApiResponse.success(player.toResponse())
    }
}
```

### 2. ApiResponse 래핑 (필수)

```kotlin
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorDetail? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> {
            return ApiResponse(success = true, data = data)
        }

        fun <T> error(code: String, message: String): ApiResponse<T> {
            return ApiResponse(
                success = false,
                error = ErrorDetail(code, message)
            )
        }
    }
}

data class ErrorDetail(
    val code: String,
    val message: String
)
```

### 3. Validation

```kotlin
data class CreatePlayerRequest(
    @field:NotBlank(message = "이름은 필수입니다")
    @field:Size(min = 2, max = 50, message = "이름은 2-50자여야 합니다")
    val name: String,

    @field:NotNull(message = "포지션은 필수입니다")
    val position: Position,

    @field:Min(value = 1, message = "타순은 1 이상이어야 합니다")
    @field:Max(value = 9, message = "타순은 9 이하여야 합니다")
    val battingOrder: Int? = null
)
```

---

## 📦 DTO 패턴 (nextup-api)

### 1. Request DTO

```kotlin
// POST /api/v1/players
data class CreatePlayerRequest(
    val name: String,
    val position: Position,
    val battingOrder: Int? = null
) {
    fun toCommand(): CreatePlayerCommand {
        return CreatePlayerCommand(
            name = this.name,
            position = this.position,
            battingOrder = this.battingOrder
        )
    }
}
```

### 2. Response DTO

```kotlin
data class PlayerResponse(
    val id: Long,
    val name: String,
    val position: String,
    val battingOrder: Int?,
    val createdAt: LocalDateTime
)

// Extension Function
fun Player.toResponse(): PlayerResponse {
    return PlayerResponse(
        id = this.id,
        name = this.name,
        position = this.position.name,
        battingOrder = this.battingOrder,
        createdAt = this.createdAt
    )
}
```

### 3. Zero Entity Leak (필수)

```kotlin
// ❌ 절대 금지 - Entity 직접 반환
@GetMapping("/{id}")
fun getPlayer(@PathVariable id: Long): Player {
    return playerService.findById(id)  // VETO!
}

// ✅ 올바른 방법 - DTO 변환
@GetMapping("/{id}")
fun getPlayer(@PathVariable id: Long): ApiResponse<PlayerResponse> {
    val player = playerService.findById(id)
        ?: throw PlayerNotFoundException(id)
    return ApiResponse.success(player.toResponse())
}
```

---

## 🗄️ Repository 패턴 (nextup-infrastructure)

### 1. Spring Data JPA

```kotlin
interface PlayerRepository : JpaRepository<Player, Long> {
    fun findByName(name: String): Player?
    fun findByPosition(position: Position): List<Player>
}
```

### 2. QueryDSL (복잡한 쿼리)

```kotlin
@Repository
class PlayerQueryRepository(
    private val queryFactory: JPAQueryFactory
) {
    fun searchPlayers(
        name: String?,
        position: Position?
    ): List<Player> {
        return queryFactory
            .selectFrom(QPlayer.player)
            .where(
                nameEq(name),
                positionEq(position)
            )
            .fetch()
    }

    private fun nameEq(name: String?): BooleanExpression? {
        return name?.let { QPlayer.player.name.contains(it) }
    }

    private fun positionEq(position: Position?): BooleanExpression? {
        return position?.let { QPlayer.player.position.eq(it) }
    }
}
```

---

## ⚠️ Exception 처리

### 1. Custom Exception

```kotlin
// nextup-core/exception/PlayerException.kt
sealed class PlayerException(message: String) : RuntimeException(message)

class PlayerNotFoundException(id: Long) :
    PlayerException("Player not found: $id")

class InvalidBattingOrderException(order: Int) :
    PlayerException("Invalid batting order: $order")
```

### 2. Global Exception Handler

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PlayerNotFoundException::class)
    fun handlePlayerNotFound(ex: PlayerNotFoundException): ResponseEntity<ApiResponse<Unit>> {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("PLAYER_NOT_FOUND", ex.message ?: "Player not found"))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Unit>> {
        val errors = ex.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", errors))
    }
}
```

---

## 🔐 Security 패턴

### 1. CORS 설정

```kotlin
@Configuration
class CorsConfig : WebMvcConfigurer {
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://nextup.com")  // 특정 Origin만
            .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true)
    }
}
```

### 2. Method Security

```kotlin
@Configuration
@EnableMethodSecurity
class SecurityConfig

// Controller
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
fun deletePlayer(@PathVariable id: Long): ApiResponse<Unit> {
    playerService.delete(id)
    return ApiResponse.success(Unit)
}
```

---

## 📋 Coding Convention 체크리스트

### Entity (nextup-core)
- [ ] Rich Domain Model 패턴 사용
- [ ] 비즈니스 로직이 Entity 내부에 있는가?
- [ ] setter는 private인가?
- [ ] @Enumerated(EnumType.STRING) 사용
- [ ] FetchType.LAZY 사용

### Service (nextup-core)
- [ ] @Transactional(readOnly = true) 기본 설정
- [ ] Port (interface) 사용
- [ ] Entity 메서드 호출하는가?

### Controller (nextup-api)
- [ ] Thin Controller (비즈니스 로직 없음)
- [ ] ApiResponse 래핑
- [ ] Entity 직접 반환하지 않음 (Zero Entity Leak)
- [ ] @Valid 사용
- [ ] CustomException 사용

### DTO (nextup-api)
- [ ] Request/Response 분리
- [ ] toCommand(), toResponse() 변환 메서드
- [ ] Validation 어노테이션

---

## 🎯 이 Skill의 장점

1. **일관성**: 모든 코드가 동일한 패턴 사용
2. **유지보수성**: 명확한 계층 분리
3. **테스트 용이성**: Hexagonal Architecture
4. **보안성**: Zero Entity Leak 강제

---

## 📚 참고 자료

- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Spring Boot Best Practices](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [JPA Best Practices](https://vladmihalcea.com/jpa-hibernate-best-practices/)
