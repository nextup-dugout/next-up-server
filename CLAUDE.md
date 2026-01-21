# ⚾️ CLAUDE.md (NEXT-UP)

> **이 문서는 NEXT-UP 프로젝트의 최상위 헌법입니다.** 모든 에이전트는 이 문서를 최우선으로 준수합니다.

---

## 📝 Project Status & Goal

* **Status**: Initial Planning & Infrastructure Setup (Phase 1).
* **Project**: 사회인 야구 기록 서비스 'NEXT-UP' 백엔드 개발.
* **Goal**: 복잡한 야구 규칙(DH 규칙 해제 등)을 player-centric 데이터 모델로 처리.

---

## 🏛️ Architecture Philosophy

### 1. 아키텍처 지향점

| 원칙 | 설명 |
|------|------|
| **Domain-Driven Design (DDD)** | 모든 비즈니스 결정은 `nextup-core`의 도메인 모델을 중심으로 이루어집니다. |
| **Hexagonal Architecture** | Core(Inside)는 비즈니스 로직, Infra/API(Outside)는 어댑터 역할을 수행합니다. |
| **Single Responsibility Principle (SRP)** | 하나의 클래스/함수는 오직 하나의 변경 이유만 가집니다. 서비스 분리를 두려워하지 않습니다. |

### 2. Hexagonal (Ports and Adapters) 구조

```
                    ┌─────────────────────────────────┐
                    │         Outside World           │
                    │   (HTTP, DB, External APIs)     │
                    └───────────────┬─────────────────┘
                                    │
              ┌─────────────────────┼─────────────────────┐
              │                     │                     │
              ▼                     ▼                     ▼
    ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
    │   nextup-api    │   │  nextup-infra   │   │  External APIs  │
    │   (Adapter)     │   │   (Adapter)     │   │   (Adapter)     │
    │  - Controller   │   │  - Repository   │   │  - Client       │
    │  - DTO          │   │  - JPA Impl     │   │                 │
    └────────┬────────┘   └────────┬────────┘   └────────┬────────┘
             │                     │                     │
             │         ┌───────────┴───────────┐         │
             │         │                       │         │
             └────────▶│     nextup-core       │◀────────┘
                       │      (Inside)         │
                       │                       │
                       │  ┌─────────────────┐  │
                       │  │  Domain Model   │  │
                       │  │  - Entities     │  │
                       │  │  - Value Objects│  │
                       │  │  - Domain Events│  │
                       │  └─────────────────┘  │
                       │                       │
                       │  ┌─────────────────┐  │
                       │  │     Ports       │  │
                       │  │  - Repository   │  │
                       │  │    Interfaces   │  │
                       │  │  - Service      │  │
                       │  │    Interfaces   │  │
                       │  └─────────────────┘  │
                       │                       │
                       └───────────────────────┘
```

### 3. 핵심 규칙

* **Core 순수성**: `nextup-core`는 **외부 환경을 모릅니다**. JPA 어노테이션 외의 프레임워크 종속성 최소화.
* **Port 정의**: Repository 인터페이스는 `nextup-core`에 정의 (Port).
* **Adapter 구현**: Repository 구현체는 `nextup-infrastructure`에 위치 (Adapter).
* **의존성 방향**: 항상 Outside → Inside 방향. Core가 Infra를 알면 **절대 안 됨**.

```kotlin
// ✅ Port (nextup-core)
interface PlayerRepository {
    fun findById(id: Long): Player?
    fun save(player: Player): Player
}

// ✅ Adapter (nextup-infrastructure)
@Repository
class PlayerRepositoryImpl(
    private val jpaRepository: PlayerJpaRepository
) : PlayerRepository {
    override fun findById(id: Long) = jpaRepository.findByIdOrNull(id)
    override fun save(player: Player) = jpaRepository.save(player)
}
```

---

## 🏗️ Multi-Module Architecture & Dependency Rules

