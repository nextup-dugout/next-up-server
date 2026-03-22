# ⚾️ CLAUDE.md (NEXT-UP)

> **이 문서는 NEXT-UP 프로젝트의 최상위 헌법입니다.** 모든 에이전트는 이 문서를 최우선으로 준수합니다.
> 상세 구현 가이드는 각 Agent/Skill/Rules 문서를 참조하세요.

---

## 📝 Project Status & Goal

| 항목 | 내용 |
|------|------|
| **Status** | Initial Planning & Infrastructure Setup (Phase 1) |
| **Project** | 사회인 야구 기록 서비스 'NEXT-UP' 백엔드 개발 |
| **Goal** | 복잡한 야구 규칙(DH 규칙 해제 등)을 player-centric 데이터 모델로 처리 |

---

## 🏛️ Architecture Principles (불변)

### 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **Domain-Driven Design** | 모든 비즈니스 결정은 `nextup-core` 도메인 모델 중심 |
| **Hexagonal Architecture** | Core(Inside) = 비즈니스 로직, Infra/API(Outside) = 어댑터 |
| **Rich Domain Model** | 비즈니스 로직은 Service가 아닌 **Entity 내부**에 캡슐화 |

### 의존성 방향 (절대 규칙)

```
Outside → Inside (항상 이 방향만 허용)
Core가 Infra를 알면 절대 안 됨
```

---

## 🏗️ Multi-Module Dependency Rules (불변)

```
nextup-api        ─┐
nextup-backoffice ─┼→ nextup-infrastructure → nextup-core → nextup-common
nextup-scorer     ─┘
```

### 모듈별 역할

| 모듈 | 역할 | 포트 |
|------|------|------|
| `nextup-api` | 일반 사용자용 공개 API (조회 위주) | 8080 |
| `nextup-backoffice` | 관리자 CRUD (협회/리그/팀/시스템 관리) | 8081 |
| `nextup-scorer` | 실시간 경기 기록 (기록원 전용, WebSocket) | 8082 |
| `nextup-infrastructure` | Repository, 외부 연동, 공통 Security | - |
| `nextup-core` | 도메인 엔티티, 비즈니스 로직 | - |
| `nextup-common` | 공통 유틸리티, Exception | - |

### 의존성 매트릭스

| 모듈 | 허용된 의존성 | 금지 |
|------|---------------|------|
| `nextup-api` | `infra`, `core`, `common` | backoffice, scorer |
| `nextup-backoffice` | `infra`, `core`, `common` | api, scorer |
| `nextup-scorer` | `infra`, `core`, `common` | api, backoffice |
| `nextup-infrastructure` | `core`, `common` | api, backoffice, scorer |
| `nextup-core` | `common` **ONLY** | infra, api, backoffice, scorer 절대 금지 |
| `nextup-common` | **NONE** (리프 모듈) | 모든 의존성 금지 |

> ⚠️ **순환 참조 절대 금지** / **Common에 비즈니스 로직 금지**
> ⚠️ **API 계층 모듈(api, backoffice, scorer)간 상호 의존 금지**

---

## 🛠️ Tech Stack

| 항목 | 버전 |
|------|------|
| **Language** | Kotlin 2.1.10 (JDK 21) |
| **Framework** | Spring Boot 3.4.1 |
| **Database** | PostgreSQL + PostGIS |
| **Build Tool** | Gradle 8.12 (Kotlin DSL) |
| **Coverage** | Jacoco 0.8.12 + Codecov |
| **GitHub** | MCP (Model Context Protocol) |

---

## 🤖 Agent & Skill 구조

> **판단(Agent)과 실행(Skill)의 분리**를 엄격히 준수합니다.

### Agents (5개)

| Agent | 역할 |
|-------|------|
| `planner` | 기능 구현 계획 수립, 모듈별 수정 사항 도출 |
| `architect` | 멀티모듈 설계, Entity/Repository 구현 |
| `implementer` | Controller/Service/DTO 전체 코드 작성 |
| `reviewer` | VETO 권한, 빌드/테스트/보안 검증 |
| `devops` | GitHub PR/Issue 관리, 문서 유지보수 |

### Skills (15개)

