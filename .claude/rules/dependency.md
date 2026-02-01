# Multi-Module Dependency Rules

## Absolute Dependency Direction

```
Outside → Inside (ONLY this direction allowed)
Core NEVER knows about Infra or API layers
```

## Module Hierarchy (IMMUTABLE)

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   nextup-api    │    │nextup-backoffice│    │  nextup-scorer  │
│  (일반 사용자)   │    │   (관리자 CRUD) │    │ (실시간 기록)    │
│    port:8080    │    │    port:8081    │    │    port:8082    │
└────────┬────────┘    └────────┬────────┘    └────────┬────────┘
         │                      │                      │
         └──────────────────────┼──────────────────────┘
                                │
                    ┌───────────▼───────────┐
                    │ nextup-infrastructure │
                    │   (Repository, 외부)   │
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │     nextup-core       │
                    │   (도메인, 비즈니스)    │
                    └───────────┬───────────┘
                                │
                    ┌───────────▼───────────┐
                    │    nextup-common      │
                    │      (공통 유틸)       │
                    └───────────────────────┘
```

## Module Roles

| Module | Role | Port |
|--------|------|------|
| `nextup-api` | 일반 사용자용 공개 API (조회 위주) | 8080 |
| `nextup-backoffice` | 관리자 CRUD (협회/리그/팀/시스템 관리) | 8081 |
| `nextup-scorer` | 실시간 경기 기록 (기록원 전용, WebSocket) | 8082 |
| `nextup-infrastructure` | Repository, 외부 연동, 공통 Security | - |
| `nextup-core` | 도메인 엔티티, 비즈니스 로직 | - |
| `nextup-common` | 공통 유틸리티, Exception | - |

## Dependency Matrix

| Module | Allowed Dependencies | FORBIDDEN |
|--------|---------------------|-----------|
| `nextup-api` | infra, core, common | backoffice, scorer |
| `nextup-backoffice` | infra, core, common | api, scorer |
| `nextup-scorer` | infra, core, common | api, backoffice |
| `nextup-infrastructure` | core, common | api, backoffice, scorer |
| `nextup-core` | **common ONLY** | infra, api, backoffice, scorer (NEVER) |
| `nextup-common` | **NONE** (leaf module) | ALL dependencies |

## Critical Rules

Before ANY commit:
- [ ] No circular dependencies
- [ ] Core does not import from Infra/API layers
- [ ] Common has zero dependencies
- [ ] No business logic in Common
- [ ] **API layer modules (api, backoffice, scorer) do NOT depend on each other**

## API Layer Isolation

```
⚠️ IMPORTANT: API 계층 모듈간 상호 의존 금지

nextup-api ←✗→ nextup-backoffice ←✗→ nextup-scorer
```

- 공통 로직은 반드시 `nextup-infrastructure`에 위치
- Controller, DTO는 각 모듈에 독립적으로 존재
- Security 설정은 각 모듈에서 개별 구성

## Violation Response

If dependency rule violated:
1. **STOP immediately**
2. Reviewer VETO - automatic REJECT
3. Refactor to correct direction
4. Build MUST pass before proceeding
