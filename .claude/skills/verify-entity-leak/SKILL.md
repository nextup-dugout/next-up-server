---
name: verify-entity-leak
description: Controller의 반환 타입을 작성하거나 수정할 때 반드시 이 스킬을 참조하라. Entity를 API 응답으로 직접 반환하는 것은 Zero Entity Leak 위반이며 즉시 REJECT 사유다. Controller에서 fun 메서드의 반환 타입을 정의하거나, Response DTO를 만들거나, Entity를 import할 때 즉시 트리거하라. 반드시 DTO/Response로 변환하여 반환해야 한다.
---

# Zero Entity Leak 검증

## Purpose

1. Controller 반환 타입에 Core Entity가 직접 노출되지 않는지 검증
2. DTO(Response/Request) 변환이 올바르게 적용되었는지 확인
3. 새로 추가된 Controller/Endpoint가 규칙을 준수하는지 검증

## When to Run

- 새로운 Controller 또는 Endpoint 추가 후
- Entity 클래스 수정 후
- DTO 변환 로직 변경 후
- PR 전 통합 검증 시

## Related Files

| File | Purpose |
|------|---------|
| `nextup-api/src/main/kotlin/com/nextup/api/controller/**/*.kt` | API 컨트롤러 |
| `nextup-backoffice/src/main/kotlin/com/nextup/backoffice/controller/**/*.kt` | 백오피스 컨트롤러 |
| `nextup-scorer/src/main/kotlin/com/nextup/scorer/controller/**/*.kt` | 스코어러 컨트롤러 |
| `nextup-core/src/main/kotlin/com/nextup/core/domain/**/*.kt` | 도메인 엔티티 |
| `nextup-api/src/main/kotlin/com/nextup/api/dto/**/*.kt` | API DTO |
| `nextup-backoffice/src/main/kotlin/com/nextup/backoffice/dto/**/*.kt` | 백오피스 DTO |

## Workflow

### Step 1: Entity 클래스 이름 수집

Core 도메인 Entity 이름을 수집합니다.

```bash
grep -rn "^class [A-Z]" nextup-core/src/main/kotlin/com/nextup/core/domain/ --include="*.kt" | sed 's/.*class \([A-Z][a-zA-Z]*\).*/\1/' | sort -u
```

주요 Entity: `AbsenceReason`, `ActivityScore`, `Appeal`, `Association`, `Attendance`, `AttendancePoll`, `AttendanceVote`, `AuditLog`, `BookingTransfer`, `BracketEntry`, `Candidate`, `CareerBattingStats`, `CareerFieldingStats`, `CareerPitchingStats`, `Certificate`, `Competition`, `CompetitionPlayer`, `CorrectionRequest`, `DeviceToken`, `Discipline`, `Election`, `ElectionVote`, `FieldingRecord`, `Game`, `GameEvent`, `GameParticipation`, `GamePlayer`, `GameResult`, `GameRules`, `GameTeam`, `League`, `LeagueSchedule`, `LineupEntry`, `LineupSubmission`, `MatchRequest`, `MatchResponse`, `MercenaryApplication`, `MercenaryParticipation`, `MercenaryRequest`, `Notification`, `NotificationPreference`, `OAuthAccount`, `OrganizationAdmin`, `Player`, `PlayerCareer`, `PlayerTeam`, `PlayerTeamHistory`, `RecordCorrection`, `RecruitmentApplication`, `RefreshToken`, `SeasonAward`, `SeasonBattingStats`, `SeasonFieldingStats`, `SeasonPitchingStats`, `SpecialGameRecord`, `Stadium`, `StadiumBooking`, `StadiumSlot`, `Team`, `TeamBlacklist`, `TeamJoinRequest`, `TeamMember`, `TeamRecruitment`, `TeamSchedule`, `User`

### Step 2: Controller 반환 타입 검사

모든 Controller의 public 함수 반환 타입에서 Entity 이름이 직접 사용되는지 검사합니다.

**도구:** Grep

**패턴:**
```
fun [a-zA-Z]+\(.*\).*: .*(AbsenceReason|ActivityScore|Appeal|Association|Attendance|AttendancePoll|AttendanceVote|AuditLog|BookingTransfer|BracketEntry|Candidate|CareerBattingStats|CareerFieldingStats|CareerPitchingStats|Certificate|Competition|CompetitionPlayer|CorrectionRequest|DeviceToken|Discipline|Election|ElectionVote|FieldingRecord|Game|GameEvent|GameParticipation|GamePlayer|GameResult|GameRules|GameTeam|League|LeagueSchedule|LineupEntry|LineupSubmission|MatchRequest|MatchResponse|MercenaryApplication|MercenaryParticipation|MercenaryRequest|Notification|NotificationPreference|OAuthAccount|OrganizationAdmin|Player|PlayerCareer|PlayerTeam|PlayerTeamHistory|RecordCorrection|RecruitmentApplication|RefreshToken|SeasonAward|SeasonBattingStats|SeasonFieldingStats|SeasonPitchingStats|SpecialGameRecord|Stadium|StadiumBooking|StadiumSlot|Team|TeamBlacklist|TeamJoinRequest|TeamMember|TeamRecruitment|TeamSchedule|User)[^a-zA-Z]
```

**대상:** `**/controller/**/*.kt`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** 1건 이상 매칭 시 Entity가 반환 타입에 포함

**수정 방법:** Entity 대신 Response DTO를 생성하여 반환

```kotlin
// FAIL
fun getPlayer(): Player

// PASS
fun getPlayer(): PlayerResponse
```

### Step 3: Import 문에서 Entity 직접 참조 검사

Controller 파일에서 Core 도메인 Entity를 직접 import하는지 검사합니다.

**도구:** Grep

**패턴:**
```
^import com\.nextup\.core\.domain\.[a-z]+\.[A-Z]
```

**대상:** `**/controller/**/*.kt`

**PASS 기준:** 매칭 결과 0건 (Controller가 Entity를 직접 import하지 않음)
**FAIL 기준:** Controller가 Entity를 직접 import (Service 계층을 통해 DTO로 변환해야 함)

**참고:** Service 반환 타입이 Entity인 경우 Controller에서 변환하는 것은 허용되나, 가능하면 Service에서 DTO로 변환 후 반환하는 것을 권장

## Output Format

```markdown
| # | 파일 | 라인 | 문제 | 심각도 |
|---|------|------|------|--------|
| 1 | `path/to/Controller.kt:42` | Entity 반환 | 🔴 REJECT |
```

## Exceptions

1. **내부 private 함수** — Controller 내부의 private/protected 함수는 검사 대상이 아님 (외부에 노출되지 않음)
2. **테스트 코드** — `src/test/` 내 파일은 프로덕션 API가 아니므로 면제
3. **Service 계층 import** — Service에서 Entity를 사용하는 것은 정상 (Controller에서만 검사)
4. **DTO 이름에 Entity 이름이 포함된 경우** — `TeamResponse`, `PlayerDetailResponse` 등 DTO 이름에 Entity 이름이 포함되는 것은 정상
