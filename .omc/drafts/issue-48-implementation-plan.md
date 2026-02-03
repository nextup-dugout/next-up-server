# Issue #48: Phase 1 실시간 기록 MVP - 구현 계획서

> 작성일: 2026-02-03
> 상태: Draft
> 참조: `.omc/plans/roadmap-2026-02.md`

---

## 개요

**목표**: 기록원이 경기 현장에서 타석 결과를 입력하면, 실시간으로 중계되고 통계가 자동 계산되는 핵심 파이프라인 구축

**핵심 흐름**:
```
감독 → 라인업 입력 → 기록원에게 제출 → 기록원 확인 → 경기 시작
                                                    ↓
                                              타석 결과 입력
                                                    ↓
                              ┌─────────────────────┼─────────────────────┐
                              ↓                     ↓                     ↓
                        이벤트 로그            박스스코어              개인 기록
                           저장               자동 갱신               자동 갱신
                              ↓                     ↓                     ↓
                              └─────────────────────┼─────────────────────┘
                                                    ↓
                                          WebSocket 브로드캐스트
                                                    ↓
                                             실시간 중계 화면
```

---

## 작업 순서 (의존성 기반)

```
1. GameEvent Entity & 이벤트 타입
   ↓
2. GameState (경기 실시간 상태)
   ↓
3. 라인업 제출 워크플로우
   ↓
4. 기록원 이벤트 입력 API
   ↓
5. WebSocket 실시간 브로드캐스트
   ↓
6. 박스스코어 자동 계산
```

---

## 1. GameEvent Entity 및 이벤트 타입 설계

### 1.1 새로 생성할 파일

#### nextup-core (도메인)

| 파일 | 설명 |
|------|------|
| `domain/game/GameEvent.kt` | 이벤트 로그 Entity |
| `domain/game/GameEventType.kt` | 이벤트 대분류 enum |
| `domain/game/BaseRunningEvent.kt` | 주루 이벤트 enum |
| `domain/game/SubstitutionType.kt` | 교체 유형 enum |
| `port/repository/GameEventRepositoryPort.kt` | Repository Port |

#### nextup-infrastructure

| 파일 | 설명 |
|------|------|
| `repository/GameEventRepository.kt` | JPA Repository 구현체 |

### 1.2 수정할 파일

| 파일 | 수정 내용 |
|------|----------|
| `domain/game/PlateAppearanceResult.kt` | 번트안타, 내야안타, 낫아웃 등 추가 |

### 1.3 상세 설계

#### GameEventType enum
```kotlin
enum class GameEventType {
    PLATE_APPEARANCE,    // 타석 결과
    BASE_RUNNING,        // 주루 이벤트 (도루, 보크 등)
    SUBSTITUTION,        // 선수 교체
    PITCHING_CHANGE,     // 투수 교체
    DEFENSIVE_CHANGE,    // 수비 교체
    INNING_START,        // 이닝 시작
    INNING_END,          // 이닝 종료
    GAME_START,          // 경기 시작
    GAME_END,            // 경기 종료
    TIMEOUT,             // 타임아웃
    REVIEW,              // 비디오 판독
    INJURY,              // 부상
    OTHER                // 기타
}
```

#### BaseRunningEvent enum
```kotlin
enum class BaseRunningEvent {
    STOLEN_BASE,         // 도루
    CAUGHT_STEALING,     // 도루 실패
    WILD_PITCH,          // 폭투
    PASSED_BALL,         // 포일
    BALK,                // 보크
    PICKOFF,             // 견제사
    ADVANCE_ON_THROW,    // 송구 사이 진루
    ADVANCE_ON_ERROR,    // 실책으로 진루
    ADVANCE_ON_FLYOUT,   // 태그업 진루
    OUT_ON_BASES         // 주루사
}
```