| Skill | 역할 |
|-------|------|
| `batch-issues` | 열린 이슈 일괄 수집 → 의존관계 분석 → Wave별 병렬 issue-progress 실행 |
| `issue-progress` | 이슈 번호 기반 전체 구현 파이프라인 자동 실행 (스킬 매핑 포함) |
| `domain-baseball` | 야구 규칙 검증 체크리스트 |
| `backend-patterns` | Kotlin/Spring Boot 컨벤션 |
| `git-workflow` | GitHub 자동화, CI/CD 연동 |
| `quality-metrics` | 빌드/테스트/커버리지/정적분석 |
| `security-audit` | OWASP Top 10 보안 체크리스트 |
| `db-manager` | PostgreSQL/PostGIS 쿼리 |
| `pre-pr-gate` | PR 생성 전 필수 품질 게이트 (ktlint→빌드→verify→커버리지, 전체 PASS 필수) |
| `verify-implementation` | 모든 verify 스킬을 순차 실행하여 통합 검증 보고서 생성 |
| `manage-skills` | 세션 변경사항 분석하여 검증 스킬 생성/업데이트 및 CLAUDE.md 관리 |
| `verify-entity-leak` | Controller 반환 타입에 Entity 직접 노출 방지 검증 |
| `verify-dependency` | 멀티모듈 의존성 방향 규칙 검증 |
| `verify-api-response` | Controller의 ApiResponse 래핑 사용 검증 |
| `verify-custom-exception` | BusinessException 계열 커스텀 예외 사용 검증 |
| `verify-url-prefix` | 모듈별 URL 프리픽스 통일 검증 |
| `verify-authorization` | mutating 엔드포인트 @PreAuthorize/@AuthenticationPrincipal 적용 검증 |
| `verify-repository-injection` | Controller에서 Repository/RepositoryPort 직접 주입 금지 검증 |

### Rules (절대 규칙)

| Rule | 내용 |
|------|------|
| `dependency` | 멀티모듈 의존성 방향 규칙 |
| `security` | Zero Entity Leak, OWASP 보안 규칙 |
| `tdd` | Core/Service 계층 TDD 필수, 80% 커버리지 |
| `authorization` | mutating 엔드포인트 @PreAuthorize 필수, identity는 @AuthenticationPrincipal에서 도출 |
| `repository-injection` | Controller에서 Repository/RepositoryPort 직접 주입 금지, 반드시 Service 경유 |

### Commands (슬래시 명령어)

| Command | 기능 |
|---------|------|
| `/batch-issues` | 열린 이슈 일괄 분석 → Wave별 병렬 구현 실행 |
| `/issue-progress` | 이슈 번호 기반 전체 구현 파이프라인 실행 |
| `/tdd` | TDD 워크플로우 활성화 |
| `/build` | Gradle 빌드 & 테스트 실행 |
| `/review` | 코드 품질 & 보안 검증 |
| `/pr` | GitHub PR 생성 |
| `/verify-implementation` | 등록된 verify 스킬 통합 실행 |
| `/manage-skills` | 세션 변경사항 기반 스킬 유지보수 |

---

## 🔄 Agent 협업 워크플로우 (필수)

### 기능 구현 흐름

```
1. planner     → 요구사항 분석, 구현 계획 수립 → GitHub Issue 생성
       ↓
2. devops      → 브랜치 생성
   architect   → Entity/Repository 설계 및 구조 생성
       ↓
3. implementer → Controller/Service/DTO 구현
       ↓
4. reviewer    → 검증 (다른 agent들과 대화하며 확인)
       ↓
5. APPROVED 후 → PR 템플릿대로 PR 작성 및 머지
```

### reviewer의 협업 검증 (필수)

**reviewer는 반드시 다른 agent들과 대화하며 검증합니다:**

| 대상 | 질문 예시 |
|------|----------|
| **planner** | "이 구현이 계획대로 된 건가요?" |
| **architect** | "이 구조가 설계 의도와 맞나요?" |
| **implementer** | "왜 이렇게 구현했나요?" |

### Agent 사용 시점

| 상황 | 사용 Agent |
|------|-----------|
| 새 기능 요청 | `planner` (이슈 생성) → `architect` → `implementer` |
| 설계/구조 생성 | `architect` 또는 `devops` |
| 버그 수정 | `implementer` |
| 구현 완료 후 | `reviewer` (필수, 승인 전까지) |
| reviewer 승인 후 | PR 템플릿대로 PR 작성 |

---

## ⚠️ Reviewer VETO 권한 (절대적)

**`reviewer` 에이전트는 다음 조건에서 무조건적 승인 거부 권한을 행사합니다:**

