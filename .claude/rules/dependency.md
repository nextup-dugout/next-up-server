# Multi-Module Dependency Rules

## Absolute Dependency Direction

```
Outside → Inside (ONLY this direction allowed)
Core NEVER knows about Infra or API
```

## Module Hierarchy (IMMUTABLE)

```
nextup-api → nextup-infrastructure → nextup-core → nextup-common
```

## Dependency Matrix

| Module | Allowed Dependencies | FORBIDDEN |
|--------|---------------------|-----------|
| `nextup-api` | infra, core, common | - |
| `nextup-core` | **common ONLY** | infra, api (NEVER) |
| `nextup-infrastructure` | core, common | api (NEVER) |
| `nextup-common` | **NONE** (leaf module) | ALL dependencies |

## Critical Rules

Before ANY commit:
- [ ] No circular dependencies
- [ ] Core does not import from Infra/API
- [ ] Common has zero dependencies
- [ ] No business logic in Common

## Violation Response

If dependency rule violated:
1. **STOP immediately**
2. Reviewer VETO - automatic REJECT
3. Refactor to correct direction
4. Build MUST pass before proceeding
