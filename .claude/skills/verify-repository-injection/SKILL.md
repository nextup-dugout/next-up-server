---
name: verify-repository-injection
description: Controller 클래스를 작성하거나 수정할 때 반드시 이 스킬을 참조하라. Controller에 Repository나 RepositoryPort를 주입하는 것은 레이어 바이패스이며 인가/감사/불변식 검증을 우회하는 심각한 아키텍처 위반이다. Controller의 constructor 파라미터를 추가하거나, import문을 작성할 때 즉시 트리거하라. 항상 Service를 통해 접근해야 한다.
---

# Controller Repository 직접 주입 금지 검증

## Purpose

1. Controller가 Service 계층을 건너뛰고 Repository/RepositoryPort를 직접 주입하는 패턴 탐지
2. 레이어 바이패스로 인한 인가 우회, 감사 로깅 누락, 도메인 불변식 검증 누락 방지
3. Hexagonal Architecture의 레이어 분리 원칙 준수 검증

## When to Run

- 새로운 Controller 추가 후
- Controller의 의존성(constructor 파라미터) 변경 후
- Repository/RepositoryPort 관련 수정 후
- PR 전 통합 검증 시

## Related Files

| File | Purpose |
|------|---------|
| `nextup-api/src/main/kotlin/com/nextup/api/controller/**/*.kt` | API 컨트롤러 |
| `nextup-backoffice/src/main/kotlin/com/nextup/backoffice/controller/**/*.kt` | 백오피스 컨트롤러 |
| `nextup-scorer/src/main/kotlin/com/nextup/scorer/controller/**/*.kt` | 스코어러 컨트롤러 |
| `nextup-core/src/main/kotlin/com/nextup/core/repository/**/*.kt` | Repository Port 인터페이스 |
| `nextup-infrastructure/src/main/kotlin/com/nextup/infrastructure/repository/**/*.kt` | Repository 구현체 |

## Workflow

### Step 1: Controller에서 Repository import 탐지

Controller 파일에서 Repository 또는 RepositoryPort를 import하는지 검사합니다.

**도구:** Grep

**패턴:**
```
^import.*\.(repository|Repository|RepositoryPort)
```

**대상:** `**/controller/**/*.kt`

**PASS 기준:** Controller에서 Repository/RepositoryPort import 0건
**FAIL 기준:** Controller가 Repository/RepositoryPort를 직접 import

**수정 방법:**
```kotlin
// FAIL — Controller가 Repository 직접 사용
import com.nextup.core.repository.TeamRepositoryPort

@RestController
class TeamController(
    private val teamRepository: TeamRepositoryPort,  // ❌ 레이어 바이패스
) {
    fun createTeam() {
        val team = teamRepository.save(team)  // ❌ Service 우회
    }
}

// PASS — Service를 통해 접근
import com.nextup.core.service.team.TeamService

@RestController
class TeamController(
    private val teamService: TeamService,  // ✅ Service 경유
) {
    fun createTeam() {
        val team = teamService.createTeam(request)  // ✅ 인가/감사/불변식 포함
    }
}
```

### Step 2: Constructor 파라미터에서 Repository 타입 탐지

Controller 클래스의 constructor에 `Repository` 또는 `RepositoryPort` 타입이 포함되는지 검사합니다.

**도구:** Grep

**패턴:**
```
(val|var)\s+\w*(repository|Repository|RepositoryPort)\w*\s*:
```

**대상:** `**/controller/**/*.kt`

**PASS 기준:** Controller constructor에 Repository 타입 파라미터 0건
**FAIL 기준:** Repository 타입이 Controller constructor에 존재

### Step 3: Controller 내에서 Repository 메서드 직접 호출 탐지

Controller 메서드 내에서 `repository.save()`, `repository.findById()`, `repository.delete()` 등 Repository 메서드를 직접 호출하는지 검사합니다.

**도구:** Grep

**패턴:**
```
(repository|Repository|RepositoryPort)\.(save|find|delete|exists|count)
```

**대상:** `**/controller/**/*.kt`

**PASS 기준:** Controller에서 Repository 메서드 직접 호출 0건
**FAIL 기준:** Controller가 Repository 메서드를 직접 호출

## Output Format

```markdown
| # | 파일 | 라인 | 문제 | 심각도 |
|---|------|------|------|--------|
| 1 | `TeamController.kt:40` | TeamRepositoryPort 직접 주입 | 🟠 HIGH (레이어 바이패스) |
| 2 | `TeamController.kt:62` | teamRepository.save() 직접 호출 | 🟠 HIGH (인가/감사 우회) |
```

## Exceptions

1. **테스트 코드** — `src/test/` 내 파일은 면제
2. **Infrastructure 모듈의 Service 구현체** — `ServiceImpl`에서 Repository를 사용하는 것은 정상 (Hexagonal Adapter)
3. **Configuration 클래스** — `@Configuration` 클래스에서 Repository를 사용하는 것은 면제 (초기 데이터 설정 등)
