# ADR-001: League/Competition CRUD API Module Placement

## Context

NEXT-UP 프로젝트는 3개의 API 모듈을 운영합니다:
- **nextup-api (8080)**: 일반 사용자 조회 API
- **nextup-backoffice (8081)**: 시스템 관리자용
- **nextup-scorer (8082)**: 기록원용 (실시간 경기 기록, WebSocket)

현재 Association(협회) CRUD는 backoffice에 구현되어 있습니다.
League(리그)와 Competition(대회)의 CRUD API를 어느 모듈에 배치할지 결정해야 합니다.

### Domain Hierarchy
```
Association (협회)
    └── League (리그) - 협회 소속
            └── Competition (대회) - 리그에서 개최하는 시즌별 대회
                    └── Game (경기) - 대회 내 경기
```

### User Roles
| Role | Description | Primary Module |
|------|-------------|----------------|
| 일반 사용자 | 기록 조회, 팀/선수 검색 | api |
| 기록원 (Scorer) | 실시간 경기 기록 입력 | scorer |
| 협회 관리자 | 본인 협회의 리그/대회 관리 | scorer |
| 시스템 관리자 | 전체 시스템 관리, 데이터 보정 | backoffice |

## Decision

**Option 2 Modified (Hybrid Approach)**를 채택합니다.

League/Competition CRUD API를 **scorer와 backoffice 두 모듈에 모두 배치**하되, 권한 범위를 명확히 구분합니다.

### Module Responsibilities

| Module | Responsibility | Authority Scope |
|--------|---------------|-----------------|
| **scorer** | 협회 관리자/기록원이 본인 협회 리그/대회 관리 | 본인 소속 협회만 |
| **backoffice** | 시스템 관리자가 전체 리그/대회 관리 | 모든 협회 |

### Implementation Structure

```
nextup-infrastructure (Shared)
├── service/
│   ├── LeagueService.kt              # 공통 비즈니스 로직
│   └── CompetitionService.kt
└── repository/

nextup-scorer
├── controller/
│   ├── LeagueScorerController.kt     # POST/PUT/DELETE 본인 협회만
│   └── CompetitionScorerController.kt
└── dto/
    └── league/
        ├── LeagueRequest.kt
        └── LeagueResponse.kt

nextup-backoffice
├── controller/
│   ├── LeagueAdminController.kt      # 전체 CRUD + activate/deactivate
│   └── CompetitionAdminController.kt
└── dto/
    └── league/
        ├── LeagueAdminRequest.kt
        └── LeagueAdminResponse.kt    # isActive 등 관리자 전용 필드
```

### Permission Matrix

| Action | scorer (협회 관리자) | backoffice (시스템 관리자) |
|--------|---------------------|---------------------------|
| Create League | O (본인 협회) | O (모든 협회) |
| Update League | O (본인 협회) | O (모든 협회) |
| Deactivate League | X | O |
| Activate League | X | O |
| List Leagues | 본인 협회만 | 전체 (비활성 포함) |
| Force Data Correction | X | O |

### Authorization Implementation

```kotlin
// scorer - 협회 관리자 권한 검증
@PostMapping
@PreAuthorize("hasRole('ASSOCIATION_ADMIN') and @associationAuthz.canManage(#request.associationId)")
fun createLeague(@RequestBody request: CreateLeagueRequest): ApiResponse<LeagueResponse>

// backoffice - 시스템 관리자 전용
@PostMapping
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
fun createLeague(@RequestBody request: CreateLeagueAdminRequest): ApiResponse<LeagueAdminResponse>
```

## Consequences

### Positive
- **사용자 경험 향상**: 각 역할의 사용자가 자신의 "홈" 시스템에서 필요한 기능에 접근
- **명확한 권한 분리**: 협회 관리자와 시스템 관리자의 권한 범위가 명확
- **Service 재사용**: infrastructure 모듈의 Service 공유로 비즈니스 로직 중복 방지
- **보안 강화**: 각 모듈에서 역할에 맞는 권한 검증 적용
- **확장성**: 향후 협회 관리자 기능 확장 시 scorer 모듈에서 독립적 개발 가능

### Negative
- **Controller/DTO 중복**: 두 모듈에 유사한 Controller와 DTO 존재
- **동기화 필요**: 비즈니스 요구사항 변경 시 두 모듈 동시 수정 필요
- **테스트 범위 증가**: 두 모듈에 대한 권한 테스트 필요

### Mitigation Strategies
1. **DTO 공통화 검토**: common-dto 모듈 분리 또는 infrastructure에 공통 DTO 정의
2. **Integration Test**: 권한 테스트를 통한 보안 검증 자동화
3. **API 문서화**: 각 모듈의 API 차이점 명확히 문서화

## Alternatives Considered

### Option 1: scorer에만 배치
- **Rejected Reason**: 시스템 관리자가 scorer 포트로 접근해야 하며, scorer의 본래 목적(실시간 경기 기록)과 역할이 혼재됨

### Option 3: 역할별 완전 분리 (backoffice에만)
- **Rejected Reason**: 협회 관리자가 backoffice 접근 권한을 받아야 하며, 이는 권한 체계를 복잡하게 만듦. 또한 협회 관리자의 일상적인 리그/대회 관리 업무가 시스템 관리 콘솔에서 수행되는 것은 부자연스러움.

## Status

**Accepted**

## Date

2026-02-02

## Related
- Association CRUD: backoffice 모듈에 구현됨 (`AssociationAdminController`)
- Association 조회: api 모듈에 구현됨 (`AssociationController`)
