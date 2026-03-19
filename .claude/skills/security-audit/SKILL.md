---
name: security-audit
description: |
  OWASP Top 10 기반 보안 취약점 검사 및 Zero Entity Leak 검증 체크리스트.
  보안 심각도(CRITICAL/HIGH/MEDIUM/LOW) 분류 및 대응 가이드를 제공한다.
user-invocable: false
context: fork
allowed-tools: Read, Glob, Grep
---

# Security Audit - OWASP & Code Security

> OWASP Top 10 기반 보안 취약점 검사 및 코드 보안 체크리스트

## 개요

이 스킬은 보안 취약점 검사를 수행합니다. OWASP Top 10을 기반으로 코드 리뷰 시 보안 이슈를 식별합니다.

## 보안 심각도 분류

| 등급 | 설명 | 대응 |
|------|------|------|
| **CRITICAL** | 즉시 악용 가능, 데이터 유출 | 즉시 수정, PR REJECT |
| **HIGH** | 악용 가능, 시스템 침해 | PR 전 수정 필수 |
| **MEDIUM** | 조건부 악용 가능 | 수정 권장 |
| **LOW** | 잠재적 위험 | 인지 후 개선 |

## OWASP Top 10 체크리스트

### A01: Broken Access Control

**검사 항목:**
- [ ] 모든 엔드포인트에 인증 필요 여부 확인
- [ ] `@PreAuthorize` 어노테이션 적용 확인
- [ ] 리소스 소유권 검증 (타인 데이터 접근 차단)
- [ ] 관리자 기능 권한 분리

**취약 코드:**
```kotlin
// ❌ CRITICAL: 인증 없이 데이터 접근
@GetMapping("/players/{id}")
fun getPlayer(@PathVariable id: Long) = playerService.findById(id)

// ✅ APPROVED: 인증 및 권한 검증
@GetMapping("/players/{id}")
@PreAuthorize("hasRole('USER')")
fun getPlayer(@PathVariable id: Long, @AuthenticationPrincipal user: User) {
    playerService.findByIdAndUserId(id, user.id)
}
```

### A02: Cryptographic Failures

**검사 항목:**
- [ ] 비밀번호 평문 저장 금지 (BCrypt 사용)
- [ ] HTTPS 강제 설정
- [ ] 민감 데이터 로깅 금지
- [ ] API 키/토큰 환경변수 사용

### A03: Injection

**검사 항목:**
- [ ] JPA/QueryDSL 파라미터 바인딩 사용
- [ ] Native Query 사용 시 parameterized query
- [ ] 사용자 입력 직접 쿼리 삽입 금지

**취약 코드:**
```kotlin
// ❌ CRITICAL: SQL Injection
@Query("SELECT * FROM players WHERE name = '$name'")
fun findByName(name: String): Player

// ✅ APPROVED: Parameterized Query
@Query("SELECT p FROM Player p WHERE p.name = :name")
fun findByName(@Param("name") name: String): Player
```

### A04: Insecure Design

**검사 항목:**
- [ ] Zero Entity Leak 준수
- [ ] 민감 데이터 응답에서 제외
- [ ] 비즈니스 로직 검증 (음수 값, 범위 초과 등)

### A05: Security Misconfiguration

**검사 항목:**
- [ ] 기본 계정/비밀번호 제거
- [ ] 디버그 모드 프로덕션 비활성화
- [ ] 불필요한 HTTP 메서드 차단
- [ ] 에러 메시지 상세 정보 숨김

### A06: Vulnerable Components

**검사 항목:**
- [ ] 의존성 보안 취약점 스캔
- [ ] 최신 버전 유지 (3개월 이내)
- [ ] 알려진 CVE 패치

### A07: Authentication Failures

**검사 항목:**
- [ ] JWT 시크릿 환경변수 사용
- [ ] 토큰 만료 시간 적절 설정
- [ ] Refresh Token 구현
- [ ] 로그인 실패 제한 (Rate Limiting)

**취약 코드:**
```kotlin
// ❌ CRITICAL: 하드코딩된 시크릿
val secret = "my-secret-key-12345"

// ✅ APPROVED: 환경변수
@Value("\${jwt.secret}")
private lateinit var secret: String
```

### A08: Data Integrity Failures

