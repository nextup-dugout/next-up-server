# Issue #14: 선수 통계 API 계층 구현 완료 보고서

## 구현 일자
2026-02-01

## 구현 내용

### Phase 1: DTO 구현
**위치**: `nextup-api/src/main/kotlin/com/nextup/api/dto/stats/`

#### 1. SeasonBattingStatsResponse.kt
- 시즌 타격 통계 응답 DTO
- 메타 정보 (id, playerId, year, createdAt, updatedAt)
- 출전 정보 (gamesPlayed)
- 기본 타격 기록 (16개 필드)
- 계산 속성 (5개 필드)
- 비율 지표 (5개 필드, String 변환)

#### 2. SeasonPitchingStatsResponse.kt
- 시즌 투수 통계 응답 DTO
- 메타 정보 (id, playerId, year, createdAt, updatedAt)
- 출전 정보 (gamesPlayed, gamesStarted)
- 기본 투수 기록 (18개 필드, nullable 2개)
- 계산 속성 (12개 필드, nullable 1개)

#### 3. CareerBattingStatsResponse.kt
- 통산 타격 통계 응답 DTO
- 메타 정보 (id, playerId, createdAt, updatedAt)
- 시즌 및 출전 정보 (seasonsPlayed, gamesPlayed)
- 기본 타격 기록 (16개 필드)
- 계산 속성 (5개 필드)
- 비율 지표 (5개 필드, String 변환)

#### 4. CareerPitchingStatsResponse.kt
- 통산 투수 통계 응답 DTO
- 메타 정보 (id, playerId, createdAt, updatedAt)
- 시즌 및 출전 정보 (seasonsPlayed, gamesPlayed, gamesStarted)
- 기본 투수 기록 (18개 필드, nullable 2개)
- 계산 속성 (12개 필드, nullable 1개)

### Phase 2: Mapper 구현
**위치**: `nextup-api/src/main/kotlin/com/nextup/api/mapper/stats/`

#### StatsMapper.kt
**Extension Functions**:
1. `SeasonBattingStats.toResponse()` - SeasonBattingStatsResponse 변환
2. `SeasonPitchingStats.toResponse()` - SeasonPitchingStatsResponse 변환
3. `CareerBattingStats.toResponse()` - CareerBattingStatsResponse 변환
4. `CareerPitchingStats.toResponse()` - CareerPitchingStatsResponse 변환
5. `List<SeasonBattingStats>.toSeasonBattingResponse()` - 리스트 변환
6. `List<SeasonPitchingStats>.toSeasonPitchingResponse()` - 리스트 변환

**변환 규칙**:
- BigDecimal → String 변환 (`.toPlainString()`)
- Player 관계 → playerId 추출
- 모든 계산 속성 포함
- Nullable 필드 처리

### Phase 3: Controller 구현
**위치**: `nextup-api/src/main/kotlin/com/nextup/api/controller/stats/`

#### PlayerStatsController.kt
**Base Path**: `/api/v1/players/{playerId}/stats`

**Endpoints (6개)**:

1. **GET /batting/season/{year}**
   - 시즌 타격 통계 조회
   - Response: `ApiResponse<SeasonBattingStatsResponse>`

2. **GET /pitching/season/{year}**
   - 시즌 투수 통계 조회
   - Response: `ApiResponse<SeasonPitchingStatsResponse>`

3. **GET /batting/career**
   - 통산 타격 통계 조회
   - Response: `ApiResponse<CareerBattingStatsResponse>`

4. **GET /pitching/career**
   - 통산 투수 통계 조회
   - Response: `ApiResponse<CareerPitchingStatsResponse>`

5. **GET /batting/seasons**
   - 모든 시즌 타격 통계 조회
   - Response: `ApiResponse<List<SeasonBattingStatsResponse>>`

6. **GET /pitching/seasons**
   - 모든 시즌 투수 통계 조회
   - Response: `ApiResponse<List<SeasonPitchingStatsResponse>>`

## 아키텍처 원칙 준수

### Zero Entity Leak (✅ 준수)
- ❌ Entity 직접 반환 없음
- ✅ 모든 응답은 DTO로 변환
- ✅ Extension Function 사용 (`.toResponse()`)

### ApiResponse 래핑 (✅ 준수)
- ✅ 모든 API 응답은 `ApiResponse.success()`로 래핑
- ✅ 성공/실패 일관성 유지
- ✅ GlobalExceptionHandler에서 에러 처리

### 의존성 규칙 (✅ 준수)
```
API → Infrastructure → Core → Common
```
- ✅ API는 Infrastructure Service 호출
- ✅ Core Entity는 API에서 노출되지 않음
- ✅ 순환 참조 없음

### REST Convention (✅ 준수)
- ✅ GET 메서드만 사용 (조회 API)
- ✅ 명사형 URL 구조 (`/players/{id}/stats/...`)
- ✅ 계층 구조 명확 (player → stats → type → period)

## 코드 품질

### Kotlin Convention
- ✅ Extension Function 활용
- ✅ data class 사용 (immutable)
- ✅ Nullable 타입 명시 (`pitchesThrown: Int?`)
- ✅ KDoc 주석 작성

