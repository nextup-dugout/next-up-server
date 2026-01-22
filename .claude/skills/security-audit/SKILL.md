# Security-Audit Skill - OWASP 기반 보안 체크리스트

> **재사용 가능한 보안 검증 지식**
> OWASP Top 10 기반 자동 보안 감사

---

## 📚 Skill 개요

### 목적
- OWASP Top 10 기반 보안 취약점 자동 검사
- Spring Security 패턴 검증
- 민감 정보 노출 방지

### 사용 시나리오
- ✅ PR 생성 전 보안 검증
- ✅ API Controller 작성 시
- ✅ 인증/인가 로직 추가 시

### 호출 방법
```
"security-audit Skill로 보안 체크해줘"
"/review" (자동 포함)
```

---

## 🔍 OWASP Top 10 체크리스트

### 1. A01:2021 – Broken Access Control

#### 체크 항목
```kotlin
// ❌ 권한 체크 없음
@DeleteMapping("/players/{id}")
fun deletePlayer(@PathVariable id: Long) { ... }

// ✅ 권한 체크
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/players/{id}")
fun deletePlayer(@PathVariable id: Long) { ... }
```

#### 검증 스크립트
```bash
# 민감한 API에 @PreAuthorize 있는지 확인
grep -r "@DeleteMapping\|@PutMapping\|@PatchMapping" --include="*.kt" \
  | grep -v "@PreAuthorize" \
  | grep "Controller"
```

---

### 2. A02:2021 – Cryptographic Failures

#### 체크 항목
```kotlin
// ❌ 평문 비밀번호 저장
data class User(val password: String)

// ✅ 암호화된 비밀번호
data class User(
    val passwordHash: String  // BCrypt 해시
)
```

#### 검증 항목
- [ ] 비밀번호 평문 저장 금지
- [ ] HTTPS 사용 강제
- [ ] 민감 데이터 암호화

---

### 3. A03:2021 – Injection

#### SQL Injection 방지
```kotlin
// ❌ SQL Injection 위험
@Query("SELECT * FROM player WHERE name = '${name}'")  // VETO!

// ✅ Parameterized Query
@Query("SELECT p FROM Player p WHERE p.name = :name")
fun findByName(@Param("name") name: String): Player?
```

#### 검증 스크립트
```bash
# 문자열 결합 쿼리 검색
grep -r '\${' --include="*.kt" | grep "@Query"
```

---

### 4. A04:2021 – Insecure Design

#### Zero Entity Leak 검증
```kotlin
// ❌ Entity 직접 노출
@GetMapping("/{id}")
fun getPlayer(@PathVariable id: Long): Player { ... }

// ✅ DTO 변환
@GetMapping("/{id}")
fun getPlayer(@PathVariable id: Long): ApiResponse<PlayerResponse> { ... }
```

#### 검증 스크립트
```bash
# Controller에서 Entity 직접 반환 검사
grep -A 5 "@GetMapping\|@PostMapping" nextup-api/**/*.kt \
  | grep "fun.*: Player\|fun.*: Team"
```

---

### 5. A05:2021 – Security Misconfiguration

#### CORS 설정 검증
```kotlin
// ❌ 모든 Origin 허용
@CrossOrigin(origins = ["*"])  // VETO!

// ✅ 특정 Origin만 허용
@CrossOrigin(origins = ["https://nextup.com"])
```

#### 체크 항목
- [ ] CORS allowedOrigins에 "*" 사용 금지
- [ ] DEBUG 모드 프로덕션 배포 금지
- [ ] 불필요한 HTTP 메서드 비활성화

---

### 6. A06:2021 – Vulnerable and Outdated Components

#### 의존성 검증
```bash
# OWASP Dependency Check
./gradlew dependencyCheckAnalyze

# 취약점 발견 시 REJECT
```

---

### 7. A07:2021 – Identification and Authentication Failures

#### 인증 검증
```kotlin
// Session 타임아웃 설정
server:
  servlet:
    session:
      timeout: 30m  # 30분

// 비밀번호 정책
@Size(min = 8, message = "비밀번호는 최소 8자 이상")
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).*$", message = "영문+숫자 조합")
```

---

### 8. A08:2021 – Software and Data Integrity Failures

#### 체크 항목
- [ ] 파일 업로드 검증
- [ ] 직렬화/역직렬화 안전성
- [ ] CI/CD 파이프라인 보안

---

### 9. A09:2021 – Security Logging and Monitoring Failures

