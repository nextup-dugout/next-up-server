---
name: batch-issues
description: |
  열린 GitHub 이슈를 자동 수집하고, 이슈 간 의존관계를 분석하여 실행 순서(Wave)를 결정한 뒤,
  Wave별로 병렬 구현 파이프라인(issue-progress)을 실행하는 배치 오케스트레이터.
  이슈 유형(entity/service/api/bug)과 관련 스킬을 판단하고, 의존 순서를 자동 조율한다.
  반드시 이 스킬을 사용하라 — 다음 상황에서:
  (1) 여러 이슈를 한꺼번에 처리: "이슈 다 진행해줘", "열린 이슈 전부 해줘", "남은 이슈 처리",
      "밀린 이슈 다 해줘", "이슈 일괄 처리", "batch issues", "open issue 전부"
  (2) 복수 이슈 번호 지정: "#440 #441 #442 진행해줘", "#123, #456 해줘", "이 세개 이슈 같이"
  (3) 이슈 목록 기반 자율 실행: "이슈 목록 보고 알아서 순서 정해서 구현", "이슈 몇개 남았는지 보고 전부 작업"
  단, 단일 이슈("#123 해줘", "423번 구현해줘")에는 issue-progress를 사용한다.
  이 스킬은 2개 이상의 이슈를 동시에 처리할 때만 사용한다.
user-invocable: true
argument-hint: "[라벨 필터] e.g. Feature, Bug (생략하면 전체 열린 이슈)"
---

# Batch Issues - 이슈 분석 → 실행 순서 결정 → 병렬 처리

열린 GitHub 이슈를 자동으로 수집하고, **이슈 간 의존관계를 분석하여 실행 순서(Wave)를 결정**한 뒤, 각 Wave 내에서 병렬로 구현 파이프라인을 실행하는 오케스트레이터 스킬이다.

핵심 차별점: 무작정 모든 이슈를 동시에 돌리는 게 아니라, **어떤 이슈를 먼저 해야 하고 어떤 이슈끼리 동시에 돌릴 수 있는지**를 스스로 판단한다.

---

## Phase 0: 이슈 수집 및 필터링

### 0-1. 열린 이슈 목록 읽기

MCP GitHub 도구로 열린 이슈를 가져온다:

```
mcp__github__list_issues:
  owner: nextup-dugout
  repo: next-up-server
  state: open
```

`$ARGUMENTS`에 라벨이 지정되었으면 해당 라벨로 필터링한다. 지정되지 않았으면 전체 열린 이슈를 대상으로 한다.

### 0-2. 이미 진행 중인 이슈 제외

다음 조건에 해당하는 이슈는 **자동 제외**한다:

1. **이미 연결된 PR이 있는 이슈** — `mcp__github__search_pull_requests`로 `Close #N` 패턴이 있는 열린 PR 확인
2. **이미 브랜치가 존재하는 이슈** — `mcp__github__list_branches`에서 `feature/#N-`, `fix/#N-`, `refactor/#N-` 패턴 매칭
3. **Pull Request 자체인 항목** — `pull_request` 필드가 있는 항목 제외

이 필터링의 목적은 중복 작업을 방지하는 것이다. 이미 누군가(사람이든 에이전트든) 작업 중인 이슈를 다시 시작하면 충돌만 생긴다.

### 0-3. 대상 이슈 수 분기

필터링 결과에 따라 분기한다:

- **0개**: `처리할 이슈가 없습니다.`를 보고하고 **종료**한다.
- **1개**: batch 오케스트레이션 없이 `issue-progress` 스킬로 직접 위임하고 **종료**한다.
- **2개 이상**: Phase 1로 진행한다.

---

## Phase 1: 이슈 분석 및 실행 계획 수립

이 Phase가 스킬의 핵심이다. 각 이슈를 깊이 읽고, 이슈 간 관계를 파악하여 실행 순서를 결정한다.

### 1-1. 각 이슈 심층 분석

모든 대상 이슈에 대해 다음을 판단한다:

**A. 이슈 유형 분류**

