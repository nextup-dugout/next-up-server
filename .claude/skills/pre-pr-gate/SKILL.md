---
name: pre-pr-gate
description: |
  PR 생성 전 필수 품질 게이트. ktlint 포맷팅, 빌드, 테스트, 커버리지, verify 스킬을 기계적으로 실행하여
  모든 항목이 통과해야만 PR 생성을 허용한다. 하나라도 실패하면 PR 생성을 차단하고 실패 리포트를 출력한다.
  issue-progress Phase 5에서 반드시 이 스킬을 호출해야 하며, 수동 PR 생성 전에도 사용할 수 있다.
  "빌드 안 돌려봤지만 코드 리뷰로 대체"는 이 스킬에서 절대 허용하지 않는다.
user-invocable: true
argument-hint: "[워크트리경로] e.g. ../next-up-worktree-426"
---

# Pre-PR Gate — PR 생성 전 필수 품질 게이트

PR을 올리기 전에 반드시 통과해야 하는 검증 단계를 **기계적으로** 실행한다.
에이전트의 판단("통과한 것 같다")이 아니라, 실제 명령어의 exit code와 출력으로 판정한다.

**핵심 원칙: 모든 게이트는 실제 실행 결과로만 판정한다. "코드 리뷰로 대체", "메모리 부족으로 생략" 등은 허용하지 않는다.**

빌드가 실패하면 PR을 만들지 말고 실패 원인을 수정한 뒤 다시 이 게이트를 돌려라.

---

## 사용법

워크트리 경로(또는 현재 작업 디렉토리)에서 실행한다:

```
/pre-pr-gate ../next-up-worktree-426
```

`$ARGUMENTS`가 없으면 현재 디렉토리를 작업 경로로 사용한다.

---

## Gate 1: ktlint 포맷팅 + 검증

ktlint 위반은 CI에서 빌드 실패를 유발한다. 커밋 전에 반드시 자동 포맷팅을 실행한다.

```bash
cd [작업경로]
./gradlew ktlintFormat --no-daemon
```

포맷팅 후 검증:

```bash
./gradlew ktlintCheck --no-daemon
```

- exit 0 → PASS
- exit non-zero → **FAIL**: 자동 포맷으로 해결 안 되는 위반이 있음. 수동 수정 필요.
  - 출력에서 위반 파일:라인 목록을 추출하여 리포트에 포함

**흔한 ktlint 위반 (자동 포맷 안 되는 것들):**
- `Newline expected before operand in multiline condition` → `&&`/`||` 연산자를 줄 시작으로 이동
- `First line of body expression fits on same line` → 단일 표현식 함수를 한 줄로 작성

---

## Gate 2: 빌드 + 테스트

전체 프로젝트 빌드와 테스트를 실행한다. 이 단계는 **절대 생략할 수 없다**.

```bash
cd [작업경로]
./gradlew clean build --no-daemon --max-workers=2
```

- exit 0 → PASS
- exit non-zero → **FAIL**: 컴파일 에러 또는 테스트 실패.

**실패 시 처리:**
1. 출력에서 `FAILED` 키워드가 포함된 라인을 추출
2. 컴파일 에러면: 에러 메시지와 파일:라인 정보를 리포트
3. 테스트 실패면: 실패한 테스트 클래스명과 메서드명을 리포트
4. **수정 후 Gate 2를 다시 실행** (Gate 1부터 다시 시작할 필요 없음)

**Gradle 데몬 크래시 시:**
- `--no-daemon`이 설정되어 있으므로 데몬 문제는 드물지만, 메모리 부족으로 크래시하면:
  1. `pkill -9 -f "GradleDaemon"` 으로 좀비 프로세스 정리
  2. `--max-workers=1`로 워커 수 줄여서 재시도
  3. 그래도 실패하면 모듈별 빌드로 분할: `./gradlew :nextup-core:build :nextup-infrastructure:build ... --no-daemon`
- **"메모리 부족으로 빌드 생략"은 절대 불가. 반드시 빌드가 통과해야 한다.**

