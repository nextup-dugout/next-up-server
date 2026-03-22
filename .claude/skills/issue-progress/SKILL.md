---
name: issue-progress
description: |
  GitHub 이슈 번호를 기반으로 전체 구현 파이프라인을 자동 실행한다.
  이슈 내용을 분석하여 관련 스킬(domain-baseball, backend-patterns, db-manager, verify-* 등)을 자동 식별하고,
  planner → architect → implementer → reviewer → PR 순서로 에이전트 파이프라인을 실행한다.
  "#숫자 진행해줘", "이슈 #숫자 작업", "#숫자 구현", "#숫자 해줘", "숫자번 이슈 시작",
  "#숫자 작업 시작해줘", "이슈 숫자 처리해줘" 등 이슈 번호가 포함된 구현 작업 요청에 반드시 이 스킬을 사용하라.
  단순 이슈 조회("이슈 보여줘", "이슈 목록")가 아닌, 실제 구현/작업 요청일 때만 트리거한다.
user-invocable: true
argument-hint: "<이슈번호> e.g. 426"
---

# Issue Progress - 이슈 기반 구현 파이프라인

GitHub 이슈를 읽고, 내용을 분석하여 관련 스킬을 식별한 뒤, 에이전트 파이프라인을 자동으로 실행하는 오케스트레이션 스킬이다.

## 이슈 번호 추출

`$ARGUMENTS` 또는 사용자 메시지에서 이슈 번호를 추출한다:

| 우선순위 | 소스 | 예시 |
|----------|------|------|
| 1 | `$ARGUMENTS` | `/issue-progress 426` → 426 |
| 2 | `#숫자` 패턴 | "#426 진행해줘" → 426 |
| 3 | 단독 숫자 | "426 해줘" → 426 |
| 4 | 현재 브랜치명 | `feature/#426-xxx` → 426 |
| 5 | 모두 실패 | 사용자에게 질문 |

---

## Phase 0: 이슈 읽기 및 스킬 매핑

### 0-1. 이슈 읽기

MCP GitHub 도구로 이슈를 읽는다:

```
mcp__github__issue_read:
  owner: nextup-dugout
  repo: next-up-server
  issue_number: <이슈번호>
```

### 0-2. 이슈 내용 → 관련 스킬 매핑

이슈 제목과 본문의 키워드를 분석하여 관련 스킬을 식별한다. 이 매핑은 이후 에이전트들에게 **어떤 규칙을 중점적으로 적용할지** 컨텍스트를 제공하기 위함이다.

| 키워드 (이슈 제목/본문에 포함) | 관련 스킬 | 에이전트에게 전달할 지시 |
|-------------------------------|-----------|------------------------|
| Game, BattingRecord, PitchingRecord, 타석, 투구, 이닝, DH, 기록, 득점, 안타, 홈런, 실점, 자책 | `domain-baseball` | 야구 규칙 체크리스트 확인 필수 |
| Controller, API, 엔드포인트, REST, 조회, 목록 | `verify-entity-leak`, `verify-api-response`, `verify-url-prefix`, `verify-repository-injection` | Controller 계층 규칙 전수 검증 |
| POST, PUT, PATCH, DELETE, 생성, 수정, 삭제, 등록 | `verify-authorization` | @PreAuthorize + @AuthenticationPrincipal 필수 |
| Entity, 도메인, 모델, JPA, 매핑, 연관관계 | `backend-patterns` | Rich Domain Model 패턴 적용 |
| Exception, 에러, 오류, 예외, throw | `verify-custom-exception` | BusinessException 계열만 사용 |
| import, 모듈, 의존성, 분리 | `verify-dependency` | 의존성 방향 Outside→Inside 준수 |
| DB, 마이그레이션, 스키마, PostGIS, 테이블, 컬럼 | `db-manager` | Flyway 마이그레이션 관리 |
| 보안, 인증, 인가, 권한, 토큰, JWT | `security-audit`, `verify-authorization` | OWASP 체크리스트 적용 |
| 테스트, TDD, 커버리지, 검증 | `tdd` | RED-GREEN-REFACTOR 사이클 |
| WebSocket, STOMP, 실시간, 브로드캐스트, SSE, 푸시, 알림 | `backend-patterns` | scorer 모듈 WebSocket 패턴 적용 |

**복합 매핑**: 하나의 이슈에 여러 키워드가 있으면 해당되는 스킬을 모두 식별한다. 예를 들어 "Game Controller 생성"이면 `domain-baseball` + `verify-entity-leak` + `verify-api-response` + `verify-authorization` + `verify-url-prefix` + `verify-repository-injection`이 모두 식별된다.

### 0-3. 식별된 스킬 SKILL.md 로드 (필수)

**0-2에서 식별된 모든 스킬의 SKILL.md 파일을 반드시 읽는다.** 이 단계는 생략할 수 없다.