| 유형 | 판단 기준 | 예시 |
|------|-----------|------|
| `entity` | 새 Entity 생성, 도메인 모델 추가/변경 | "선수 이적 기록 Entity 추가" |
| `service` | 비즈니스 로직 추가, 기존 Entity 활용 | "이적 기록 집계 서비스 구현" |
| `api` | Controller/DTO 추가, 기존 Service 활용 | "이적 기록 조회 API" |
| `bug` | 기존 코드 수정, 독립적 | "타석 기록 집계 오류 수정" |
| `refactor` | 구조 개선, 독립적 | "기록 서비스 리팩토링" |
| `infra` | DB 마이그레이션, 설정 변경 | "PostGIS 인덱스 추가" |

**B. 관련 스킬 매핑**

`.claude/skills/issue-progress/SKILL.md`의 Phase 0 step 0-2 "이슈 내용 → 관련 스킬 매핑" 테이블과 동일한 로직을 따른다. issue-progress를 참조하여 이슈 제목/본문 키워드에서 관련 스킬을 식별한다.

**C. 영향 범위 파악**

이슈 본문에서 언급되는 모듈과 파일 경로를 추출한다:
- 어떤 모듈을 건드리는지 (core, infra, api, backoffice, scorer)
- 어떤 Entity/Service/Controller에 관련되는지
- 기존 코드베이스를 Grep/Glob으로 확인하여 실제 존재 여부 파악

### 1-2. 이슈 간 의존관계 분석

이슈들 사이의 선후관계를 판단한다. 판단 기준:

**의존 관계가 있는 경우:**
- 이슈 A가 Entity를 만들고, 이슈 B가 그 Entity의 API를 만드는 경우 → A가 먼저
- 이슈 A가 Service를 만들고, 이슈 B가 그 Service를 호출하는 Controller를 만드는 경우 → A가 먼저
- 이슈 A가 DB 마이그레이션을 추가하고, 이슈 B가 그 테이블을 사용하는 경우 → A가 먼저
- 이슈 본문에 "~을 선행으로", "~가 완료된 후", "depends on #N" 등 명시적 의존 언급

**독립적인 경우:**
- 서로 다른 도메인 영역을 다루는 이슈 (예: 팀 관련 vs 경기 기록 관련)
- 같은 도메인이라도 서로 다른 Entity/모듈을 수정하는 경우
- 버그 수정, 리팩토링 등 기존 코드만 수정하는 이슈

**의존관계가 불확실한 경우:**
- 확신이 없으면 **안전하게 별도 Wave로 분리**한다 (같은 Wave에 넣지 않음)
- 잘못 병렬로 돌려서 빌드 실패하는 것보다, 순차로 돌리는 게 낫다

**순환 의존 발견 시:**
- A→B, B→A 순환이 감지되면 사용자에게 보고하고 수동 판단을 요청한다
- 순환 의존은 자동으로 해소할 수 없으므로 이슈 분리나 병합이 필요하다

### 1-3. Wave 편성

의존관계를 바탕으로 이슈를 **Wave(실행 단계)**로 그룹화한다:

```
Wave 1: 기반 작업 (Entity 생성, DB 마이그레이션, Core 모듈)
  ↓ Wave 1 완료 후
Wave 2: 의존 작업 (Service 구현, Wave 1 결과에 의존하는 작업)
  ↓ Wave 2 완료 후
Wave 3: API 계층 (Controller/DTO, Wave 2 Service에 의존)
  ↓
Wave N: 독립 작업 (버그 수정, 리팩토링 — 어느 Wave에든 배치 가능)
```

**편성 규칙:**
- 같은 Wave 안의 이슈들은 서로 의존하지 않으므로 **동시 실행 가능**
- 다음 Wave는 이전 Wave가 모두 완료된 후 시작
- 독립적인 이슈(bug, refactor)는 **가장 빠른 Wave에 배치**하여 대기 시간을 줄인다
- Wave 내 이슈가 1개뿐이면 다른 독립 이슈와 합쳐서 병렬성을 높인다

### 1-4. 실행 계획 표시

분석 결과를 사용자에게 보여준다:

