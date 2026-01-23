# TDD Requirements

## Minimum Test Coverage: 80%

Coverage target applies to:
- nextup-core module (Entity, Service, Domain Events, Value Objects)
- nextup-infrastructure module (Repository implementations)
- nextup-api module (Controller, Exception handlers)

## TDD Workflow (MANDATORY)

```
1. Write test first (RED)
   ↓
2. Run test - it should FAIL
   ↓
3. Write minimal implementation (GREEN)
   ↓
4. Run test - it should PASS
   ↓
5. Refactor (IMPROVE)
   ↓
6. Verify coverage (80%+)
```

## Test Types (ALL required)

1. **Unit Tests** - Entity business logic, Service layer, Utilities
2. **Integration Tests** - Repository queries, API endpoints, Database operations
3. **E2E Tests** - Critical user flows (REST Assured or WebTestClient)

## Mandatory TDD Layers

**MUST apply TDD:**
- ✅ `nextup-core/Entity` business logic (Rich Domain Model)
- ✅ `nextup-core/Service` layer
- ✅ `nextup-core/Domain Events`
- ✅ `nextup-core/Value Objects`

**Optional (AI decides based on complexity):**
- 🟡 Controller (mostly integration tests)
- 🟡 Repository (QueryDSL tests)
- 🟡 DTO/Mapper (simple transformations)
- 🟡 Configuration classes

## Test Naming Convention

```kotlin
// Entity business logic test
class GameTest {
    @Test
    fun `should cancel game when status is SCHEDULED`() {
        // given
        val game = Game.create(...)

        // when
        game.cancel("비 때문에 취소")

        // then
        assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
    }
}

// Service layer test
class GameServiceTest {
    @Test
    fun `should throw exception when canceling already started game`() {
        // given
        val game = Game.create(...).apply { start() }

        // when & then
        assertThrows<IllegalStateException> {
            gameService.cancelGame(game.id, "취소 사유")
        }
    }
}
```

## Test Isolation

- Each test MUST be independent
- Use @Transactional for database tests
- Clean up test data after each test
- No shared mutable state between tests

## Coverage Verification

Before ANY commit:
- [ ] Run `./gradlew test jacocoTestReport`
- [ ] Verify coverage ≥ 80% in Core/Service layers
- [ ] All tests PASS
- [ ] No ignored tests without valid reason

## Troubleshooting Test Failures

1. Use **scenario-tester** agent for test design
2. Check test isolation (no shared state)
3. Verify mocks are correct (prefer real objects in integration tests)
4. Fix implementation, not tests (unless tests are wrong)
5. If stuck, use **risk-manager** agent for analysis

## When to Skip TDD

TDD is NOT required for:
- Simple CRUD without business logic
- DTO/Mapper without transformation logic
- Configuration/Setup classes
- Trivial getters/setters

**Use AI judgment for borderline cases.**

## Agent Support

- **scenario-tester** - Use PROACTIVELY for test design and implementation
- **risk-manager** - Use when tests fail to analyze root cause
- **reviewer** - Enforces 80% coverage, auto-REJECT if below threshold
