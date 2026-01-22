# /review Command - 품질 & 보안 검증

> **빌드 + 테스트 + 커버리지 + 보안** 전체 검증

## 목적

PR 생성 전 모든 품질 기준을 충족하는지 자동으로 검증합니다.

## 사용법

```
/review
```

## 실행 흐름

### 1. 빌드 & 테스트 (quality-metrics Skill)
```bash
./gradlew clean build test
```

### 2. 보안 검증 (security-audit Skill)

#### Entity Leak 검사
```bash
grep -r "fun.*: Player\|fun.*: Team" nextup-api/**/*.kt

✅ Entity 직접 반환 없음
```

#### SQL Injection 검사
```bash
grep -r '\${' --include="*.kt" | grep "@Query"

✅ 문자열 결합 쿼리 없음
```

#### CORS 검사
```bash
grep -r '@CrossOrigin(origins = \["\*"\])' --include="*.kt"

✅ CORS wildcard 없음
```

### 3. 도메인 로직 검증 (domain-baseball Skill)
```
해당 시 야구 규칙 준수 확인
```

### 4. 커버리지 검증
```
- nextup-core: 80% 이상 필수
- Codecov가 PR 시 자동 체크
```

## 검수 보고서

```markdown
# 검수 보고서

## 판정: ✅ PASS / ❌ REJECT

### 🔴 Critical
1. 빌드: ✅ PASS
2. 테스트: ✅ PASS
3. 커버리지: ✅ 82%
4. Entity Leak: ✅ PASS
5. SQL Injection: ✅ PASS

### 🟠 High
6. ApiResponse: ✅ PASS
7. 컨벤션: ✅ PASS

## 종합 판정
✅ APPROVE - PR 생성 가능

다음 단계: `/pr`로 PR 생성하세요
```

## Skills 참조

- **quality-metrics**: 빌드/테스트/커버리지
- **security-audit**: OWASP 보안 체크
- **domain-baseball**: 야구 로직 검증

## Agents 협업

- **reviewer**: 최종 검수 담당
- **devops**: PASS 시 PR 생성

## REJECT 시 조치

### 빌드 실패
```
→ implementer에게 수정 요청
```

### 커버리지 미달
```
→ implementer에게 테스트 추가 요청
```

### 보안 이슈
```
→ implementer에게 즉시 수정 요청
```

## 체크리스트

- [ ] 빌드 성공
- [ ] 테스트 통과
- [ ] 커버리지 80% 이상
- [ ] Entity Leak 없음
- [ ] SQL Injection 없음
- [ ] CORS 안전
- [ ] ApiResponse 사용
- [ ] 컨벤션 준수

## 예시

```
User: "/review"

Reviewer Agent:
1. quality-metrics Skill 실행
   ✅ 빌드 성공
   ✅ 45개 테스트 통과
   ✅ 커버리지 82%

2. security-audit Skill 실행
   ✅ Entity Leak 없음
   ✅ SQL Injection 없음
   ✅ CORS 안전

3. 최종 판정
   ✅ APPROVE

4. 다음 단계
   "/pr로 PR 생성하세요"
```

## 이 Command의 장점

- ✅ PR 전 자동 검증
- ✅ Skills로 빠른 체크
- ✅ 명확한 PASS/REJECT
- ✅ 재작업 방향 제시
