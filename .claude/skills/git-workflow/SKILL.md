# Git-Workflow Skill - GitHub 자동화 + Codecov 연동

> **재사용 가능한 GitHub 관리 도구**
> MCP GitHub + Codecov + PR 자동화

---

## 📚 Skill 개요

### 목적
- GitHub 이슈/PR 자동 생성 및 관리
- Codecov 커버리지 80% PR 머지 조건 강제
- 브랜치 전략 및 컨벤션 준수

### 사용 시나리오
- ✅ 이슈/PR 생성 시
- ✅ 브랜치 관리 시
- ✅ Codecov 통합 시

### 호출 방법
```
"/pr" - PR 자동 생성
"git-workflow Skill로 이슈 만들어줘"
```

---

## 🏗️ 브랜치 전략

### 브랜치 네이밍 컨벤션

```
feature/#이슈번호 간단한 설명
hotfix/#이슈번호 간단한 설명
release/v<버전>
```

#### 예시
```bash
# Feature 브랜치
feature/#5 Agent/Skill 구조 개선
feature/#12 Player API 구현

# Hotfix 브랜치
hotfix/#23 타율 계산 버그 수정

# Release 브랜치
release/v1.0.0
```

### 브랜치 생성 (MCP)
```kotlin
// MCP GitHub 사용
mcp__github__create_branch(
    owner = "nextup-dugout",
    repo = "next-up-server",
    branch = "feature/#5 Agent/Skill 구조 개선",
    from_branch = "develop"
)
```

---

## 📝 이슈 생성

### 이슈 템플릿

**반드시 `.github/ISSUE_TEMPLATE/`에 정의된 템플릿을 사용하세요.**

사용 가능한 템플릿:
- `feature.yml` - 새 기능 요청
- `bug.yml` - 버그 리포트
- `refactor.yml` - 리팩토링
- `suggestion.yml` - 제안

### 이슈 생성 (MCP)
```kotlin
mcp__github__issue_write(
    method = "create",
    owner = "nextup-dugout",
    repo = "next-up-server",
    title = "Agent/Skill 구조 개선 및 코드 커버리지 설정",
    body = """
## 목표
...

## 작업 내용
- [ ] Task 1
- [ ] Task 2

## 기대 효과
...
    """,
    labels = ["enhancement", "testing"]
)
```

---

## 🔀 Pull Request 생성

### PR 컨벤션

#### 제목 형식
```
[#이슈번호] 간단한 설명
```

#### 예시
```
[#5] Agent/Skill 구조 개선 및 Codecov 설정
[#12] Player API 엔드포인트 추가
```

### PR 템플릿

**반드시 `.github/PULL_REQUEST_TEMPLATE.md`에 정의된 템플릿을 사용하세요.**

PR 생성 시 GitHub가 자동으로 해당 템플릿을 로드합니다.

### PR 생성 (MCP)
```kotlin
mcp__github__create_pull_request(
    owner = "nextup-dugout",
    repo = "next-up-server",
    title = "[#5] Agent/Skill 구조 개선 및 Codecov 설정",
    head = "feature/#5 Agent/Skill 구조 개선",
    base = "develop",
    body = """
## 관련 이슈
Closes #5

## 변경 사항
- Agent 13개 → 5개 통합
- Jacoco + Codecov 설정
- TDD 규칙 추가

## 테스트 체크리스트
- [x] 단위 테스트 작성 및 통과
- [x] 커버리지 82% 달성

## Codecov 리포트
[![codecov](https://codecov.io/gh/nextup-dugout/next-up-server/branch/feature%2F%235-agent-skill-refactoring/graph/badge.svg)](https://codecov.io/gh/nextup-dugout/next-up-server)
    """,
    draft = false
)
```

---

## 📊 Codecov PR 통합

### 1. PR 머지 조건

```yaml
# .codecov.yml
coverage:
  status:
    project:
      default:
        target: 80%           # 전체 프로젝트 80%
        threshold: 1%         # 1% 감소까지 허용

    patch:
      default:
        target: 80%           # 새 코드는 무조건 80%
        threshold: 0%         # 감소 불허
```

### 2. PR 자동 코멘트

Codecov가 PR에 자동으로 코멘트를 작성합니다:

