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

**재실행 정책: FAIL 시 항상 Gate 1부터 순서대로 다시 실행한다.** 코드를 수정하면 ktlintFormat이 다시 필요할 수 있으므로, 부분 재실행은 하지 않는다.

---

## 사용법

워크트리 경로(또는 현재 작업 디렉토리)에서 실행한다:

```
/pre-pr-gate ../next-up-worktree-426
```

`$ARGUMENTS`가 없으면 현재 디렉토리를 작업 경로로 사용한다.

소스 코드 변경이 없는 PR(스킬 문서, 설정 파일만 변경)에서는 Gate 3, 4를 건너뛸 수 있다.

---

## Gate 1: ktlint 자동 포맷팅 (fix-first 단계)

이 Gate는 "검증"이 아니라 "자동 수정" 단계다. Gate 2의 빌드가 ktlintCheck를 포함하므로, Gate 1의 목적은 **빌드 전에 자동 수정 가능한 위반을 미리 고치는 것**이다.

```bash
cd [작업경로]
./gradlew ktlintFormat --no-daemon
```

ktlintFormat이 수정한 파일이 있으면 `git diff`로 확인하고, 변경사항을 스테이징한다.

**자동 포맷으로 해결 안 되는 흔한 위반:**
- `Newline expected before operand in multiline condition` → `&&`/`||` 연산자를 줄 시작으로 이동
- `First line of body expression fits on same line` → 단일 표현식 함수를 한 줄로 작성

이런 위반은 수동으로 수정한다. Gate 2에서 ktlintCheck가 자동 실행되므로, 여기서 별도로 ktlintCheck를 돌릴 필요는 없다.

→ Gate 1은 항상 PASS (자동 수정 단계이므로). Gate 2로 진행.

---

## Gate 2: 빌드 + 테스트 + ktlint 검증

전체 프로젝트 빌드를 실행한다. 이 빌드에는 컴파일, ktlintCheck, 전체 테스트가 모두 포함된다. **이 단계는 절대 생략할 수 없다.**

```bash
cd [작업경로]
./gradlew clean build --no-daemon --max-workers=2
```

- exit 0 → PASS (컴파일 + ktlint + 테스트 모두 통과)
- exit non-zero → **FAIL**

**실패 시 진단:**
1. 출력에서 `FAILED` 키워드가 포함된 태스크를 확인
2. `ktlintCheck FAILED` → Gate 1에서 수동 수정이 필요했던 위반이 남아있음. 수정 후 Gate 1부터 재시작
3. `compileKotlin FAILED` → 컴파일 에러. 에러 메시지의 파일:라인 정보로 수정
4. `test FAILED` → 테스트 실패. 실패한 테스트 클래스명과 메서드명을 리포트
   - **@PreAuthorize 추가 후 테스트 실패**: Spring Security test context 미설정이 원인일 가능성 높음. `@WithMockUser` 또는 `SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(...)` 추가 필요

**Gradle 데몬 크래시 시:**
1. `pkill -9 -f "GradleDaemon"` 으로 좀비 프로세스 정리
2. `--max-workers=1`로 워커 수 줄여서 재시도
3. 그래도 실패하면 모듈별 빌드: `./gradlew :nextup-core:build :nextup-infrastructure:build :nextup-api:build :nextup-scorer:build :nextup-backoffice:build --no-daemon`
4. **"메모리 부족으로 빌드 생략"은 절대 불가. 반드시 빌드가 통과해야 한다.**

---

## Gate 3: 변경 파일 기반 verify 스킬 실행

변경된 파일을 분석하여 관련 verify 스킬을 자동 선택하고 실행한다.

### Step 1: 파일명 기반 선택

```bash
git diff --name-only develop...HEAD -- '*.kt'
```

| 파일 경로 패턴 | 실행할 verify 스킬 |
|---------------|-------------------|
| `*/controller/**/*.kt` | `verify-entity-leak`, `verify-api-response`, `verify-url-prefix`, `verify-repository-injection` |

### Step 2: 컨텐츠 기반 선택

```bash
git diff develop...HEAD -- '*.kt'
```

diff 출력에서 **추가된 라인(`+`로 시작하는 줄)**을 검사한다:

| 컨텐츠 패턴 (추가된 라인에서 검색) | 실행할 verify 스킬 |
|----------------------------------|-------------------|
| `@PostMapping`, `@PutMapping`, `@PatchMapping`, `@DeleteMapping` | `verify-authorization` |
| `throw ` (throw 키워드 + 공백) | `verify-custom-exception` |
| `import com.nextup.core.` 가 `*/infrastructure/**` 또는 `*/api/**` 파일에 존재 | `verify-dependency` |

### Step 3: 실행

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
- 변경된 `.kt` 파일이 없으면 (스킬/설정 파일만 변경) → Gate 3 SKIP (PASS 처리)

---

## Gate 4: 커버리지 검증

CI와 동일한 Jacoco 커버리지 검증을 실행한다. Gate 2에서 빌드가 완료되었으므로 Jacoco 리포트가 이미 생성되어 있다.

```bash
cd [작업경로]
./gradlew jacocoTestCoverageVerification --no-daemon
```

- exit 0 → PASS (모듈별 커버리지 80%+ 통과)
- exit non-zero → **FAIL**: 어떤 모듈의 커버리지가 부족한지 출력에서 확인

**FAIL 시 추가 확인:**

변경된 파일의 커버리지를 구체적으로 파악하기 위해:

```bash
# 변경된 소스 파일 목록 (테스트 제외)
git diff --name-only develop...HEAD -- '*.kt' | grep -v 'Test.kt' | grep -v '/test/'

# 각 모듈의 HTML 리포트에서 해당 파일의 커버리지 확인
# 예: nextup-core/build/reports/jacoco/test/html/index.html
```

미커버 파일 목록과 함께 "테스트를 추가하라"는 구체적 지시를 리포트에 포함한다.

**예외:**
- 소스 코드 변경이 없는 PR → Gate 4 SKIP (PASS 처리)

---

## 최종 리포트

모든 Gate 실행 후 결과를 요약한다:

```markdown
## Pre-PR Gate 결과

| Gate | 항목 | 결과 |
|------|------|------|
| 1 | ktlint 자동 포맷팅 | ✅ DONE |
| 2 | 빌드 + 테스트 + ktlint 검증 | ✅ PASS / ❌ FAIL |
| 3 | verify 스킬 검증 | ✅ PASS (N개 스킬) / ❌ FAIL / ⏭ SKIP |
| 4 | 커버리지 검증 | ✅ PASS / ❌ FAIL / ⏭ SKIP |

**최종 판정: PASS / FAIL**
```

### PASS 시
→ PR 생성 진행 가능. 이 리포트를 PR 본문의 "To Reviewer" 섹션에 포함한다.

### FAIL 시
→ **PR 생성 불가**. 실패한 Gate의 구체적 수정 가이드를 출력한다.
→ 수정 후 이 스킬을 다시 실행하여 **Gate 1부터** 전체 게이트를 재검증한다.
