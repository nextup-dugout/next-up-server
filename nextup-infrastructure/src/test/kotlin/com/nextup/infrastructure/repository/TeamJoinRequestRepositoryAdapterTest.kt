package com.nextup.infrastructure.repository

import com.nextup.core.common.PageCommand
import com.nextup.core.domain.team.JoinRequestStatus
import com.nextup.core.domain.team.TeamJoinRequest
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.repository.findByIdOrNull

@DisplayName("TeamJoinRequestRepositoryAdapter 테스트")
class TeamJoinRequestRepositoryAdapterTest {

    private val jpaRepository: TeamJoinRequestRepository = mockk()
    private lateinit var adapter: TeamJoinRequestRepositoryAdapter

    @BeforeEach
    fun setUp() {
        adapter = TeamJoinRequestRepositoryAdapter(jpaRepository)
    }

    @Test
    @DisplayName("save - TeamJoinRequest 저장 시 JPA repository에 위임한다")
    fun `should delegate save to jpa repository`() {
        // given
        val request = mockk<TeamJoinRequest>()
        every { jpaRepository.save(request) } returns request

        // when
        val result = adapter.save(request)

        // then
        assertThat(result).isEqualTo(request)
        verify(exactly = 1) { jpaRepository.save(request) }
    }

    @Test
    @DisplayName("findByIdOrNull - ID로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByIdOrNull to jpa repository`() {
        // given
        val request = mockk<TeamJoinRequest>()
        every { jpaRepository.findByIdOrNull(1L) } returns request

        // when
        val result = adapter.findByIdOrNull(1L)

        // then
        assertThat(result).isEqualTo(request)
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
    @DisplayName("findByTeamId - teamId로 목록 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamId to jpa repository`() {
        // given
        val requests = listOf(mockk<TeamJoinRequest>(), mockk<TeamJoinRequest>())
        every { jpaRepository.findByTeamId(1L) } returns requests

        // when
        val result = adapter.findByTeamId(1L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(requests)
        verify(exactly = 1) { jpaRepository.findByTeamId(1L) }
    }

    @Test
    @DisplayName("findByTeamIdAndStatus - teamId와 status로 페이지 조회 시 JPA repository에 위임한다")
    fun `should delegate findByTeamIdAndStatus to jpa repository`() {
        // given
        val pageCommand = PageCommand(page = 0, size = 10)
        val request = mockk<TeamJoinRequest>()
        val page = PageImpl(listOf(request))
        every {
            jpaRepository.findByTeamIdAndStatus(1L, JoinRequestStatus.PENDING, any())
        } returns page

        // when
        val result = adapter.findByTeamIdAndStatus(1L, JoinRequestStatus.PENDING, pageCommand)

        // then
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0]).isEqualTo(request)
        verify(exactly = 1) {
            jpaRepository.findByTeamIdAndStatus(1L, JoinRequestStatus.PENDING, any())
        }
    }

    @Test
    @DisplayName("findByUserId - userId로 목록 조회 시 JPA repository에 위임한다")
    fun `should delegate findByUserId to jpa repository`() {
        // given
        val requests = listOf(mockk<TeamJoinRequest>())
        every { jpaRepository.findByUserId(10L) } returns requests

        // when
        val result = adapter.findByUserId(10L)

        // then
        assertThat(result).isEqualTo(requests)
        verify(exactly = 1) { jpaRepository.findByUserId(10L) }
    }

    @Test
    @DisplayName("findPendingByTeamIdAndUserId - 대기 중인 신청 조회 시 JPA repository에 위임한다")
    fun `should delegate findPendingByTeamIdAndUserId to jpa repository`() {
        // given
        val request = mockk<TeamJoinRequest>()
        every { jpaRepository.findPendingByTeamIdAndUserId(1L, 2L) } returns request

        // when
        val result = adapter.findPendingByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isEqualTo(request)
        verify(exactly = 1) { jpaRepository.findPendingByTeamIdAndUserId(1L, 2L) }
    }

    @Test
    @DisplayName("findPendingByTeamIdAndUserId - 대기 중인 신청이 없을 때 null을 반환한다")
    fun `should return null when no pending request exists`() {
        // given
        every { jpaRepository.findPendingByTeamIdAndUserId(1L, 2L) } returns null

        // when
        val result = adapter.findPendingByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("existsPendingByTeamIdAndUserId - 대기 중인 신청 존재 여부 확인 시 JPA repository에 위임한다")
    fun `should delegate existsPendingByTeamIdAndUserId to jpa repository`() {
        // given
        every { jpaRepository.existsPendingByTeamIdAndUserId(1L, 2L) } returns true

        // when
        val result = adapter.existsPendingByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isTrue()
        verify(exactly = 1) { jpaRepository.existsPendingByTeamIdAndUserId(1L, 2L) }
    }

    @Test
    @DisplayName("existsPendingByTeamIdAndUserId - 대기 중인 신청이 없을 때 false를 반환한다")
    fun `should return false when no pending request exists`() {
        // given
        every { jpaRepository.existsPendingByTeamIdAndUserId(1L, 2L) } returns false

        // when
        val result = adapter.existsPendingByTeamIdAndUserId(1L, 2L)

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("delete - TeamJoinRequest 삭제 시 JPA repository에 위임한다")
    fun `should delegate delete to jpa repository`() {
        // given
        val request = mockk<TeamJoinRequest>()
        justRun { jpaRepository.delete(request) }

        // when
        adapter.delete(request)

        // then
        verify(exactly = 1) { jpaRepository.delete(request) }
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
}