| # | VETO 조건 | 심각도 |
|---|-----------|--------|
| 1 | CLAUDE.md 의존성 규칙 위반 | 🔴 즉시 REJECT |
| 2 | `./gradlew build` 실패 | 🔴 즉시 REJECT |
| 3 | 테스트 실패 | 🔴 즉시 REJECT |
| 4 | 보안 취약점 (CRITICAL/HIGH) | 🔴 즉시 REJECT |
| 5 | Entity 직접 노출 (Zero Entity Leak 위반) | 🔴 즉시 REJECT |
| 6 | 야구 규칙 위반 | 🟠 REJECT |
| 7 | ApiResponse 미사용 | 🟠 REJECT |
| 8 | CustomException 미사용 | 🟠 REJECT |
| 9 | 커밋/PR 컨벤션 위반 | 🟠 REJECT |
| 10 | detekt bugs 발견 | 🟡 SKIP (Kotlin 2.1.x 미지원으로 비활성화) |
| 11 | 커버리지 미달 (Jacoco 80% 또는 Codecov 85%) | 🟠 REJECT |

**거부권은 절대적이며, 다른 에이전트가 무효화할 수 없습니다.**

---

## 📂 Output Structure & Transparency Rules

### 디렉토리 구조

```
outputs/
├── summary.md           # 복잡한 작업 협업 요약 (자동 갱신)
├── briefs/              # 구현 설계도 (planner)
├── reports/             # 검수/감사 보고서 (reviewer)
├── quality/             # 정적 분석 결과
├── docs/                # API 명세, ADR (architect)
├── build/               # 빌드/테스트 결과
└── agents-trace/        # 상세 추적 로그 (명시 요청 시)
```

### 자동 생성 조건

| 상황 | 생성 위치 | 내용 |
|------|-----------|------|
| 에이전트 3개 이상 협업 | `outputs/summary.md` | 협업 과정 요약 |
| reviewer REJECT | `outputs/reports/review-reject-{date}.md` | 거부 사유 및 판정 |
| 빌드/테스트 실패 | `outputs/build/failure-{date}.log` | 실패 원인 분석 |
| 사용자 명시 요청 | `outputs/agents-trace/` | 상세 추적 로그 |

---

## ❗ Documentation Integrity

> **Self-Update Rule**: 구조 또는 규칙 변경 시, **반드시 `CLAUDE.md`를 먼저 갱신**해야 합니다.

---

## 📜 Change History

| 날짜 | 변경 내용 |
|------|-----------|
| 2026-02-11 | .claude/ 설정 리라이트: 스펙 활용도 35%→100%, Agent Memory 전체 적용, MCP 3서버, Hooks 4개, 커버리지 다층 기준 문서화 |
| 2026-02-03 | Agent 협업 워크플로우 추가, Skill 내용 Agent에 merge |
| 2026-02-01 | 모듈 구조 확장 (4개→6개): backoffice, scorer 모듈 추가 |
| 2026-01-23 | Agent/Skill 구조 개선 (13개→5개 Agent, 4개→6개 Skill) |
| 2026-01-23 | Rules, Commands 추가 |
| 2026-01-23 | Jacoco + Codecov 설정 추가 |
| 2026-01-23 | Output Structure 투명성 규칙 추가 |
| 2026-01-22 | 헌법 리팩토링 - 상세 가이드 Agent/Skill로 분리 |
| 2026-01-22 | PR Convention 추가 |
| 2026-01-21 | JPA Convention, MCP 연동 추가 |

---

## 📚 상세 가이드 참조

| 주제 | 참조 문서 |
|------|-----------|
| 구현 계획 수립 | `.claude/agents/planner.md` |
| Entity/Repository 설계 | `.claude/agents/architect.md` |
| Controller/DTO 구현 | `.claude/agents/implementer.md` |
| 코드 검수 | `.claude/agents/reviewer.md` |
| GitHub 관리 | `.claude/agents/devops.md` |
| 야구 규칙 | `.claude/skills/domain-baseball/SKILL.md` |
| Kotlin/Spring 패턴 | `.claude/skills/backend-patterns/SKILL.md` |
| Git/CI/CD | `.claude/skills/git-workflow/SKILL.md` |
| 품질/커버리지 | `.claude/skills/quality-metrics/SKILL.md` |
| 보안 체크 | `.claude/skills/security-audit/SKILL.md` |
| 의존성 규칙 | `.claude/rules/dependency.md` |
| 보안 규칙 | `.claude/rules/security.md` |
| TDD 규칙 | `.claude/rules/tdd.md` |
