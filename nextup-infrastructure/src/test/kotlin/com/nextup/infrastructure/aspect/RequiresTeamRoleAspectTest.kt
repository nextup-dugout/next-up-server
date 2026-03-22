package com.nextup.infrastructure.aspect

import com.nextup.core.annotation.RequiresTeamRole
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.reflect.MethodSignature
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

@DisplayName("RequiresTeamRoleAspect 테스트")
class RequiresTeamRoleAspectTest {

    private val teamMemberRepository = mockk<TeamMemberRepositoryPort>()
    private val aspect = RequiresTeamRoleAspect(teamMemberRepository)

    private val joinPoint = mockk<ProceedingJoinPoint>(relaxed = true)
    private val signature = mockk<MethodSignature>(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { joinPoint.signature } returns signature
    }

    @AfterEach
    fun tearDown() {
        SecurityContextHolder.clearContext()
        clearMocks(teamMemberRepository, joinPoint, signature)
    }

    private fun setAuthenticatedUser(userId: Long) {
        val auth =
            UsernamePasswordAuthenticationToken(
                userId,
                null,
                emptyList(),
            )
        SecurityContextHolder.getContext().authentication = auth
    }

    private fun createAnnotation(
        roles: Array<TeamMemberRole>,
        teamIdParam: String = "teamId",
    ): RequiresTeamRole {
        val annotation = mockk<RequiresTeamRole>()
        every { annotation.roles } returns roles
        every { annotation.teamIdParam } returns teamIdParam
        return annotation
    }

