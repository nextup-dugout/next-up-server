# /pr Command - GitHub PR 자동 생성

> **GitHub MCP**로 PR 자동 생성 + Codecov 연동

## 목적

reviewer 승인 후 GitHub PR을 자동으로 생성하고, Codecov 커버리지 체크를 연동합니다.

## 사용법

```
/pr
```

## 전제 조건

```
✅ /review 명령으로 검수 완료
✅ reviewer APPROVE 받음
✅ 커버리지 80% 이상
```

## 실행 흐름

### 1. Git 상태 확인
```bash
git status
git diff develop...HEAD
```

### 2. 변경 사항 분석
```
Modified files:
- nextup-core/Player.kt
- nextup-api/PlayerController.kt

Changes:
- Player에 changeBattingOrder() 추가
- PATCH /api/v1/players/{id}/batting-order 추가
```

### 3. PR 제목/본문 생성

#### 제목
```
[#5] Player 타순 변경 API 추가
```

#### 본문
```markdown
## 관련 이슈
Closes #5

## 변경 사항
- Player Entity에 changeBattingOrder() 메서드 추가
- PATCH /api/v1/players/{id}/batting-order 엔드포인트 추가
- ChangeBattingOrderRequest DTO 추가

## 테스트 체크리스트
- [x] 단위 테스트 작성 및 통과
- [x] 통합 테스트 작성 및 통과
- [x] 커버리지 82% 달성

## Codecov 리포트
커버리지는 PR 생성 후 GitHub Actions에서 자동으로 체크됩니다.

🚀 Generated with [Claude Code](https://claude.com/claude-code)
```

### 4. PR 생성 (MCP)
```kotlin
mcp__github__create_pull_request(
    owner = "nextup-dugout",
    repo = "next-up-server",
    title = "[#5] Player 타순 변경 API 추가",
    head = "feature/#5-player-api",
    base = "develop",
    body = "..." // 위 본문
)
```

### 5. Codecov 자동 체크

GitHub Actions가 자동 실행:
```yaml
1. 테스트 실행
2. Jacoco 리포트 생성
3. Codecov 업로드
4. 커버리지 80% 체크
```

PR에 자동 코멘트:
```markdown
## Codecov Report
✅ Coverage: 82% (target: 80%)

이 PR은 커버리지 기준을 충족합니다.
```

### 6. PR URL 반환
```
✅ PR 생성 완료!
🔗 https://github.com/nextup-dugout/next-up-server/pull/6
```

## Skills 참조

- **git-workflow**: GitHub MCP 사용법
- **quality-metrics**: CI/CD 확인

## Agents 협업

- **devops**: PR 생성 담당
- **reviewer**: APPROVE 후 허용

## PR 머지 조건

| 조건 | 차단 여부 |
|------|-----------|
| Codecov < 80% | ❌ 머지 불가 |
| 빌드 실패 | ❌ 머지 불가 |
| 테스트 실패 | ❌ 머지 불가 |
| Reviewer 승인 없음 | ❌ 머지 불가 |

## 체크리스트

- [ ] reviewer APPROVE 받음
- [ ] 커버리지 80% 이상
- [ ] PR 제목 컨벤션 ([#번호])
- [ ] 관련 이슈 Closes #번호
- [ ] 테스트 체크리스트 완료

## 예시

```
User: "/pr"

DevOps Agent:
1. reviewer 승인 확인
   ✅ APPROVE 받음

2. Git 상태 확인
   Modified: 5 files
   Added: 3 files

3. 변경 사항 분석
   - Player 타순 변경 기능 추가

4. PR 생성 (MCP)
   Title: [#5] Player 타순 변경 API 추가
   Base: develop
   Head: feature/#5-player-api

5. PR 생성 완료
   🔗 https://github.com/nextup-dugout/next-up-server/pull/6

6. Codecov 자동 체크 시작
   GitHub Actions가 실행 중...
```

## 이 Command의 장점

- ✅ MCP로 빠른 PR 생성
- ✅ 컨벤션 자동 준수
- ✅ Codecov 자동 연동
- ✅ 80% 미달 시 자동 차단
