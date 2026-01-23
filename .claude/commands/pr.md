# /pr - Create Pull Request

GitHub PR을 생성합니다.

## Prerequisites

PR 생성 전 필수 확인:
1. ✅ `/review` 통과 (APPROVED)
2. ✅ 모든 변경사항 커밋 완료
3. ✅ 원격 브랜치에 푸시 완료

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
  title: [브랜치명 그대로]
  body: [PR 템플릿]
  head: [feature-branch]
  base: develop
```

## PR Title Rule

**PR 제목 = 브랜치명 그대로**

| 브랜치 | PR 제목 |
|--------|---------|
| `feat/#1-user-auth` | `feat/#1-user-auth` |
| `fix/#2-login-error` | `fix/#2-login-error` |

## PR Template

```markdown
## Summary

>- Close #[이슈번호]

[변경 사항 요약]

## Tasks

- [완료한 작업 1]
- [완료한 작업 2]

## To Reviewer

[리뷰어에게 전달할 사항]

## Screenshot

(해당 시 스크린샷 첨부)
```

## 사용 예시

```
User: /pr
Assistant:
PR 생성을 시작합니다...

1. 현재 상태 확인...
   Branch: feat/#5-agent-skill-refactoring
   Commits: 8 ahead of develop

2. 변경사항 확인...
   - 15 files changed
   - +1,234 / -567 lines

3. /review 상태 확인...
   ✅ APPROVED

4. PR 생성 중...

## PR Created Successfully

- **Title**: feat/#5-agent-skill-refactoring
- **URL**: https://github.com/nextup-dugout/next-up-server/pull/6
- **Base**: develop
- **Head**: feat/#5-agent-skill-refactoring

다음 단계:
- 리뷰어 지정
- CI 통과 확인
- 머지
```

## PR without Review (비권장)

```
User: /pr --skip-review
Assistant:
⚠️ 경고: /review를 건너뜁니다.

빌드 실패, 테스트 실패, 보안 취약점이 있을 수 있습니다.
계속하시겠습니까? (y/n)
```

## 활용 Skills

- `git-workflow`: GitHub 자동화

## 활용 Agents

- `devops`: PR 생성 및 관리
- `reviewer`: PR 생성 전 검수
