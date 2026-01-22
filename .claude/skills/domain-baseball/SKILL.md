# Domain-Baseball Skill - 야구 규칙 지식 베이스

> **재사용 가능한 야구 도메인 지식**
> 다른 야구 관련 프로젝트에서도 이 Skill을 복사하여 즉시 활용 가능

---

## 📚 Skill 개요

### 목적
- 야구 규칙 지식을 체계화하여 제공
- 도메인 로직 검증을 위한 규칙 데이터베이스
- KBO, 사회인 야구 규정 기반 사실 확인

### 사용 시나리오
- ✅ nextup-core Entity 비즈니스 로직 작성 시
- ✅ 야구 기록 계산 로직 구현 시
- ✅ DH 규칙, 타격/투수 기록 검증 시

### 호출 방법
```
"domain-baseball Skill을 참조하여 DH 규칙 검증해줘"
"타율 계산이 야구 규칙에 맞는지 domain-baseball로 확인해줘"
```

---

## ⚾ 야구 규칙 지식

### 1. DH (지명타자) 규칙

#### 1.1 DH 지정 조건
```
조건:
- 경기 시작 전 선발 투수의 타격을 대신할 타자 지정
- 선발 라인업 카드 제출 시 명시
- 한 경기에 최대 1명

코드 예시:
fun canDesignateDH(pitcher: Pitcher, inning: Int): Boolean {
    return inning == 0  // 경기 시작 전만 가능
}
```

#### 1.2 DH 해제 조건
```
해제 시나리오:
1. DH가 수비 위치에 들어갈 때 (투수 포함 모든 포지션)
2. 투수가 타격 위치로 이동할 때
3. DH와 투수의 타순을 바꿀 때

결과:
- DH 해제 후 투수는 본인 타순에 타격
- 재지정 불가능

코드 예시:
fun releaseDH(dh: Player, newPosition: Position): Boolean {
    if (newPosition != null) {
        // DH가 수비 위치로 이동 → DH 해제
        return true
    }
    return false
}
```

#### 1.3 DH 교체 규칙
```
허용:
- DH를 다른 타자로 교체 가능 (DH 유지)
- 투수를 다른 투수로 교체 가능 (DH 유지)

금지:
- 해제된 DH 재지정

코드 예시:
fun replaceDH(currentDH: Player, newDH: Player, dhActive: Boolean): Boolean {
    if (!dhActive) {
        throw IllegalStateException("DH가 이미 해제됨")
    }
    return true  // 교체 가능
}
```

---

### 2. 타격 기록 규칙

#### 2.1 안타 (Hit)
```
정의:
- 타자가 쳐서 안전하게 1루 이상 진루
- 수비 실책이 아닌 경우

종류:
- 1루타 (Single)
- 2루타 (Double)
- 3루타 (Triple)
- 홈런 (Home Run)

코드 예시:
enum class HitType {
    SINGLE,   // 1루 안착
    DOUBLE,   // 2루 안착
    TRIPLE,   // 3루 안착
    HOMERUN   // 홈 도달
}

fun recordHit(bases: Int): HitType {
    return when (bases) {
        1 -> HitType.SINGLE
        2 -> HitType.DOUBLE
        3 -> HitType.TRIPLE
        4 -> HitType.HOMERUN
        else -> throw IllegalArgumentException("Invalid bases")
    }
}
```

#### 2.2 타점 (RBI - Run Batted In)
```
부여 조건:
- 타자의 타격으로 주자가 홈에 도달
- 희생타, 희생플라이 포함
- 병살타는 타점 인정

제외:
- 수비 실책으로 인한 득점
- 병살타로 3아웃 시 타점 불인정

코드 예시:
fun calculateRBI(hit: Hit, runnersScored: Int, isDoublePlay: Boolean, isError: Boolean): Int {
    if (isError) return 0
    if (isDoublePlay && hit.outs == 3) return 0
    return runnersScored
}
```

