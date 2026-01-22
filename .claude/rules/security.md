# 보안 규칙 (Security Rules)

> **절대 규칙 - Reviewer VETO 대상**
> Critical/High 취약점 발견 시 무조건 REJECT

---

## 🛡️ Zero Entity Leak 원칙

**Entity는 절대 외부에 직접 노출하지 않습니다.**

### 규칙

```kotlin
// ❌ 절대 금지 - Entity 직접 반환
@GetMapping("/players/{id}")
fun getPlayer(@PathVariable id: Long): Player {
    return playerRepository.findById(id)  // VETO!
}

// ✅ 올바른 방법 - DTO 변환
@GetMapping("/players/{id}")
fun getPlayer(@PathVariable id: Long): ApiResponse<PlayerResponse> {
    val player = playerRepository.findById(id)
    return ApiResponse.success(player.toResponse())  // DTO 반환
}
```

### 이유

1. **보안**: Entity 내부 구조 노출 방지
2. **유연성**: API 스펙 변경 시 Entity 변경 불필요
3. **성능**: 필요한 필드만 전송 (lazy loading 문제 방지)

---

## 🔐 OWASP Top 10 준수

### 1. Injection 방지

```kotlin
// ❌ SQL Injection 위험
@Query("SELECT * FROM player WHERE name = '${name}'")  // VETO!

// ✅ Parameterized Query
@Query("SELECT p FROM Player p WHERE p.name = :name")
fun findByName(@Param("name") name: String): Player?
```

### 2. 인증/인가 (Authentication/Authorization)

```kotlin
// ❌ 권한 체크 없음
@DeleteMapping("/players/{id}")
fun deletePlayer(@PathVariable id: Long) {  // VETO!
    playerService.delete(id)
}

// ✅ 권한 체크
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/players/{id}")
fun deletePlayer(@PathVariable id: Long) {
    playerService.delete(id)
}
```

### 3. 민감 데이터 노출 방지

```kotlin
// ❌ 비밀번호 평문 반환
data class UserResponse(
    val email: String,
    val password: String  // VETO!
)

// ✅ 민감 정보 제외
data class UserResponse(
    val email: String
    // password 제외
)
```

### 4. XSS (Cross-Site Scripting) 방지

```kotlin
// ❌ 사용자 입력 직접 반환
@GetMapping("/search")
fun search(@RequestParam query: String): String {
    return "<div>$query</div>"  // VETO! XSS 위험
}

// ✅ 입력 검증 & 이스케이프
@GetMapping("/search")
fun search(@RequestParam @Valid query: SearchRequest): ApiResponse<SearchResponse> {
    val sanitized = HtmlUtils.htmlEscape(query.keyword)
    return ApiResponse.success(searchService.search(sanitized))
}
```

### 5. CORS 설정

```kotlin
// ❌ 모든 Origin 허용
@CrossOrigin(origins = ["*"])  // VETO! 프로덕션에서 금지

// ✅ 특정 Origin만 허용
@CrossOrigin(origins = ["https://nextup.com"])
```

---

## 🚨 필수 체크 항목

### API Layer

- [ ] Entity 직접 반환하지 않았는가?
- [ ] ApiResponse 래핑 사용했는가?
- [ ] CustomException 사용했는가?
- [ ] 권한 체크 (@PreAuthorize) 있는가?
- [ ] 입력 검증 (@Valid) 있는가?

### Data Layer

- [ ] Parameterized Query 사용했는가?
- [ ] SQL Injection 가능성은 없는가?
- [ ] 민감 정보 로깅하지 않았는가?

### Configuration

- [ ] CORS 설정이 안전한가?
- [ ] 하드코딩된 비밀번호/토큰은 없는가?
- [ ] 환경변수 사용했는가?

---

## 🔍 검증 방법

### 1. 자동 검증 (security-audit Skill)

```bash
# OWASP Dependency Check
./gradlew dependencyCheckAnalyze

# Detekt Security Rules
./gradlew detekt
```

### 2. 수동 검증 체크리스트

```
[ ] Entity Leak 검사: Controller에서 Entity 반환하는가?
[ ] SQL Injection: 문자열 결합 쿼리 있는가?
[ ] 권한 체크: 민감한 API에 @PreAuthorize 있는가?
[ ] 입력 검증: @Valid 사용했는가?
[ ] 민감 정보: 로그/응답에 비밀번호 있는가?
```

---

## ⚖️ Severity Level

| Level | 조건 | 조치 |
|-------|------|------|
| 🔴 **Critical** | Entity Leak, SQL Injection | **즉시 REJECT** |
| 🔴 **High** | 권한 체크 누락, XSS 위험 | **즉시 REJECT** |
| 🟠 **Medium** | CORS 설정 미흡 | 수정 권고 |
| 🟡 **Low** | 로깅 개선 필요 | 선택적 수정 |

---

## 📚 참고 자료

- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Kotlin Security Best Practices](https://kotlinlang.org/docs/security.html)

---

## 🎯 이 규칙의 목적

1. **보안 사고 예방**: 잠재적 취약점 사전 차단
2. **규제 준수**: OWASP 표준 준수
3. **신뢰성**: 사용자 데이터 안전하게 보호
4. **품질**: 보안을 고려한 설계 습관 형성