```
각 식별된 스킬에 대해:
  Read .claude/skills/[스킬명]/SKILL.md
```

- 읽은 내용에서 **핵심 규칙과 위반 예시/올바른 예시**를 추출한다
- 추출한 규칙은 이후 Phase 2(planner), Phase 4(implementer), Phase 5(reviewer) 에이전트 프롬프트에 **원문 그대로** 포함한다
- 스킬 파일이 존재하지 않으면 해당 스킬은 매핑에서 제외하고 사용자에게 알린다

**예시**: `verify-dependency`가 식별되었으면:
```
Read .claude/skills/verify-dependency/SKILL.md
→ 핵심 규칙 추출: "의존성 방향 Outside→Inside만 허용, Core가 Infra/API를 알면 절대 안 됨, API 계층 모듈간 상호 의존 금지"
→ 이 규칙을 implementer/reviewer 프롬프트에 포함
```

### 0-4. 파이프라인 트랙 결정

이슈 분석 결과에 따라 실행 트랙을 결정한다. 모든 이슈에 전체 파이프라인을 돌리면 단순 이슈에서 과잉 비용이 발생하므로, 복잡도에 맞게 Phase를 조절한다:

| 트랙 | 조건 | 실행 Phase |
|------|------|------------|
| 경량(Light) | 단순 버그 수정, 필드 1-2개 추가, 설정 변경, 문서 수정 | 1 → 4 → 5 → 6 (planner/architect 생략) |
| 표준(Standard) | Controller 추가, 기존 Entity 활용 기능, Service 로직 추가 | 1 → 2 → 4 → 5 → 6 (architect 생략) |
| 심층(Full) | 새 Entity/도메인, DB 마이그레이션 포함, 복수 모듈 변경 | 모든 Phase 실행 |

**판단 기준:**
- 이슈 라벨: Bug → 경량 우선, Feature → 표준/심층
- 키워드: Entity/마이그레이션/JPA → 심층, Controller/API → 표준, fix/수정/버그 → 경량
- 영향 모듈 수: 1개 → 경량/표준, 2개+ → 표준/심층, 3개+ → 심층

### 0-5. 사용자에게 실행 계획 표시

```markdown
## #N: [이슈 제목]

**요약**: [이슈 본문 1-2줄 요약]
**라벨**: [Feature/Bug/Refactor]
**트랙**: [경량/표준/심층]

### 식별된 관련 스킬
- `domain-baseball` — 야구 기록 도메인 관련
- `verify-authorization` — mutating 엔드포인트 포함
- ...

### 실행 계획
1. 브랜치 생성: `feature/#N-이슈명`
2. **planner** → 구현 계획 수립 (표준/심층만)
3. **architect** → Entity/Repository 설계 (심층만)
4. **implementer** → 코드 구현 (관련 스킬 규칙 적용)
5. **reviewer** → 빌드/테스트/검증
6. PR 생성

진행할까요?
```

사용자 확인을 받은 후 다음 Phase로 진행한다.

---

## Phase 1: 브랜치 생성 + 워크트리 생성

브랜치 명명 규칙은 `.claude/skills/git-workflow/SKILL.md`의 브랜치 명명 규칙을 따른다. 이슈 라벨에 따라:

| 라벨 | 브랜치 패턴 |
|------|-------------|
| Feature | `feature/#N-기능명` |
| Bug | `fix/#N-버그명` |
| Refactor | `refactor/#N-대상` |
| 기타/없음 | `feature/#N-기능명` (기본값) |

### 1-1. 원격 브랜치 생성

```
mcp__github__create_branch:
  owner: nextup-dugout
  repo: next-up-server
  branch: [타입]/#[이슈번호]-[이슈-제목-kebab-case]
  from_branch: develop
```

### 1-2. 워크트리 생성

**현재 작업 디렉토리(develop)를 건드리지 않고** 별도 워크트리에서 작업한다:

```bash
git fetch origin
git worktree add ../next-up-worktree-[이슈번호] -b [브랜치명] origin/[브랜치명]
```

- 워크트리 경로: `../next-up-worktree-[이슈번호]` (프로젝트 루트의 형제 디렉토리)
- 이후 **모든 Phase(2~6)는 워크트리 경로에서** 작업한다
- 에이전트 호출 시 워크트리 경로를 명시적으로 전달한다

> **주의**: 동일 브랜치에 대한 워크트리가 이미 존재하면 `git worktree list`로 확인 후 기존 워크트리를 재사용한다.

---

## Phase 2: 구현 계획 (planner 에이전트)

> **경량(Light) 트랙은 이 Phase를 건너뛴다.**

planner 에이전트를 Agent 도구로 호출한다:

