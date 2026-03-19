---
name: tdd
description: |
  TDD(Test-Driven Development) 워크플로우 활성화. RED-GREEN-REFACTOR 사이클을 안내하고
  대상 클래스/메서드에 대한 테스트 우선 개발을 수행한다.
user-invocable: true
argument-hint: "[ClassName.methodName] e.g. Game.cancel"
allowed-tools: Bash, Read, Write, Edit, Glob, Grep
---

# /tdd - TDD Workflow

TDD(Test-Driven Development) 워크플로우를 활성화합니다.

## Arguments

`$ARGUMENTS`를 통해 대상 클래스/메서드를 지정할 수 있습니다.

| 사용법 | 설명 |
|--------|------|
| `/tdd` | TDD 모드 활성화 (대상 입력 대기) |
| `/tdd Game` | Game 엔티티 대상 TDD |
| `/tdd GameService.cancelGame` | 특정 메서드 대상 TDD |
| `/tdd BattingRecord` | BattingRecord 엔티티 대상 TDD |

## 테스트 도구

| 도구 | 버전 | 용도 |
|------|------|------|
| JUnit 5 | Spring Boot 내장 | 테스트 프레임워크 |
| Mockk | 1.13.14 | Kotlin 모킹 라이브러리 |
| AssertJ | 3.27.3 | 풍부한 assertion |

## 테스트 파일 규칙

### 위치
```
nextup-core/src/test/kotlin/com/nextup/core/domain/{도메인}/{EntityName}Test.kt
nextup-core/src/test/kotlin/com/nextup/core/service/{도메인}/{ServiceName}Test.kt
```

### 명명 규칙
- 파일명: `{대상클래스}Test.kt`
- 테스트명: `@DisplayName("한글 설명")` 사용
- 그룹화: `@Nested inner class`로 메서드별 분류

### 테스트 구조 패턴

```kotlin
@DisplayName("Game 엔티티 테스트")
class GameTest {

    private lateinit var game: Game

    @BeforeEach
    fun setUp() {
        game = createGame()
    }

    @Nested
    @DisplayName("cancel()")
    inner class Cancel {
        @Test
        @DisplayName("SCHEDULED 상태에서 취소하면 CANCELLED로 변경된다")
        fun cancelScheduledGame() {
            // given
            // game은 setUp에서 SCHEDULED 상태로 생성됨

            // when
            game.cancel("비 때문에 취소")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
        }

        @Test
        @DisplayName("이미 시작된 경기는 취소할 수 없다")
        fun cannotCancelStartedGame() {
            // given
            game.start()

            // when & then
            assertThatThrownBy { game.cancel("취소 불가") }
                .isInstanceOf(InvalidGameStateException::class.java)
        }
    }

    private fun createGame(): Game = Game.create(
        homeTeamId = 1L,
        awayTeamId = 2L
    )
}
```

## Workflow

```
1. Write test first (RED)
   - 실패하는 테스트 작성
   - @DisplayName으로 의도를 명확히 기술
   - given-when-then 패턴 사용

2. Run test - it should FAIL
   - ./gradlew :nextup-core:test --tests "*{TestClass}*" --no-daemon --max-workers=2
   - 예상대로 실패하는지 확인

3. Write minimal implementation (GREEN)
   - 테스트를 통과하는 최소한의 코드 작성
   - 오버엔지니어링 금지

4. Run test - it should PASS
   - ./gradlew :nextup-core:test --tests "*{TestClass}*" --no-daemon --max-workers=2
   - 테스트 통과 확인

5. Refactor (IMPROVE)
   - 코드 품질 개선
   - 중복 제거, 네이밍 개선

6. Verify coverage (80%+)
   - ./gradlew jacocoTestReport --no-daemon --max-workers=2
   - 커버리지 80% 이상 확인
```

> **주의**: Gradle 빌드는 동시 실행 금지. 항상 `--no-daemon --max-workers=2` 플래그 사용.

## TDD 필수 적용 대상

- `nextup-core/domain/**` Entity 비즈니스 로직
- `nextup-core/service/**` Service 인터페이스 계층
- Domain Events
- Value Objects (GameState, Score 등)

## TDD 선택 적용 (AI 판단)

- Controller (주로 통합 테스트)
- Repository (QueryDSL 테스트)
- DTO/Mapper (단순 변환)
- Configuration

## 커버리지 제외 대상

Jacoco 측정에서 제외되는 패키지:
- `**/config/**`
- `**/dto/**`
- `**/exception/**`
- `**/mapper/**`
- `**/HealthController*`
- `**/domain/event/**`

## Assertion 패턴

```kotlin
// 값 검증
assertThat(result.name).isEqualTo("홍길동")
assertThat(result.members).hasSize(3)
assertThat(result.status).isIn(GameStatus.SCHEDULED, GameStatus.IN_PROGRESS)

// 예외 검증
assertThatThrownBy { game.cancel("이유") }
    .isInstanceOf(InvalidGameStateException::class.java)
    .hasMessageContaining("cancel")

// 컬렉션 검증
assertThat(results)
    .extracting("name")
    .containsExactly("팀A", "팀B")
```

## Mockk 패턴

```kotlin
// Mock 생성
val repository = mockk<GameRepositoryPort>()
val service = GameScheduleServiceImpl(repository)

// Stubbing
every { repository.findById(1L) } returns Optional.of(game)
every { repository.save(any()) } answers { firstArg() }

// Verification
verify(exactly = 1) { repository.save(any()) }
```