```markdown
## Codecov Report
> Merging #5 into develop will **increase** coverage by 2.34%

@@            Coverage Diff            @@
##           develop      #5     +/-   ##
=========================================
+ Coverage    77.66%  80.00%  +2.34%
=========================================
  Files           10      12      +2
  Lines          250     310     +60
=========================================
+ Hits           194     248     +54
- Misses          56      62      +6

✅ All checks have passed
```

### 3. PR 머지 차단

커버리지 80% 미달 시 PR 머지 불가:

```markdown
## Codecov Report
❌ Coverage decreased (-2.5%) to 77.5%

Project coverage is below 80%
This pull request cannot be merged until coverage is increased.
```

### 4. GitHub Actions 연동

```yaml
# .github/workflows/pr.yml
name: PR Check

on:
  pull_request:
    branches: [ develop, main ]

jobs:
  test:
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'

      - name: Run tests with coverage
        run: ./gradlew test jacocoRootReport

      - name: Upload to Codecov
        uses: codecov/codecov-action@v3
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml
          fail_ci_if_error: true  # 커버리지 미달 시 CI 실패

      - name: Verify coverage threshold
        run: ./gradlew jacocoTestCoverageVerification
```

---

## 🎯 Commit Convention

### 형식
```
<type>: <subject>

<body>

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

### Type 종류
- `feat`: 새 기능
- `fix`: 버그 수정
- `refactor`: 리팩토링
- `test`: 테스트 추가
- `docs`: 문서 수정
- `chore`: 빌드/설정 변경

### 예시
```
feat: Player API 엔드포인트 추가

- GET /api/v1/players 목록 조회
- GET /api/v1/players/{id} 상세 조회
- POST /api/v1/players 선수 생성

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

---

## 🚀 자동화 워크플로우

### /pr 명령어

PR 자동 생성 워크플로우:

```bash
1. git status 확인
2. git diff 분석
3. 변경 사항 요약
4. PR 제목/본문 생성
5. Codecov 뱃지 추가
6. MCP로 PR 생성
7. PR URL 반환
```

### 실행 예시
```
User: /pr

Agent:
1. git status 확인 중...
   - Modified: 5 files
   - Added: 3 files

2. 변경 사항 분석 중...
   - Agent 구조 개선
   - Jacoco 설정 추가
   - Codecov 연동

3. PR 생성 중...
   - Title: [#5] Agent/Skill 구조 개선 및 Codecov 설정
   - Base: develop
   - Head: feature/#5 Agent/Skill 구조 개선

✅ PR 생성 완료!
🔗 https://github.com/nextup-dugout/next-up-server/pull/6
```

---

## 📋 체크리스트

### 이슈 생성 시
- [ ] 이슈 템플릿 사용
- [ ] 명확한 목표 작성
- [ ] 작업 내용 체크리스트 작성
- [ ] 적절한 레이블 추가

### 브랜치 생성 시
- [ ] 컨벤션 준수 (`feature/#번호 간단한 설명`)
- [ ] develop 브랜치에서 분기

### PR 생성 시
- [ ] PR 제목 컨벤션 준수 (`[#번호] 설명`)
- [ ] 관련 이슈 번호 포함 (`Closes #번호`)
- [ ] 테스트 체크리스트 완료
- [ ] Codecov 80% 이상 달성
- [ ] 빌드/테스트 통과

### 커밋 시
- [ ] Commit 컨벤션 준수
- [ ] Co-Authored-By 추가
- [ ] 의미 있는 커밋 메시지

---

## ⚠️ PR 머지 차단 조건

| 조건 | 차단 여부 |
|------|-----------|
| Codecov < 80% | ❌ 머지 불가 |
| 빌드 실패 | ❌ 머지 불가 |
| 테스트 실패 | ❌ 머지 불가 |
| Reviewer 승인 없음 | ❌ 머지 불가 |

---

## 🎯 이 Skill의 장점

1. **자동화**: MCP로 빠른 이슈/PR 생성
2. **일관성**: 컨벤션 자동 준수
3. **품질 보증**: Codecov 80% 강제
4. **가시성**: PR에서 커버리지 확인 가능

---

## 📚 참고 자료

- [GitHub MCP Documentation](https://github.com/anthropics/mcp)
- [Codecov Documentation](https://docs.codecov.com/)
- [Conventional Commits](https://www.conventionalcommits.org/)