#### 2.3 타율 (Batting Average)
```
공식:
타율 = 안타 수 / 타수

타수 포함:
- 일반 타격
- 병살타

타수 제외:
- 볼넷 (Walk)
- 사구 (Hit by Pitch)
- 희생타 (Sacrifice Hit)
- 희생플라이 (Sacrifice Fly)
- 타격 방해 (Interference)

코드 예시:
data class BattingStats(
    val hits: Int,
    val atBats: Int  // 타수
) {
    fun calculateAverage(): Double {
        if (atBats == 0) return 0.000
        return (hits.toDouble() / atBats).round(3)
    }
}

fun isCountedAsAtBat(result: PlateAppearanceResult): Boolean {
    return when (result) {
        PlateAppearanceResult.HIT -> true
        PlateAppearanceResult.OUT -> true
        PlateAppearanceResult.WALK -> false          // 볼넷 제외
        PlateAppearanceResult.HIT_BY_PITCH -> false  // 사구 제외
        PlateAppearanceResult.SACRIFICE_HIT -> false // 희생타 제외
        PlateAppearanceResult.SACRIFICE_FLY -> false // 희생플라이 제외
    }
}
```

#### 2.4 출루율 (OBP - On-Base Percentage)
```
공식:
OBP = (안타 + 볼넷 + 사구) / (타수 + 볼넷 + 사구 + 희생플라이)

코드 예시:
data class OnBaseStats(
    val hits: Int,
    val walks: Int,
    val hitByPitch: Int,
    val atBats: Int,
    val sacrificeFlies: Int
) {
    fun calculateOBP(): Double {
        val numerator = hits + walks + hitByPitch
        val denominator = atBats + walks + hitByPitch + sacrificeFlies
        if (denominator == 0) return 0.000
        return (numerator.toDouble() / denominator).round(3)
    }
}
```

---

### 3. 투수 기록 규칙

#### 3.1 승리투수 (Winning Pitcher)
```
선발투수 자격:
- 5이닝 이상 투구
- 팀이 리드 중 교체
- 교체 후에도 리드 유지하여 승리

구원투수 자격:
- 1아웃 이상 기록
- 팀이 리드 중 또는 동점에서 투구
- 승리에 가장 기여한 투수 (기록원 판단)

코드 예시:
fun canBeWinningPitcher(pitcher: Pitcher, innings: Double, isStarter: Boolean, teamWinning: Boolean): Boolean {
    return if (isStarter) {
        innings >= 5.0 && teamWinning
    } else {
        innings >= 0.1  // 1아웃 이상
    }
}
```

#### 3.2 자책점 (ERA - Earned Run Average)
```
정의:
- 투수의 실책이 아닌 정상적인 플레이로 인한 실점

제외:
- 수비 실책으로 인한 실점
- 패스볼/폭투 제외 (자책점 포함)

공식:
ERA = (자책점 * 9) / 이닝

코드 예시:
data class PitchingStats(
    val earnedRuns: Int,  // 자책점
    val innings: Double
) {
    fun calculateERA(): Double {
        if (innings == 0.0) return 0.00
        return ((earnedRuns * 9.0) / innings).round(2)
    }
}

fun isEarnedRun(run: Run, causedByError: Boolean): Boolean {
    return !causedByError  // 실책이 아니면 자책점
}
```

#### 3.3 세이브 (Save)
```
조건:
1. 승리팀의 구원투수
2. 마무리 투수 (승리투수 아님)
3. 다음 중 하나:
   - 3점 이하 리드에서 1이닝 이상 투구
   - 동점 주자를 출루시키고 경기 종료까지 투구
   - 3이닝 이상 투구

코드 예시:
fun canEarnSave(
    pitcher: Pitcher,
    leadRuns: Int,
    inningsPitched: Double,
    isWinningPitcher: Boolean,
    teamWon: Boolean
): Boolean {
    if (!teamWon || isWinningPitcher) return false

    return when {
        inningsPitched >= 3.0 -> true                      // 3이닝 이상
        leadRuns <= 3 && inningsPitched >= 1.0 -> true     // 3점차 이하 1이닝 이상
        else -> false
    }
}
```

---

### 4. 수비 기록 규칙

#### 4.1 자살 (Putout)
```
정의:
- 수비수가 직접 아웃시킨 경우

예시:
- 땅볼을 잡아 1루수에게 송구 → 1루수 자살
- 플라이볼 포구 → 포구한 수비수 자살
- 삼진 → 포수 자살

코드 예시:
fun recordPutout(fielder: Fielder, outType: OutType): Putout {
    return Putout(
        fielderPosition = fielder.position,
        outType = outType
    )
}
```

#### 4.2 보살 (Assist)
```
정의:
- 아웃에 기여한 송구

예시:
- 유격수가 땅볼 잡아 1루 송구 → 유격수 보살

코드 예시:
fun recordAssist(fielders: List<Fielder>): List<Assist> {
    // 자살한 수비수를 제외한 모든 수비수에게 보살
    return fielders.dropLast(1).map { Assist(it.position) }
}
```

