# TDD 규칙 (Test-Driven Development Rules)

> **Core/Service 계층 필수 규칙**
> 비즈니스 로직은 테스트 없이 작성 불가

---

## 🎯 TDD 적용 기준

### 🔴 Tier 1: TDD 필수 (Always)

**nextup-core 모듈의 모든 비즈니스 로직**

```kotlin
// 필수 적용 대상
nextup-core/
├── entity/           # Entity 비즈니스 메서드
├── service/          # Service 계층
├── domain/           # Domain Events, Value Objects
└── validator/        # 도메인 검증 로직
```

#### 예시

```kotlin
// ❌ 테스트 없이 비즈니스 로직 작성 금지
class Player(
    val name: String,
    val position: Position
) {
    fun changeBattingOrder(newOrder: Int): Player {
        // 타순 변경 로직 - TDD 필수!
        return copy(battingOrder = newOrder)
    }
}

// ✅ 테스트 먼저 작성
class PlayerTest {
    @Test
    fun `타순을 변경할 수 있다`() {
        // Given
        val player = Player("홍길동", Position.PITCHER)

        // When
        val updated = player.changeBattingOrder(3)

        // Then
        assertThat(updated.battingOrder).isEqualTo(3)
    }
}
```

---

### 🟡 Tier 2: TDD 선택 (AI Judgment)

**복잡도/중요도에 따라 AI가 판단**

```kotlin
// AI 판단 기준
nextup-api/
├── controller/       # 간단 → 통합 테스트, 복잡 → TDD
└── dto/              # 단순 변환 → 스킵

nextup-infrastructure/
├── repository/       # Spring Data JPA → 스킵, 복잡 쿼리 → TDD
└── adapter/          # 외부 API 연동 → Mock 테스트
```

#### AI 판단 가이드

| 대상 | 조건 | TDD 여부 |
|------|------|----------|
| **Controller** | 단순 CRUD | ❌ 통합 테스트로 충분 |
| **Controller** | 복잡한 비즈니스 조건 | ✅ TDD 권장 |
| **Repository** | Spring Data 메서드 | ❌ 스킵 |
| **Repository** | 복잡한 QueryDSL | ✅ TDD 권장 |
| **DTO/Mapper** | 단순 변환 | ❌ 스킵 |
| **Configuration** | 설정 클래스 | ❌ 스킵 |

---

### 🟢 Tier 3: TDD 불필요 (Skip)

```kotlin
// 테스트 불필요
- Configuration 클래스
- DTO (단순 data class)
- Mapper (toEntity, toDto)
- Constants
- Enum (비즈니스 로직 없는 경우)
```

---

## 🔄 TDD 워크플로우

### Red-Green-Refactor Cycle

```
1. ❌ Red: 실패하는 테스트 작성
   └─> @Test fun `기능 설명`() { ... }

2. ✅ Green: 최소한의 코드로 테스트 통과
   └─> fun 기능() { ... }

3. ♻️ Refactor: 코드 개선 (테스트는 유지)
   └─> 중복 제거, 명확성 향상
```

### 예시: 야구 기록 계산 TDD

```kotlin
// Step 1: Red - 테스트 먼저
@Test
fun `타율을 계산할 수 있다`() {
    val record = BattingRecord(hits = 3, atBats = 10)
    assertThat(record.calculateAverage()).isEqualTo(0.300)
}
// 컴파일 에러 → calculateAverage 메서드 없음

// Step 2: Green - 구현
class BattingRecord(
    val hits: Int,
    val atBats: Int
) {
    fun calculateAverage(): Double {
        if (atBats == 0) return 0.0
        return hits.toDouble() / atBats
    }
}
// 테스트 통과

// Step 3: Refactor - 개선
class BattingRecord(
    val hits: Int,
    val atBats: Int
) {
    fun calculateAverage(): Double =
        if (atBats == 0) 0.0
        else (hits.toDouble() / atBats).round(3)

    private fun Double.round(decimals: Int): Double =
        "%.${decimals}f".format(this).toDouble()
}
```

