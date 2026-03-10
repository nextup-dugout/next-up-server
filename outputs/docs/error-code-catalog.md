# NEXT-UP Error Code Catalog

> 프론트엔드 연동을 위한 에러 코드 카탈로그입니다.
> 모든 에러 응답은 `ApiResponse.error(code, message)` 형식으로 반환됩니다.

## Response Format

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 메시지"
  }
}
```

## HTTP Status Code Mapping

| Exception Type | HTTP Status |
|---|---|
| `NotFoundException` | 404 NOT_FOUND |
| `BusinessException` | 400 BAD_REQUEST |
| `InvalidStateException` | 400 BAD_REQUEST |
| `InvalidInputException` | 400 BAD_REQUEST |
| `ForbiddenException` | 403 FORBIDDEN |
| `AuthenticationException` | 401 UNAUTHORIZED |
| Unhandled Exception | 500 INTERNAL_SERVER_ERROR |

---

## Authentication & Authorization (401/403)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `INVALID_TOKEN` | 401 | Invalid token | 유효하지 않은 토큰 |
| `TOKEN_EXPIRED` | 401 | Token has expired | 토큰 만료 |
| `REFRESH_TOKEN_NOT_FOUND` | 404 | Refresh token not found | 리프레시 토큰 없음 |
| `REFRESH_TOKEN_EXPIRED` | 401 | Refresh token has expired | 리프레시 토큰 만료 |
| `REFRESH_TOKEN_REVOKED` | 401 | Refresh token has been revoked | 리프레시 토큰 폐기됨 |
| `INVALID_CREDENTIALS` | 401 | Invalid email or password | 잘못된 로그인 정보 |
| `UNSUPPORTED_OAUTH2_PROVIDER` | 401 | Unsupported OAuth2 provider: {provider} | 지원하지 않는 OAuth2 제공자 |
| `OAUTH2_PROCESSING_ERROR` | 401 | {message} | OAuth2 처리 오류 |

## User (사용자)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `USER_NOT_FOUND` | 404 | User not found: {id} | 사용자 없음 |
| `EMAIL_DUPLICATE` | 400 | Email already exists: {email} | 이메일 중복 |
| `OAUTH_ALREADY_LINKED` | 400 | OAuth account already linked: {provider} | OAuth 계정 이미 연결됨 |
| `OAUTH_ACCOUNT_NOT_FOUND` | 404 | OAuth account not found | OAuth 계정 없음 |
| `INSUFFICIENT_AUTH_METHOD` | 400 | At least one authentication method is required | 최소 1개 인증 수단 필요 |
| `USER_DEACTIVATED` | 400 | User is deactivated: {id} | 비활성화된 사용자 |
| `PLAYER_NOT_LINKED` | 400 | User does not have a linked player | 선수 프로필 미연결 |

## Association (협회)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `ASSOCIATION_NOT_FOUND` | 404 | Association not found: {id} | 협회 없음 |
| `ASSOCIATION_NAME_DUPLICATE` | 400 | Association name already exists: {name} | 협회명 중복 |

## League (리그)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `LEAGUE_NOT_FOUND` | 404 | League not found: {id} | 리그 없음 |
| `LEAGUE_NAME_DUPLICATE` | 400 | League name already exists in association | 리그명 중복 |

## Team (팀)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `TEAM_NOT_FOUND` | 404 | Team not found: {id} | 팀 없음 |
| `TEAM_ALREADY_EXISTS` | 400 | Team already exists: {name} | 팀 중복 |
| `INVALID_TEAM_STATE` | 400 | {message} | 잘못된 팀 상태 |

## Team Membership (팀 멤버십)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `TEAM_MEMBER_NOT_FOUND` | 404 | Team member not found: {id} | 팀 멤버 없음 |
| `ALREADY_TEAM_MEMBER` | 400 | User is already a member of team | 이미 팀 멤버 |
| `ALREADY_IN_TEAM` | 400 | User is already a member of team '{name}' | 이미 다른 팀 소속 |
| `TEAM_JOIN_REQUEST_NOT_FOUND` | 404 | Team join request not found: {id} | 가입 요청 없음 |
| `DUPLICATE_JOIN_REQUEST` | 400 | User already has a pending join request | 중복 가입 요청 |
| `BLACKLISTED_USER` | 400 | User is blacklisted from team | 블랙리스트 사용자 |
| `UNIFORM_NUMBER_ALREADY_TAKEN` | 400 | Uniform number is already taken | 등번호 중복 |
| `INVALID_UNIFORM_NUMBER` | 400 | Invalid uniform number (1-99) | 잘못된 등번호 |
| `INSUFFICIENT_TEAM_ROLE` | 400 | Insufficient team role | 팀 권한 부족 |
| `OWNER_CANNOT_LEAVE` | 400 | OWNER는 다른 OWNER를 지정한 후 탈퇴할 수 있습니다 | 구단주 탈퇴 불가 |

## Competition (대회)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `COMPETITION_NOT_FOUND` | 404 | Competition not found: {id} | 대회 없음 |
| `INVALID_COMPETITION_STATE` | 400 | {message} | 잘못된 대회 상태 |
| `COMPETITION_PLAYER_NOT_FOUND` | 404 | Competition player not found: {id} | 대회 선수 없음 |
| `UNREGISTERED_PLAYER` | 400 | 리그에 등록되지 않은 선수가 포함 | 미등록 선수 |

## Game & Record (경기 & 기록)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `GAME_NOT_FOUND` | 404 | Game not found: {id} | 경기 없음 |
| `INVALID_GAME_STATE` | 400 | {message} | 잘못된 경기 상태 |
| `GAME_PLAYER_NOT_FOUND` | 404 | GamePlayer not found: {id} | 경기 참여 선수 없음 |
| `BATTING_RECORD_NOT_FOUND` | 404 | Batting record not found | 타격 기록 없음 |
| `PITCHING_RECORD_NOT_FOUND` | 404 | Pitching record not found | 투수 기록 없음 |
| `FIELDING_RECORD_NOT_FOUND` | 404 | Fielding record not found | 수비 기록 없음 |
| `RECORD_ALREADY_EXISTS` | 400 | Record already exists for GamePlayer | 기록 중복 |
| `UNDO_NOT_AVAILABLE` | 400 | {reason} | 되돌리기 불가 |
| `NO_EVENT_TO_UNDO` | 404 | 되돌릴 이벤트가 없습니다 | 되돌릴 이벤트 없음 |
| `PITCH_EVENT_NOT_FOUND` | 404 | PitchEvent not found: {id} | 투구 이벤트 없음 |

## Record Correction (기록 정정)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `RECORD_CORRECTION_NOT_FOUND` | 404 | 기록 정정 내역을 찾을 수 없습니다 | 정정 기록 없음 |
| `INVALID_CORRECTION_FIELD` | 400 | 유효하지 않은 정정 필드입니다: {fieldName} | 잘못된 정정 필드 |
| `INVALID_CORRECTION_VALUE` | 400 | 유효하지 않은 정정 값입니다: {fieldName}={value} | 잘못된 정정 값 |

## Stats (통계)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `PLAYER_NOT_FOUND` | 404 | Player not found: {playerId} | 선수 없음 |
| `SEASON_BATTING_STATS_NOT_FOUND` | 404 | Season batting stats not found | 시즌 타격 통계 없음 |
| `SEASON_PITCHING_STATS_NOT_FOUND` | 404 | Season pitching stats not found | 시즌 투수 통계 없음 |
| `SEASON_FIELDING_STATS_NOT_FOUND` | 404 | Season fielding stats not found | 시즌 수비 통계 없음 |
| `CAREER_BATTING_STATS_NOT_FOUND` | 404 | Career batting stats not found | 통산 타격 통계 없음 |
| `CAREER_PITCHING_STATS_NOT_FOUND` | 404 | Career pitching stats not found | 통산 투수 통계 없음 |
| `CAREER_FIELDING_STATS_NOT_FOUND` | 404 | Career fielding stats not found | 통산 수비 통계 없음 |
| `STATS_VALIDATION_ERROR` | 400 | {message} | 통계 검증 오류 |

## Lineup (라인업)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `DUPLICATE_PLAYER_IN_LINEUP` | 400 | 라인업에 중복된 선수가 있습니다 | 선수 중복 |
| `NO_CATCHER_IN_LINEUP` | 400 | 라인업에 포수(C)가 최소 1명 필요합니다 | 포수 없음 |
| `INVALID_DH_RULE` | 400 | {message} | DH 규칙 위반 |
| `NON_ATTENDING_PLAYER_IN_LINEUP` | 400 | 참석이 아닌 선수가 포함 | 미참석 선수 |
| `LINEUP_NOT_EXCHANGED` | 403 | 양 팀 라인업 교환이 완료되지 않았습니다 | 라인업 교환 미완료 |
| `LINEUP_EXCHANGE_REJECTED` | 400 | 라인업 교환이 거부되었습니다 | 라인업 교환 거부 |
| `LINEUP_EXCHANGE_NOT_AUTHORIZED` | 403 | 라인업 교환 승인/거부 권한 없음 | 라인업 교환 권한 없음 |

## Discipline (징계)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `DISCIPLINE_NOT_FOUND` | 404 | Discipline not found: {id} | 징계 없음 |
| `INVALID_DISCIPLINE_STATE` | 400 | {message} | 잘못된 징계 상태 |
| `PLAYER_INELIGIBLE` | 400 | Player is ineligible to play: {reason} | 출전 자격 없음 |

## Election (선거)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `ELECTION_NOT_FOUND` | 404 | Election not found: {id} | 선거 없음 |
| `CANDIDATE_NOT_FOUND` | 404 | Candidate not found: {id} | 후보자 없음 |
| `DUPLICATE_VOTE` | 400 | User has already voted | 중복 투표 |
| `UNAUTHORIZED_EMERGENCY_ELECTION` | 403 | MANAGER 권한 필요 | 비상 선거 권한 없음 |
| `ACTIVE_ELECTION_ALREADY_EXISTS` | 400 | Team already has an active election | 진행 중인 선거 존재 |
| `INVALID_ACTING_OWNER` | 400 | Member is not eligible for acting owner | 대행 자격 없음 |

## Attendance (출석)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `ATTENDANCE_POLL_NOT_FOUND` | 404 | 출석 투표를 찾을 수 없습니다 | 출석 투표 없음 |
| `ATTENDANCE_POLL_CLOSED` | 400 | 출석 투표가 마감되었습니다 | 출석 투표 마감 |
| `ALREADY_VOTED` | 400 | 이미 투표했습니다 | 중복 투표 |
| `ACTIVITY_SCORE_NOT_FOUND` | 404 | 활동 점수를 찾을 수 없습니다 | 활동 점수 없음 |
| `ATTENDANCE_VOTE_NOT_FOUND` | 404 | Attendance vote not found | 출석 투표 없음 |
| `VOTE_CLOSED` | 400 | 경기가 시작되어 투표가 마감되었습니다 | 투표 마감 |

## Stadium & Booking (구장 & 예약)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `STADIUM_NOT_FOUND` | 404 | Stadium not found: {id} | 구장 없음 |
| `SLOT_NOT_FOUND` | 404 | Stadium slot not found: {id} | 구장 슬롯 없음 |
| `BOOKING_NOT_FOUND` | 404 | Stadium booking not found: {id} | 예약 없음 |
| `BOOKING_TRANSFER_NOT_FOUND` | 404 | Booking transfer not found: {id} | 양도 없음 |
| `BOOKING_TRANSFER_FORBIDDEN` | 403 | {message} | 양도 권한 없음 |
| `BOOKING_TRANSFER_INVALID_STATE` | 400 | {message} | 잘못된 양도 상태 |

## Match Request (매칭)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `MATCH_REQUEST_NOT_FOUND` | 404 | 매칭 요청을 찾을 수 없습니다 | 매칭 요청 없음 |
| `MATCH_RESPONSE_NOT_FOUND` | 404 | 매칭 응답을 찾을 수 없습니다 | 매칭 응답 없음 |

## Certificate (증명서)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `CERTIFICATE_NOT_FOUND` | 404 | 증명서를 찾을 수 없습니다 | 증명서 없음 |
| `CERTIFICATE_EXPIRED` | 400 | 만료된 증명서입니다 | 증명서 만료 |
| `CERTIFICATE_REVOKED` | 400 | 취소된 증명서입니다 | 증명서 취소됨 |

## Player Team (선수 팀 이력)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `PLAYER_TEAM_HISTORY_NOT_FOUND` | 404 | Player team history not found | 선수 팀 이력 없음 |
| `PLAYER_NOT_IN_TEAM` | 404 | Player is not affiliated with team | 선수 팀 소속 아님 |
| `PLAYER_ALREADY_IN_LEAGUE` | 400 | Player is already active in league | 선수 이미 리그 소속 |
| `INVALID_PLAYER_TEAM_STATUS` | 400 | {message} | 잘못된 선수 팀 상태 |
| `PLAYER_TRANSFER_NOT_ALLOWED` | 400 | {message} | 이적 불가 |

## Organization Admin (조직 관리자)

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `ORGANIZATION_ADMIN_NOT_FOUND` | 404 | Organization admin not found | 조직 관리자 없음 |
| `ORGANIZATION_ADMIN_ALREADY_EXISTS` | 400 | Organization admin already exists | 조직 관리자 중복 |
| `ORGANIZATION_NOT_FOUND` | 404 | Organization not found | 조직 없음 |
| `INVALID_ORGANIZATION_TYPE` | 400 | Invalid organization type | 잘못된 조직 유형 |
| `UNAUTHORIZED_ORGANIZATION_ACCESS` | 400 | Unauthorized access to organization | 조직 접근 권한 없음 |
| `SAME_LEAGUE_CONFLICT` | 400 | Cannot manage multiple teams in same league | 같은 리그 팀 중복 관리 |
| `INSUFFICIENT_ROLE_LEVEL` | 400 | Insufficient role level | 권한 등급 부족 |
| `ORGANIZATION_ADMIN_DEACTIVATED` | 400 | Organization admin is deactivated | 비활성화된 관리자 |

## Other

| Code | HTTP | Message | Description |
|------|------|---------|-------------|
| `APPEAL_NOT_FOUND` | 404 | 이의 제기를 찾을 수 없습니다 | 이의 제기 없음 |
| `BRACKET_ENTRY_NOT_FOUND` | 404 | 대진표 엔트리를 찾을 수 없습니다 | 대진표 없음 |
| `DEVICE_TOKEN_NOT_FOUND` | 404 | 디바이스 토큰을 찾을 수 없습니다 | 디바이스 토큰 없음 |
| `NOTIFICATION_NOT_FOUND` | 404 | 알림을 찾을 수 없습니다 | 알림 없음 |
| `RECRUITMENT_NOT_FOUND` | 404 | 모집 공고를 찾을 수 없습니다 | 모집 공고 없음 |
| `SCHEDULE_NOT_FOUND` | 404 | Schedule not found: {id} | 일정 없음 |
| `INVALID_SCHEDULE_STATE` | 400 | {message} | 잘못된 일정 상태 |
| `AUDIT_LOG_NOT_FOUND` | 404 | Audit log not found: {id} | 감사 로그 없음 |