```markdown
## 일괄 처리 실행 계획

### 분석 결과
| # | 이슈 | 유형 | 관련 스킬 | 영향 모듈 |
|---|------|------|-----------|-----------|
| 1 | #440 선수 이적 Entity | entity | backend-patterns, domain-baseball | core, infra |
| 2 | #441 이적 기록 조회 API | api | verify-entity-leak, verify-api-response | api, core |
| 3 | #442 타석 기록 집계 버그 | bug | domain-baseball | core |
| 4 | #443 기록원 권한 추가 | service | verify-authorization, security-audit | scorer, core |

### 의존관계
- #441(API) → #440(Entity): 이적 Entity가 있어야 조회 API 구현 가능

### 실행 순서
**Wave 1** (동시 실행): #440, #442, #443
  - #440: 이적 Entity 생성 (core, infra)
  - #442: 타석 기록 버그 수정 (core) — 독립적
  - #443: 기록원 권한 (scorer, core) — 독립적

**Wave 2** (Wave 1 완료 후): #441
  - #441: 이적 기록 조회 API (#440 결과 필요)

바로 진행합니다.
```

사용자 확인을 **기다리지 않고** 바로 Phase 2로 넘어간다.

---

## Phase 2: Wave별 순차 실행

Wave 단위로 순서대로 실행한다. **같은 Wave 안의 이슈들은 병렬**, **Wave 간에는 순차** 실행이다.

### 2-1. 에이전트 생성 방법

각 이슈마다 Agent 도구를 사용하여 독립된 에이전트를 생성한다. 같은 Wave의 모든 에이전트는 **한 번의 메시지에서 동시에** Agent 호출한다:

```
Agent:
  description: "이슈 #[N] 구현"
  prompt: "[2-3의 프롬프트 템플릿]"
  isolation: "worktree"
  run_in_background: true
  mode: "bypassPermissions"
```

**규칙:**
- **Wave 내 병렬**: 같은 Wave의 Agent 호출을 한 메시지에 모두 포함
- **Wave 간 순차**: 이전 Wave의 모든 에이전트 완료 알림을 받은 후 다음 Wave 시작
- **격리**: `isolation: "worktree"`로 각 에이전트가 독립된 git 워크트리에서 작업
- **백그라운드**: `run_in_background: true`로 병렬 실행
- **동시 에이전트 상한: 3개**. Wave 내 이슈가 4개 이상이면 라운드를 나눈다:
  - 예: Wave에 이슈 7개 → Round 1 (3개) → Round 2 (3개) → Round 3 (1개)
  - 각 라운드의 모든 에이전트가 완료된 후 다음 라운드를 시작한다
  - 이유: 16GB 메모리 환경에서 Gradle `--no-daemon` 빌드가 에이전트당 ~2-3GB를 소비하므로, 4개 이상 동시 빌드 시 OOM으로 Gradle 데몬이 크래시한다. 3개가 안정적인 상한이다.

### 2-2. Wave 간 코드 연결

다음 Wave를 시작할 때, 이전 Wave의 코드가 develop에 머지되지 않은 상태이므로 다음 Wave 에이전트의 프롬프트에 **이전 Wave 브랜치를 merge하라**고 지시한다:

```
## 선행 Wave 코드 merge (반드시 작업 시작 전에 실행)
git fetch origin
git merge origin/feature/#440-transfer-entity
```

이렇게 하면 이전 Wave에서 추가된 Entity/Service가 워크트리에 실제로 존재하므로 빌드가 가능하다.

### 2-3. 에이전트에게 issue-progress 위임

각 에이전트는 `issue-progress` 스킬을 사용하여 이슈를 처리한다. batch-issues는 구현 방법을 직접 지시하지 않고, 분석 결과와 컨텍스트만 전달한다:

