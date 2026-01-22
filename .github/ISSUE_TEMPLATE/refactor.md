---
name: Refactor
about: 리팩터링을 해야할 부분이 있나요?
labels: refactoring
---

## 🔨 리팩터링 목적
<!-- 왜 리팩터링이 필요한지 명확하게 설명해주세요 -->

**예시:**
> 현재 PlayerService에 비즈니스 로직이 집중되어 있어 Rich Domain Model 원칙을 위반하고 있습니다.
> 도메인 로직을 Player Entity로 이동하여 응집도를 높이고 테스트 가능성을 개선해야 합니다.

## 📊 현재 문제점
<!-- 현재 코드의 문제점을 구체적으로 작성해주세요 -->

**예시:**
- PlayerService가 700줄 이상으로 비대함
- 도메인 로직이 Service에 산재되어 있음
- Entity가 Anemic Domain Model 상태
- 단위 테스트 작성이 어려움 (Service 의존성 과다)

## 🎯 개선 방향
<!-- 리팩터링 후 달성하고자 하는 상태를 작성해주세요 -->

**예시:**
- Player Entity에 비즈니스 로직 캡슐화
- Service는 트랜잭션 관리 및 조율만 담당
- 도메인 로직의 단위 테스트 용이성 확보
- CLAUDE.md의 Rich Domain Model 원칙 준수

## 📋 작업 내용
<!-- 리팩터링 작업을 단계별로 세분화하여 작성해주세요 -->

**예시:**
- [ ] Player Entity에 `changeBattingOrder()` 메서드 이동
- [ ] Player Entity에 `calculateBattingAverage()` 메서드 이동
- [ ] PlayerService의 도메인 로직 제거 (조율 로직만 남김)
- [ ] 단위 테스트 리팩터링
- [ ] PlayerService 통합 테스트 작성
- [ ] 커버리지 80% 이상 유지 확인

## 📌 Before / After
<!-- 코드 변경 전후를 보여주면 좋습니다 -->

**Before:**
```kotlin
// PlayerService.kt
fun changeBattingOrder(playerId: Long, newOrder: Int) {
    require(newOrder in 1..9) { "타순은 1-9 사이" }
    val player = playerRepository.findById(playerId)
    player.battingOrder = newOrder
    playerRepository.save(player)
}
```

**After:**
```kotlin
// Player.kt (Entity)
fun changeBattingOrder(newOrder: Int): Player {
    require(newOrder in 1..9) { "타순은 1-9 사이" }
    return copy(battingOrder = newOrder)
}

// PlayerService.kt
fun changeBattingOrder(playerId: Long, newOrder: Int) {
    val player = playerRepository.findById(playerId)
    val updated = player.changeBattingOrder(newOrder)
    playerRepository.save(updated)
}
```

## ⚠️ 주의사항
<!-- 리팩터링 시 주의할 점이 있다면 작성해주세요 -->

**예시:**
- 기존 테스트가 깨지지 않도록 주의
- CLAUDE.md 의존성 규칙 준수 필수
- 커버리지 80% 이상 유지
