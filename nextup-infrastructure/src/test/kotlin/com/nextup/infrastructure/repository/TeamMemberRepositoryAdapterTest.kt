package com.nextup.infrastructure.repository

import com.nextup.core.domain.team.TeamMember
import com.nextup.core.domain.team.TeamMemberStatus
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull

@DisplayName("TeamMemberRepositoryAdapter 테스트")
class TeamMemberRepositoryAdapterTest {

    private val jpaRepository: TeamMemberRepository = mockk()
    private lateinit var adapter: TeamMemberRepositoryAdapter

    @BeforeEach
    fun setUp() {
        adapter = TeamMemberRepositoryAdapter(jpaRepository)
    }

    @Test
    @DisplayName("save - TeamMember 저장 시 JPA repository에 위임한다")
    fun `should delegate save to jpa repository`() {
        // given
        val teamMember = mockk<TeamMember>()
        every { jpaRepository.save(teamMember) } returns teamMember

        // when
        val result = adapter.save(teamMember)

        // then
        assertThat(result).isEqualTo(teamMember)
        verify(exactly = 1) { jpaRepository.save(teamMember) }
    }

    @Test
    @DisplayName("findByIdOrNull - ID로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByIdOrNull to jpa repository`() {
        // given
        val teamMember = mockk<TeamMember>()
        every { jpaRepository.findByIdOrNull(1L) } returns teamMember

        // when
        val result = adapter.findByIdOrNull(1L)

        // then
        assertThat(result).isEqualTo(teamMember)
        verify(exactly = 1) { jpaRepository.findByIdOrNull(1L) }
    }

