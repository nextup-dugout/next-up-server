# NEXT-UP 실시간 기록 시스템 로드맵

> 작성일: 2026-02-03
> 상태: 계획 수립 완료

---

## 프로젝트 비전

**"현장 기록원의 입력 → 실시간 문자 중계 → 선수/팀 스탯 즉시 반영 → 시즌/통산 기록 자동 누적"**

사회인 야구 선수들도 프로 선수들처럼 자신의 모든 플레이를 정교한 수치로 관리받을 수 있는 원스톱 데이터 파이프라인 구축

---

## 현재 구현 상태

### 완료된 것 (85-90%)
- [x] Entity 17개 (User, Player, Game, BattingRecord, PitchingRecord, Stats 등)
- [x] Service 8개 (User, Association, League, Competition, BattingRecord, PitchingRecord 등)
- [x] Repository 17개 (Hexagonal Architecture Port + 구현체)
- [x] JWT + OAuth2 인증 (Google, Kakao, Naver)
- [x] WebSocket 설정 (STOMP/SockJS) - 설정만 완료

### 미구현
- [ ] 실시간 이벤트 로그 (GameEvent)
- [ ] 라이브 중계 WebSocket 로직
- [ ] 박스스코어 자동 생성
- [ ] 경기 실시간 상태 관리 (이닝, 아웃, 주자)
- [ ] 라인업 제출 워크플로우
- [ ] 협회/리그 운영 기능 (순위, 대진표)
- [ ] PDF 공식 기록지 출력

---

## 로드맵 개요

```
[Phase 1] 실시간 기록 MVP (핵심)
    ↓
[Phase 2] 조회/통계 고도화
    ↓
[Phase 3] 운영 기능
    ↓
[Phase 4] 고도화 및 확장
```

---

## Phase 1: 실시간 기록 MVP

### 목표
기록원이 경기 현장에서 타석 결과를 입력하면, 실시간으로 중계되고 통계가 자동 계산되는 핵심 파이프라인 구축

### 핵심 흐름
```
감독 → 라인업 입력 → 기록원에게 제출
                          ↓
                    기록원 확인 → 경기 시작
                          ↓
                    타석 결과 입력
                          ↓
            ┌─────────────┼─────────────┐
            ↓             ↓             ↓
      이벤트 로그    박스스코어     개인 기록
         저장        자동 갱신      자동 갱신
            ↓             ↓             ↓
            └─────────────┼─────────────┘
                          ↓
                   WebSocket 브로드캐스트
                          ↓
                    실시간 중계 화면
```

### 작업 목록

#### 1.1 경기 실시간 상태 관리
- **GameState Value Object 또는 Embeddable**
  - 현재 이닝 (회/초말)
  - 현재 아웃 카운트 (0-2)
  - 현재 주자 상황 (1루, 2루, 3루 점유 여부 + 주자 ID)
  - 현재 타순 (홈/원정 각각)
  - 현재 투수 ID

#### 1.2 이벤트 타입 정의
- **PlateAppearanceResult enum**
  ```
  // 안타
  SINGLE, DOUBLE, TRIPLE, HOME_RUN, BUNT_HIT, INFIELD_HIT

  // 아웃
  STRIKEOUT, STRIKEOUT_SWINGING, GROUND_OUT, FLY_OUT,
  LINE_OUT, DOUBLE_PLAY, TRIPLE_PLAY

  // 출루
  WALK, INTENTIONAL_WALK, HIT_BY_PITCH,
  ERROR, FIELDERS_CHOICE, DROPPED_THIRD_STRIKE

  // 희생
  SACRIFICE_BUNT, SACRIFICE_FLY

  // 기타
  INTERFERENCE
  ```

- **BaseRunningEvent enum**
  ```
  STOLEN_BASE, CAUGHT_STEALING, WILD_PITCH, PASSED_BALL,
  BALK, PICKOFF, ADVANCE_ON_THROW
  ```

#### 1.3 GameEvent Entity
```kotlin
@Entity
class GameEvent(
    val game: Game,
    val inning: Int,
    val isTopInning: Boolean,
    val outCount: Int,
    val eventType: GameEventType,  // PLATE_APPEARANCE, BASE_RUNNING, SUBSTITUTION, etc.
    val description: String,       // "우전 안타, 2루 주자 홈인"
    val batter: GamePlayer?,
    val pitcher: GamePlayer?,
    val runnersBeforeJson: String, // 이벤트 전 주자 상황
    val runnersAfterJson: String,  // 이벤트 후 주자 상황
    val plateAppearanceResult: PlateAppearanceResult?,
    val runsScored: Int = 0,
    val rbis: Int = 0,
    val timestamp: Instant
)
```

#### 1.4 라인업 제출 워크플로우
- **LineupSubmission Entity**
  - 감독이 생성한 라인업
  - 상태: DRAFT, SUBMITTED, CONFIRMED, REJECTED
  - 기록원 확인 시 GamePlayer로 변환

