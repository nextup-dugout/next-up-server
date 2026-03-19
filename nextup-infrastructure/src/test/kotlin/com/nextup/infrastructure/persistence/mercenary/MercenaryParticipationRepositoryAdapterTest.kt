package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryParticipation
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("MercenaryParticipationRepositoryAdapter 테스트")
class MercenaryParticipationRepositoryAdapterTest {
    private lateinit var jpaRepository: MercenaryParticipationJpaRepository
    private lateinit var adapter: MercenaryParticipationRepositoryAdapter

    private lateinit var participation: MercenaryParticipation

    @BeforeEach
    fun setUp() {
        jpaRepository = mockk()
        adapter = MercenaryParticipationRepositoryAdapter(jpaRepository)

        participation =
            MercenaryParticipation.create(
                gameId = 100L,
                playerId = 50L,
                teamId = 10L,
            )
    }

    @Nested
    @DisplayName("save")
    inner class Save {
        @Test
        fun `용병 참가 기록을 저장한다`() {
            // given
            every { jpaRepository.save(participation) } returns participation

            // when
            val result = adapter.save(participation)

            // then
            assertThat(result).isEqualTo(participation)
            verify { jpaRepository.save(participation) }
        }
    }

    @Nested
    @DisplayName("findByGameId")
    inner class FindByGameId {
        @Test
        fun `경기 ID로 참가 목록을 조회한다`() {
            // given
            every { jpaRepository.findByGameId(100L) } returns listOf(participation)

            // when
            val result = adapter.findByGameId(100L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(participation)
            verify { jpaRepository.findByGameId(100L) }
        }

        @Test
        fun `참가 기록이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByGameId(999L) } returns emptyList()

            // when
            val result = adapter.findByGameId(999L)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByGameId(999L) }
        }
    }

    @Nested
    @DisplayName("findByPlayerId")
    inner class FindByPlayerId {
        @Test
        fun `선수 ID로 참가 이력을 조회한다`() {
            // given
            every { jpaRepository.findByPlayerId(50L) } returns listOf(participation)

            // when
            val result = adapter.findByPlayerId(50L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(participation)
            verify { jpaRepository.findByPlayerId(50L) }
        }

        @Test
        fun `참가 이력이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByPlayerId(999L) } returns emptyList()

            // when
            val result = adapter.findByPlayerId(999L)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByPlayerId(999L) }
        }
    }

    @Nested
    @DisplayName("findByTeamId")
    inner class FindByTeamId {
        @Test
        fun `팀 ID로 참가 목록을 조회한다`() {
            // given
            every { jpaRepository.findByTeamId(10L) } returns listOf(participation)

            // when
            val result = adapter.findByTeamId(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0]).isEqualTo(participation)
            verify { jpaRepository.findByTeamId(10L) }
        }

        @Test
        fun `참가 기록이 없으면 빈 목록을 반환한다`() {
            // given
            every { jpaRepository.findByTeamId(999L) } returns emptyList()

            // when
            val result = adapter.findByTeamId(999L)

            // then
            assertThat(result).isEmpty()
            verify { jpaRepository.findByTeamId(999L) }
        }
    }

    @Nested
    @DisplayName("existsByGameIdAndPlayerId")
    inner class ExistsByGameIdAndPlayerId {
        @Test
        fun `해당 경기에 선수가 이미 참가한 경우 true를 반환한다`() {
            // given
            every { jpaRepository.existsByGameIdAndPlayerId(100L, 50L) } returns true

            // when
            val result = adapter.existsByGameIdAndPlayerId(100L, 50L)

            // then
            assertThat(result).isTrue()
            verify { jpaRepository.existsByGameIdAndPlayerId(100L, 50L) }
        }

        @Test
        fun `해당 경기에 선수가 참가하지 않은 경우 false를 반환한다`() {
            // given
            every { jpaRepository.existsByGameIdAndPlayerId(100L, 999L) } returns false

            // when
            val result = adapter.existsByGameIdAndPlayerId(100L, 999L)

            // then
            assertThat(result).isFalse()
            verify { jpaRepository.existsByGameIdAndPlayerId(100L, 999L) }
        }
    }
}