---

## Gate 3: 변경 파일 기반 verify 스킬 실행

`git diff --name-only develop...HEAD`로 변경된 파일 목록을 확인하고, 파일 경로 패턴에 따라 관련 verify 스킬을 자동 선택하여 실행한다.

### 자동 선택 규칙

| 변경 파일 패턴 | 실행할 verify 스킬 |
|---------------|-------------------|
| `*/controller/**/*.kt` | `verify-entity-leak`, `verify-api-response`, `verify-url-prefix`, `verify-repository-injection` |
| `*Mapping`, `*PostMapping`, `*PutMapping`, `*PatchMapping`, `*DeleteMapping` (컨텐츠) | `verify-authorization` |
| `*/core/**` import가 `*/infrastructure/**` 또는 `*/api/**`에서 사용 | `verify-dependency` |
| `throw` 키워드가 포함된 변경 | `verify-custom-exception` |

### 실행 방법

각 선택된 verify 스킬의 SKILL.md를 읽고, 해당 규칙에 따라 변경된 파일을 검증한다:

```
각 verify 스킬에 대해:
1. Read .claude/skills/[스킬명]/SKILL.md
2. 변경된 파일 중 해당 스킬의 검증 대상을 식별
3. 규칙 위반 여부를 코드에서 직접 확인 (Grep/Read 사용)
4. 위반 발견 → FAIL + 위반 내용 리포트
5. 위반 없음 → PASS
```

- 모든 verify 스킬 PASS → Gate 3 PASS
- 하나라도 FAIL → **Gate 3 FAIL**: 위반 스킬명과 구체적 위반 내용을 리포트

---

## Gate 4: 커버리지 확인

Jacoco 리포트에서 변경된 파일의 커버리지를 확인한다.

```bash
cd [작업경로]
# Gate 2에서 이미 빌드가 완료되었으므로 Jacoco 리포트가 생성되어 있다
find . -path "*/build/reports/jacoco/test/jacocoTestReport.xml" -type f
```

### 커버리지 파싱

Jacoco XML 리포트에서 변경된 소스 파일의 라인 커버리지를 추출한다:

```bash
# 변경된 kt 파일 목록
git diff --name-only develop...HEAD -- '*.kt' | grep -v 'Test.kt'

# 각 파일에 대해 Jacoco XML에서 INSTRUCTION covered/missed 추출
```

### 판정 기준

- 변경 파일의 평균 라인 커버리지 **80% 이상** → PASS
- **80% 미만** → **FAIL**: 미커버 파일 목록과 현재 커버리지를 리포트
  - 특히 새로 추가된 Service/Controller 파일의 커버리지가 낮으면 명시
  - "테스트를 추가하라"는 구체적 지시 포함

**예외:**
- DTO, Configuration, Application 클래스는 커버리지 대상에서 제외
- `src/test/` 파일은 대상에서 제외

---

## 최종 리포트

모든 Gate 실행 후 결과를 요약한다:

```markdown
## Pre-PR Gate 결과

| Gate | 항목 | 결과 |
|------|------|------|
| 1 | ktlint 포맷팅 + 검증 | ✅ PASS / ❌ FAIL |
| 2 | 빌드 + 테스트 | ✅ PASS / ❌ FAIL |
| 3 | verify 스킬 검증 | ✅ PASS (N개 스킬) / ❌ FAIL |
| 4 | 커버리지 (80%+) | ✅ PASS (XX%) / ❌ FAIL (XX%) |

**최종 판정: PASS / FAIL**
```

### PASS 시
→ PR 생성 진행 가능. 이 리포트를 PR 본문의 "To Reviewer" 섹션에 포함한다.

### FAIL 시
→ **PR 생성 불가**. 실패한 Gate의 구체적 수정 가이드를 출력한다.
→ 수정 후 이 스킬을 다시 실행하여 전체 게이트를 재검증한다.
→ 부분 재실행은 지원하지 않는다 (항상 Gate 1부터 순서대로).