```
Agent:
  description: "이슈 #N 구현 계획 수립"
  subagent_type: "planner"
  prompt: |
    이슈 #N: [이슈 제목]
    이슈 전문: [본문]
    관련 스킬: [식별된 스킬 목록과 핵심 규칙]
    워크트리 경로: ../next-up-worktree-[이슈번호]

    이 이슈에 대한 모듈별 구현 계획을 수립하라.
    .claude/agents/planner.md의 계획 포맷을 따르라.
```

planner가 계획을 출력하면 **사용자 승인을 받는다**. 승인 없이 다음 Phase로 넘어가지 않는다.

---

## Phase 3: 설계 (architect 에이전트)

> **경량(Light)/표준(Standard) 트랙은 이 Phase를 건너뛴다.**

architect 에이전트를 Agent 도구로 호출한다:

```
Agent:
  description: "이슈 #N Entity/Repository 설계"
  subagent_type: "architect"
  prompt: |
    이슈 #N: [이슈 제목]
    planner 계획: [Phase 2 결과]
    관련 스킬: [식별된 스킬 + 핵심 규칙]
    워크트리 경로: ../next-up-worktree-[이슈번호]

    Entity/Repository 설계 및 DB 마이그레이션 계획을 수립하라.
    .claude/agents/architect.md를 따르라.
```

---

## Phase 4: 구현 (implementer 에이전트)

implementer 에이전트를 Agent 도구로 호출한다:

```
Agent:
  description: "이슈 #N 코드 구현"
  subagent_type: "implementer"
  prompt: |
    이슈 #N: [이슈 제목]
    구현 계획: [Phase 2 planner 결과 (있으면)]
    설계: [Phase 3 architect 결과 (있으면)]
    관련 스킬: [식별된 스킬 + 핵심 규칙]
    워크트리 경로: ../next-up-worktree-[이슈번호]

    계획에 따라 코드를 구현하라.
    식별된 스킬 규칙을 선제적으로 적용하라.
    .claude/agents/implementer.md를 따르라.
```

- **작업 디렉토리**: `../next-up-worktree-[이슈번호]`
- **Phase 0에서 식별된 스킬의 규칙을 구현 중에 선제적으로 적용**:
  - `domain-baseball` → 야구 규칙 체크리스트 확인하며 구현
  - `verify-authorization` → @PreAuthorize + @AuthenticationPrincipal 처음부터 적용
  - `verify-entity-leak` → DTO 변환을 처음부터 적용
  - `verify-custom-exception` → BusinessException 계열만 사용
  - 기타 식별된 스킬의 규칙도 동일하게 적용
- Core/Service 계층은 TDD 적용 (tdd 스킬 식별 시 특히 강조)

---

## Phase 5: 검증 (reviewer 에이전트)

reviewer 에이전트를 Agent 도구로 호출한다:

```
Agent:
  description: "이슈 #N 코드 검증"
  subagent_type: "reviewer"
  prompt: |
    이슈 #N: [이슈 제목]
    관련 스킬: [식별된 스킬 목록]
    워크트리 경로: ../next-up-worktree-[이슈번호]
    이전 Phase 결과: [planner 계획, architect 설계, implementer 구현 내용 요약]

    빌드/테스트/검증을 수행하라.
    .claude/agents/reviewer.md를 따르라.
```

- **작업 디렉토리**: `../next-up-worktree-[이슈번호]`
1. `./gradlew clean build --no-daemon` 실행 (**워크트리 경로에서**)
2. `verify-implementation` 실행 (7개 verify 스킬 통합 검증)
3. **Phase 0에서 식별된 스킬에 대해 중점 검증** — 식별된 스킬의 규칙 위반이 없는지 특별히 확인
4. VETO 조건 확인 (CLAUDE.md 참조)

**reviewer에게 이전 Phase 컨텍스트 전달**: reviewer가 planner/architect/implementer의 의도를 이해할 수 있도록, 이전 Phase의 계획/설계/구현 요약을 프롬프트에 포함한다.

**reviewer REJECT 시**: 사유를 분석하고 implementer에게 수정을 요청한 뒤 다시 reviewer를 호출한다. 최대 3회 반복 후에도 REJECT이면 사용자에게 보고하고 **파이프라인 중단 처리**를 수행한다.

---

## Phase 6: 커밋, PR 생성 및 워크트리 정리

reviewer APPROVED 후 **워크트리 경로에서** 작업한다:

### 6-1. 커밋 & 푸시 (워크트리에서)

커밋 메시지는 `.claude/skills/git-workflow/SKILL.md`의 Udacity Style 컨벤션을 따른다:

```bash
cd ../next-up-worktree-[이슈번호]
git add [변경된 파일들]
git commit -m "타입(#이슈번호): 설명"
git push -u origin [브랜치명]
```

### 6-2. PR 생성

