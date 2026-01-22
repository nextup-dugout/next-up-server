# /tdd Command - TDD 워크플로우 활성화

> **Red → Green → Refactor** 사이클로 TDD 개발 진행

## 목적

Core/Service 계층 개발 시 TDD를 강제하고, 테스트 커버리지 80%를 보장합니다.

## 사용법

```
/tdd
```

## 실행 흐름

### 1. 현재 파일 확인
```
- nextup-core/Entity → TDD 필수
- nextup-core/Service → TDD 필수
- nextup-api/Controller → TDD 선택 (간단하면 통합 테스트)
```

### 2. TDD 사이클 시작

#### Red: 실패하는 테스트 작성
```kotlin
@Test
fun `타순을 변경할 수 있다`() {
    // Given
    val player = Player("홍길동", Position.PITCHER)

    // When
    val updated = player.changeBattingOrder(3)

    // Then
    assertThat(updated.battingOrder).isEqualTo(3)
}
// 컴파일 에러 → changeBattingOrder 메서드 없음
```

#### Green: 최소 구현
```kotlin
class Player(
    val name: String,
    val position: Position,
    var battingOrder: Int? = null
) {
    fun changeBattingOrder(newOrder: Int): Player {
        return copy(battingOrder = newOrder)
    }
}
// 테스트 통과
```

#### Refactor: 개선
```kotlin
fun changeBattingOrder(newOrder: Int): Player {
    require(newOrder in 1..9) { "타순은 1-9 사이" }
    return copy(battingOrder = newOrder)
}
// 검증 로직 추가, 테스트는 유지
```

### 3. 커버리지 확인
```bash
./gradlew test jacocoTestReport
# nextup-core: 80% 이상 필수
```

## Skills 참조

- **tdd**: TDD 규칙 (Red-Green-Refactor)
- **domain-baseball**: 도메인 로직 검증
- **quality-metrics**: 커버리지 측정

## 체크리스트

- [ ] Core/Service 계층인가? → TDD 필수
- [ ] 테스트 먼저 작성 (Red)
- [ ] 최소 구현 (Green)
- [ ] 리팩토링 (Refactor)
- [ ] 커버리지 80% 이상

## 예시

```
User: "/tdd"

Agent:
1. 현재 파일 확인: Player.kt (nextup-core)
   → TDD 필수

2. Red: 테스트 작성 안내
   "PlayerTest.kt에 `타순을 변경할 수 있다()` 테스트 작성"

3. Green: 최소 구현 안내
   "changeBattingOrder() 메서드 작성"

4. Refactor: 개선 안내
   "타순 1-9 검증 추가"

5. 커버리지 확인
   ✅ Player.kt: 85%
```

## 이 Command의 장점

- ✅ TDD 사이클 명확히 안내
- ✅ Core/Service 품질 보증
- ✅ 커버리지 80% 자동 달성
