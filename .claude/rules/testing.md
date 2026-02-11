---
paths:
  - "**/*Test.kt"
  - "**/*Tests.kt"
  - "**/test/**/*.kt"
---

# Testing Conventions

> 테스트 코드 작성 규칙. MockK 기반 단위 테스트, given/when/then 패턴.

## 테스트 프레임워크
- Mocking: **MockK** 사용 (Mockito 금지)
- Assertion: **AssertJ** 사용
- Test Runner: JUnit 5

## 테스트 구조
- `@Nested` inner class로 API endpoint별/메서드별 그룹화
- 한글 backtick 테스트명 사용: `` `should cancel game when status is SCHEDULED` ``
- given/when/then 3-section 패턴 (주석으로 구분)

## 테스트 유형별 패턴
- **Entity 테스트**: 순수 단위 테스트 (Spring context 미로딩)
- **Service 테스트**: MockK로 Repository mock
- **Controller 테스트**: `MockMvc` + MockK 기반 단위 테스트 (Spring context 미로딩)
- **Repository 테스트**: `@DataJpaTest` + H2

## 테스트 격리
- 각 테스트는 독립적 실행 가능
- `@Transactional`로 DB 테스트 롤백
- 공유 가변 상태 금지

## 커버리지
- 최소 80% (Jacoco)
- Core/Service 계층 필수 적용