`/pr` 스킬을 참조하여 PR 생성. `.claude/skills/git-workflow/SKILL.md`의 커밋/PR 컨벤션도 함께 따른다.

#### PR 템플릿 사용 (필수)

`.github/PULL_REQUEST_TEMPLATE.md`의 구조를 반드시 따른다:

```markdown
## Summary

>- Close #[이슈번호]

[PR 요약 — 무엇을 왜 변경했는지]

## Tasks

- [x] 완료된 작업 1
- [x] 완료된 작업 2

## To Reviewer

[리뷰어에게 전달할 아키텍처 결정, 주의사항 등]

## Screenshot

_(백엔드 작업이므로 대부분 삭제)_
```

#### 이슈 연결 (필수)

- Summary 섹션에 `>- Close #[이슈번호]` 형식으로 반드시 포함하여 PR 머지 시 이슈가 자동 닫히도록 한다
- 이슈 라벨에 맞는 PR 타이틀 프리픽스를 사용한다: `fix(#N):`, `feat(#N):`, `refactor(#N):`

#### PR 본문 포맷 주의 (필수)

`mcp__github__create_pull_request`의 `body` 파라미터에 마크다운을 전달할 때, **`\n` 이스케이프 시퀀스를 사용하지 말고 실제 줄바꿈을 사용**하라. `\n`을 사용하면 리터럴 `\n` 문자열이 그대로 들어가 마크다운 렌더링이 깨진다.

```
// ❌ WRONG: \n 이스케이프 사용
body: "## Summary\n\n- 내용1\n- 내용2"

// ✅ CORRECT: 실제 줄바꿈 사용
body: "## Summary

- 내용1
- 내용2"
```

### 6-3. 워크트리 정리

PR 생성 완료 후 **원래 프로젝트 디렉토리로 돌아와서** 워크트리를 정리한다:

```bash
cd /Users/nninjoon/Documents/GitHub/next-up-server
git worktree remove ../next-up-worktree-[이슈번호]
```

> **주의**: PR 머지 전이라도 이미 push 완료되었으므로 워크트리는 안전하게 제거 가능하다. 추가 수정이 필요하면 다시 워크트리를 생성하면 된다.

---

## 파이프라인 중단 처리

다음 상황에서 파이프라인이 중단될 수 있다:
1. 사용자가 Phase 0 또는 Phase 2에서 실행을 거부
2. reviewer가 3회 연속 REJECT
3. 에이전트 실행 중 복구 불가 에러 발생

**중단 시 필수 정리:**

```bash
cd /Users/nninjoon/Documents/GitHub/next-up-server
git worktree remove ../next-up-worktree-[이슈번호] --force
```

워크트리를 정리하지 못한 경우 사용자에게 수동 정리 명령을 안내한다. 워크트리가 남으면 디스크와 git 상태가 오염되므로, 어떤 이유로든 파이프라인이 중단되면 반드시 정리를 시도한다.

---

## 주의사항

- **MCP GitHub 도구 사용**: `gh` CLI는 미인증 상태이므로 반드시 `mcp__github__*` 도구 사용
- **사용자 확인**: Phase 0 실행 계획과 Phase 2 planner 계획 수립 후 반드시 사용자 승인
- **관련 스킬 컨텍스트 전달**: 각 에이전트 호출 시 Phase 0에서 식별된 스킬 목록과 핵심 규칙을 프롬프트에 포함
- **Gradle 빌드**: `--no-daemon` 옵션을 사용하여 다른 워크트리/프로젝트의 Gradle 데몬과 충돌을 방지한다. 동일 워킹 디렉토리에서 Gradle 빌드를 동시에 실행하지 않는다.
- **Flyway 마이그레이션 번호**: 마이그레이션을 추가할 때 기존 파일의 최대 버전 번호를 확인하고 +1로 할당한다. 번호 충돌이 발생하면 빌드가 실패하므로 반드시 확인.
- **워크트리 격리**: Phase 1에서 워크트리를 생성하면 Phase 2~6의 **모든 파일 읽기/쓰기/빌드는 워크트리 경로에서** 수행한다. 원본 프로젝트 디렉토리(develop)는 건드리지 않는다
- **워크트리 경로 규칙**: `../next-up-worktree-[이슈번호]` 형식. 프로젝트 루트의 형제 디렉토리로 생성한다
- **워크트리 충돌 방지**: `git worktree list`로 기존 워크트리 확인 후 중복 생성 방지
- **워크트리 정리**: PR 생성 후 반드시 `git worktree remove`로 정리한다. 파이프라인 중단 시에도 "파이프라인 중단 처리" 절차를 따른다
- **git-workflow 스킬**: Phase 6에서는 관련 스킬 매핑 여부와 무관하게 `.claude/skills/git-workflow/SKILL.md`를 반드시 참조하여 커밋 메시지와 PR 컨벤션을 따른다
