package com.nextup.infrastructure.repository

import com.nextup.core.domain.game.AttendanceStatus
import com.nextup.core.domain.game.GameParticipation
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull

@DisplayName("GameAttendanceVoteRepositoryAdapter 테스트")
class GameAttendanceVoteRepositoryAdapterTest {

    private val jpaRepository: AttendanceVoteRepository = mockk()
    private lateinit var adapter: GameAttendanceVoteRepositoryAdapter

    @BeforeEach
    fun setUp() {
        adapter = GameAttendanceVoteRepositoryAdapter(jpaRepository)
    }

    @Test
    @DisplayName("save - GameParticipation 저장 시 JPA repository에 위임한다")
    fun `should delegate save to jpa repository`() {
        // given
        val vote = mockk<GameParticipation>()
        every { jpaRepository.save(vote) } returns vote

        // when
        val result = adapter.save(vote)

        // then
        assertThat(result).isEqualTo(vote)
        verify(exactly = 1) { jpaRepository.save(vote) }
    }

    @Test
    @DisplayName("saveAll - 여러 GameParticipation 저장 시 JPA repository에 위임한다")
    fun `should delegate saveAll to jpa repository`() {
        // given
        val votes = listOf(mockk<GameParticipation>(), mockk<GameParticipation>())
        every { jpaRepository.saveAll(votes) } returns votes

        // when
        val result = adapter.saveAll(votes)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(votes)
        verify(exactly = 1) { jpaRepository.saveAll(votes) }
    }

    @Test
    @DisplayName("findByIdOrNull - ID로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByIdOrNull to jpa repository`() {
        // given
        val vote = mockk<GameParticipation>()
        every { jpaRepository.findByIdOrNull(1L) } returns vote

        // when
        val result = adapter.findByIdOrNull(1L)

        // then
        assertThat(result).isEqualTo(vote)
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
    @DisplayName("findByGameId - gameId로 투표 목록 조회 시 JPA repository에 위임한다")
    fun `should delegate findByGameId to jpa repository`() {
        // given
        val votes = listOf(mockk<GameParticipation>(), mockk<GameParticipation>())
        every { jpaRepository.findByGameId(1L) } returns votes

        // when
        val result = adapter.findByGameId(1L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(votes)
        verify(exactly = 1) { jpaRepository.findByGameId(1L) }
    }

    @Test
    @DisplayName("findByGameIdAndMemberId - gameId와 memberId로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByGameIdAndMemberId to jpa repository`() {
        // given
        val vote = mockk<GameParticipation>()
        every { jpaRepository.findByGameIdAndMemberId(1L, 2L) } returns vote

        // when
        val result = adapter.findByGameIdAndMemberId(1L, 2L)

        // then
        assertThat(result).isEqualTo(vote)
        verify(exactly = 1) { jpaRepository.findByGameIdAndMemberId(1L, 2L) }
    }

    @Test
    @DisplayName("findByGameIdAndMemberId - 투표가 없을 때 null을 반환한다")
    fun `should return null when vote not found`() {
        // given
        every { jpaRepository.findByGameIdAndMemberId(1L, 2L) } returns null

        // when
        val result = adapter.findByGameIdAndMemberId(1L, 2L)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("findByGameIdAndStatus - gameId와 status로 조회 시 JPA repository에 위임한다")
    fun `should delegate findByGameIdAndStatus to jpa repository`() {
        // given
        val votes = listOf(mockk<GameParticipation>())
        every { jpaRepository.findByGameIdAndStatus(1L, AttendanceStatus.ATTENDING) } returns votes

        // when
        val result = adapter.findByGameIdAndStatus(1L, AttendanceStatus.ATTENDING)

        // then
        assertThat(result).hasSize(1)
        assertThat(result).isEqualTo(votes)
        verify(exactly = 1) { jpaRepository.findByGameIdAndStatus(1L, AttendanceStatus.ATTENDING) }
    }

    @Test
    @DisplayName("findNonVotersByGameId - 미투표자 조회 시 JPA repository에 위임한다")
    fun `should delegate findNonVotersByGameId to jpa repository`() {
        // given
        val votes = listOf(mockk<GameParticipation>(), mockk<GameParticipation>())
        every { jpaRepository.findNonVotersByGameId(1L) } returns votes

        // when
        val result = adapter.findNonVotersByGameId(1L)

        // then
        assertThat(result).hasSize(2)
        assertThat(result).isEqualTo(votes)
        verify(exactly = 1) { jpaRepository.findNonVotersByGameId(1L) }
    }

    @Test
    @DisplayName("countByGameId - gameId로 투표 수 조회 시 JPA repository에 위임한다")
    fun `should delegate countByGameId to jpa repository`() {
        // given
        every { jpaRepository.countByGameId(1L) } returns 10L

        // when
        val result = adapter.countByGameId(1L)

        // then
        assertThat(result).isEqualTo(10L)
        verify(exactly = 1) { jpaRepository.countByGameId(1L) }
    }

    @Test
    @DisplayName("countByGameIdAndStatus - gameId와 status로 투표 수 조회 시 JPA repository에 위임한다")
    fun `should delegate countByGameIdAndStatus to jpa repository`() {
        // given
        every { jpaRepository.countByGameIdAndStatus(1L, AttendanceStatus.ATTENDING) } returns 7L

        // when
        val result = adapter.countByGameIdAndStatus(1L, AttendanceStatus.ATTENDING)

        // then
        assertThat(result).isEqualTo(7L)
        verify(exactly = 1) { jpaRepository.countByGameIdAndStatus(1L, AttendanceStatus.ATTENDING) }
    }

    @Test
    @DisplayName("delete - GameParticipation 삭제 시 JPA repository에 위임한다")
    fun `should delegate delete to jpa repository`() {
        // given
        val vote = mockk<GameParticipation>()
        justRun { jpaRepository.delete(vote) }

        // when
        adapter.delete(vote)

        // then
        verify(exactly = 1) { jpaRepository.delete(vote) }
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
