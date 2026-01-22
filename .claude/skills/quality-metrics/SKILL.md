# Quality-Metrics Skill - 빌드/테스트/커버리지 통합 검증

> **재사용 가능한 품질 검증 도구**
> Gradle + Jacoco + Codecov + ktlint + detekt 통합

---

## 📚 Skill 개요

### 목적
- Gradle 빌드 자동 실행 및 결과 분석
- JUnit 테스트 실행 및 실패 원인 파악
- Jacoco 코드 커버리지 측정 (80% 기준)
- Codecov PR 연동
- ktlint 코드 스타일 검사
- detekt 정적 분석

### 사용 시나리오
- ✅ PR 생성 전 품질 검증
- ✅ 빌드 실패 시 원인 분석
- ✅ 커버리지 80% 미만 시 차단

### 호출 방법
```
"/build" - 빌드 & 테스트 실행
"/review" - 품질 전체 검증 (자동 포함)
```

---

## 🏗️ Gradle Build 검증

### 1. 빌드 실행
```bash
./gradlew clean build
```

### 2. 결과 분석
```kotlin
// 빌드 성공
BUILD SUCCESSFUL in 1m 23s

// 빌드 실패
FAILURE: Build failed with an exception.
* What went wrong:
Execution failed for task ':nextup-core:compileKotlin'.
> Compilation error. See log for more details.
```

### 3. 실패 원인 분류

#### 컴파일 에러
```
Error: Unresolved reference: PlayerRepository
→ import 누락 또는 의존성 문제
```

#### 테스트 실패
```
PlayerTest > 타순을 변경할 수 있다() FAILED
    Expected: 3
    Actual: null
→ 비즈니스 로직 오류
```

#### 의존성 문제
```
Could not resolve com.nextup:nextup-core:0.0.1-SNAPSHOT
→ 모듈 의존성 설정 오류
```

---

## 🧪 JUnit 테스트 검증

### 1. 테스트 실행
```bash
./gradlew test
```

### 2. 테스트 리포트 확인
```bash
# HTML 리포트 생성
open build/reports/tests/test/index.html
```

### 3. 실패 테스트 분석
```kotlin
// 테스트 실패 예시
@Test
fun `타율을 계산할 수 있다`() {
    val record = BattingRecord(hits = 3, atBats = 10)
    val average = record.calculateAverage()

    // Expected: 0.300, Actual: 0.30
    assertThat(average).isEqualTo(0.300)  // FAIL
}

// 원인: 소수점 자리수 문제
// 해결: round(3) 추가
```

---

## 📊 Jacoco 코드 커버리지

### 1. Gradle 설정

#### 루트 build.gradle.kts
```kotlin
plugins {
    id("org.gradle.jacoco") version "0.8.12"
}

subprojects {
    apply(plugin = "jacoco")

    jacoco {
        toolVersion = "0.8.12"
    }

    tasks.test {
        finalizedBy(tasks.jacocoTestReport)
    }

    tasks.jacocoTestReport {
        dependsOn(tasks.test)
        reports {
            xml.required.set(true)  // Codecov용
            html.required.set(true)  // 로컬 확인용
        }
    }

    tasks.jacocoTestCoverageVerification {
        violationRules {
            rule {
                limit {
                    minimum = "0.80".toBigDecimal()  // 80%
                }
            }
        }
    }
}
```

#### 멀티모듈 통합 리포트
```kotlin
// 루트 build.gradle.kts
tasks.register<JacocoReport>("jacocoRootReport") {
    dependsOn(subprojects.map { it.tasks.test })

    additionalSourceDirs.setFrom(subprojects.flatMap { it.sourceSets.main.get().allSource.srcDirs })
    sourceDirectories.setFrom(subprojects.flatMap { it.sourceSets.main.get().allSource.srcDirs })
    classDirectories.setFrom(subprojects.flatMap { it.sourceSets.main.get().output })
    executionData.setFrom(subprojects.flatMap { it.tasks.jacocoTestReport.get().executionData })

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
```

### 2. 커버리지 실행
```bash
# 테스트 + 커버리지 리포트
./gradlew test jacocoTestReport

# 통합 리포트
./gradlew jacocoRootReport

# 커버리지 검증
./gradlew jacocoTestCoverageVerification
```

### 3. 리포트 확인
```bash
# HTML 리포트 열기
open build/reports/jacoco/jacocoRootReport/html/index.html

# XML 리포트 위치 (Codecov 업로드용)
build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml
```

### 4. 커버리지 기준

| 모듈 | 목표 커버리지 | 필수 여부 |
|------|--------------|-----------|
| **nextup-core** | **80% 이상** | ✅ 필수 (PR 블록) |
| **nextup-api** | 70% 이상 | 권장 |
| **nextup-infrastructure** | 60% 이상 | 권장 |

---

## 🎯 Codecov 설정

### 1. codecov.yml 생성
```yaml
# .codecov.yml
codecov:
  require_ci_to_pass: yes  # CI 통과 필수

coverage:
  precision: 2
  round: down
  range: "70...100"

  status:
    project:
      default:
        target: 80%           # 전체 프로젝트 80%
        threshold: 1%         # 1% 감소까지 허용
        if_ci_failed: error   # CI 실패 시 에러

    patch:
      default:
        target: 80%           # 새 코드는 무조건 80%
        threshold: 0%         # 감소 불허
        if_ci_failed: error

# 모듈별 설정
flags:
  core:
    paths:
      - nextup-core/
    target: 80%

  api:
    paths:
      - nextup-api/
    target: 70%

  infrastructure:
    paths:
      - nextup-infrastructure/
    target: 60%

# 커버리지 제외 파일
ignore:
  - "**/*Test.kt"
  - "**/*Config.kt"
  - "**/Application.kt"

# PR 코멘트 설정
comment:
  layout: "reach,diff,flags,files,footer"
  behavior: default
  require_changes: no
```

