---
name: verify-dependency
description: 멀티모듈 의존성 방향 규칙을 검증합니다. 모듈 간 import/의존성 변경 후 사용.
---

# 모듈 의존성 방향 검증

## Purpose

1. `nextup-core`가 상위 계층(infra, api, backoffice, scorer)을 import하지 않는지 검증
2. `nextup-common`이 다른 모듈을 import하지 않는지 검증
3. API 계층 모듈(api, backoffice, scorer) 간 상호 의존이 없는지 검증
4. `nextup-infrastructure`가 API 계층을 import하지 않는지 검증

## When to Run

- 새로운 import 문 추가 후
- 모듈 간 클래스 이동 후
- `build.gradle.kts` 의존성 변경 후
- PR 전 통합 검증 시

## Related Files

| File | Purpose |
|------|---------|
| `nextup-core/src/**/*.kt` | Core 모듈 소스 |
| `nextup-common/src/**/*.kt` | Common 모듈 소스 |
| `nextup-infrastructure/src/**/*.kt` | Infra 모듈 소스 |
| `nextup-api/src/**/*.kt` | API 모듈 소스 |
| `nextup-backoffice/src/**/*.kt` | 백오피스 모듈 소스 |
| `nextup-scorer/src/**/*.kt` | 스코어러 모듈 소스 |
| `*/build.gradle.kts` | 모듈별 Gradle 빌드 설정 |

## Workflow

### Step 1: Core → 상위 계층 역방향 의존 검사

`nextup-core`가 infra/api/backoffice/scorer를 import하는지 검사합니다.

**도구:** Grep

**패턴:**
```
^import com\.nextup\.(infrastructure|api|backoffice|scorer)
```

**대상:** `nextup-core/src/**/*.kt`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** 1건 이상 매칭 시 의존성 방향 위반 (🔴 즉시 REJECT)

**수정 방법:** Core에서 필요한 인터페이스를 Port로 정의하고, 구현을 Infra에 위치

### Step 2: Common → 다른 모듈 의존 검사

`nextup-common`이 다른 모듈을 import하는지 검사합니다.

**도구:** Grep

**패턴:**
```
^import com\.nextup\.(core|infrastructure|api|backoffice|scorer)
```

**대상:** `nextup-common/src/**/*.kt`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** 1건 이상 매칭 시 Common 리프 모듈 규칙 위반 (🔴 즉시 REJECT)

**수정 방법:** Common에 비즈니스 로직을 두지 않고, 순수 유틸리티/예외만 포함

### Step 3: API 계층 상호 의존 검사

api ↔ backoffice ↔ scorer 간 상호 import가 없는지 검사합니다.

**도구:** Grep

**패턴 (api):**
```
^import com\.nextup\.(backoffice|scorer)
```
**대상:** `nextup-api/src/**/*.kt`

**패턴 (backoffice):**
```
^import com\.nextup\.(api|scorer)
```
**대상:** `nextup-backoffice/src/**/*.kt`

**패턴 (scorer):**
```
^import com\.nextup\.(api|backoffice)
```
**대상:** `nextup-scorer/src/**/*.kt`

**PASS 기준:** 모든 패턴에서 매칭 결과 0건
**FAIL 기준:** 1건 이상 매칭 시 API 계층 격리 위반 (🔴 즉시 REJECT)

**수정 방법:** 공통 로직은 `nextup-infrastructure`로 이동

### Step 4: Infrastructure → API 계층 역방향 의존 검사

`nextup-infrastructure`가 API 계층을 import하는지 검사합니다.

**도구:** Grep

**패턴:**
```
^import com\.nextup\.(api|backoffice|scorer)
```

**대상:** `nextup-infrastructure/src/**/*.kt`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** 1건 이상 매칭 시 의존성 방향 위반 (🔴 즉시 REJECT)

### Step 5: Core → Spring Data 프레임워크 의존 검사

`nextup-core`가 Spring Data를 import하는지 검사합니다. Core는 프레임워크에 독립적이어야 합니다.

**도구:** Grep

**패턴:**
```
^import org\.springframework\.data
```

**대상:** `nextup-core/src/main/kotlin/**/*.kt`

**PASS 기준:** 매칭 결과 0건 (Core가 Spring Data를 사용하지 않음)
**FAIL 기준:** 1건 이상 매칭 시 프레임워크 의존성 위반 (🔴 즉시 REJECT)

**수정 방법:** `Page`/`Pageable` → Core의 `PageCommand`/`PageResult` 커스텀 타입 사용, `JpaRepository` → `RepositoryPort` 인터페이스 사용

### Step 6: Core Gradle에서 Spring Data 의존성 금지 검사

`nextup-core/build.gradle.kts`에 `spring-boot-starter-data-jpa` 또는 `spring-data` 계열 의존성이 없는지 검사합니다.

**도구:** Grep

**패턴:**
```
spring-boot-starter-data-jpa|spring-data
```

**대상:** `nextup-core/build.gradle.kts`

**PASS 기준:** 매칭 결과 0건
**FAIL 기준:** Spring Data 계열 의존성 발견 (🔴 즉시 REJECT)

**수정 방법:** `spring-boot-starter-data-jpa` → `jakarta.persistence-api` + `hibernate-core` + `spring-tx` + `spring-context`로 교체

### Step 7: Gradle 프로젝트 의존성 검사

각 모듈의 `build.gradle.kts`에서 금지된 프로젝트 의존성이 없는지 검사합니다.

**도구:** Grep

**패턴 (core):**
```
project.*:(nextup-infrastructure|nextup-api|nextup-backoffice|nextup-scorer)
```
**대상:** `nextup-core/build.gradle.kts`

**패턴 (common):**
```
project.*:nextup-
```
**대상:** `nextup-common/build.gradle.kts`

**PASS 기준:** 금지된 의존성 없음
**FAIL 기준:** 금지된 프로젝트 의존성 발견 (🔴 즉시 REJECT)

## Output Format

```markdown
| # | 모듈 | 파일 | 위반 | 심각도 |
|---|------|------|------|--------|
| 1 | core | `Service.kt:3` | infra import | 🔴 REJECT |
```

## Exceptions

1. **테스트 코드의 테스트 유틸** — `src/test/`에서 테스트 편의를 위해 다른 모듈의 테스트 유틸을 참조하는 경우 (단, 프로덕션 코드는 불가)
2. **Gradle testImplementation** — 테스트 의존성으로 다른 모듈을 참조하는 것은 허용 (프로덕션 의존성만 검사)
3. **패키지 이름이 유사한 경우** — `com.nextup.common.dto.ApiResponse`는 common 모듈이므로 api 모듈 참조가 아님
