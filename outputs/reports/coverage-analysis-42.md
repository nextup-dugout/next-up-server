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