#### 1.5 기록원 입력 API (scorer 모듈)
- `POST /api/games/{gameId}/lineups/confirm` - 라인업 확정
- `POST /api/games/{gameId}/start` - 경기 시작
- `POST /api/games/{gameId}/events` - 이벤트 입력
- `POST /api/games/{gameId}/half-inning` - 이닝 전환
- `POST /api/games/{gameId}/end` - 경기 종료

#### 1.6 WebSocket 실시간 브로드캐스트
- `/topic/games/{gameId}/events` - 이벤트 발생 시 전송
- `/topic/games/{gameId}/scoreboard` - 스코어보드 변경 시 전송
- `/topic/games/{gameId}/state` - 경기 상태 변경 시 전송

#### 1.7 박스스코어 자동 계산
- 이닝별 득점 자동 집계
- 팀별 R/H/E 합계
- 개인 타격/투수 기록 실시간 갱신

### 예상 산출물
- GameEvent, LineupSubmission Entity
- GameEventType, PlateAppearanceResult enum
- GameEventService, LineupService
- ScorerController (이벤트 입력 API)
- WebSocket 브로드캐스트 로직
- 박스스코어 계산 로직

---

## Phase 2: 조회/통계 고도화

### 목표
선수와 일반 사용자가 기록을 쉽게 조회하고 분석할 수 있는 기능

### 작업 목록

#### 2.1 선수 기록실
- 시즌별 상세 통계 조회
- 통산 기록 조회
- 대회별 기록 필터링
- 최근 N경기 폼 분석

#### 2.2 경기 타임라인 조회
- GameEvent 기반 이벤트 타임라인
- "문자 중계" 스타일 경기 다시보기

#### 2.3 팀 통계 대시보드
- 팀 성적 요약
- 포지션별 depth 분석
- 팀 타율/방어율 등

#### 2.4 상대 전적 분석
- "A 투수 vs B 타자" 역대 전적
- 팀 간 상대 전적

---

## Phase 3: 운영 기능

### 목표
협회/리그 관리자와 팀 운영진을 위한 관리 기능

### 작업 목록

#### 3.1 라인업 빌더 (팀 감독용)
- 드래그 앤 드롭 타순 편집
- 포지션 배치
- 라인업 저장 및 제출

#### 3.2 리그 순위 자동 산정
- 승률/승점 기반 순위 계산
- 동률 시 상대 전적 적용
- 실시간 순위표 공시

#### 3.3 대회 대진표 편성
- 리그 방식 일정 자동 생성
- 토너먼트 대진표 생성
- 경기장/일정 연동

#### 3.4 개인 타이틀 집계
- 타격왕, 홈런왕, 다승왕 등
- 규정 타석/이닝 검증
- 시즌 종료 시 자동 확정

---

## Phase 4: 고도화 및 확장

### 목표
차별화된 기능과 공식 연동

### 작업 목록

#### 4.1 투구 상세 기록 (선택적 확장)
- 볼카운트 추적
- 투구별 이벤트 (볼/스트라이크/파울/헛스윙)
- 투구 수 실시간 집계

#### 4.2 공식 기록지 PDF 출력
- 대한야구소프트볼협회 양식
- 경기 결과 자동 생성
- 기록 증명서 발급

#### 4.3 팀 운영 확장
- 팀원 출석/활동 관리
- 팀비 관리 (선택)

#### 4.4 알림/푸시
- 경기 시작 알림
- 관심 선수 기록 알림

---

## 기술적 고려사항

### 이벤트 아키텍처
- **State + Event Log 방식** 채택
  - 현재 상태: Game, GameTeam, GamePlayer에 저장
  - 이벤트 이력: GameEvent 테이블에 저장
  - 나중에 Event Sourcing으로 확장 가능

### 실시간 통신
- STOMP over WebSocket (이미 설정됨)
- SockJS fallback 지원
- 토픽 기반 pub/sub

### 데이터 정합성
- 이벤트 입력 시 트랜잭션으로 상태 + 이벤트 동시 저장
- 낙관적 락으로 동시 입력 방지

---

## GitHub Issue 계획

### Epic Issues
- `[Epic] Phase 1: 실시간 기록 MVP`
- `[Epic] Phase 2: 조회/통계 고도화`
- `[Epic] Phase 3: 운영 기능`
- `[Epic] Phase 4: 고도화 및 확장`

### Phase 1 Sub-issues
1. `feat: GameEvent Entity 및 이벤트 타입 설계`
2. `feat: 경기 실시간 상태 관리 (GameState)`
3. `feat: 라인업 제출 워크플로우`
4. `feat: 기록원 이벤트 입력 API`
5. `feat: WebSocket 실시간 브로드캐스트`
6. `feat: 박스스코어 자동 계산`

---

## 다음 단계

1. GitHub Epic Issue 생성
2. Phase 1 Sub-issues 생성
3. Phase 1 첫 번째 작업 시작: GameEvent Entity 설계
