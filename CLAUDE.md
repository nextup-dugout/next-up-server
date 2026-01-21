# ⚾️ CLAUDE.md (NEXT-UP)

> **이 문서는 NEXT-UP 프로젝트의 최상위 헌법입니다.** 모든 에이전트는 이 문서를 최우선으로 준수합니다.
> 상세 구현 가이드는 각 Agent/Skill 문서를 참조하세요.

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
nextup-api → nextup-infrastructure → nextup-core → nextup-common
```

| 모듈 | 허용된 의존성 | 금지 |
|------|---------------|------|
| `nextup-api` | `infra`, `core`, `common` | - |
| `nextup-core` | `common` **ONLY** | infra, api 절대 금지 |
| `nextup-infrastructure` | `core`, `common` | api 금지 |
| `nextup-common` | **NONE** (리프 모듈) | 모든 의존성 금지 |

> ⚠️ **순환 참조 절대 금지** / **Common에 비즈니스 로직 금지**

---

## 🛠️ Tech Stack

| 항목 | 버전 |
|------|------|
| **Language** | Kotlin 2.1.10 (JDK 21) |
| **Framework** | Spring Boot 3.4.1 |
| **Database** | PostgreSQL + PostGIS |
| **Build Tool** | Gradle 8.12 (Kotlin DSL) |
| **GitHub** | MCP (Model Context Protocol) |

---

## 🤖 Agent & Skill 분리 원칙

> **판단(Agent)과 실행(Skill)의 분리**를 엄격히 준수합니다.

### Council Team (전략/거버넌스)
`planner`, `tech-lead`, `reviewer`, `risk-manager`, `baseball-expert`, `security-auditor`

### Execution Team (개발 실행)
`modeler`, `logic-broker`, `api-specialist`, `data-transformer`, `scenario-tester`, `github-manager`, `knowledge-manager`

### Skills (재사용 가능 도구)
`build-validator`, `git-toolkit`, `db-manager`, `code-quality`

> 상세 역할 및 가이드는 `.claude/agents/`, `.claude/skills/` 참조

---

## ⚠️ Reviewer VETO 권한 (절대적)

**`reviewer` 에이전트는 다음 조건에서 무조건적 승인 거부 권한을 행사합니다:**

| # | VETO 조건 | 심각도 |
|---|-----------|--------|
| 1 | CLAUDE.md 의존성 규칙 위반 | 🔴 즉시 REJECT |
| 2 | `./gradlew build` 실패 | 🔴 즉시 REJECT |
| 3 | 테스트 실패 | 🔴 즉시 REJECT |
| 4 | `security-auditor` Critical/High 취약점 | 🔴 즉시 REJECT |
| 5 | Entity 직접 노출 (Zero Entity Leak 위반) | 🔴 즉시 REJECT |
| 6 | `baseball-expert` REJECT 판정 | 🟠 해당 판정 존중 |
| 7 | ApiResponse 미사용 | 🟠 REJECT |
| 8 | CustomException 미사용 | 🟠 REJECT |
| 9 | 커밋/PR 컨벤션 위반 | 🟠 REJECT |
| 10 | `code-quality` detekt bugs 발견 | 🟠 REJECT |

**거부권은 절대적이며, 다른 에이전트가 무효화할 수 없습니다.**

> 상세 검증 항목은 `.claude/agents/reviewer.md` 참조

---

## 📂 Output Structure

```
outputs/
├── briefs/          # 구현 설계도
├── reports/         # 검수/감사 보고서
├── quality/         # 정적 분석 결과
├── docs/            # API 명세, ADR
└── build/           # 테스트 결과
```

---

## ❗ Documentation Integrity

> **Self-Update Rule**: 구조 또는 규칙 변경 시, **반드시 `CLAUDE.md`를 먼저 갱신**해야 합니다.

---

## 📜 Change History

| 날짜 | 변경 내용 |
|------|-----------|
| 2026-01-22 | 헌법 리팩토링 - 상세 가이드 Agent/Skill로 분리 |
| 2026-01-22 | PR Convention 추가 |
| 2026-01-21 | JPA Convention, MCP 연동 추가 |
| 2025-01-21 | Architecture Philosophy, Error Standards 추가 |
| 2025-01-20 | Agent/Skill 아키텍처, 초기 문서 생성 |

---

## 📚 상세 가이드 참조

| 주제 | 참조 문서 |
|------|-----------|
| API/DTO/에러 처리 | `.claude/agents/api-specialist.md` |
| JPA/Entity 규칙 | `.claude/agents/modeler.md` |
| Git/PR 컨벤션 | `.claude/skills/git-toolkit/SKILL.md` |
| Reviewer 검증 상세 | `.claude/agents/reviewer.md` |
| 보안 감사 | `.claude/agents/security-auditor.md` |
| 야구 규칙 | `.claude/agents/baseball-expert.md` |