### 2. GitHub Actions 연동
```yaml
# .github/workflows/test.yml
name: Test & Coverage

on:
  push:
    branches: [ develop, main ]
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
          distribution: 'temurin'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run tests with coverage
        run: ./gradlew test jacocoRootReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          token: ${{ secrets.CODECOV_TOKEN }}
          files: ./build/reports/jacoco/jacocoRootReport/jacocoRootReport.xml
          flags: unittests
          name: codecov-umbrella
          fail_ci_if_error: true

      - name: Verify coverage
        run: ./gradlew jacocoTestCoverageVerification
```

### 3. Codecov 토큰 설정
```bash
# GitHub Secrets에 등록
# Settings > Secrets and variables > Actions > New repository secret
Name: CODECOV_TOKEN
Value: [Codecov 대시보드에서 복사]
```

### 4. PR 자동 코멘트 예시
```markdown
## Codecov Report
> Merging #15 into develop will **increase** coverage by 0.34%

@@            Coverage Diff            @@
##           develop     #15     +/-   ##
=========================================
+ Coverage    79.66%  80.00%  +0.34%
=========================================
  Files           12      13      +1
  Lines          298     310     +12
  Branches        28      30      +2
=========================================
+ Hits           237     248     +11
- Misses          61      62      +1
```

---

## 🎨 ktlint 코드 스타일

### 1. 설정
```kotlin
// build.gradle.kts
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}

ktlint {
    version.set("1.0.1")
    android.set(false)
    outputToConsole.set(true)
    ignoreFailures.set(false)
}
```

### 2. 실행
```bash
# 스타일 검사
./gradlew ktlintCheck

# 자동 포맷
./gradlew ktlintFormat
```

### 3. 검증 항목
- 들여쓰기 (4 spaces)
- 최대 줄 길이 (120자)
- Import 정렬
- 불필요한 공백 제거

---

## 🔬 detekt 정적 분석

### 1. 설정
```kotlin
// build.gradle.kts
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.detekt {
    reports {
        html.required.set(true)
        xml.required.set(true)
    }
}
```

### 2. 실행
```bash
./gradlew detekt
```

### 3. 검증 항목
- Bugs (잠재적 버그)
- Code Smell (나쁜 코드 패턴)
- Complexity (복잡도)
- Performance (성능 이슈)

---

## 🚀 통합 검증 스크립트

### scripts/full-check.sh
```bash
#!/bin/bash
set -e

echo "=== Quality Metrics Check ==="

# 1. 빌드
echo "1. Building project..."
./gradlew clean build
echo "✅ Build PASS"

# 2. 테스트
echo "2. Running tests..."
./gradlew test
echo "✅ Tests PASS"

# 3. 커버리지
echo "3. Checking coverage..."
./gradlew jacocoRootReport jacocoTestCoverageVerification
echo "✅ Coverage ≥ 80%"

# 4. 코드 스타일
echo "4. Checking code style..."
./gradlew ktlintCheck
echo "✅ Code style PASS"

# 5. 정적 분석
echo "5. Running static analysis..."
./gradlew detekt
echo "✅ Detekt PASS"

echo ""
echo "🎉 All quality metrics PASSED!"
```

---

## 📋 체크리스트

- [ ] `./gradlew build` 성공
- [ ] 모든 테스트 통과
- [ ] nextup-core 커버리지 ≥ 80%
- [ ] ktlint 검사 통과
- [ ] detekt 버그 0건

**하나라도 실패 시 → REJECT**

---

## 📊 보고서 템플릿

```markdown
# 품질 검증 보고서

## 빌드
- 상태: ✅ PASS
- 시간: 1m 23s

## 테스트
- 전체: 45개
- 성공: 45개
- 실패: 0개
- 상태: ✅ PASS

## 커버리지
- nextup-core: 82.5% ✅
- nextup-api: 71.3% ✅
- nextup-infrastructure: 62.1% ✅
- 전체: 78.9% (목표: 80%)
- 상태: ❌ FAIL (전체 커버리지 미달)

## 코드 품질
- ktlint: ✅ PASS
- detekt: ❌ FAIL (bugs: 2건)

## 종합 판정
❌ REJECT - 커버리지 미달, detekt bugs 발견

## 수정 필요 사항
1. [ ] nextup-api 커버리지 향상 (71.3% → 75%)
2. [ ] detekt bugs 2건 수정
```

---

## 🎯 이 Skill의 장점

1. **자동화**: 전체 품질 검증 한 번에
2. **일관성**: 모든 PR에 동일한 기준 적용
3. **사전 방지**: 머지 전 품질 보장
4. **가시성**: 커버리지 뱃지로 현황 파악

---

## 📚 참고 자료

- [Jacoco Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Codecov Documentation](https://docs.codecov.com/)
- [ktlint Rules](https://pinterest.github.io/ktlint/)
- [detekt Rules](https://detekt.dev/docs/intro)
