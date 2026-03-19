# Repository Injection Rules (Controller 레이어 규칙)

## 절대 규칙: Controller에서 Repository 직접 주입 금지

Controller 클래스를 작성하거나 수정할 때:

1. **Repository/RepositoryPort를 constructor에 주입하지 마라** — 반드시 Service를 통해 접근
2. **Controller에서 Repository 메서드를 직접 호출하지 마라** — `save()`, `findById()`, `delete()` 등
3. **Controller에서 Core 도메인 Repository를 import하지 마라**

## 왜 금지인가

Controller가 Repository를 직접 사용하면:
- **인가(Authorization) 우회**: Service에서 수행해야 할 권한 검증을 건너뜀
- **감사 로깅 누락**: AuditLogAspect가 Service 레벨에서 동작하므로 기록이 남지 않음
- **도메인 불변식 무시**: Entity의 비즈니스 규칙 검증을 우회
- **배치 최적화 불가**: Service에서 N+1 해결 등 최적화를 적용할 수 없음

## 위반 예시 (이렇게 작성하면 안 됨)

```kotlin
// ❌ REJECT: Controller에 Repository 주입
@RestController
class TeamController(
    private val teamRepository: TeamRepositoryPort,  // ❌
    private val leagueRepository: LeagueRepositoryPort,  // ❌
) {
    @PostMapping
    fun createTeam(@RequestBody request: CreateTeamRequest): ApiResponse<TeamResponse> {
        val league = leagueRepository.findById(request.leagueId)  // ❌ Service 우회
            ?: throw LeagueNotFoundException(request.leagueId)
        val team = Team(league, request.name, ...)  // ❌ Entity 직접 생성
        teamRepository.save(team)  // ❌ Repository 직접 호출
    }
}
```

## 올바른 예시 (반드시 이렇게 작성)

```kotlin
// ✅ PASS: Service를 통해 접근
@RestController
class TeamController(
    private val teamService: TeamService,  // ✅ Service만 주입
) {
    @PreAuthorize("hasRole('USER')")
    @PostMapping
    fun createTeam(
        @AuthenticationPrincipal userId: Long,
        @RequestBody request: CreateTeamRequest,
    ): ApiResponse<TeamResponse> {
        val team = teamService.createTeam(userId, request)  // ✅ Service 경유
        return ApiResponse.success(TeamResponse.from(team))
    }
}
```

## 예외

- `@Configuration` 클래스에서 초기 데이터 설정 시 Repository 사용은 허용
- 테스트 코드(`src/test/`)에서는 면제