    private fun mockTeamMember(
        teamId: Long,
        userId: Long,
        role: TeamMemberRole,
    ): TeamMember {
        val member = mockk<TeamMember>()
        every { member.role } returns role
        every { teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns member
        return member
    }

    @Nested
    @DisplayName("인가 성공 케이스")
    inner class AuthorizationSuccess {

        @Test
        @DisplayName("OWNER 역할이 OWNER 요구 조건을 충족한다")
        fun ownerPassesOwnerRequirement() {
            // given
            val userId = 1L
            val teamId = 10L
            setAuthenticatedUser(userId)
            mockTeamMember(teamId, userId, TeamMemberRole.OWNER)

            every { signature.parameterNames } returns arrayOf("teamId")
            every { joinPoint.args } returns arrayOf(teamId)
            every { joinPoint.proceed() } returns "success"

            val annotation = createAnnotation(arrayOf(TeamMemberRole.OWNER))

            // when
            val result = aspect.checkTeamRole(joinPoint, annotation)

            // then
            assertThat(result).isEqualTo("success")
            verify { joinPoint.proceed() }
        }

        @Test
        @DisplayName("MANAGER 역할이 OWNER_OR_MANAGER 요구 조건을 충족한다")
        fun managerPassesOwnerOrManagerRequirement() {
            // given
            val userId = 2L
            val teamId = 20L
            setAuthenticatedUser(userId)
            mockTeamMember(teamId, userId, TeamMemberRole.MANAGER)

            every { signature.parameterNames } returns arrayOf("teamId")
            every { joinPoint.args } returns arrayOf(teamId)
            every { joinPoint.proceed() } returns "ok"

            val annotation =
                createAnnotation(arrayOf(TeamMemberRole.OWNER, TeamMemberRole.MANAGER))

            // when
            val result = aspect.checkTeamRole(joinPoint, annotation)

            // then
            assertThat(result).isEqualTo("ok")
        }

        @Test
        @DisplayName("MEMBER 역할이 전체 역할 허용 시 성공한다")
        fun memberPassesAllRolesRequirement() {
            // given
            val userId = 3L
            val teamId = 30L
            setAuthenticatedUser(userId)
            mockTeamMember(teamId, userId, TeamMemberRole.MEMBER)

            every { signature.parameterNames } returns arrayOf("teamId")
            every { joinPoint.args } returns arrayOf(teamId)
            every { joinPoint.proceed() } returns "data"

            val annotation =
                createAnnotation(
                    arrayOf(
                        TeamMemberRole.OWNER,
                        TeamMemberRole.MANAGER,
                        TeamMemberRole.MEMBER,
                    ),
                )

            // when
            val result = aspect.checkTeamRole(joinPoint, annotation)

            // then
            assertThat(result).isEqualTo("data")
        }
    }

    @Nested
    @DisplayName("인가 실패 케이스")
    inner class AuthorizationFailure {

        @Test
        @DisplayName("MEMBER가 OWNER 전용 요구 조건에서 실패한다")
        fun memberFailsOwnerRequirement() {
            // given
            val userId = 4L
            val teamId = 40L
            setAuthenticatedUser(userId)
            mockTeamMember(teamId, userId, TeamMemberRole.MEMBER)

            every { signature.parameterNames } returns arrayOf("teamId")
            every { joinPoint.args } returns arrayOf(teamId)

            val annotation = createAnnotation(arrayOf(TeamMemberRole.OWNER))

            // when & then
            assertThatThrownBy { aspect.checkTeamRole(joinPoint, annotation) }
                .isInstanceOf(AccessDeniedException::class.java)
                .hasMessageContaining("팀 역할 권한 부족")
        }

        @Test
        @DisplayName("팀에 소속되지 않은 사용자는 실패한다")
        fun nonMemberFails() {
            // given
            val userId = 5L
            val teamId = 50L
            setAuthenticatedUser(userId)
            every { teamMemberRepository.findByTeamIdAndUserId(teamId, userId) } returns null

            every { signature.parameterNames } returns arrayOf("teamId")
            every { joinPoint.args } returns arrayOf(teamId)

            val annotation = createAnnotation(arrayOf(TeamMemberRole.MEMBER))

            // when & then
            assertThatThrownBy { aspect.checkTeamRole(joinPoint, annotation) }
                .isInstanceOf(AccessDeniedException::class.java)
                .hasMessageContaining("미소속")
        }

        @Test
        @DisplayName("인증되지 않은 요청은 실패한다")
        fun unauthenticatedRequestFails() {
            // given
            SecurityContextHolder.clearContext()

            every { signature.parameterNames } returns arrayOf("teamId")
            every { joinPoint.args } returns arrayOf(10L)

            val annotation = createAnnotation(arrayOf(TeamMemberRole.OWNER))

            // when & then
            assertThatThrownBy { aspect.checkTeamRole(joinPoint, annotation) }
                .isInstanceOf(AccessDeniedException::class.java)
                .hasMessageContaining("인증 정보가 없습니다")
        }
    }

    @Nested
    @DisplayName("teamId 추출 케이스")
    inner class TeamIdExtraction {

        @Test
        @DisplayName("요청 객체의 필드에서 teamId를 추출한다")
        fun extractsTeamIdFromRequestObject() {
            // given
            val userId = 6L
            val teamId = 60L
            setAuthenticatedUser(userId)
            mockTeamMember(teamId, userId, TeamMemberRole.OWNER)

            val requestObj = TestRequest(teamId = teamId)
            every { signature.parameterNames } returns arrayOf("request")
            every { joinPoint.args } returns arrayOf(requestObj)
            every { joinPoint.proceed() } returns "extracted"

            val annotation = createAnnotation(arrayOf(TeamMemberRole.OWNER))

            // when
            val result = aspect.checkTeamRole(joinPoint, annotation)

            // then
            assertThat(result).isEqualTo("extracted")
        }

        @Test
        @DisplayName("teamId 파라미터를 찾을 수 없으면 예외가 발생한다")
        fun throwsWhenTeamIdNotFound() {
            // given
            val userId = 7L
            setAuthenticatedUser(userId)

            every { signature.parameterNames } returns arrayOf("otherId")
            every { joinPoint.args } returns arrayOf(99L)

            val annotation = createAnnotation(arrayOf(TeamMemberRole.OWNER))

            // when & then
            assertThatThrownBy { aspect.checkTeamRole(joinPoint, annotation) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("teamId 파라미터를 찾을 수 없습니다")
        }
    }

    data class TestRequest(
        val teamId: Long
    )
}