#### GameEvent Entity
```kotlin
@Entity
@Table(name = "game_events")
class GameEvent(
    @ManyToOne(fetch = FetchType.LAZY)
    val game: Game,

    val inning: Int,
    val isTopInning: Boolean,
    val outCountBefore: Int,
    val outCountAfter: Int,

    @Enumerated(EnumType.STRING)
    val eventType: GameEventType,

    val description: String,  // "우전 안타, 2루 주자 홈인"

    @ManyToOne(fetch = FetchType.LAZY)
    val batter: GamePlayer?,

    @ManyToOne(fetch = FetchType.LAZY)
    val pitcher: GamePlayer?,

    @ManyToOne(fetch = FetchType.LAZY)
    val involvedRunner: GamePlayer?,  // 주루 이벤트 시 해당 주자

    // 주자 상황 (JSON)
    @Column(columnDefinition = "TEXT")
    val runnersBefore: String,  // {"first": 5, "second": null, "third": 12}

    @Column(columnDefinition = "TEXT")
    val runnersAfter: String,

    @Enumerated(EnumType.STRING)
    val plateAppearanceResult: PlateAppearanceResult?,

    @Enumerated(EnumType.STRING)
    val baseRunningEvent: BaseRunningEvent?,

    val runsScored: Int = 0,
    val rbis: Int = 0,
    val isEarnedRun: Boolean = true,

    val eventOrder: Int,  // 경기 내 이벤트 순서

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity()
```

### 1.4 테스트

| 테스트 파일 | 커버리지 |
|------------|---------|
| `GameEventTest.kt` | Entity 생성, 유효성 검증 |

---

## 2. 경기 실시간 상태 관리 (GameState)

### 2.1 새로 생성할 파일

#### nextup-core

| 파일 | 설명 |
|------|------|
| `domain/game/GameState.kt` | 경기 실시간 상태 Embeddable |
| `domain/game/BaseOccupancy.kt` | 주자 상황 Value Object |

### 2.2 수정할 파일

| 파일 | 수정 내용 |
|------|----------|
| `domain/game/Game.kt` | GameState embedded 추가, 상태 변경 메서드 |

### 2.3 상세 설계

#### GameState (Embeddable)
```kotlin
@Embeddable
class GameState(
    var outs: Int = 0,              // 현재 아웃 카운트 (0-2)
    var balls: Int = 0,             // 볼 카운트 (0-3)
    var strikes: Int = 0,           // 스트라이크 카운트 (0-2)

    var runnerOnFirst: Long? = null,   // 1루 주자 GamePlayer ID
    var runnerOnSecond: Long? = null,  // 2루 주자 GamePlayer ID
    var runnerOnThird: Long? = null,   // 3루 주자 GamePlayer ID

    var homeBattingOrder: Int = 1,     // 홈팀 현재 타순 (1-9)
    var awayBattingOrder: Int = 1,     // 원정팀 현재 타순 (1-9)

    var currentPitcherId: Long? = null,  // 현재 투수 ID
    var currentBatterId: Long? = null    // 현재 타자 ID
)
```

#### Game Entity 수정
```kotlin
@Entity
class Game(
    // ... 기존 필드

    @Embedded
    var state: GameState = GameState()
) {
    // 상태 변경 메서드
    fun recordOut()
    fun advanceRunner(from: Base, to: Base)
    fun clearBases()
    fun nextBatter()
    fun resetCount()
    // ...
}
```

### 2.4 테스트

| 테스트 파일 | 커버리지 |
|------------|---------|
| `GameStateTest.kt` | 아웃 카운트, 주자 이동, 타순 진행 |
| `GameTest.kt` (수정) | 상태 변경 메서드 테스트 추가 |

---

## 3. 라인업 제출 워크플로우

### 3.1 새로 생성할 파일

#### nextup-core

| 파일 | 설명 |
|------|------|
| `domain/game/LineupSubmission.kt` | 라인업 제출 Entity |
| `domain/game/LineupSubmissionStatus.kt` | 제출 상태 enum |
| `domain/game/LineupEntry.kt` | 라인업 엔트리 (선수/타순/포지션) |
| `port/repository/LineupSubmissionRepositoryPort.kt` | Repository Port |
| `service/game/LineupService.kt` | 라인업 Service |

#### nextup-infrastructure

| 파일 | 설명 |
|------|------|
| `repository/LineupSubmissionRepository.kt` | JPA Repository |

#### nextup-scorer (API)

| 파일 | 설명 |
|------|------|
| `dto/lineup/LineupRequest.kt` | 라인업 요청 DTO |
| `dto/lineup/LineupResponse.kt` | 라인업 응답 DTO |
| `controller/lineup/LineupController.kt` | 라인업 API |

### 3.2 상세 설계

#### LineupSubmissionStatus enum
```kotlin
enum class LineupSubmissionStatus {
    DRAFT,      // 작성 중
    SUBMITTED,  // 기록원에게 제출됨
    CONFIRMED,  // 기록원 확인 완료
    REJECTED    // 기록원 반려
}
```