---

## 📋 테스트 커버리지 기준

### Jacoco Coverage 목표

| 계층 | 목표 커버리지 | 필수 여부 |
|------|--------------|-----------|
| **nextup-core** | **80% 이상** | ✅ 필수 (PR 블록) |
| **nextup-api** | 70% 이상 | 권장 |
| **nextup-infrastructure** | 60% 이상 | 권장 |

### Codecov PR 정책

```yaml
# codecov.yml
coverage:
  status:
    project:
      default:
        target: 80%        # 전체 프로젝트 80%
        threshold: 1%      # 1% 감소까지 허용
    patch:
      default:
        target: 80%        # 새 코드는 무조건 80%
```

**PR 머지 조건:**
- ✅ 전체 커버리지 80% 이상
- ✅ 새로 추가된 코드(Patch) 커버리지 80% 이상
- ❌ 기준 미달 시 PR 머지 불가

---

## 🧪 테스트 작성 원칙

### 1. Given-When-Then 패턴

```kotlin
@Test
fun `DH 규칙 해제 시 투수가 타격할 수 있다`() {
    // Given: 테스트 준비
    val game = Game(dhRule = false)
    val pitcher = Player(position = Position.PITCHER)

    // When: 동작 실행
    val canBat = game.canBat(pitcher)

    // Then: 결과 검증
    assertThat(canBat).isTrue()
}
```

### 2. 테스트 이름 명확히

```kotlin
// ❌ 불명확
@Test
fun test1() { ... }

// ✅ 명확
@Test
fun `DH 규칙이 활성화된 경우 투수는 타격할 수 없다`() { ... }
```

### 3. 한 테스트는 한 가지만

```kotlin
// ❌ 여러 검증
@Test
fun testPlayer() {
    // 이름 검증
    // 포지션 검증
    // 타순 검증  // 너무 많음!
}

// ✅ 분리
@Test fun `선수 이름을 설정할 수 있다`() { ... }
@Test fun `선수 포지션을 변경할 수 있다`() { ... }
@Test fun `선수 타순을 지정할 수 있다`() { ... }
```

---

## 🚀 Commands

### /tdd 명령어

TDD 워크플로우 활성화:

```bash
/tdd
```

실행 내용:
1. 현재 작업 파일 확인
2. Core/Service 계층인가? → TDD 강제 활성화
3. 테스트 파일 자동 생성
4. Red-Green-Refactor 가이드 제공

---

## 🔍 검증 방법

### CI/CD Pipeline

```bash
# 1. 테스트 실행
./gradlew test

# 2. 커버리지 검증
./gradlew jacocoTestCoverageVerification

# 3. 리포트 생성
./gradlew jacocoTestReport

# 4. Codecov 업로드
bash <(curl -s https://codecov.io/bash)
```

### 로컬 검증

```bash
# 커버리지 확인
./gradlew jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

---

## ⚠️ Reviewer 체크 항목

- [ ] Core/Service 계층에 테스트 있는가?
- [ ] 테스트가 실패하지 않는가?
- [ ] 커버리지 80% 이상인가?
- [ ] Given-When-Then 패턴 사용했는가?
- [ ] 테스트 이름이 명확한가?

**Core/Service 계층에 테스트 없으면 → 즉시 REJECT**

---

## 🎯 이 규칙의 목적

1. **신뢰성**: 비즈니스 로직 버그 사전 방지
2. **문서화**: 테스트가 코드의 사용법 설명
3. **리팩토링 안전성**: 테스트가 있어야 자신감 있게 개선 가능
4. **설계 개선**: TDD는 좋은 설계를 유도함

---

## 📚 참고 자료

- [Kotlin Testing Guide](https://kotlinlang.org/docs/jvm-test-using-junit.html)
- [Spring Boot Testing](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing)
- [Jacoco Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Codecov Documentation](https://docs.codecov.com/)