### 패턴 일관성
- ✅ BattingRecordResponse/PitchingRecordResponse 패턴 재사용
- ✅ Mapper 네이밍 통일 (`.toResponse()`)
- ✅ Controller 메서드명 명확 (`getSeasonBattingStats`)

## 빌드 검증

### API 모듈 빌드
```
./gradlew :nextup-api:build -x test
BUILD SUCCESSFUL in 8s
11 actionable tasks: 4 executed, 7 up-to-date
```

### 전체 빌드
```
./gradlew build -x test
BUILD SUCCESSFUL in 1s
11 actionable tasks: 11 up-to-date
```

## 파일 목록

### 새로 생성된 파일 (6개)

#### DTO (4개)
1. `/nextup-api/src/main/kotlin/com/nextup/api/dto/stats/SeasonBattingStatsResponse.kt`
2. `/nextup-api/src/main/kotlin/com/nextup/api/dto/stats/SeasonPitchingStatsResponse.kt`
3. `/nextup-api/src/main/kotlin/com/nextup/api/dto/stats/CareerBattingStatsResponse.kt`
4. `/nextup-api/src/main/kotlin/com/nextup/api/dto/stats/CareerPitchingStatsResponse.kt`

#### Mapper (1개)
5. `/nextup-api/src/main/kotlin/com/nextup/api/mapper/stats/StatsMapper.kt`

#### Controller (1개)
6. `/nextup-api/src/main/kotlin/com/nextup/api/controller/stats/PlayerStatsController.kt`

### 수정된 파일
없음 (신규 구현)

## API 명세

### 1. 시즌 타격 통계
```http
GET /api/v1/players/{playerId}/stats/batting/season/{year}
```
**Response**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "playerId": 123,
    "year": 2024,
    "gamesPlayed": 50,
    "atBats": 200,
    "hits": 60,
    "battingAverage": "0.300",
    "ops": "0.850",
    ...
  },
  "error": null
}
```

### 2. 시즌 투수 통계
```http
GET /api/v1/players/{playerId}/stats/pitching/season/{year}
```
**Response**:
```json
{
  "success": true,
  "data": {
    "id": 2,
    "playerId": 123,
    "year": 2024,
    "gamesPlayed": 20,
    "wins": 5,
    "losses": 3,
    "earnedRunAverage": "3.50",
    "whip": "1.20",
    ...
  },
  "error": null
}
```

### 3. 통산 타격 통계
```http
GET /api/v1/players/{playerId}/stats/batting/career
```

### 4. 통산 투수 통계
```http
GET /api/v1/players/{playerId}/stats/pitching/career
```

### 5. 모든 시즌 타격 통계
```http
GET /api/v1/players/{playerId}/stats/batting/seasons
```
**Response**: 배열 형태

### 6. 모든 시즌 투수 통계
```http
GET /api/v1/players/{playerId}/stats/pitching/seasons
```
**Response**: 배열 형태

## 에러 처리

### GlobalExceptionHandler 활용
- `IllegalArgumentException` → 400 Bad Request (통계 없음)
- `NotFoundException` → 404 Not Found (존재하지 않는 선수)
- 기타 예외 → 500 Internal Server Error

**에러 응답 예시**:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOT_FOUND",
    "message": "선수 ID 999 의 2024년도 타격 통계가 존재하지 않습니다."
  }
}
```

## 다음 단계

1. ~~Controller 구현~~ ✅ 완료
2. ~~DTO/Mapper 구현~~ ✅ 완료
3. Integration Test 작성 (선택사항)
4. API 문서화 (Swagger/OpenAPI)
5. E2E 테스트 (선택사항)

## 검증 체크리스트

- [x] Zero Entity Leak 준수
- [x] ApiResponse 래핑
- [x] 의존성 규칙 준수
- [x] REST Convention 준수
- [x] Extension Function 활용
- [x] BigDecimal → String 변환
- [x] Nullable 타입 처리
- [x] KDoc 주석 작성
- [x] 빌드 성공 (API 모듈)
- [x] 빌드 성공 (전체)
- [x] GlobalExceptionHandler 활용
- [x] 기존 패턴 재사용

## 참고 사항

### BigDecimal → String 변환 이유
클라이언트(JavaScript, Swift 등)에서 BigDecimal 타입을 직접 처리할 수 없으므로,
`.toPlainString()`을 사용하여 문자열로 변환합니다.

예시:
- `BigDecimal("0.300")` → `"0.300"` (String)
- `BigDecimal("3.50")` → `"3.50"` (String)

### Service 레이어 재사용
`PlayerStatsService`가 이미 Infrastructure 모듈에 구현되어 있어,
API 계층에서는 DTO 변환과 Controller만 구현하였습니다.

## 결론

Issue #14의 API 계층 구현이 성공적으로 완료되었습니다.
Zero Entity Leak, ApiResponse 래핑, REST Convention 등
모든 아키텍처 원칙을 준수하였으며, 빌드도 성공하였습니다.

6개의 엔드포인트를 통해 선수의 시즌/통산 타격/투수 통계를 조회할 수 있으며,
모든 응답은 일관된 형태로 제공됩니다.
