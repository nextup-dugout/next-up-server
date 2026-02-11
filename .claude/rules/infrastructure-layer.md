---
paths:
  - "nextup-infrastructure/**/*.kt"
---

# Infrastructure Layer Rules

> nextup-infrastructure 모듈 전용 규칙. Repository 구현, 외부 연동, 공통 Security.

## 의존성 방향
- `nextup-core`와 `nextup-common`만 의존 허용
- `nextup-api`, `nextup-backoffice`, `nextup-scorer` 의존 금지

## Repository 구현
- `*RepositoryPort` 인터페이스의 구현체 위치
- `JpaRepository` 확장으로 기본 CRUD 제공
- 복잡한 쿼리는 QueryDSL 사용 (`*RepositoryCustom` + `*RepositoryImpl`)
- Native Query 사용 시 반드시 parameterized query

## JPA 컨벤션
- `@Enumerated(EnumType.STRING)` 필수 (ORDINAL 금지)
- Lazy Loading 기본, 필요시만 Fetch Join
- N+1 문제 주의 - QueryDSL fetchJoin() 활용

## Security 설정
- 각 API 모듈(api, backoffice, scorer)의 공통 Security 컴포넌트 제공
- JWT 인증/인가 처리
