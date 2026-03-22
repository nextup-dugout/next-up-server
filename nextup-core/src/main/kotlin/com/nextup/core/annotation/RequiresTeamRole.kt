package com.nextup.core.annotation

import com.nextup.core.domain.team.TeamMemberRole

/**
 * 팀 역할 기반 인가 어노테이션
 *
 * Controller 메서드에 적용하여 요청자의 팀 역할을 AOP로 검증합니다.
 * `@PreAuthorize`의 SpEL 표현식 대신 타입 안전한 어노테이션 방식을 제공합니다.
 *
 * 사용 예시:
 * ```kotlin
 * @RequiresTeamRole(roles = [TeamMemberRole.OWNER, TeamMemberRole.MANAGER])
 * @PostMapping
 * fun createSchedule(@PathVariable teamId: Long, ...) { ... }
 * ```
 *
 * AOP Aspect가 다음을 자동 수행합니다:
 * 1. SecurityContext에서 userId를 추출
 * 2. teamId 파라미터를 메서드 인자/경로변수/요청 본문에서 식별
 * 3. TeamMemberRepositoryPort로 팀 멤버 역할 조회
 * 4. 역할이 허용 목록에 없으면 AccessDeniedException 발생
 *
 * @param roles 허용되는 팀 멤버 역할 목록 (OR 조건)
 * @param teamIdParam teamId를 추출할 파라미터 이름 (기본값: "teamId")
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class RequiresTeamRole(
    val roles: Array<TeamMemberRole>,
    val teamIdParam: String = "teamId",
)
