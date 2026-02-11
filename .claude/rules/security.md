# Security Rules

## Zero Entity Leak (ABSOLUTE RULE)

**NEVER expose Entity directly to API layer.**

```kotlin
// NEVER: Direct Entity exposure
@GetMapping("/players/{id}")
fun getPlayer(@PathVariable id: Long): Player  // ❌ REJECT

// ALWAYS: DTO conversion
@GetMapping("/players/{id}")
fun getPlayer(@PathVariable id: Long): PlayerResponse  // ✅ APPROVED
```

## Mandatory Security Checks

Before ANY commit:
- [ ] **Zero Entity Leak**: No Entity in Controller return type
- [ ] **No hardcoded secrets**: API keys, passwords, tokens in environment variables
- [ ] **Input validation**: All user inputs validated at Controller layer
- [ ] **SQL injection prevention**: Use JPA/QueryDSL parameterized queries only
- [ ] **Authentication**: Protected endpoints have @PreAuthorize
- [ ] **Authorization**: Verify user permissions in Service layer
- [ ] **Rate limiting**: Apply to all public endpoints
- [ ] **Error messages**: Don't leak sensitive data in exceptions

## Secret Management

```kotlin
// NEVER: Hardcoded secrets
val apiKey = "sk-proj-xxxxx"  // ❌ REJECT

// ALWAYS: Environment variables
@Value("\${openai.api-key}")
private lateinit var apiKey: String  // ✅ APPROVED

// ALWAYS: Fail fast if missing
init {
    require(apiKey.isNotBlank()) { "OPENAI_API_KEY not configured" }
}
```

## OWASP Top 10 Checklist

- [ ] **A01: Broken Access Control** - Verify authorization in Service layer
- [ ] **A02: Cryptographic Failures** - Use HTTPS, encrypt sensitive data
- [ ] **A03: Injection** - Parameterized queries, input validation
- [ ] **A04: Insecure Design** - Zero Entity Leak enforcement
- [ ] **A05: Security Misconfiguration** - No default credentials
- [ ] **A06: Vulnerable Components** - Keep dependencies updated
- [ ] **A07: Authentication Failures** - Implement JWT/OAuth properly
- [ ] **A08: Data Integrity Failures** - Validate all external data
- [ ] **A09: Logging Failures** - Log security events, no sensitive data
- [ ] **A10: SSRF** - Validate all external URLs

## Security Response Protocol

If security issue found:
1. **STOP immediately**
2. Use **security-auditor** agent for review
3. Fix CRITICAL issues before continuing
4. Rotate any exposed secrets
5. Review entire codebase for similar issues
6. Reviewer VETO - automatic REJECT