#### LineupSubmission Entity
```kotlin
@Entity
@Table(name = "lineup_submissions")
class LineupSubmission(
    @ManyToOne(fetch = FetchType.LAZY)
    val game: Game,

    @ManyToOne(fetch = FetchType.LAZY)
    val gameTeam: GameTeam,

    @ManyToOne(fetch = FetchType.LAZY)
    val submittedBy: User,  // 감독

    @Enumerated(EnumType.STRING)
    var status: LineupSubmissionStatus = LineupSubmissionStatus.DRAFT,

    @OneToMany(mappedBy = "submission", cascade = [CascadeType.ALL])
    val entries: MutableList<LineupEntry> = mutableListOf(),

    var useDH: Boolean = true,  // DH 사용 여부

    var submittedAt: Instant? = null,
    var confirmedAt: Instant? = null,
    var rejectedReason: String? = null,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {
    fun submit()
    fun confirm()
    fun reject(reason: String)
    fun toGamePlayers(): List<GamePlayer>
}
```

#### LineupEntry Entity
```kotlin
@Entity
@Table(name = "lineup_entries")
class LineupEntry(
    @ManyToOne(fetch = FetchType.LAZY)
    val submission: LineupSubmission,

    @ManyToOne(fetch = FetchType.LAZY)
    val player: Player,

    val battingOrder: Int?,  // null = 벤치

    @Enumerated(EnumType.STRING)
    val position: Position,

    val backNumber: Int?,

    val isStarter: Boolean = true,

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
)
```

### 3.3 API 설계

| Method | Endpoint | 설명 | 권한 |
|--------|----------|------|------|
| POST | `/api/scorer/games/{gameId}/lineups` | 라인업 초안 생성 | 감독 |
| PUT | `/api/scorer/games/{gameId}/lineups/{id}` | 라인업 수정 | 감독 |
| POST | `/api/scorer/games/{gameId}/lineups/{id}/submit` | 기록원에게 제출 | 감독 |
| POST | `/api/scorer/games/{gameId}/lineups/{id}/confirm` | 라인업 확정 | 기록원 |
| POST | `/api/scorer/games/{gameId}/lineups/{id}/reject` | 라인업 반려 | 기록원 |
| GET | `/api/scorer/games/{gameId}/lineups` | 라인업 목록 조회 | 기록원, 감독 |

### 3.4 테스트

| 테스트 파일 | 커버리지 |
|------------|---------|
| `LineupSubmissionTest.kt` | Entity 상태 변경 |
| `LineupServiceTest.kt` | Service TDD |
| `LineupControllerTest.kt` | API 통합 테스트 |

---

## 4. 기록원 이벤트 입력 API

### 4.1 새로 생성할 파일

#### nextup-core

| 파일 | 설명 |
|------|------|
| `service/game/GameEventService.kt` | 이벤트 처리 Service |
| `service/game/GameProgressService.kt` | 경기 진행 Service |

#### nextup-scorer

| 파일 | 설명 |
|------|------|
| `dto/event/GameEventRequest.kt` | 이벤트 입력 DTO |
| `dto/event/GameEventResponse.kt` | 이벤트 응답 DTO |
| `dto/game/GameStateResponse.kt` | 경기 상태 DTO |
| `controller/game/GameScorerController.kt` | 기록원 API |

### 4.2 API 설계

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/scorer/games/{gameId}/start` | 경기 시작 |
| POST | `/api/scorer/games/{gameId}/events/plate-appearance` | 타석 결과 입력 |
| POST | `/api/scorer/games/{gameId}/events/base-running` | 주루 이벤트 입력 |
| POST | `/api/scorer/games/{gameId}/events/substitution` | 선수 교체 |
| POST | `/api/scorer/games/{gameId}/half-inning` | 이닝 전환 |
| POST | `/api/scorer/games/{gameId}/end` | 경기 종료 |
| GET | `/api/scorer/games/{gameId}/state` | 현재 상태 조회 |
| GET | `/api/scorer/games/{gameId}/events` | 이벤트 목록 조회 |
| DELETE | `/api/scorer/games/{gameId}/events/{eventId}` | 이벤트 취소 (되돌리기) |

### 4.3 상세 Request DTO

#### PlateAppearanceRequest
```kotlin
data class PlateAppearanceRequest(
    val result: PlateAppearanceResult,
    val batterId: Long,
    val pitcherId: Long,
    val rbis: Int = 0,
    val runnerMovements: List<RunnerMovement> = emptyList(),
    val description: String? = null
)