#### 로깅 규칙
```kotlin
// ❌ 민감 정보 로깅
log.info("User login: ${user.email}, password: ${user.password}")

// ✅ 안전한 로깅
log.info("User login: ${user.email}")  // 비밀번호 제외
```

#### 검증 스크립트
```bash
# password, token 등 민감 정보 로깅 검사
grep -r "log.*password\|log.*token" --include="*.kt"
```

---

### 10. A10:2021 – Server-Side Request Forgery (SSRF)

#### 외부 요청 검증
```kotlin
// 사용자 입력 URL 요청 시 검증 필수
fun fetchExternalData(url: String): String {
    require(isAllowedDomain(url)) { "허용되지 않은 도메인" }
    // ...
}
```

---

## 🚨 자동 검증 스크립트

### 실행 방법
```bash
# 전체 보안 감사
.claude/skills/security-audit/scripts/audit.sh

# 개별 검증
.claude/skills/security-audit/scripts/check-entity-leak.sh
.claude/skills/security-audit/scripts/check-sql-injection.sh
.claude/skills/security-audit/scripts/check-logging.sh
```

### audit.sh
```bash
#!/bin/bash
echo "=== Security Audit ==="

# 1. Entity Leak 검사
echo "1. Checking Entity Leak..."
if grep -r "fun.*: Player\|fun.*: Team" nextup-api/**/*.kt; then
    echo "❌ FAIL: Entity 직접 반환 발견"
    exit 1
fi

# 2. SQL Injection 검사
echo "2. Checking SQL Injection..."
if grep -r '\${' --include="*.kt" | grep "@Query"; then
    echo "❌ FAIL: 문자열 결합 쿼리 발견"
    exit 1
fi

# 3. CORS 검사
echo "3. Checking CORS..."
if grep -r '@CrossOrigin(origins = \["\*"\])' --include="*.kt"; then
    echo "❌ FAIL: CORS wildcard 사용"
    exit 1
fi

# 4. 민감 정보 로깅 검사
echo "4. Checking Sensitive Logging..."
if grep -r "log.*password\|log.*token" --include="*.kt"; then
    echo "❌ FAIL: 민감 정보 로깅 발견"
    exit 1
fi

echo "✅ Security Audit PASS"
```

---

## 📊 Severity Level

| Level | 조건 | 조치 |
|-------|------|------|
| 🔴 **Critical** | Entity Leak, SQL Injection, 평문 비밀번호 | **즉시 REJECT** |
| 🔴 **High** | 권한 체크 누락, CORS wildcard | **즉시 REJECT** |
| 🟠 **Medium** | 민감 정보 로깅, 세션 타임아웃 미설정 | 수정 권고 |
| 🟡 **Low** | 로깅 개선 필요 | 선택적 수정 |

---

## 📋 보고서 템플릿

```markdown
# 보안 감사 보고서

## 검사 대상
- 모듈: nextup-api
- 파일: PlayerController.kt

## 판정: ✅ PASS / ❌ REJECT

## 검증 결과

### Critical Issues (🔴)
1. Entity Leak 검사
   - 상태: ✅ PASS
   - 확인: 모든 API가 DTO 반환

2. SQL Injection 검사
   - 상태: ❌ FAIL
   - 위치: PlayerRepository.kt:15
   - 코드: `@Query("SELECT * FROM player WHERE name = '${name}'")`
   - 조치: Parameterized Query로 변경 필요

### High Issues (🔴)
3. 권한 체크
   - 상태: ❌ FAIL
   - 위치: PlayerController.kt:45
   - 코드: `@DeleteMapping` without `@PreAuthorize`
   - 조치: `@PreAuthorize("hasRole('ADMIN')")` 추가

### Medium Issues (🟠)
4. CORS 설정
   - 상태: ✅ PASS

### Low Issues (🟡)
5. 로깅
   - 상태: ✅ PASS

## 종합 판정
❌ REJECT - Critical/High 이슈 2건 발견

## 수정 필요 사항
1. [ ] PlayerRepository.kt:15 - SQL Injection 수정
2. [ ] PlayerController.kt:45 - 권한 체크 추가
```

---

## 🎯 이 Skill의 장점

1. **자동화**: 스크립트로 빠른 검증
2. **표준화**: OWASP 기반 일관된 보안 기준
3. **사전 방지**: PR 전 보안 이슈 발견
4. **교육 효과**: 보안 패턴 학습

---

## 📚 참고 자료

- [OWASP Top 10 2021](https://owasp.org/www-project-top-ten/)
- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [Kotlin Security Best Practices](https://kotlinlang.org/docs/security.html)
