---
paths:
  - "nextup-infrastructure/**/*.kt"
---

# Infrastructure Layer Rules

> nextup-infrastructure 모듈 전용 규칙. Hexagonal Architecture의 **Outbound Adapter** 계층.
> Core의 Port(인터페이스)를 구현하는 Repository, ServiceImpl, 외부 연동, 공통 Security.

## 의존성 방향
- `nextup-core`와 `nextup-common`만 의존 허용
- `nextup-api`, `nextup-backoffice`, `nextup-scorer` 의존 금지

## Service 구현
- Core 모듈의 `*Service` 인터페이스를 `*ServiceImpl`로 구현
- `@Service`, `@Transactional` 적용
- 위치: `infrastructure/service/{domain}/` 패키지

## Repository 구현 (2가지 전략 혼용)

### 전략 1: Direct JPA + Port (대부분)
- `JpaRepository` + `*RepositoryPort` 동시 확장
- 위치: `infrastructure/repository/`
- 예시: `TeamRepository extends JpaRepository<Team, Long>, TeamRepositoryPort`

### 전략 2: Adapter wrapping (복잡한 쿼리)
- 내부 `*JpaRepository` + 외부 `*RepositoryAdapter`
- 위치: `infrastructure/persistence/{domain}/`
- 예시: `BracketEntryJpaRepository` + `BracketEntryRepositoryAdapter`

### 공통 규칙
- Native Query 사용 시 반드시 parameterized query

## JPA 컨벤션
- `@Enumerated(EnumType.STRING)` 필수 (ORDINAL 금지)
- Lazy Loading 기본, 필요시만 Fetch Join
- N+1 문제 주의 - `@EntityGraph` 또는 JPQL `JOIN FETCH` 활용

## Security 컴포넌트 (`infrastructure/security/`)
- **JWT**: `JwtTokenProvider`, `JwtAuthenticationFilter`, `JwtProperties`
- **OAuth2**: `CustomOAuth2UserService` (Kakao, Google, Naver)
- **Rate Limiting**: `RateLimitFilter` (Bucket4j + Caffeine)
- **UserDetails**: `CustomUserDetailsService`, `CustomUserDetails`
- **Handler**: `CustomAccessDeniedHandler`, `CustomAuthenticationEntryPoint`
- **Auth**: `AuthenticationService`

## Security Filter Chain
```
Request → RateLimitFilter → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → ...
```
