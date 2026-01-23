---
name: git-toolkit
description: |
  GitHub CLI(gh)를 사용하여 물리적인 GitHub 조작을 수행하는 실행 스킬.
  이슈 생성, 브랜치 관리, PR 생성 등의 형상 관리 작업을 자동화한다.
  반드시 .github/ 디렉토리의 템플릿을 사용하여 이슈/PR을 생성한다.
---

# Git-Toolkit Skill - GitHub 조작 스킬

## 개요

이 스킬은 GitHub 조작을 수행합니다. 판단(Agent)과 실행(Skill)의 분리 원칙에 따라, 이 스킬은 **실행만** 담당하며 전략적 판단은 `github-manager` 에이전트가 수행합니다.

## 접근 방식

| 방식 | 용도 | 우선순위 |
|------|------|----------|
| **MCP GitHub** | 이슈/PR 조회, 생성, 관리 | **1순위 (권장)** |
| **GitHub CLI (`gh`)** | MCP 미지원 기능, 로컬 스크립트 | 2순위 (대안) |

> ⚠️ MCP가 연동되어 있으면 **MCP 도구를 우선 사용**합니다. `gh` CLI는 MCP로 처리할 수 없는 작업에만 사용합니다.

## 사전 조건

- **MCP GitHub 서버 연동** (권장): `claude mcp add --transport http github https://api.githubcopilot.com/mcp/`
- GitHub CLI(`gh`) 설치 및 인증 완료 (대안)
- Git 설정 완료 (user.name, user.email)
- 프로젝트 저장소에 푸시 권한 보유

---

## 📋 GitHub 템플릿 (필수 사용)

> ⚠️ **Issue 및 PR 생성 시 반드시 `.github/` 디렉토리의 템플릿을 준수해야 합니다.**
> 템플릿 미준수 시 `reviewer`에 의해 REJECT 됩니다.

### Issue Templates (`.github/ISSUE_TEMPLATE/`)

> ⚠️ **Issue 생성 시 반드시 해당하는 템플릿을 선택하고 모든 필수 항목을 작성해야 합니다.**

| 템플릿 | 파일 | 레이블 | 용도 |
|--------|------|--------|------|
| ✨ Feature | `feature.yml` | `✨ Feature` | 새로운 기능 요청 |
| 🐞 Bug | `bug.yml` | `🐞 Bug` | 버그 리포트 |
| 🔨 Refactor | `refactor.yml` | `🔨 Refactor` | 리팩토링 요청 |
| 💡 Suggestion | `suggestion.yml` | `💡 Suggestion` | 제안 사항 |

### Issue 본문 형식

#### ✨ Feature (기능)
```markdown
## Describe
새로운 기능에 대한 설명을 작성해 주세요!

## Tasks
- [ ] 해야 하는 일 1
- [ ] 해야 하는 일 2
- [ ] 해야 하는 일 3

## ETC
더 전달할 내용이 있다면 여기에 작성해주세요!
```

#### 🐞 Bug (버그)
```markdown
## 버그 설명
버그에 대한 설명을 작성해 주세요!

## 예상 결과
예상한 결과를 작성해 주세요!

## 실제 결과
실제 결과를 작성해 주세요!

## 시뮬레이션
1. 첫 번째 단계
2. 두 번째 단계
3. 버그 발생

## ETC
더 전달할 내용이 있다면 여기에 작성해주세요!
```

#### 🔨 Refactor (리팩토링)
```markdown
## Describe
왜 리팩터링을 해야하는지 설명해주세요!

## Tasks
- [ ] 리팩토링 작업 1
- [ ] 리팩토링 작업 2

## ETC
더 전달할 내용이 있다면 여기에 작성해주세요!
```

### PR Template (`.github/PULL_REQUEST_TEMPLATE.md`)

> ⚠️ **PR 생성 시 반드시 이 템플릿을 준수해야 합니다.** 위반 시 `reviewer`에 의해 REJECT 됩니다.

```markdown
## Summary

>- 관련 있는 Issue를 태그해주세요. (example : Close #1)

**해당 PR에 대한 요약을 작성해주세요.**

## Tasks

- 해당 PR에 포함된 작업을 작성해주세요.
- 해당 PR에 포함된 작업을 작성해주세요.

## To Reviewer

_(없을 경우 삭제) 더 전달할 내용이 있다면 여기에 작성해주세요._

## Screenshot

_(없을 경우 삭제) 작업한 내용에 대한 스크린샷을 첨부해주세요._
```

---

## MCP 사용 예시 (권장)

MCP GitHub 연동 시, 다음과 같이 MCP 도구를 직접 호출합니다:

### 이슈 조회
```
mcp__github__list_issues: owner, repo, state
mcp__github__issue_read: method="get", owner, repo, issue_number
```

### 이슈 생성
```
mcp__github__issue_write: method="create", owner, repo, title, body, labels
```

