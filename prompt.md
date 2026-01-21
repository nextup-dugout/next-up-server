# # NEXT-UP 백엔드 개발 팀 자동화 시스템 구축

사회인 야구 기록 서비스 'NEXT-UP'의 백엔드 개발 및 운영 전 과정을 자동화하라. 모든 에이전트는 프로젝트 루트의 `CLAUDE.md`를 최상위 헌법으로 간주하며, **판단(Agent)과 실행(Skill)의 분리 원칙**을 엄격히 준수한다.

## 프로젝트 구조

```
.claude/
├── agents/
│   │ # 0. 가버넌스 및 전략 팀 (Council - Sonnet)
│   ├── planner.md                   # 기획 에이전트 (Brief 작성)
│   ├── tech-lead.md                 # 기술 리드 (ADR/기술 스택 결정)
│   ├── risk-manager.md              # 리스크 관리자 (실패 대응 루프)
│   ├── reviewer.md                  # 검수 에이전트 (거부권/헌법 검사)
│   ├── baseball-expert.md           # 야구 전문가 (PDF 규칙 팩트 체크)
│   │
│   │ # 1. 개발 및 테스트 팀 (Execution - Haiku/Sonnet)
│   ├── modeler.md                   # Core/Entity 개발
│   ├── logic-broker.md              # Infra/Service 구현
│   ├── api-specialist.md            # API/Controller 설계
│   ├── data-transformer.md          # DTO/Mapper 전담
│   ├── scenario-tester.md           # 시나리오/통합 테스트
│   ├── github-manager.md            # 이슈/브랜치/PR 관리
│   └── knowledge-manager.md         # CLAUDE.md 최신화
│
└── skills/
    │ # 2. 빌드 및 품질 팀
    ├── build-validator/
    │   ├── SKILL.md
    │   └── scripts/
    │       ├── validate_build.sh     # ./gradlew build 실행
    │       └── test_analyzer.py      # 테스트 결과 분석
    │
    │ # 3. 인프라 및 자동화 팀
    └── git-toolkit/
    │   ├── SKILL.md
    │   ├── scripts/
    │   │   ├── gh_issue_creator.sh   # 이슈 생성 래퍼
    │   │   └── gh_pr_manager.py      # PR 생성 및 레이블링
    │
    └── db-manager/
        ├── SKILL.md
        ├── scripts/
        │   ├── check_postgis.py      # GIS 쿼리 검증
        │   └── generate_ddl.sh       # Flyway 마이그레이션 생성
        └── references/
            └── postgis-spec.md

```

---

## 워크플로우

```
[요구사항 입력]
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ 1. 기획 및 준비 (planner, tech-lead, github-manager)    │
│    - 요구사항 분석 및 구현 브리프(brief.md) 작성         │
│    - 기술 스택 결정(ADR) 및 거절/수락 판단               │
│    - GitHub 이슈 생성 및 컨벤션 브랜치 생성              │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ 2. 병렬 구현 및 검증 (Dev Team & baseball-expert)       │
│                                                         │
│   [개발 팀 - Subagents]        [품질 팀 - Skill]         │
│   ├── modeler/broker/api      ├── 빌드/테스트 자동 검증 │
│   ├── scenario-tester         ├── DB/GIS 쿼리 유효성 검사 │
│   ├── data-transformer ───────┤ (baseball-expert)       │
│   └── github-manager (커밋)    └── 도메인/규칙 팩트 체크 │
└─────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────┐
│ 3. reviewer & risk-manager (검수 및 대응)               │
│    - CLAUDE.md 헌법 및 기술 결정 준수 여부 판정          │
│    - 빌드 실패 시 risk-manager가 재작업 루프 결정        │
│    - 승인 시 github-manager가 최종 PR 생성               │
└─────────────────────────────────────────────────────────┘
    │
    ▼
[outputs/ 및 GitHub에 최종 결과물 아카이빙]

```

---

## 에이전트 설계 원칙