의존성 충돌 방지와 관심사 분리를 위한 엄격한 멀티모듈 구조를 따릅니다.

```
┌─────────────────────────────────────────────────────┐
│                    nextup-api                        │
│         (REST APIs, DTOs, Security)                  │
└─────────────────────┬───────────────────────────────┘
                      │ depends on
          ┌───────────┴───────────┐
          ▼                       ▼
┌─────────────────┐     ┌─────────────────────────────┐
│  nextup-infra   │     │       nextup-core            │
│  (JPA, PostGIS, │────▶│  (Pure Domain, Entities)     │
│   Clients)      │     └──────────────┬──────────────┘
└────────┬────────┘                    │
         │                             │
         └──────────────┬──────────────┘
                        ▼
              ┌─────────────────┐
              │  nextup-common  │
              │   (Utilities)   │
              └─────────────────┘
```

### 모듈별 규칙

| 모듈 | 역할 | Hexagonal 역할 | 허용된 의존성 |
|------|------|----------------|---------------|
| `nextup-api` | REST API, DTO, Security | **Driving Adapter** (In) | `infra`, `core`, `common` |
| `nextup-core` | 순수 도메인 로직, JPA Entity | **Core (Inside)** | `common` **ONLY** |
| `nextup-infrastructure` | 영속성(JPA, PostGIS), 외부 클라이언트 | **Driven Adapter** (Out) | `core`, `common` |
| `nextup-common` | 범용 유틸리티, 상수 | **Shared Kernel** | **NONE** (리프 모듈) |

> ⚠️ **Dependency Constraints**:
> * **No Circular Dependencies**: 모듈 간 순환 참조 **절대 금지**.
> * **Common Module Guard**: `nextup-common`에 비즈니스 로직이나 무거운 라이브러리 금지.
> * **Boundary Integrity**: 인프라 세부사항이 `nextup-core`로 누수되지 않도록 유지.

---

## 📢 Communication & Error Standards

에이전트들이 API 레이어를 개발할 때 반드시 따라야 할 **'응답의 법'**입니다.

### 1. DTO 변환 원칙

| 규칙 | 설명 |
|------|------|
| **Zero Entity Leak** | Entity는 **절대** `nextup-api` 밖으로 노출되지 않습니다. |
| **Mapping Responsibility** | `data-transformer` 또는 Controller 레이어에서 반드시 DTO로 변환합니다. |

```kotlin
// ❌ 잘못됨: Entity 직접 반환
@GetMapping("/{id}")
fun getPlayer(@PathVariable id: Long): Player  // VETO 대상!

// ✅ 올바름: DTO로 변환하여 반환
@GetMapping("/{id}")
fun getPlayer(@PathVariable id: Long): ApiResponse<PlayerResponse>
```

### 2. 표준 응답 객체 (ApiResponse)

**모든 API 응답은 일관된 구조**를 가져야 합니다.

```kotlin
// nextup-api/src/.../common/ApiResponse.kt
data class ApiResponse<T>(
    val status: Int,
    val message: String,
    val data: T? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T, message: String = "Success"): ApiResponse<T> =
            ApiResponse(status = 200, message = message, data = data)

        fun <T> created(data: T, message: String = "Created"): ApiResponse<T> =
            ApiResponse(status = 201, message = message, data = data)

        fun error(status: Int, message: String): ApiResponse<Nothing> =
            ApiResponse(status = status, message = message, data = null)
    }
}
```