data class RunnerMovement(
    val runnerId: Long,
    val fromBase: Base?,  // null = 타자
    val toBase: Base?,    // null = 홈 or 아웃
    val isOut: Boolean = false,
    val isScored: Boolean = false,
    val isEarnedRun: Boolean = true
)

enum class Base { FIRST, SECOND, THIRD }
```

### 4.4 테스트

| 테스트 파일 | 커버리지 |
|------------|---------|
| `GameEventServiceTest.kt` | 이벤트 처리 로직 TDD |
| `GameProgressServiceTest.kt` | 경기 진행 로직 TDD |
| `GameScorerControllerTest.kt` | API 통합 테스트 |

---

## 5. WebSocket 실시간 브로드캐스트

### 5.1 새로 생성할 파일

#### nextup-scorer

| 파일 | 설명 |
|------|------|
| `websocket/GameBroadcastService.kt` | 브로드캐스트 Service |
| `websocket/message/GameEventMessage.kt` | 이벤트 메시지 DTO |
| `websocket/message/ScoreboardMessage.kt` | 스코어보드 메시지 DTO |
| `websocket/message/GameStateMessage.kt` | 상태 메시지 DTO |

### 5.2 수정할 파일

| 파일 | 수정 내용 |
|------|----------|
| `config/WebSocketConfig.kt` | 인증 핸들러 추가 (선택) |

### 5.3 토픽 설계

| Topic | 설명 | 전송 시점 |
|-------|------|----------|
| `/topic/games/{gameId}/events` | 이벤트 발생 | 모든 이벤트 입력 시 |
| `/topic/games/{gameId}/scoreboard` | 스코어보드 | 점수 변경 시 |
| `/topic/games/{gameId}/state` | 경기 상태 | 상태 변경 시 |
| `/topic/games/{gameId}/lineup` | 라인업 | 라인업 확정/변경 시 |

### 5.4 브로드캐스트 연동

```kotlin
@Service
class GameBroadcastService(
    private val messagingTemplate: SimpMessagingTemplate
) {
    fun broadcastEvent(gameId: Long, event: GameEventMessage) {
        messagingTemplate.convertAndSend(
            "/topic/games/$gameId/events",
            event
        )
    }

    fun broadcastScoreboard(gameId: Long, scoreboard: ScoreboardMessage) {
        messagingTemplate.convertAndSend(
            "/topic/games/$gameId/scoreboard",
            scoreboard
        )
    }

    fun broadcastState(gameId: Long, state: GameStateMessage) {
        messagingTemplate.convertAndSend(
            "/topic/games/$gameId/state",
            state
        )
    }
}
```

### 5.5 테스트

| 테스트 파일 | 커버리지 |
|------------|---------|
| `GameBroadcastServiceTest.kt` | 메시지 전송 검증 |
| WebSocket 통합 테스트 (선택) | 실제 연결 테스트 |

---

## 6. 박스스코어 자동 계산

### 6.1 새로 생성할 파일

#### nextup-core

| 파일 | 설명 |
|------|------|
| `service/game/BoxScoreService.kt` | 박스스코어 계산 Service |

#### nextup-scorer

| 파일 | 설명 |
|------|------|
| `dto/boxscore/BoxScoreResponse.kt` | 박스스코어 응답 DTO |
| `controller/game/BoxScoreController.kt` | 박스스코어 API |

### 6.2 박스스코어 구성

```kotlin
data class BoxScoreResponse(
    val gameId: Long,
    val homeTeam: TeamBoxScore,
    val awayTeam: TeamBoxScore,
    val inningScores: List<InningScore>,
    val currentInning: String,  // "5회말"
    val gameStatus: GameStatus
)

data class TeamBoxScore(
    val teamId: Long,
    val teamName: String,
    val runs: Int,
    val hits: Int,
    val errors: Int,
    val batters: List<BatterBoxScore>,
    val pitchers: List<PitcherBoxScore>
)

data class BatterBoxScore(
    val playerId: Long,
    val playerName: String,
    val position: String,
    val battingOrder: Int?,
    val atBats: Int,
    val runs: Int,
    val hits: Int,
    val rbis: Int,
    val walks: Int,
    val strikeouts: Int,
    val avg: String  // ".333"
)