    @Test
    @DisplayName("findByIdOrNull - 존재하지 않는 ID 조회 시 null을 반환한다")
    fun `should return null when id not found`() {
        // given
        every { jpaRepository.findByIdOrNull(99L) } returns null

        // when
        val result = adapter.findByIdOrNull(99L)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByTeamIdAndUserId - teamId와 userId로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamIdAndUserId to jpa repository`() {
        // given
        val teamMember = mockk<TeamMember>()
        every { jpaRepository.findByTeamIdAndUserId(1L, 2L) } returns teamMember

        // when
        val result = adapter.findByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isEqualTo(teamMember)
        verify(exactly = 1) { jpaRepository.findByTeamIdAndUserId(1L, 2L) }
    }

    @Test
    @DisplayName("findByTeamId - teamId로 목록 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamId to jpa repository`() {
        // given
        val members = listOf(mockk<TeamMember>(), mockk<TeamMember>())
        every { jpaRepository.findByTeamId(1L) } returns members

        // when
        val result = adapter.findByTeamId(1L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(members)
        verify(exactly = 1) { jpaRepository.findByTeamId(1L) }
    }

    @Test
    @DisplayName("findByTeamIdAndStatus - teamId와 status로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamIdAndStatus to jpa repository`() {
        // given
        val members = listOf(mockk<TeamMember>())
        every { jpaRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns members

        // when
        val result = adapter.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)

        // then
        assertThat(result).hasSize(1)
        assertThat(result).isEqualTo(members)
        verify(exactly = 1) { jpaRepository.findByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) }
    }

    @Test
    @DisplayName("findByTeamIdAndStatusIn - teamId와 status 목록으로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamIdAndStatusIn to jpa repository`() {
        // given
        val statuses = setOf(TeamMemberStatus.ACTIVE, TeamMemberStatus.SUSPENDED)
        val members = listOf(mockk<TeamMember>(), mockk<TeamMember>())
        every { jpaRepository.findByTeamIdAndStatusIn(1L, statuses) } returns members

        // when
        val result = adapter.findByTeamIdAndStatusIn(1L, statuses)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(members)
        verify(exactly = 1) { jpaRepository.findByTeamIdAndStatusIn(1L, statuses) }
    }

    @Test
    @DisplayName("findByUserId - userId로 목록 조회 시 JPA repository에 위임한다")
    fun `should delegate findByUserId to jpa repository`() {
        // given
        val members = listOf(mockk<TeamMember>())
        every { jpaRepository.findByUserId(10L) } returns members

        // when
        val result = adapter.findByUserId(10L)

        // then
        assertThat(result).isEqualTo(members)
        verify(exactly = 1) { jpaRepository.findByUserId(10L) }
    }

    @Test
    @DisplayName("findActiveByUserId - 활성 상태 멤버 조회 시 JPA repository에 위임한다")
    fun `should delegate findActiveByUserId to jpa repository`() {
        // given
        val teamMember = mockk<TeamMember>()
        every { jpaRepository.findActiveByUserId(10L) } returns teamMember

        // when
        val result = adapter.findActiveByUserId(10L)

        // then
        assertThat(result).isEqualTo(teamMember)
        verify(exactly = 1) { jpaRepository.findActiveByUserId(10L) }
    }

    @Test
    @DisplayName("findByTeamIdWithUserAndPlayer - 페이지 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamIdWithUserAndPlayer to jpa repository`() {
        // given
        val pageable = mockk<Pageable>()
        val member = mockk<TeamMember>()
        val page = PageImpl(listOf(member))
        every { jpaRepository.findByTeamIdWithUserAndPlayer(1L, pageable) } returns page

        // when
        val result = adapter.findByTeamIdWithUserAndPlayer(1L, pageable)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0]).isEqualTo(member)
        verify(exactly = 1) { jpaRepository.findByTeamIdWithUserAndPlayer(1L, pageable) }
    }

    @Test
    @DisplayName("existsByTeamIdAndUserId - 존재 여부 확인 시 JPA repository에 위임한다")
    fun `should delegate existsByTeamIdAndUserId to jpa repository`() {
        // given
        every { jpaRepository.existsByTeamIdAndUserId(1L, 2L) } returns true

        // when
        val result = adapter.existsByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isTrue()
        verify(exactly = 1) { jpaRepository.existsByTeamIdAndUserId(1L, 2L) }
    }

    @Test
    @DisplayName("existsByTeamIdAndUniformNumberAndStatus - 등번호와 상태로 존재 여부 확인 시 JPA repository에 위임한다")
    fun `should delegate existsByTeamIdAndUniformNumberAndStatus to jpa repository`() {
        // given
        every {
            jpaRepository.existsByTeamIdAndUniformNumberAndStatus(1L, 7, TeamMemberStatus.ACTIVE)
        } returns false

        // when
        val result = adapter.existsByTeamIdAndUniformNumberAndStatus(1L, 7, TeamMemberStatus.ACTIVE)

        // then
        assertThat(result).isFalse()
        verify(exactly = 1) {
            jpaRepository.existsByTeamIdAndUniformNumberAndStatus(1L, 7, TeamMemberStatus.ACTIVE)
        }
    }

    @Test
    @DisplayName("countOwnersByTeamId - 팀 OWNER 수 조회 시 JPA repository에 위임한다")
    fun `should delegate countOwnersByTeamId to jpa repository`() {
        // given
        every { jpaRepository.countOwnersByTeamId(1L) } returns 1L

        // when
        val result = adapter.countOwnersByTeamId(1L)

        // then
        assertThat(result).isEqualTo(1L)
        verify(exactly = 1) { jpaRepository.countOwnersByTeamId(1L) }
    }

    @Test
    @DisplayName("delete - TeamMember 삭제 시 JPA repository에 위임한다")
    fun `should delegate delete to jpa repository`() {
        // given
        val teamMember = mockk<TeamMember>()
        justRun { jpaRepository.delete(teamMember) }

        // when
        adapter.delete(teamMember)

        // then
        verify(exactly = 1) { jpaRepository.delete(teamMember) }
    }

    @Test
    @DisplayName("deleteById - ID로 삭제 시 JPA repository에 위임한다")
    fun `should delegate deleteById to jpa repository`() {
        // given
        justRun { jpaRepository.deleteById(1L) }

        // when
        adapter.deleteById(1L)

        // then
        verify(exactly = 1) { jpaRepository.deleteById(1L) }
    }

    @Test
    @DisplayName("findByPlayerIdActive - 활성 선수 ID로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByPlayerIdActive to jpa repository`() {
        // given
        val members = listOf(mockk<TeamMember>())
        every { jpaRepository.findByPlayerIdActive(5L) } returns members

        // when
        val result = adapter.findByPlayerIdActive(5L)

        // then
        assertThat(result).isEqualTo(members)
        verify(exactly = 1) { jpaRepository.findByPlayerIdActive(5L) }
    }

    @Test
    @DisplayName("findByPlayerIdsActive - 선수 ID 목록으로 활성 멤버 일괄 조회 시 JPA repository에 위임한다")
    fun `should delegate findByPlayerIdsActive to jpa repository`() {
        // given
        val members = listOf(mockk<TeamMember>(), mockk<TeamMember>())
        every { jpaRepository.findByPlayerIdsActive(listOf(10L, 20L)) } returns members

        // when
        val result = adapter.findByPlayerIdsActive(listOf(10L, 20L))

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(members)
        verify(exactly = 1) { jpaRepository.findByPlayerIdsActive(listOf(10L, 20L)) }
    }

    @Test
    @DisplayName("findByPlayerIdsActive - 빈 목록 전달 시 JPA 호출 없이 빈 리스트를 반환한다")
    fun `should return empty list without jpa call when playerIds is empty`() {
        // when
        val result = adapter.findByPlayerIdsActive(emptyList())

        // then
        assertThat(result).isEmpty()
        verify(exactly = 0) { jpaRepository.findByPlayerIdsActive(any()) }
    }

    @Test
    @DisplayName("countByTeamIdAndStatus - 팀 상태별 멤버 수 조회 시 JPA repository에 위임한다")
    fun `should delegate countByTeamIdAndStatus to jpa repository`() {
        // given
        every { jpaRepository.countByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) } returns 5L

        // when
        val result = adapter.countByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE)

        // then
        assertThat(result).isEqualTo(5L)
        verify(exactly = 1) { jpaRepository.countByTeamIdAndStatus(1L, TeamMemberStatus.ACTIVE) }
    }
}