**응답 예시:**
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "김선수",
    "position": "투수"
  },
  "timestamp": "2025-01-20T15:30:00"
}
```

### 3. 에러 핸들링 전략

#### ErrorCode 열거형

```kotlin
// nextup-common/src/.../exception/ErrorCode.kt
enum class ErrorCode(
    val status: Int,
    val message: String
) {
    // 400 Bad Request
    INVALID_INPUT(400, "잘못된 입력입니다"),
    INVALID_PLAYER_STATUS(400, "유효하지 않은 선수 상태입니다"),

    // 404 Not Found
    PLAYER_NOT_FOUND(404, "선수를 찾을 수 없습니다"),
    TEAM_NOT_FOUND(404, "팀을 찾을 수 없습니다"),
    GAME_NOT_FOUND(404, "경기를 찾을 수 없습니다"),

    // 409 Conflict
    PLAYER_ALREADY_IN_TEAM(409, "이미 팀에 소속된 선수입니다"),
    DUPLICATE_BACK_NUMBER(409, "이미 사용 중인 등번호입니다"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다")
}
```

#### CustomException 정의

```kotlin
// nextup-common/src/.../exception/CustomException.kt
open class CustomException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.message,
    override val cause: Throwable? = null
) : RuntimeException(message, cause)

// 도메인별 예외 (예시)
class PlayerNotFoundException(playerId: Long) : CustomException(
    errorCode = ErrorCode.PLAYER_NOT_FOUND,
    message = "Player not found: $playerId"
)

class PlayerAlreadyInTeamException(playerId: Long, teamId: Long) : CustomException(
    errorCode = ErrorCode.PLAYER_ALREADY_IN_TEAM,
    message = "Player $playerId is already in team $teamId"
)
```

#### Global Exception Handler

```kotlin
// nextup-api/src/.../exception/GlobalExceptionHandler.kt
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ApiResponse<Nothing>> {
        return ResponseEntity
            .status(e.errorCode.status)
            .body(ApiResponse.error(e.errorCode.status, e.message))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error(400, message))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ApiResponse<Nothing>> {
        // 로깅 필수
        return ResponseEntity
            .internalServerError()
            .body(ApiResponse.error(500, "서버 내부 오류가 발생했습니다"))
    }
}
```

### 4. Reviewer 검증 항목 (VETO 대상)

| 위반 사항 | VETO 여부 |
|-----------|-----------|
| Entity를 Controller 반환 타입으로 사용 | **즉시 REJECT** |
| `ApiResponse`로 감싸지 않은 응답 | **REJECT** |
| `RuntimeException` 직접 throw | **REJECT** (CustomException 사용) |
| ErrorCode 없이 하드코딩된 에러 메시지 | **REJECT** |

---

## 🛠️ Tech Stack

| 항목 | 버전 |
|------|------|
| **Language** | Kotlin 2.1.10 (JDK 21) |
| **Framework** | Spring Boot 3.4.1 |
| **Database** | PostgreSQL + PostGIS |
| **Build Tool** | Gradle 8.12 (Kotlin DSL) |
| **GitHub Integration** | MCP (Model Context Protocol) |

### MCP 연동

GitHub 작업(이슈, PR, 브랜치 관리 등)은 **MCP를 통해 직접 접근**합니다.

```bash
# 설정된 MCP 서버
claude mcp add --transport http github https://api.githubcopilot.com/mcp/
```

> MCP를 통해 `github-manager` 에이전트가 GitHub API에 직접 접근하여 이슈 생성, PR 관리 등을 수행합니다.

---

## 🤖 Agent & Skill Architecture

**판단(Agent)과 실행(Skill)의 분리 원칙**을 엄격히 준수합니다.

### Agent 구조 (`.claude/agents/`)

#### Council Team (Sonnet) - 전략 및 거버넌스
| Agent | 역할 |
|-------|------|
| `planner` | 기획 총괄, Brief 작성 |
| `tech-lead` | 기술 스택/ADR 결정 |
| `reviewer` | **헌법 검사, 최종 승인/거부권** |
| `risk-manager` | 실패 원인 분석, 재작업 루프 |
| `baseball-expert` | 야구 규칙 팩트 체크 |
| `security-auditor` | **OWASP 기반 보안 취약점 검사** |

#### Execution Team (Haiku/Sonnet) - 개발 실행
| Agent | 역할 |
|-------|------|
| `modeler` | Core/Entity 개발 |
| `logic-broker` | Infra/Repository 구현 |
| `api-specialist` | API/Controller 설계 |
| `data-transformer` | DTO/Mapper 전담 |
| `scenario-tester` | 시나리오/통합 테스트 |
| `github-manager` | 이슈/브랜치/PR 관리 |
| `knowledge-manager` | CLAUDE.md 최신화 |

### Skill 구조 (`.claude/skills/`)

| Skill | 역할 | 스크립트 |
|-------|------|----------|
| `build-validator` | Gradle 빌드/테스트 검증 | `validate_build.sh`, `test_analyzer.py` |
| `git-toolkit` | GitHub 이슈/PR 조작 | `gh_issue_creator.sh`, `gh_pr_manager.py` |
| `db-manager` | PostGIS 검증, DDL 생성 | `check_postgis.py`, `generate_ddl.sh` |
| `code-quality` | ktlint/detekt 정적 분석 | `run_ktlint.sh`, `run_detekt.sh`, `quality_report.py` |

### ⚠️ Reviewer 거부권 (VETO POWER)

**`reviewer` 에이전트는 다음 조건에서 무조건적 승인 거부 권한을 행사합니다:**

1. **CLAUDE.md 의존성 규칙 위반** → 즉시 REJECT
2. **`./gradlew build` 실패** → 즉시 REJECT
3. **테스트 실패** → 즉시 REJECT
4. **커밋 컨벤션 위반** → REJECT
5. **`baseball-expert` REJECT 판정** → 해당 판정 존중
6. **Entity 직접 노출 (Zero Entity Leak 위반)** → 즉시 REJECT
7. **ApiResponse 미사용** → REJECT
8. **CustomException 미사용 (RuntimeException 직접 throw)** → REJECT
9. **`security-auditor` Critical/High 취약점 발견** → 즉시 REJECT
10. **`code-quality` detekt potential-bugs 발견** → REJECT
11. **ktlint 에러 또는 경고 10개 이상** → REJECT

**거부권은 절대적이며, 다른 에이전트가 무효화할 수 없습니다.**

---

## 📌 Git & Coding Convention

### 1. Commit Message (Udacity Style)

```
type: subject (50자 이내)

