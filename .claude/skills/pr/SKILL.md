---
name: pr
description: |
  GitHub PR 생성 워크플로우. 브랜치 푸시, PR 제목/본문 작성, 이슈 자동 연결을 수행한다.
  /review 통과 후 PR 생성을 권장한다.
user-invocable: true
argument-hint: "[issue-number] e.g. 42"
allowed-tools: Bash, Read, Glob, Grep
---

# /pr - Create Pull Request

GitHub PR을 생성합니다.

## Arguments

`$ARGUMENTS`를 통해 이슈 번호를 지정할 수 있습니다.

| 사용법 | 설명 |
|--------|------|
| `/pr` | 브랜치명에서 이슈 번호 자동 추출 |
| `/pr 123` | #123 이슈에 연결된 PR 생성 |
| `/pr --skip-review` | review 검증 건너뛰기 (비권장) |

## Prerequisites

PR 생성 전 필수 확인:
1. `/review` 통과 (APPROVED)
2. 모든 변경사항 커밋 완료
3. 원격 브랜치에 푸시 완료

## PR Creation Process

### 1. 현재 상태 확인
```bash
git status
git log --oneline -5
```

### 2. 브랜치 푸시
```bash
git push -u origin [branch-name]
```

### 3. PR 생성 (MCP)
```
mcp__github__create_pull_request:
  owner: nextup-dugout
  repo: next-up-server
  title: [타입(#이슈번호): 간단한 설명]
  body: [PR 템플릿]
  head: [feature-branch]
  base: develop
```

## PR Title Rule

**PR 제목 = `타입(#이슈번호): 간단한 설명`**

| 브랜치 | PR 제목 |
|--------|---------|
| `feat/#1-user-auth` | `feat(#1): 사용자 인증 기능 구현` |
| `fix/#2-login-error` | `fix(#2): 로그인 오류 수정` |
| `feat/#54-box-score` | `feat(#54): 박스스코어 자동 계산 및 Exception 개선` |

## PR Template

```markdown
## Summary

>- close #[이슈번호]

[변경 사항 요약]

## Tasks

- [완료한 작업 1]
- [완료한 작업 2]

## To Reviewer

[리뷰어에게 전달할 사항]

## Screenshot

(해당 시 스크린샷 첨부)
```

### Example

```markdown
## Summary

>- close #52

Phase 2: 조회/통계 API 고도화 구현

## Tasks

- [x] 경기 타임라인 API 구현
- [x] 팀 통계 API 구현

## To Reviewer

- 커버리지: 79%
- 빌드/테스트: 통과

## Screenshot

N/A (API 전용)
```

> **주의**: Summary의 `>- close #이슈번호` 형식을 정확히 지켜야 이슈가 자동으로 닫힙니다.

## PR without Review (비권장)

```
User: /pr --skip-review
Assistant:
경고: /review를 건너뜁니다.

빌드 실패, 테스트 실패, 보안 취약점이 있을 수 있습니다.
계속하시겠습니까? (y/n)
```

