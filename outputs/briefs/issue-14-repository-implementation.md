# Issue #14: Stats Repository 구현 완료

## 개요
14번 이슈의 Infrastructure Repository 4개를 성공적으로 구현했습니다.

## 구현 위치
`nextup-infrastructure/src/main/kotlin/com/nextup/infrastructure/repository/stats/`

## 구현된 Repository

### 1. SeasonBattingStatsRepository
**파일**: `SeasonBattingStatsRepository.kt`

**주요 메서드**:
- `findByPlayerIdAndYear()` - 선수 ID와 연도로 시즌 타격 통계 조회
- `findAllByPlayerId()` - 선수의 모든 시즌 타격 통계 조회 (연도 내림차순)
- `findAllByYear()` - 특정 연도의 모든 시즌 타격 통계 조회
- `findTopByBattingAverage()` - 타율 상위 N명 조회 (최소 타수 조건)
- `findTopByHomeRuns()` - 홈런 상위 N명 조회
- `findTopByRunsBattedIn()` - 타점 상위 N명 조회
- `findTopByOps()` - OPS 상위 N명 조회 (최소 타석 조건)

**특징**:
- 타율/OPS 계산은 JPQL에서 직접 수행
- 리더보드용 쿼리 포함 (최소 타수/타석 조건 적용)

### 2. SeasonPitchingStatsRepository
**파일**: `SeasonPitchingStatsRepository.kt`

**주요 메서드**:
- `findByPlayerIdAndYear()` - 선수 ID와 연도로 시즌 투수 통계 조회
- `findAllByPlayerId()` - 선수의 모든 시즌 투수 통계 조회 (연도 내림차순)
- `findAllByYear()` - 특정 연도의 모든 시즌 투수 통계 조회
- `findTopByEra()` - ERA 상위 N명 조회 (최소 이닝 조건, ASC 정렬)
- `findTopByWins()` - 승수 상위 N명 조회
- `findTopByStrikeouts()` - 삼진 상위 N명 조회
- `findTopBySaves()` - 세이브 상위 N명 조회
- `findTopByWhip()` - WHIP 상위 N명 조회 (최소 이닝 조건, ASC 정렬)

**특징**:
- ERA 계산: `(earnedRuns * 27.0) / inningsPitchedOuts` (9이닝 환산)
- WHIP 계산: `(hitsAllowed + walksAllowed) * 3.0 / inningsPitchedOuts` (1이닝 환산)
- 낮을수록 좋은 지표는 ASC 정렬 (ERA, WHIP)

### 3. CareerBattingStatsRepository
**파일**: `CareerBattingStatsRepository.kt`

**주요 메서드**:
- `findByPlayerId()` - 선수 ID로 통산 타격 통계 조회
- `findTopByBattingAverage()` - 통산 타율 상위 N명 조회 (최소 타수 조건)
- `findTopByHomeRuns()` - 통산 홈런 상위 N명 조회
- `findTopByRunsBattedIn()` - 통산 타점 상위 N명 조회
- `findTopByHits()` - 통산 안타 상위 N명 조회
- `findTopByStolenBases()` - 통산 도루 상위 N명 조회
- `findTopByOps()` - 통산 OPS 상위 N명 조회 (최소 타석 조건)
- `findTopBySluggingPercentage()` - 통산 장타율 상위 N명 조회 (최소 타수 조건)

**특징**:
- OneToOne 관계 (Player당 하나의 CareerBattingStats)
- 통산 기록 리더보드용 쿼리 포함

### 4. CareerPitchingStatsRepository
**파일**: `CareerPitchingStatsRepository.kt`

**주요 메서드**:
- `findByPlayerId()` - 선수 ID로 통산 투수 통계 조회
- `findTopByEra()` - 통산 ERA 상위 N명 조회 (최소 이닝 조건, ASC 정렬)
- `findTopByWins()` - 통산 승수 상위 N명 조회
- `findTopByStrikeouts()` - 통산 삼진 상위 N명 조회
- `findTopBySaves()` - 통산 세이브 상위 N명 조회
- `findTopByWhip()` - 통산 WHIP 상위 N명 조회 (최소 이닝 조건, ASC 정렬)
- `findTopByInningsPitched()` - 통산 이닝 상위 N명 조회
- `findTopByStrikeoutToWalkRatio()` - 통산 K/BB 비율 상위 N명 조회 (최소 이닝 및 볼넷 조건)

**특징**:
- OneToOne 관계 (Player당 하나의 CareerPitchingStats)
- K/BB 비율 계산 시 0으로 나누기 방지 (walksAllowed > 0 조건)

## 주요 설계 결정

### 1. JPQL 내에서 지표 계산
**이유**: Entity의 계산 프로퍼티는 JPA 쿼리에서 사용할 수 없으므로, JPQL에서 직접 계산식 작성

**예시**:
```kotlin
// 타율 계산: hits / atBats
ORDER BY (CAST(s.hits AS double) / CAST(s.atBats AS double)) DESC

// ERA 계산: (earnedRuns * 9) / innings
ORDER BY (CAST(s.earnedRuns AS double) * 27.0 / CAST(s.inningsPitchedOuts AS double)) ASC
```

### 2. 최소 자격 조건
**타격 지표**:
- 타율, OPS, 장타율: 최소 타수/타석 조건 적용
- 홈런, 타점, 안타: 자격 조건 없음 (절대값 비교)

**투수 지표**:
- ERA, WHIP: 최소 이닝 조건 적용
- 승수, 삼진, 세이브: 자격 조건 없음 (절대값 비교)

### 3. 정렬 방향
- **DESC (높을수록 좋음)**: 타율, 홈런, 타점, OPS, 승수, 삼진, 세이브
- **ASC (낮을수록 좋음)**: ERA, WHIP

## 빌드 검증

```bash
./gradlew clean build
```

**결과**: ✅ BUILD SUCCESSFUL

- 모든 모듈 컴파일 성공
- 전체 테스트 통과
- 의존성 규칙 준수 (Infrastructure → Core → Common)

## 다음 단계

1. **Service 계층 구현** (#12 이슈)
   - StatsService 작성
   - BattingRecord/PitchingRecord 저장 시 Stats 자동 업데이트 로직

2. **API 계층 구현**
   - StatsController 작성
   - DTO 및 Mapper 작성
   - 리더보드 API 엔드포인트 제공

3. **테스트 작성**
   - Repository 통합 테스트
   - Service 단위 테스트
   - API E2E 테스트

## 파일 목록

1. `/nextup-infrastructure/src/main/kotlin/com/nextup/infrastructure/repository/stats/SeasonBattingStatsRepository.kt`
2. `/nextup-infrastructure/src/main/kotlin/com/nextup/infrastructure/repository/stats/SeasonPitchingStatsRepository.kt`
3. `/nextup-infrastructure/src/main/kotlin/com/nextup/infrastructure/repository/stats/CareerBattingStatsRepository.kt`
4. `/nextup-infrastructure/src/main/kotlin/com/nextup/infrastructure/repository/stats/CareerPitchingStatsRepository.kt`

---

**작성일**: 2026-02-01
**작성자**: implementer agent
**검증 상태**: ✅ 빌드 성공, 컴파일 오류 없음