body (선택, 72자 줄바꿈)

footer (선택, 이슈 참조)
```

**Type 종류:**
| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서화 |
| `chore` | 빌드 설정, 의존성 등 |

> ⚠️ **ZERO BROKEN BUILDS**: 커밋 전 `./gradlew build` 통과 **필수**.

### 2. Branch Convention

```
feat/[이슈번호]-[기능명]      # 기능 개발
fix/[이슈번호]-[버그명]       # 버그 수정
refactor/[이슈번호]-[대상]    # 리팩토링
docs/[이슈번호]-[문서명]      # 문서화
```

### 3. Pull Request Convention

> ⚠️ **PR 생성 시 반드시 `.github/PULL_REQUEST_TEMPLATE.md` 템플릿을 준수해야 합니다.**

```markdown
## Summary

>- 관련 이슈 태그 (예: Close #1)

**PR 요약 작성**

## Tasks

- 포함된 작업 목록
- 포함된 작업 목록

## To Reviewer

_(선택) 리뷰어에게 전달할 내용_

## Screenshot

_(선택) 스크린샷 첨부_
```

| 규칙 | 설명 |
|------|------|
| **Issue 연결 필수** | Summary에 `>- Close #이슈번호` 형식으로 관련 이슈 태그 |
| **Tasks 명시** | PR에 포함된 모든 작업을 bullet point로 나열 |
| **템플릿 준수** | 템플릿 구조를 임의로 변경하지 않음 |

### 4. Development Principles

* **Rich Domain**: 비즈니스 로직은 Service가 아닌 **Entity 내부**에 캡슐화.
  ```kotlin
  // ✅ 올바름: Entity 내부에 로직
  player.joinTeam(team)

  // ❌ 잘못됨: Service에서 처리
  playerService.assignTeam(player, team)
  ```
* **Auditing**: 모든 JPA Entity는 **`BaseTimeEntity`** 상속 필수.
* **Immutability**: `val`과 `data class` 우선 사용.
* **SRP**: 서비스가 거대해지면 기능별 소규모 컴포넌트로 분리.

### 5. JPA Convention

> ⚠️ **@OneToMany 사용 금지**: 성능 및 설계 문제로 `@OneToMany` 양방향 매핑을 사용하지 않습니다.

| 규칙 | 설명 |
|------|------|
| **ManyToOne만 사용** | 자식 엔티티에서 부모로의 단방향 참조만 허용 |
| **컬렉션 조회는 Repository** | 부모→자식 조회가 필요하면 Repository 메서드 사용 |
| **Lazy Loading 기본** | `@ManyToOne(fetch = FetchType.LAZY)` 필수 |
| **N+1 방지** | 필요 시 `@EntityGraph` 또는 `fetch join` 사용 |

```kotlin
// ❌ 잘못됨: OneToMany 양방향 매핑
@Entity
class Association {
    @OneToMany(mappedBy = "association")
    val leagues: MutableList<League> = mutableListOf()
}

// ✅ 올바름: ManyToOne 단방향만 사용
@Entity
class League(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "association_id")
    val association: Association
)

// ✅ 올바름: 컬렉션 조회는 Repository에서
interface LeagueRepository {
    fun findByAssociationId(associationId: Long): List<League>
}
```

---

## 📂 Output Folder Structure

```
outputs/
├── briefs/
│   └── brief.md                # 구현 설계도
├── reports/
│   ├── review-report.md        # 헌법/빌드 검수 보고서
│   ├── domain-check.md         # 야구 규칙 검증 보고서
│   ├── security-audit.md       # 보안 감사 보고서
│   └── rework-order.md         # 재작업 지시서
├── quality/
│   ├── ktlint-report.json      # 코드 스타일 검사 결과
│   ├── detekt-report.json      # 정적 분석 결과
│   └── quality-report.json     # 통합 품질 리포트
├── docs/
│   ├── api-spec.md             # API 명세
│   └── adr/                    # 기술 의사결정 기록
└── build/
    └── test-summary.log        # 테스트 결과 요약
```

---

## ❗ Documentation Integrity (Mandatory)

> **Self-Update Rule**: 구조 또는 규칙 변경 시, **반드시 `CLAUDE.md`를 먼저 갱신**해야 합니다. 문서와 코드는 항상 동기화 상태를 유지해야 합니다.

---

## 📜 Change History

| 날짜 | 변경 내용 | 담당 |
|------|-----------|------|
| 2026-01-22 | Pull Request Convention 추가 (템플릿 준수 규칙) | knowledge-manager |
| 2026-01-21 | JPA Convention 추가 (@OneToMany 금지, ManyToOne 단방향만 사용) | knowledge-manager |
| 2026-01-21 | MCP GitHub 연동 추가, Tech Stack 버전 안정화 (Kotlin 2.1.10, Spring Boot 3.4.1, Gradle 8.12) | knowledge-manager |
| 2025-01-21 | `security-auditor` Agent 추가 (OWASP Top 10 기반) | knowledge-manager |
| 2025-01-21 | `code-quality` Skill 추가 (ktlint, detekt) | knowledge-manager |
| 2025-01-21 | Reviewer VETO 항목 확장 (보안, 코드품질) | knowledge-manager |
| 2025-01-21 | Architecture Philosophy (DDD, Hexagonal) 추가 | knowledge-manager |
| 2025-01-21 | Communication & Error Standards 추가 | knowledge-manager |
| 2025-01-21 | Reviewer VETO 항목 확장 (Entity Leak, ApiResponse, CustomException) | knowledge-manager |
| 2025-01-20 | Agent/Skill 아키텍처 추가, 워크플로우 정의 | knowledge-manager |
| 2025-01-20 | 초기 문서 생성 | - |