```
이슈 #[N]을 issue-progress 스킬에 따라 자율적으로 구현하라.

.claude/skills/issue-progress/SKILL.md를 읽고 그대로 따르되,
다음 두 가지 사용자 확인 단계만 생략한다:
- issue-progress Phase 0의 마지막 단계 "진행할까요?" 사용자 확인
- issue-progress Phase 2에서 planner 계획 후 "사용자 승인"

나머지 모든 Phase(브랜치 생성, 스킬 매핑, 설계, 구현, 검증, PR 생성)는 그대로 실행한다.

## 사전 분석 결과 (batch-issues가 판단한 정보)
- 이슈 유형: [entity/service/api/bug/refactor/infra]
- 관련 스킬: [식별된 스킬 목록]
- 영향 모듈: [core/infra/api/backoffice/scorer]

## 선행 Wave 코드 merge (Wave 2+ 에만 포함)
작업 시작 전에 다음을 실행하라:
git fetch origin
git merge origin/[이전 Wave 브랜치명]
```

이렇게 하면 구현의 세부 사항은 `issue-progress`가 전부 관리하고, batch-issues는 **순서 조율과 컨텍스트 전달**에만 집중한다.

---

## Phase 3: 진행 상황 모니터링 및 Wave 전환

### 3-1. Wave 내 모니터링

에이전트들은 백그라운드에서 실행된다. 완료 알림이 올 때마다 결과를 수집한다:
- 이슈 번호
- 성공/실패 여부
- PR URL (성공 시)
- 실패 사유 (실패 시)
- 생성된 브랜치명 (다음 Wave merge용)

### 3-2. Wave 전환

현재 Wave의 **모든 에이전트가 완료**되면:
1. 성공한 에이전트의 브랜치명을 수집한다. **브랜치가 remote에 push 완료된 상태임을 확인**한다 (issue-progress Phase 6에서 push가 완료됨).
2. 실패한 이슈가 있으면 기록하되, 다음 Wave에 의존하는 이슈만 영향을 받는다
3. 다음 Wave 에이전트 프롬프트에 **선행 Wave 브랜치 merge 명령**을 포함하여 생성한다

**실패 전파 규칙:**
- Wave 1의 이슈 A가 실패했고, Wave 2의 이슈 B가 A에 의존하면 → B도 건너뛴다 (사유: "선행 이슈 #A 실패")
- Wave 1의 이슈 A가 실패했지만, Wave 2의 이슈 C가 A에 의존하지 않으면 → C는 정상 진행

### 3-3. 최종 리포트

모든 Wave가 완료되면 사용자에게 요약을 보고한다:

```markdown
## 일괄 처리 결과

### Wave 1 (기반 작업)
| 이슈 | 유형 | 결과 | PR |
|------|------|------|----|
| #440 선수 이적 Entity | entity | ✅ 성공 | PR #450 |
| #442 타석 기록 버그 | bug | ✅ 성공 | PR #451 |
| #443 기록원 권한 | service | ✅ 성공 | PR #452 |

### Wave 2 (#440 완료 후)
| 이슈 | 유형 | 결과 | PR |
|------|------|------|----|
| #441 이적 기록 조회 API | api | ✅ 성공 | PR #453 |

**전체**: 4/4 성공
```

실패한 이슈가 있으면 사유를 간략히 설명하고, 사용자가 개별적으로 `issue-progress`로 재시도할 수 있음을 안내한다.

---

## 주의사항

- **MCP GitHub 도구 사용**: `gh` CLI는 미인증 상태이므로 반드시 `mcp__github__*` 도구 사용
- **머지 충돌**: 병렬로 생성된 PR들이 같은 파일을 수정하면 머지 시 충돌이 발생할 수 있다. 이는 머지 시점에 해결한다.
- **Flyway 마이그레이션 번호**: 여러 이슈가 동시에 마이그레이션을 추가하면 번호가 충돌할 수 있다. 각 에이전트는 기존 마이그레이션 파일의 최대 버전 번호를 확인한 뒤 +1로 동적 할당한다. 같은 Wave에서 충돌 가능성이 있으면 사전에 번호를 배분한다.
- **Gradle 데몬**: 워크트리는 물리적으로 분리되어 있지만 Gradle daemon은 공유될 수 있다. 각 에이전트는 `--no-daemon` 옵션으로 Gradle을 실행하여 데몬 충돌을 방지한다.
- **PR 머지 순서**: Wave 순서대로 PR을 머지하는 것을 권장한다. Wave 2 PR을 Wave 1보다 먼저 머지하면 충돌이 발생할 수 있다.
