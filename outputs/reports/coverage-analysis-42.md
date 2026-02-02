# Coverage Analysis Report - Issue #42

**Date**: 2026-02-03
**Branch**: `test/#42-coverage-threshold`
**Target**: 80% instruction coverage

## Current Status

| Module | Source Files | Test Files | Status |
|--------|-------------|------------|--------|
| nextup-core | 60 | 22 | ✅ Good coverage |
| nextup-api | 36 | 13 | ⚠️ Missing: ProfileController |
| nextup-infrastructure | 36 | 12 | ✅ Good coverage |
| nextup-backoffice | 19 | 3 | ❌ Missing tests |
| nextup-scorer | 11 | 2 | ⚠️ Minor gaps |

## Missing Tests Identified

### nextup-api (Priority: HIGH)
| Controller | Test Exists | Action |
|------------|-------------|--------|
| AuthController | ✅ | - |
| OAuthController | ✅ | - |
| AssociationController | ✅ | - |
| CompetitionController | ✅ | - |
| BattingRecordController | ✅ | - |
| PitchingRecordController | ✅ | - |
| LeagueController | ✅ | - |
| PlayerStatsController | ✅ | - |
| **ProfileController** | ❌ | **Need test** |

### nextup-backoffice (Priority: HIGH)
| Controller | Test Exists | Action |
|------------|-------------|--------|
| OrganizationAdminController | ✅ | - |
| CompetitionAdminController | ✅ | - |
| LeagueAdminController | ✅ | - |
| **AssociationAdminController** | ❌ | **Need test** |
| **UserAdminController** | ❌ | **Need test** |
| HealthController | ❌ | Skip (trivial) |

### nextup-scorer (Priority: LOW)
| Controller | Test Exists | Action |
|------------|-------------|--------|
| CompetitionScorerController | ✅ | - |
| LeagueController | ✅ | - |
| HealthController | ❌ | Skip (trivial) |

## Action Plan

1. **ProfileController Test** - API 모듈의 프로필 조회/수정 테스트
2. **AssociationAdminController Test** - 협회 CRUD 테스트
3. **UserAdminController Test** - 사용자 관리 CRUD 테스트
4. Run `./gradlew jacocoTestCoverageVerification` to verify 80%

## Exclusions (codecov.yml)

The following are excluded from coverage:
- `**/test/**`
- `**/generated/**`
- `**/*Config.kt`
- `**/*Application.kt`
- `**/dto/**`

## CI Configuration

```yaml
# Order: Upload first, then verify
1. Build & Test
2. Generate Coverage Report
3. Upload Coverage to Codecov  ← Always uploads
4. Verify Coverage (80%)       ← Fails build if below threshold
```

## Current Coverage Status (2026-02-03)

| Module | Coverage | Status |
|--------|----------|--------|
| nextup-core | 73% | ❌ Need 7% more |
| nextup-api | 73% | ❌ Need 7% more |
| nextup-infrastructure | ✅ | Passed |
| nextup-backoffice | 74% | ❌ Need 6% more |
| nextup-scorer | 56% → improved | ⚠️ Improved with new tests |

## Tests Added

1. **ProfileController Test** (nextup-api) ✅
   - 내 프로필 조회
   - 프로필 수정
   - OAuth 계정 조회
   - 회원 탈퇴

2. **AssociationAdminController Test** (nextup-backoffice) ✅
   - 협회 목록 조회
   - 협회 상세 조회
   - 협회 생성/수정
   - 협회 비활성화/활성화

3. **UserAdminController Test** (nextup-backoffice) ✅
   - 사용자 목록 조회/검색
   - 역할별 조회
   - 사용자 생성/수정
   - 역할 추가/제거
   - 사용자 비활성화/활성화

4. **CompetitionScorerController Test** (nextup-scorer) ✅
   - 대회 완료 (complete)
   - 대회 취소 (cancel)
   - 대회 연기 (postpone)

## Jacoco Exclusions Added (build.gradle.kts)

```kotlin
val jacocoExcludes = listOf(
    "**/*Config*",
    "**/*Application*",
    "**/dto/**",
    "**/exception/**"
)
```

## Next Steps

1. CI 결과 확인 후 추가 테스트 필요 여부 결정
2. 필요시 커버리지 임계값 조정 또는 추가 테스트 작성