### PR 생성
```
mcp__github__create_pull_request: owner, repo, title, body, head, base
```

### PR 조회/리뷰
```
mcp__github__pull_request_read: method="get"|"get_diff"|"get_files", owner, repo, pullNumber
mcp__github__pull_request_review_write: method="create", owner, repo, pullNumber, event, body
```

### 브랜치 관리
```
mcp__github__list_branches: owner, repo
mcp__github__create_branch: owner, repo, branch, from_branch
```

---

## GitHub CLI 사용 예시 (대안)

MCP로 처리할 수 없는 작업이나 로컬 스크립트 실행 시 `gh` CLI를 사용합니다.

### 이슈 생성 (Feature)

```bash
gh issue create \
  --title "✨ Agent/Skill 아키텍처 구축" \
  --label "✨ Feature" \
  --body "$(cat <<'EOF'
## Describe
NEXT-UP 백엔드 자동화를 위한 Agent/Skill 아키텍처를 구축합니다.

## Tasks
- [ ] Council Agent 생성 (planner, tech-lead, reviewer, risk-manager, baseball-expert, security-auditor)
- [ ] Execution Agent 생성 (modeler, logic-broker, api-specialist, data-transformer, scenario-tester, github-manager, knowledge-manager)
- [ ] Skills 생성 (build-validator, git-toolkit, db-manager, code-quality)
- [ ] CLAUDE.md 헌법 문서 작성

## ETC
prompt.md의 v3.0 설계 원칙을 따릅니다.
EOF
)"
```

### 이슈 생성 (Bug)

```bash
gh issue create \
  --title "🐞 빌드 실패 오류" \
  --label "🐞 Bug" \
  --body "$(cat <<'EOF'
## 버그 설명
./gradlew build 실행 시 컴파일 에러가 발생합니다.

## 예상 결과
빌드가 성공적으로 완료되어야 합니다.

## 실제 결과
PlayerRepository.kt:45에서 타입 불일치 에러 발생

## 시뮬레이션
1. main 브랜치 checkout
2. ./gradlew build 실행
3. 에러 발생

## ETC
Kotlin 2.1.10 버전 사용 중
EOF
)"
```

### PR 생성

```bash
gh pr create \
  --title "feat: Agent/Skill 아키텍처 구축" \
  --base develop \
  --body "$(cat <<'EOF'
## Summary

Close #1

Agent/Skill 기반의 백엔드 자동화 아키텍처를 구축했습니다.

## Tasks

- Council Agent 6개 생성 (planner, tech-lead, reviewer, risk-manager, baseball-expert, security-auditor)
- Execution Agent 7개 생성
- Skills 4개 생성 (build-validator, git-toolkit, db-manager, code-quality)
- CLAUDE.md 헌법 문서 작성

## To Reviewer

prompt.md의 v3.0 설계 원칙을 준수하여 구현했습니다.
Reviewer 거부권(VETO)이 명문화되어 있습니다.
EOF
)"
```

---

## 브랜치 명명 규칙

이 스킬을 통해 생성되는 브랜치는 다음 규칙을 따릅니다:

| 타입 | 브랜치 패턴                  | 예시                                 |
|------|-------------------------|------------------------------------|
| 기능 | `feat/#[이슈번호]-[기능명]`    | `feat/#1-agent-skill-architecture` |
| 수정 | `fix/#[이슈번호]-[버그명]`     | `fix/#2-build-failure`             |
| 리팩토링 | `refactor/#[이슈번호]-[대상]` | `refactor/#3-player-service`       |
| 문서 | `docs/#[이슈번호]-[문서명]`    | `docs/#4-api-documentation`        |

---

## PR 제목 규칙

PR 제목은 **브랜치명을 그대로 사용**합니다.

| 브랜치명 | PR 제목 |
|---------|---------|
| `feat/#1-agent-skill-architecture` | `feat/#1-agent-skill-architecture` |
| `fix/#2-build-failure` | `fix/#2-build-failure` |
| `refactor/#3-player-service` | `refactor/#3-player-service` |

---

## 커밋 메시지 컨벤션 (Udacity Style)

```
type: subject (50자 이내)

body (선택, 72자 줄바꿈)

Refs #이슈번호
```

### Type 종류
| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 | `feat: Agent 아키텍처 구축` |
| `fix` | 버그 수정 | `fix: 빌드 실패 수정` |
| `refactor` | 리팩토링 | `refactor: PlayerService 분리` |
| `test` | 테스트 | `test: PlayerRepository 테스트 추가` |
| `docs` | 문서화 | `docs: CLAUDE.md 업데이트` |
| `chore` | 기타 | `chore: Gradle 설정 변경` |

---

## 호출 에이전트

- `github-manager`: 이슈/브랜치/PR 전체 생명주기 관리