#### 4.3 실책 (Error)
```
정의:
- 정상적인 플레이로 아웃시킬 수 있었으나 실수로 실패

종류:
- 포구 실책
- 송구 실책
- 방해 (Interference)

영향:
- 타자에게 안타 기록 X
- 자책점 계산 시 제외

코드 예시:
enum class ErrorType {
    FIELDING,   // 포구 실책
    THROWING,   // 송구 실책
    INTERFERENCE
}

fun recordError(fielder: Fielder, errorType: ErrorType): Error {
    return Error(
        fielderPosition = fielder.position,
        type = errorType
    )
}
```

---

### 5. 특수 상황 규칙

#### 5.1 인필드 플라이 (Infield Fly)
```
조건:
- 0아웃 또는 1아웃
- 주자가 1, 2루 또는 만루
- 내야수가 보통의 수비로 포구 가능한 플라이볼

선언:
- 심판이 "인필드 플라이" 선언
- 타자 즉시 아웃
- 주자는 진루 의무 없음 (태그 후 진루 가능)

코드 예시:
fun isInfieldFly(
    outs: Int,
    runnersOn: List<Base>,
    isFairFly: Boolean,
    canBeRoutinelyCaught: Boolean
): Boolean {
    if (outs >= 2) return false
    if (!isFairFly || !canBeRoutinelyCaught) return false

    val hasRunnerOnFirst = runnersOn.contains(Base.FIRST)
    val hasRunnerOnSecond = runnersOn.contains(Base.SECOND)

    return hasRunnerOnFirst && hasRunnerOnSecond  // 1, 2루 또는 만루
}
```

#### 5.2 보크 (Balk)
```
정의:
- 투수의 부정 투구

주요 보크 상황:
- 세트 포지션에서 정지하지 않고 투구
- 1루에 견제구 시 발을 1루 방향으로 안 뺌
- 투구 동작 중 중단

결과:
- 모든 주자 1개 루 진루
- 주자 없으면 볼 카운트

코드 예시:
fun applyBalk(runners: List<Runner>): List<Runner> {
    return runners.map { runner ->
        runner.advanceBase(1)  // 모든 주자 1루씩 진루
    }
}
```

---

## 🔍 검증 체크리스트

### DH 규칙 검증
```kotlin
- [ ] DH 지정 시점이 경기 시작 전인가?
- [ ] DH 해제 조건 (수비 위치 이동)이 명확한가?
- [ ] DH 해제 후 재지정 방지되어 있는가?
```

### 타격 기록 검증
```kotlin
- [ ] 타율 계산 시 타수에 볼넷/희생타 제외했는가?
- [ ] 타점 계산 시 실책 득점 제외했는가?
- [ ] 안타 종류가 진루한 루수와 일치하는가?
```

### 투수 기록 검증
```kotlin
- [ ] 승리투수 자격 (선발 5이닝, 구원 1아웃)이 맞는가?
- [ ] 자책점 계산 시 실책 실점 제외했는가?
- [ ] 세이브 조건 (3점차 이하 1이닝)이 정확한가?
```

---

## 📋 출력 포맷 (검증 보고서)

```markdown
# 야구 규칙 검증 보고서

## 검증 대상
- Entity: `Player`
- 메서드: `changeBattingOrder()`
- 요청: DH 규칙 준수 여부

## 판정: ✅ PASS / ❌ REJECT

## 검증 상세

### DH 지정 시점
**규칙**: 경기 시작 전 선발 라인업 제출 시
**구현**:
\`\`\`kotlin
fun canDesignateDH(inning: Int): Boolean {
    return inning == 0
}
\`\`\`
**판정**: ✅ PASS

### DH 해제 조건
**규칙**: DH가 수비 위치에 들어갈 때
**구현**:
\`\`\`kotlin
fun releaseDH(newPosition: Position?): Boolean {
    return newPosition != null
}
\`\`\`
**판정**: ✅ PASS

## 종합 판정
✅ 모든 DH 규칙 준수
```

---

## 🎯 이 Skill의 장점

1. **재사용성**: 다른 야구 프로젝트에도 즉시 적용 가능
2. **정확성**: KBO 공식 규칙 기반 팩트 체크
3. **효율성**: 규칙 찾는 시간 단축
4. **일관성**: 모든 Agent가 동일한 규칙 기준 사용

---

## 📚 참고 자료

- KBO 공식 야구 규칙
- 사회인 야구 리그 규정
- `.claude/knowledge/rules/` (PDF 규칙집)