**검사 항목:**
- [ ] 외부 입력 검증 (@Valid, @NotNull)
- [ ] 직렬화 데이터 검증
- [ ] 파일 업로드 검증

### A09: Logging Failures

**검사 항목:**
- [ ] 인증 실패 로깅
- [ ] 민감 데이터 로깅 금지 (비밀번호, 토큰)
- [ ] 로그 인젝션 방지

### A10: SSRF (Server-Side Request Forgery)

**검사 항목:**
- [ ] 외부 URL 요청 시 화이트리스트 검증
- [ ] 내부 네트워크 접근 차단
- [ ] URL 파라미터 검증

## 프로젝트 보안 컴포넌트 (검증 대상)

### JWT 인증

| 컴포넌트 | 위치 | 검증 포인트 |
|----------|------|-------------|
| `JwtTokenProvider` | `infrastructure/security/jwt/` | 시크릿 환경변수, 토큰 만료시간 |
| `JwtAuthenticationFilter` | `infrastructure/security/jwt/` | 필터 체인 순서 |
| `JwtProperties` | `infrastructure/security/jwt/` | 설정값 검증 |

### OAuth2 소셜 로그인

| 컴포넌트 | 위치 | 검증 포인트 |
|----------|------|-------------|
| `CustomOAuth2UserService` | `infrastructure/security/oauth2/` | 사용자 정보 검증 |
| `OAuth2UserPrincipal` | `infrastructure/security/oauth2/` | Principal 래핑 |
| `OAuth2UserInfo` | `infrastructure/security/oauth2/` | Google, Kakao, Naver 프로바이더 |
| `OAuth2UserInfoFactory` | `infrastructure/security/oauth2/` | 팩토리 패턴 |
| `OAuth2AuthenticationSuccessHandler` | `infrastructure/security/handler/` | 성공 후 토큰 발급 |
| `OAuth2AuthenticationFailureHandler` | `infrastructure/security/handler/` | 실패 처리 |
| `AuthCodeStore` | `infrastructure/security/oauth2/` | 인가 코드 저장 |

### 접근 제어 및 필터

| 컴포넌트 | 위치 | 검증 포인트 |
|----------|------|-------------|
| `CustomUserDetails` | `infrastructure/security/` | UserDetails 래핑 |
| `CustomUserDetailsService` | `infrastructure/security/` | DB 조회 |
| `CustomAuthenticationEntryPoint` | `infrastructure/security/handler/` | 401 응답 형식 |
| `CustomAccessDeniedHandler` | `infrastructure/security/handler/` | 403 응답 형식 |
| `AuthenticationService` | `infrastructure/security/` | 인증 로직 통합 |
| `RateLimitFilter` | `infrastructure/security/filter/` | Bucket4j + Caffeine |

### 커스텀 보안 표현식

| 컴포넌트 | 위치 | 검증 포인트 |
|----------|------|-------------|
| `LineupSecurityExpression` | `infrastructure/security/` | 라인업 접근 권한 |
| `TeamSecurityExpression` | `infrastructure/security/` | 팀 관련 권한 |

### Security Filter Chain
```
Request → RateLimitFilter → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → ...
```

## 보안 이슈 발견 시 대응

### CRITICAL/HIGH 발견
1. **즉시 수정** — PR 진행 중단
2. **reviewer에게 보고** — VETO 발동
3. **수정 후 재검사** — 보안 확인 후 진행

### MEDIUM/LOW 발견
1. **이슈 생성** — 추후 수정 예정
2. **주석 추가** — 인지 표시
3. **다음 스프린트** — 개선 계획

## 보안 리포트 템플릿

```markdown
# Security Audit Report

## 검사 대상
- PR: #123
- 파일: 15개
- 검사일: 2026-01-23

## 발견 사항

### CRITICAL (0건)
없음

### HIGH (1건)
1. **A01: Broken Access Control**
   - 파일: PlayerController.kt:45
   - 내용: 인증 없이 플레이어 정보 접근
   - 수정: @PreAuthorize 추가 필요

## 결론
**REJECT** - HIGH 이슈 해결 후 재검토
```

## Agent 협업

이 Skill을 활용하는 Agent:
- **reviewer**: 보안 검사 후 CRITICAL/HIGH 시 VETO
- **implementer**: 구현 시 보안 체크리스트 참조
- **architect**: 설계 단계에서 보안 고려
