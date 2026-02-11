****---
name: git-workflow
description: |
  GitHub 브랜치/커밋/PR 워크플로우 및 CI/CD 연동 가이드.
  브랜치 명명, 커밋 메시지 컨벤션(Udacity Style), PR 템플릿을 제공한다.
user-invocable: false
allowed-tools: Read, Glob, Grep, Bash
---

# Git Workflow - GitHub Automation & CI/CD

> GitHub 자동화 및 CI/CD 연동을 위한 워크플로우 정의

## 개요

이 스킬은 GitHub 조작 및 CI/CD 파이프라인 연동을 수행합니다. MCP GitHub 서버를 우선 사용하고, 지원하지 않는 기능은 GitHub CLI를 사용합니다.

## 접근 방식

| 방식 | 용도 | 우선순위 |
|------|------|----------|
| **MCP GitHub** | 이슈/PR 조회, 생성, 관리 | **1순위 (권장)** |
| **GitHub CLI (`gh`)** | MCP 미지원 기능, 로컬 스크립트 | 2순위 (대안) |

## 브랜치 명명 규칙

| 타입 | 브랜치 패턴 | 예시 |
|------|-------------|------|
| 기능 | `feat/#[이슈번호]-[기능명]` | `feat/#1-agent-skill-architecture` |
| 수정 | `fix/#[이슈번호]-[버그명]` | `fix/#2-build-failure` |
| 리팩토링 | `refactor/#[이슈번호]-[대상]` | `refactor/#3-player-service` |
| 문서 | `docs/#[이슈번호]-[문서명]` | `docs/#4-api-documentation` |

## PR 제목 규칙

**`type: 설명 (#이슈번호)` 형식을 사용합니다.**

| 브랜치명 | PR 제목 |
|---------|---------|
| `feat/#1-agent-skill-architecture` | `feat: Agent Skill 아키텍처 구축 (#1)` |
| `fix/#2-build-failure` | `fix: 빌드 실패 수정 (#2)` |

## 커밋 메시지 컨벤션 (Udacity Style)

```
type: subject (50자 이내)

body (선택, 72자 줄바꿈)

Refs #이슈번호
```

| Type | 설명 | 예시 |
|------|------|------|
| `feat` | 새로운 기능 | `feat: Agent 아키텍처 구축` |
| `fix` | 버그 수정 | `fix: 빌드 실패 수정` |
| `refactor` | 리팩토링 | `refactor: PlayerService 분리` |
| `test` | 테스트 | `test: PlayerRepository 테스트 추가` |
| `docs` | 문서화 | `docs: CLAUDE.md 업데이트` |
| `chore` | 기타 | `chore: Gradle 설정 변경` |

## GitHub Templates (필수 사용)

### Issue Templates (`.github/ISSUE_TEMPLATE/`)

| 템플릿 | 파일 | 레이블 |
|--------|------|--------|
| Feature | `feature.yml` | `Feature` |
| Bug | `bug.yml` | `Bug` |
| Refactor | `refactor.yml` | `Refactor` |
| Suggestion | `suggestion.yml` | `Suggestion` |

### PR Template (`.github/PULL_REQUEST_TEMPLATE.md`)

```markdown
## Summary
>- 관련 있는 Issue를 태그해주세요. (example : Close #1)

**해당 PR에 대한 요약을 작성해주세요.**

## Tasks
- 해당 PR에 포함된 작업을 작성해주세요.

## To Reviewer
_(없을 경우 삭제)_
```

## CI/CD 연동

### GitHub Actions Workflow

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [develop, main]
  pull_request:
    branches: [develop, main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Build & Test
        run: ./gradlew build

      - name: Upload Coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml
```

### Codecov 연동

PR에 자동으로 커버리지 코멘트가 추가됩니다.

```yaml
# codecov.yml
coverage:
  status:
    project:
      default:
        target: 85%
    patch:
      default:
        target: 85%

comment:
  layout: "reach,diff,flags,files"
  behavior: default
```

## MCP 사용 예시

### 이슈 관리
```
mcp__github__list_issues: owner, repo, state
mcp__github__issue_read: method="get", owner, repo, issue_number
mcp__github__issue_write: method="create", owner, repo, title, body, labels
```

### PR 관리
```
mcp__github__create_pull_request: owner, repo, title, body, head, base
mcp__github__pull_request_read: method="get"|"get_diff", owner, repo, pullNumber
mcp__github__merge_pull_request: owner, repo, pullNumber, merge_method
```

### 브랜치 관리
```
mcp__github__list_branches: owner, repo
mcp__github__create_branch: owner, repo, branch, from_branch
```

## Agent 협업

이 Skill을 활용하는 Agent:
- **devops**: GitHub PR/Issue 전체 생명주기 관리
- **reviewer**: PR 리뷰 및 머지 승인