[클로드 코드 sub-agents 가이드](https://code.claude.com/docs/ko/sub-agents)를 준수하며, 에이전트(판단)와 스킬(실행)의 경계를 엄격히 분리한다.

각 subagent(.claude/agents/*.md)는 다음 형식을 따라:

```yaml
---
name: [agent-name]
description: |
  [구체적인 역할 설명]
  [언제 사용되는지 - "MUST BE USED" 또는 "USE PROACTIVELY" 포함]
tools: [필요한 도구들]
model: sonnet (Council) 또는 haiku (Execution)
---

[시스템 프롬프트]
- 역할 정의 (Agent vs Skill 경계 준수)
- 작업 프로세스
- 출력 포맷

```

---

## 각 에이전트 상세 설계

### 0. 오케스트레이터 및 전략 팀 (Council - Sonnet)

#### planner.md (기획 에이전트)

* **역할**: 기능 구현 총괄 설계 및 `brief.md` 작성
* **tools**: Read, Write, Bash, Glob
* **주요 기능**:
  - 요구사항을 분석하여 `core`, `infra`, `api` 모듈별 수정 사항 도출
  - 기술적으로 불가능하거나 비용 과다 시 `tech-lead`와 협의하여 반려
* **출력**: 구현 브리프 (outputs/briefs/brief.md)

#### tech-lead.md (기술 리드)

* **역할**: 기술 스택 선정 및 아키텍처 의사결정 (ADR)
* **tools**: Read, Write, Search
* **주요 기능**:
  - JPA vs QueryDSL, SSE vs WebSocket 등 기술적 갈림길 결론 도출
  - 신규 라이브러리 도입 타당성 검토 및 ADR 문서 작성
* **출력**: 기술 결정 리포트 (docs/adr/ADR-XXX.md)

#### reviewer.md (검수 에이전트)

* **역할**: CLAUDE.md 헌법 수호 및 최종 승인 거부권 행사
* **tools**: Read, Bash, Glob
* **주요 기능**:
  - `CLAUDE.md`의 의존성 규칙 위반 시 무조건적 승인 거부
  - `build-validator` 스킬 결과에 따른 물리적 빌드 상태 판정
* **출력**: 검수 리포트 (outputs/reports/review-report.md)

#### risk-manager.md (리스크 관리자)

* **역할**: 실패 원인 분석 및 재작업 루프 지정
* **tools**: Read, Bash
* **주요 기능**: 빌드/테스트 실패 시 원인을 분류(설계/구현/규칙)하여 `planner` 또는 개발팀으로 반려

### 1. 개발 및 테스트 실행 팀 (Execution - Haiku/Sonnet)

#### baseball-expert.md (도메인 전문가)

* **역할**: PDF 규칙집 기반 야구 로직 팩트 체크
* **tools**: Read, Glob
* **스펙**: `.claude/knowledge/rules/`의 PDF 조항 번호를 근거로 로직 승인/거절
* **출력**: [PASS/REJECT] 판정 보고서

#### github-manager.md (형상 관리자)

* **역할**: `git-toolkit`을 활용한 GitHub 생명주기 자동화
* **tools**: Bash, Read, Write
* **스펙**: 이슈 생성 → 브랜치 생성 → 커밋 컨벤션 관리 → PR 생성
* **출력**: GitHub Issue/PR

---

## Skill 설계

[클로드 코드 skills 가이드](https://code.claude.com/docs/ko/skills)를 준수하며, 실제 실행 가능한 스크립트를 포함한다.

### 2. 빌드 품질 팀 (build-validator)

* **SKILL.md**: Gradle 빌드 및 JUnit 테스트 결과 자동 분석
* **워크플로우**: `./gradlew build` 실행 후 실패 시 로그를 `risk-manager`에게 전달

### 3. 인프라 및 자동화 팀 (git-toolkit, db-manager)

* **git-toolkit**: `gh` CLI를 사용하여 물리적인 GitHub 조작 수행
* **db-manager**: PostGIS 쿼리 유효성 체크 및 Flyway 마이그레이션 DDL 자동 생성

---

## 코딩 스타일 가이드 (프로젝트 헌법)

### 아키텍처 원칙

* **Multi-Module**: api → infra → core → common 의존성 방향 엄수
* **Purity**: `core` 모듈은 순수 Kotlin 로직만 포함 (프레임워크 종속성 금지)
* **Rich Domain**: 비즈니스 로직은 Service가 아닌 Entity 내부에 캡슐화

### 한국어 및 소통 스타일

* **톤앤매너**: 전문적인 엔지니어 톤, AI틱한 수식어 제거
* **정확성**: 팩트와 수치, 규칙 조항 번호 중심의 소통
* **인수인계**: 모든 결정 사유는 ADR 및 `CLAUDE.md` History에 기록

---

## 출력 폴더 구조

```
outputs/
├── briefs/
│   └── brief.md                # 구현 설계도
├── reports/
│   ├── review-report.md         # 헌법/빌드 검수 보고서
│   └── domain-check.md         # 야구 규칙 검증 보고서
├── docs/
│   ├── api-spec.md             # API 명세
│   └── adr/                    # 기술 의사결정 기록
└── build/
    └── test-summary.log        # 테스트 결과 요약

```

---

## 요청사항

1. 위 구조와 워크플로우에 따라 `.claude/` 내 에이전트/스킬 파일의 **운영 가능한 초안 템플릿을 생성하라.**
2. **Council 에이전트**(planner, tech-lead, risk-manager, reviewer, baseball-expert)는 반드시 **sonnet**을 사용하도록 명시하라.
3. **Reviewer**는 `CLAUDE.md` 위반 시 PR 승인을 거부할 수 있는 절대적 권한을 시스템 프롬프트에 명문화하라.
4. **Agent vs Skill 분리**: 모든 에이전트가 물리적 조작 시 반드시 해당 Skill을 호출하도록 설계하라.
5. 모든 내용은 전문적인 한국어로 상세히 작성하며, **이 형식을 반드시 지켜라.**

---
