# 경기(Game) 생성 플로우 정의

> **Issue**: #253
> **작성 기준**: 코드베이스 분석 기반 (추측 없음)
> **최종 수정**: 2026-03-08

---

## 목차

1. [개요](#1-개요)
2. [핵심 발견: Game 직접 생성 API 부재](#2-핵심-발견-game-직접-생성-api-부재)
3. [경로 1: 리그 대진표(LeagueSchedule) 기반 경기 생성](#3-경로-1-리그-대진표leagueschedule-기반-경기-생성)
4. [경로 2: 친선 경기(MatchRequest) 매칭](#4-경로-2-친선-경기matchrequest-매칭)
5. [경로 3: 토너먼트 대진표(BracketEntry) 기반](#5-경로-3-토너먼트-대진표bracketentry-기반)
6. [Game 엔티티 구조](#6-game-엔티티-구조)
7. [경기 생명주기 (Lifecycle)](#7-경기-생명주기-lifecycle)
8. [프론트엔드 개발자 가이드](#8-프론트엔드-개발자-가이드)
9. [현재 미구현 사항](#9-현재-미구현-사항)

---

## 1. 개요

NEXT-UP 시스템에서 **Game 엔티티를 직접 생성하는 REST API 엔드포인트는 현재 존재하지 않는다.**

Game은 반드시 상위 개념(대진표, 매칭 등)을 통해 간접적으로 생성되어야 하는 구조로 설계되어 있다. 현재 코드베이스에서 Game 객체를 `new Game(...)` 으로 생성하는 프로덕션 코드(테스트 제외)는 존재하지 않으며, `LeagueSchedule.linkGame(game)` 메서드가 대진표와 경기를 연결하는 유일한 브릿지 역할을 한다.

### 관련 모듈 및 역할

| 모듈 | Game 관련 역할 |
|------|---------------|
| `nextup-core` | Game 엔티티, GameTeam, LeagueSchedule, MatchRequest/Response 도메인 모델 |
| `nextup-infrastructure` | GameLifecycleServiceImpl (시작/종료/취소), GameScheduleServiceImpl (조회) |
| `nextup-backoffice` | ScheduleAdminController (대진표 CRUD), CompetitionAdminController (대회 관리) |
| `nextup-scorer` | GameScorerController (경기 진행/종료/몰수/취소 - 기록원 전용) |
| `nextup-api` | GameScheduleController (경기 조회 - 일반 사용자, 읽기 전용) |

---

## 2. 핵심 발견: Game 직접 생성 API 부재

### 분석 결과

코드베이스 전체를 탐색한 결과, 다음 사실을 확인했다:

| 항목 | 상태 |
|------|------|
| `Game.create()` 팩토리 메서드 | **존재하지 않음** (public 생성자만 존재) |
| `GameLifecycleService.createGame()` | **존재하지 않음** (startGame, endGame, forfeitGame, cancelGame만 존재) |
| backoffice의 Game 생성 API | **존재하지 않음** (ScheduleAdminController는 LeagueSchedule만 생성) |
| scorer의 Game 생성 API | **존재하지 않음** (GameScorerController는 이미 존재하는 Game을 조작) |
| api의 Game 생성 API | **존재하지 않음** (GameScheduleController는 조회 전용) |
| infrastructure의 Game 생성 서비스 | **존재하지 않음** |

### Game 엔티티의 현재 생성자

```kotlin
// nextup-core/.../domain/game/Game.kt
class Game(
    val competition: Competition,   // 필수: 소속 대회
    var scheduledAt: LocalDateTime,  // 필수: 경기 예정 시간
    var location: String? = null,
    var fieldName: String? = null,
    var gameNumber: Int? = null,
    var status: GameStatus = GameStatus.SCHEDULED,
    var currentInning: Int = 0,
    var isTopInning: Boolean = true,
    var totalInnings: Int = 9,
    // ... 기타 필드
) : BaseTimeEntity()
```

Game은 `companion object.create()` 팩토리 패턴 없이 **public 생성자**를 사용한다. 이는 프로젝트의 Rich Domain Model 규칙(`private constructor` + `companion object.create()`)을 아직 적용하지 않은 상태이다.

---

## 3. 경로 1: 리그 대진표(LeagueSchedule) 기반 경기 생성

### 개요

리그전(라운드 로빈) 대회에서 관리자가 대진표를 생성한 후, 대진표에 Game을 연결하는 방식이다. 현재 **LeagueSchedule 생성까지만 구현**되어 있고, **Schedule -> Game 변환 서비스는 미구현** 상태이다.

### 현재 구현된 플로우

```
[backoffice 관리자]
       |
       v
(1) 대회(Competition) 생성
       |  POST /api/backoffice/competitions
       |  → CompetitionAdminController.createCompetition()
       |  → CompetitionService.create()
       v
(2) 대진표(LeagueSchedule) 생성 (수동 또는 자동)
       |
       |  [수동 생성]
       |  POST /api/backoffice/competitions/{id}/schedule
       |  → ScheduleAdminController.createSchedule()
       |  → LeagueScheduleService.createSchedule()
       |  → LeagueSchedule.create() 팩토리 메서드
       |
       |  [자동 생성 - 라운드 로빈]
       |  POST /api/backoffice/competitions/{id}/schedule/generate
       |  → ScheduleAdminController.generateSchedule()
       |  → LeagueScheduleService.generateRoundRobinSchedule()
       |  → RoundRobinScheduleGenerator.generate()
       |  → LeagueSchedule.create() 반복
       v
(3) ⚠️ Game 생성 및 연결 (미구현)
       |  LeagueSchedule.linkGame(game) 메서드는 존재하지만,
       |  이를 호출하는 서비스/API가 아직 없음
       v
(4) 기록원이 경기 시작
       POST /api/scorer/games/{gameId}/start
       → GameScorerController.startGame()
       → GameLifecycleService.startGame()
```

### LeagueSchedule.linkGame() 메서드

```kotlin
// nextup-core/.../domain/schedule/LeagueSchedule.kt
fun linkGame(game: Game) {
    require(status == ScheduleStatus.SCHEDULED || status == ScheduleStatus.POSTPONED) {
        "예정 또는 연기 상태의 대진표만 경기를 연결할 수 있습니다."
    }
    this.game = game
    this.status = ScheduleStatus.GAME_CREATED
}
```

이 메서드는:
- `SCHEDULED` 또는 `POSTPONED` 상태의 대진표에만 Game을 연결할 수 있다.
- 연결 후 대진표 상태가 `GAME_CREATED`로 변경된다.
- **현재 이 메서드를 호출하는 프로덕션 서비스 코드가 없다** (테스트에서만 사용).

### 대진표 상태 전이

```
SCHEDULED ──linkGame()──→ GAME_CREATED ──complete()──→ COMPLETED
    |
    ├──postpone()──→ POSTPONED ──linkGame()──→ GAME_CREATED
    |                    |
    └──cancel()────→ CANCELLED ←──cancel()────┘
```

### 관련 API 엔드포인트 (backoffice)

| HTTP 메서드 | URL | 설명 |
|------------|-----|------|
| `GET` | `/api/backoffice/competitions/{competitionId}/schedule` | 대진표 목록 조회 |
| `POST` | `/api/backoffice/competitions/{competitionId}/schedule` | 대진표 수동 생성 |
| `POST` | `/api/backoffice/competitions/{competitionId}/schedule/generate` | 라운드 로빈 자동 생성 |
| `POST` | `/api/backoffice/competitions/{competitionId}/schedule/validate` | 대진표 검증 (dry-run) |
| `PUT` | `/api/backoffice/competitions/{competitionId}/schedule/{id}` | 대진표 수정 |
| `DELETE` | `/api/backoffice/competitions/{competitionId}/schedule/{id}` | 대진표 삭제 |
| `POST` | `/api/backoffice/competitions/{competitionId}/schedule/postpone-bulk` | 일괄 연기 (우천) |
| `PUT` | `/api/backoffice/competitions/{competitionId}/schedule/{id}/reschedule` | 연기 경기 재조정 |

### 관련 엔티티

| 엔티티 | 파일 경로 | 역할 |
|--------|----------|------|
| `Competition` | `nextup-core/.../domain/competition/Competition.kt` | 대회 (Game의 상위 엔티티) |
| `GameRules` | `nextup-core/.../domain/competition/GameRules.kt` | 대회별 경기 규칙 (이닝수, 머시룰 등) |
| `LeagueSchedule` | `nextup-core/.../domain/schedule/LeagueSchedule.kt` | 리그 대진표 (Game과 1:1 연결) |
| `Game` | `nextup-core/.../domain/game/Game.kt` | 경기 |
| `GameTeam` | `nextup-core/.../domain/game/GameTeam.kt` | 경기 참여 팀 (홈/원정) |

---

## 4. 경로 2: 친선 경기(MatchRequest) 매칭

### 개요

팀 간 연습/친선 경기 매칭 시스템이다. 현재 **매칭 요청/응답/수락까지만 구현**되어 있고, **매칭 성사 후 Game 생성은 미구현** 상태이다.

### 현재 구현된 플로우

```
[팀 A - 매칭 요청]
       |
       v
(1) 매칭 요청 생성
       |  MatchingService.createRequest()
       |  → MatchRequest.create(team, date, time, location, message, skillLevel)
       |  → 상태: OPEN
       v
(2) 팀 B가 응답
       |  MatchingService.respondToRequest()
       |  → MatchResponse.create(matchRequest, respondTeam, message)
       |  → 응답 상태: PENDING
       v
(3) 팀 A가 응답 수락
       |  MatchingService.acceptResponse(requestId, responseId)
       |  → matchResponse.accept()  → 응답 상태: ACCEPTED
       |  → matchRequest.match()    → 요청 상태: MATCHED
       v
(4) ⚠️ Game 생성 (미구현)
       |  매칭 성사 후 Game을 생성하는 로직이 없음
       |  친선 경기는 Competition 소속이 아닐 수 있어 별도 처리 필요
       v
(5) 경기 진행 (Game 생성 후)
```

### MatchRequest 상태 전이

```
OPEN ──match()──→ MATCHED
  |
  ├──cancel()──→ CANCELLED
  |
  └──expire()──→ EXPIRED (생성 후 30일 경과)
```

### MatchResponse 상태 전이

```
PENDING ──accept()──→ ACCEPTED
    |
    └──reject()──→ REJECTED
```

### 관련 엔티티

| 엔티티 | 파일 경로 | 역할 |
|--------|----------|------|
| `MatchRequest` | `nextup-core/.../domain/match/MatchRequest.kt` | 매칭 요청 (팀, 선호 날짜/장소, 실력대) |
| `MatchResponse` | `nextup-core/.../domain/match/MatchResponse.kt` | 매칭 응답 (응답 팀, 메시지) |
| `SkillLevel` | `nextup-core/.../domain/match/SkillLevel.kt` | 실력 수준 |

### 주요 서비스

| 서비스 | 파일 경로 | 역할 |
|--------|----------|------|
| `MatchingService` | `nextup-core/.../service/match/MatchingService.kt` | 매칭 요청/응답/수락/취소 조율 |

---

## 5. 경로 3: 토너먼트 대진표(BracketEntry) 기반

### 개요

토너먼트(단판/더블 엘리미네이션) 대회의 대진표 시스템이다. 현재 **대진표 생성과 승자 진출까지만 구현**되어 있고, **BracketEntry -> Game 변환은 미구현** 상태이다.

### 현재 구현된 플로우

```
[backoffice 관리자]
       |
       v
(1) 대회 생성 (type: TOURNAMENT)
       |  CompetitionAdminController.createCompetition()
       v
(2) 토너먼트 대진표 생성
       |  POST /api/backoffice/brackets/{competitionId}/generate
       |  → BracketManagementController
       |  → BracketGeneratorService.generateSingleElimination()
       |    또는 generateDoubleElimination()
       |  → BracketEntry 다수 생성 (라운드별 매치 슬롯)
       v
(3) ⚠️ 각 BracketEntry에 대한 Game 생성 (미구현)
       |
       v
(4) 승자 진출
       |  POST /api/backoffice/brackets/entries/{id}/advance
       |  → BracketGeneratorService.advanceWinner()
       |  → bracketEntry.recordWinner(winnerTeam)
```

### 관련 엔티티

| 엔티티 | 파일 경로 | 역할 |
|--------|----------|------|
| `BracketEntry` | `nextup-core/.../domain/competition/BracketEntry.kt` | 토너먼트 매치 슬롯 |

---

## 6. Game 엔티티 구조

### 엔티티 관계도

```
Competition (대회)
    │
    ├── GameRules (경기 규칙 - Embedded)
    │
    ├── LeagueSchedule (리그 대진표) ──── 1:1 ──── Game
    │                                                │
    ├── BracketEntry (토너먼트 대진표)                  ├── GameTeam (홈/원정)
    │                                                │      └── Team
    └── ... (N개 경기)                                  │
                                                     ├── GamePlayer (출전 선수)
                                                     │      └── Player
                                                     ├── GameEvent (경기 이벤트)
                                                     ├── BattingRecord (타격 기록)
                                                     ├── PitchingRecord (투구 기록)
                                                     ├── FieldingRecord (수비 기록)
                                                     ├── GameParticipation (출석)
                                                     └── LineupSubmission (라인업)
```

### Game 생성 시 필수 데이터

Game 객체를 생성하려면 최소한 다음이 필요하다:

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `competition` | `Competition` | 필수 | 소속 대회 |
| `scheduledAt` | `LocalDateTime` | 필수 | 경기 예정 시각 |
| `location` | `String?` | 선택 | 경기장 |
| `fieldName` | `String?` | 선택 | 구장명 |
| `gameNumber` | `Int?` | 선택 | 경기 번호 |
| `totalInnings` | `Int` | 기본값 9 | 이닝 수 (GameRules.defaultInnings 반영) |

Game 생성 후 반드시 **GameTeam 2개(홈/원정)**도 함께 생성해야 경기가 정상 작동한다.

---

## 7. 경기 생명주기 (Lifecycle)

### 상태 전이 다이어그램

```
                    ┌──────────────┐
                    │  SCHEDULED   │ (경기 예정)
                    │  (초기 상태)  │
                    └──────┬───────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        postpone()     start()     cancel()
              │            │            │
              v            v            v
        ┌──────────┐ ┌───────────┐ ┌───────────┐
        │POSTPONED │ │IN_PROGRESS│ │ CANCELLED │
        │  (연기)   │ │ (진행 중)  │ │  (취소)    │
        └────┬─────┘ └─────┬─────┘ └───────────┘
             │             │
        cancel()    ┌──────┼──────────┐
             │      │      │          │
             v  finish() callGame() forfeit()
        ┌────────┐  │      │          │
        │CANCELLED│  v      v          v
        └────────┘ ┌──────┐ ┌──────┐ ┌─────────┐
                   │FINISH│ │CALLED│ │FORFEITED│
                   │(종료) │ │(콜드) │ │ (몰수)   │
                   └──────┘ └──────┘ └─────────┘
```

### 상태별 API 엔드포인트 (scorer 모듈)

| 상태 전이 | API | Controller 메서드 |
|-----------|-----|------------------|
| SCHEDULED -> IN_PROGRESS | `POST /api/scorer/games/{gameId}/start` | `GameScorerController.startGame()` |
| IN_PROGRESS -> FINISHED | `POST /api/scorer/games/{gameId}/end` | `GameScorerController.endGame()` |
| IN_PROGRESS -> CALLED | `POST /api/scorer/games/{gameId}/end` (reason=MERCY_RULE/WEATHER) | `GameScorerController.endGame()` |
| SCHEDULED/IN_PROGRESS -> FORFEITED | `POST /api/scorer/games/{gameId}/forfeit` | `GameScorerController.forfeitGame()` |
| SCHEDULED/POSTPONED -> CANCELLED | `POST /api/scorer/games/{gameId}/cancel` | `GameScorerController.cancelGame()` |

### 서비스 계층

| 서비스 인터페이스 | 구현체 | 위치 |
|------------------|--------|------|
| `GameLifecycleService` | `GameLifecycleServiceImpl` | `nextup-infrastructure` |
| `GameScheduleService` | `GameScheduleServiceImpl` | `nextup-infrastructure` |

---

## 8. 프론트엔드 개발자 가이드

### 현재 사용 가능한 API 흐름

현재 코드베이스에서 프론트엔드가 사용할 수 있는 경기 관련 API는 다음과 같다:

#### 1단계: 대회 및 대진표 설정 (backoffice 관리자)

```
1. 대회 생성
   POST /api/backoffice/competitions
   Body: { leagueId, name, year, season, type, startDate, ... }

2-A. 대진표 수동 생성
   POST /api/backoffice/competitions/{competitionId}/schedule
   Body: { round, matchNumber, homeTeamId, awayTeamId, scheduledDate, scheduledTime?, venue? }

2-B. 대진표 자동 생성 (라운드 로빈)
   POST /api/backoffice/competitions/{competitionId}/schedule/generate
   Body: { teamIds: [1, 2, 3, ...], doubleRoundRobin: false }
```

#### 2단계: Game 생성 (미구현 -- 구현 필요)

현재 LeagueSchedule에서 Game으로 변환하는 API가 없다. 구현 시 예상되는 흐름:

```
POST /api/backoffice/competitions/{competitionId}/schedule/{scheduleId}/create-game
→ Schedule 정보(homeTeam, awayTeam, date, venue)로 Game + GameTeam 2개 생성
→ LeagueSchedule.linkGame(game) 호출
→ Schedule 상태: SCHEDULED → GAME_CREATED
```

#### 3단계: 경기 조회 (일반 사용자)

```
- 경기 목록:      GET /api/v1/games?date={date}&teamId={teamId}&competitionId={competitionId}
- 경기 상세:      GET /api/v1/games/{gameId}
- 팀 경기 일정:   GET /api/v1/teams/{teamId}/games
- 다가오는 경기:  GET /api/v1/teams/{teamId}/games/upcoming?limit=5
- 캘린더 뷰:     GET /api/v1/games/calendar?year={year}&month={month}&teamId={teamId}
- 출전 가능 선수: GET /api/v1/games/{gameId}/available-roster?teamId={teamId}
```

#### 4단계: 경기 진행 (기록원)

```
1. 경기 시작:     POST /api/scorer/games/{gameId}/start
2. 타석 결과:     POST /api/scorer/games/{gameId}/plate-appearances
3. 주루 플레이:   POST /api/scorer/games/{gameId}/base-running
4. 선수 교체:     POST /api/scorer/games/{gameId}/substitutions
5. 반 이닝 진행:  POST /api/scorer/games/{gameId}/half-inning
6. 경기 종료:     POST /api/scorer/games/{gameId}/end
7. 몰수 처리:     POST /api/scorer/games/{gameId}/forfeit
8. 경기 취소:     POST /api/scorer/games/{gameId}/cancel
9. 되돌리기:      POST /api/scorer/games/{gameId}/undo
```

---

## 9. 현재 미구현 사항

코드 분석 결과, 다음 기능이 아직 구현되지 않았다:

### 필수 구현 필요

| 우선순위 | 항목 | 설명 |
|---------|------|------|
| **P0** | Schedule -> Game 변환 서비스 | `LeagueSchedule`에서 `Game` + `GameTeam`을 생성하고 `linkGame()`으로 연결하는 서비스 |
| **P0** | Schedule -> Game 변환 API | backoffice에서 호출할 엔드포인트 |
| **P1** | Game.create() 팩토리 메서드 | 현재 public 생성자 사용 중. Rich Domain Model 규칙에 따라 `private constructor` + `companion object.create()` 패턴 적용 필요 |
| **P1** | GameTeam 자동 생성 | Game 생성 시 홈팀/원정팀 GameTeam 2개를 함께 생성하는 로직 |

### 선택 구현

| 우선순위 | 항목 | 설명 |
|---------|------|------|
| **P2** | Match -> Game 변환 | 친선 경기 매칭 성사(MATCHED) 후 Game 자동 생성 |
| **P2** | BracketEntry -> Game 변환 | 토너먼트 각 매치에 대한 Game 생성 |
| **P3** | 대진표 일괄 Game 생성 | 대진표 전체를 한번에 Game으로 변환하는 벌크 API |

### 구현 시 고려사항

1. **Competition 연관**: Game은 반드시 Competition에 속해야 한다. 친선 경기의 경우 "친선 경기" 전용 Competition을 생성하거나, Competition 없이도 Game을 생성할 수 있도록 스키마 변경이 필요하다.

2. **GameRules 반영**: Game 생성 시 Competition의 GameRules에서 `defaultInnings`, `timeLimitMinutes`, `pitchCountLimit` 등을 Game에 반영해야 한다 (예: `totalInnings = competition.gameRules.defaultInnings`).

3. **트랜잭션 범위**: Game + GameTeam 2개 생성은 하나의 트랜잭션 내에서 처리해야 한다.

4. **Schedule 상태 검증**: `linkGame()` 호출 전 Schedule 상태가 `SCHEDULED` 또는 `POSTPONED`인지 검증한다 (엔티티 내부에서 이미 검증).

---

## 부록: 전체 경기 생성 시퀀스 (구현 완료 시 예상)

```
┌──────────┐     ┌───────────────┐     ┌──────────────────┐     ┌──────────────┐
│ 관리자    │     │  backoffice   │     │  infrastructure  │     │    core      │
│(프론트)   │     │  Controller   │     │   ServiceImpl    │     │  Domain      │
└────┬─────┘     └──────┬────────┘     └────────┬─────────┘     └──────┬───────┘
     │                  │                       │                      │
     │ 1. 대회 생성      │                       │                      │
     │─────────────────>│                       │                      │
     │                  │──────────────────────>│                      │
     │                  │                       │──────────────────────>│
     │                  │                       │   Competition 저장    │
     │                  │<──────────────────────│<─────────────────────│
     │<─────────────────│                       │                      │
     │                  │                       │                      │
     │ 2. 대진표 생성    │                       │                      │
     │─────────────────>│                       │                      │
     │                  │──────────────────────>│                      │
     │                  │                       │──────────────────────>│
     │                  │                       │ LeagueSchedule.create│
     │                  │<──────────────────────│<─────────────────────│
     │<─────────────────│                       │                      │
     │                  │                       │                      │
     │ 3. 경기 생성      │                       │                      │
     │   (미구현)        │                       │                      │
     │─────────────────>│                       │                      │
     │                  │──────────────────────>│                      │
     │                  │                       │──────── Game(...) ──>│
     │                  │                       │── GameTeam(HOME) ───>│
     │                  │                       │── GameTeam(AWAY) ───>│
     │                  │                       │── linkGame(game) ───>│
     │                  │                       │   status→GAME_CREATED│
     │                  │<──────────────────────│<─────────────────────│
     │<─────────────────│                       │                      │
     │                  │                       │                      │
┌────┴─────┐     ┌──────┴────────┐     ┌────────┴─────────┐     ┌──────┴───────┐
│ 기록원    │     │    scorer     │     │  infrastructure  │     │    core      │
│(프론트)   │     │  Controller   │     │   ServiceImpl    │     │  Domain      │
└────┬─────┘     └──────┬────────┘     └────────┬─────────┘     └──────┬───────┘
     │                  │                       │                      │
     │ 4. 경기 시작      │                       │                      │
     │─────────────────>│                       │                      │
     │                  │──────────────────────>│                      │
     │                  │                       │──────────────────────>│
     │                  │                       │      game.start()    │
     │                  │                       │   status→IN_PROGRESS │
     │                  │<──────────────────────│<─────────────────────│
     │<─────────────────│                       │                      │
     │                  │                       │                      │
     │ 5. 경기 진행...   │                       │                      │
     │ (타석/주루/교체)   │                       │                      │
     │                  │                       │                      │
     │ 6. 경기 종료      │                       │                      │
     │─────────────────>│                       │                      │
     │                  │──────────────────────>│                      │
     │                  │                       │──────────────────────>│
     │                  │                       │     game.finish()    │
     │                  │                       │   status→FINISHED    │
     │                  │<──────────────────────│<─────────────────────│
     │<─────────────────│                       │                      │
```