data class PitcherBoxScore(
    val playerId: Long,
    val playerName: String,
    val inningsPitched: String,  // "5.2"
    val hits: Int,
    val runs: Int,
    val earnedRuns: Int,
    val walks: Int,
    val strikeouts: Int,
    val decision: String?  // "W", "L", "S", "H", null
)
```

### 6.3 자동 갱신 로직

이벤트 입력 시 자동 갱신:
1. **타석 결과** → BattingRecord, PitchingRecord 갱신
2. **득점** → GameTeam.totalScore 갱신, 이닝별 점수 기록
3. **안타** → GameTeam.totalHits 갱신
4. **실책** → GameTeam.totalErrors 갱신

### 6.4 API 설계

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/scorer/games/{gameId}/boxscore` | 박스스코어 조회 |
| GET | `/api/games/{gameId}/boxscore` | 공개 박스스코어 조회 (api 모듈) |

### 6.5 테스트

| 테스트 파일 | 커버리지 |
|------------|---------|
| `BoxScoreServiceTest.kt` | 박스스코어 계산 로직 TDD |

---

## 파일 생성 요약

### nextup-core (16개)

**신규 생성:**
1. `domain/game/GameEvent.kt`
2. `domain/game/GameEventType.kt`
3. `domain/game/BaseRunningEvent.kt`
4. `domain/game/SubstitutionType.kt`
5. `domain/game/GameState.kt`
6. `domain/game/BaseOccupancy.kt`
7. `domain/game/LineupSubmission.kt`
8. `domain/game/LineupSubmissionStatus.kt`
9. `domain/game/LineupEntry.kt`
10. `port/repository/GameEventRepositoryPort.kt`
11. `port/repository/LineupSubmissionRepositoryPort.kt`
12. `service/game/LineupService.kt`
13. `service/game/GameEventService.kt`
14. `service/game/GameProgressService.kt`
15. `service/game/BoxScoreService.kt`

**수정:**
16. `domain/game/Game.kt` - GameState embedded 추가
17. `domain/game/PlateAppearanceResult.kt` - 추가 결과 타입

### nextup-infrastructure (2개)

1. `repository/GameEventRepository.kt`
2. `repository/LineupSubmissionRepository.kt`

### nextup-scorer (14개)

**DTO:**
1. `dto/lineup/LineupRequest.kt`
2. `dto/lineup/LineupResponse.kt`
3. `dto/event/GameEventRequest.kt`
4. `dto/event/GameEventResponse.kt`
5. `dto/game/GameStateResponse.kt`
6. `dto/boxscore/BoxScoreResponse.kt`

**Controller:**
7. `controller/lineup/LineupController.kt`
8. `controller/game/GameScorerController.kt`
9. `controller/game/BoxScoreController.kt`

**WebSocket:**
10. `websocket/GameBroadcastService.kt`
11. `websocket/message/GameEventMessage.kt`
12. `websocket/message/ScoreboardMessage.kt`
13. `websocket/message/GameStateMessage.kt`

**Config (수정):**
14. `config/WebSocketConfig.kt`

### 테스트 (12개)

1. `GameEventTest.kt`
2. `GameStateTest.kt`
3. `LineupSubmissionTest.kt`
4. `LineupServiceTest.kt`
5. `GameEventServiceTest.kt`
6. `GameProgressServiceTest.kt`
7. `BoxScoreServiceTest.kt`
8. `LineupControllerTest.kt`
9. `GameScorerControllerTest.kt`
10. `BoxScoreControllerTest.kt`
11. `GameBroadcastServiceTest.kt`
12. `GameTest.kt` (수정)

---

## 예상 일정

| 작업 | 예상 파일 수 |
|------|-------------|
| 1. GameEvent Entity & 이벤트 타입 | ~7개 |
| 2. GameState | ~4개 |
| 3. 라인업 워크플로우 | ~10개 |
| 4. 기록원 이벤트 입력 API | ~8개 |
| 5. WebSocket 브로드캐스트 | ~5개 |
| 6. 박스스코어 자동 계산 | ~4개 |

**총 예상**: ~38개 파일 (신규 + 수정 + 테스트)

---

## 의존성 체크리스트

- [ ] Core → Common only
- [ ] Infrastructure → Core, Common only
- [ ] Scorer → Infrastructure, Core, Common
- [ ] 순환 참조 없음
- [ ] Zero Entity Leak (모든 API는 DTO 반환)

---

## 다음 단계

1. **계획 승인** 받기
2. **1번 작업 시작**: GameEvent Entity 및 이벤트 타입 설계
3. TDD 원칙 준수하여 테스트 먼저 작성
