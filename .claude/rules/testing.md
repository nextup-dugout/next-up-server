---
paths:
  - "**/*Test.kt"
  - "**/*Tests.kt"
  - "**/test/**/*.kt"
---

# Testing Conventions

> 테스트 코드 작성 규칙. MockK 기반 단위 테스트, given/when/then 패턴.

## 테스트 프레임워크
- Mocking: **MockK** 1.13.14 (Mockito 금지)
- Assertion: **AssertJ** 3.27.3
- Test Runner: JUnit 5
- Parameterized: `junit-jupiter-params`

## 테스트 구조
- `@DisplayName("ClassName")` 클래스 레벨 어노테이션 필수
- `@Nested` inner class로 API endpoint별/메서드별 그룹화
- `@DisplayName("HTTP_METHOD /path")` Nested 클래스에도 적용
- 한글 backtick 테스트명 사용: `` `should cancel game when status is SCHEDULED` ``
- given/when/then 3-section 패턴 (주석으로 구분)

## 테스트 유형별 패턴

### Entity 테스트 (순수 단위 테스트)
- Spring context 미로딩
- `Game.create(...)` 팩토리로 직접 생성

### Service 테스트 (MockK 단위 테스트)
- MockK로 RepositoryPort mock
- Service 직접 인스턴스화

### Controller 테스트 (직접 인스턴스화, MockMvc 미사용)
- **Controller를 직접 인스턴스화** (Spring context 미로딩)
- MockK로 Service mock
- 반환값 검증 (ApiResponse 래핑 확인)

```kotlin
@DisplayName("TeamController")
class TeamControllerTest {
    private val service: TeamMembershipService = mockk()
    private val controller = TeamController(service)

    @Nested
    @DisplayName("POST /api/v1/teams")
    inner class CreateTeam {
        @Test
        fun `should create team and register owner`() {
            // given
            val request = CreateTeamRequest(name = "팀명")
            every { service.createTeam(any()) } returns TeamResponse(...)

            // when
            val result = controller.createTeam(request)

            // then
            assertThat(result.success).isTrue()
        }
    }
}
```

### Repository 테스트 (통합 테스트, 선택적)
- `@DataJpaTest` + H2

## 테스트 격리
- 각 테스트는 독립적 실행 가능
- `@Transactional`로 DB 테스트 롤백
- 공유 가변 상태 금지

## 커버리지
- Jacoco 최소 80% (로컬 빌드)
- Codecov 85% (PR 머지 기준)
- Core/Service 계층 필수 적용
