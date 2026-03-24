package com.nextup.infrastructure.service.discipline

import com.nextup.common.exception.PlayerBanNotFoundException
import com.nextup.core.domain.discipline.PlayerBan
import com.nextup.core.port.repository.PlayerBanRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("PlayerBanServiceImpl 테스트")
class PlayerBanServiceImplTest {
    private lateinit var playerBanRepository: PlayerBanRepositoryPort
    private lateinit var playerBanService: PlayerBanServiceImpl

    @BeforeEach
    fun setUp() {
        playerBanRepository = mockk()
        playerBanService = PlayerBanServiceImpl(playerBanRepository)
    }

    private fun createBan(
        id: Long = 1L,
        playerId: Long = 10L,
        competitionId: Long = 100L,
        reason: String = "폭력 행위",
        issuedBy: String = "관리자",
    ): PlayerBan {
        val ban =
            PlayerBan.create(
                playerId = playerId,
                competitionId = competitionId,
                reason = reason,
                issuedBy = issuedBy,
            )
        // set id via reflection
        val idField = PlayerBan::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(ban, id)
        return ban
    }

    @Nested
    @DisplayName("issueBan")
    inner class IssueBan {
        @Test
        fun `제재를 정상적으로 발급할 수 있다`() {
            // given
            every { playerBanRepository.save(any()) } returnsArgument 0

            // when
            val result =
                playerBanService.issueBan(
                    playerId = 10L,
                    competitionId = 100L,
                    reason = "폭력 행위",
                    issuedBy = "관리자",
                )

            // then
            assertThat(result.playerId).isEqualTo(10L)
            assertThat(result.competitionId).isEqualTo(100L)
            assertThat(result.reason).isEqualTo("폭력 행위")
            assertThat(result.issuedBy).isEqualTo("관리자")
            verify(exactly = 1) { playerBanRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `ID로 제재를 조회할 수 있다`() {
            // given
            val ban = createBan(id = 1L)
            every { playerBanRepository.findByIdOrNull(1L) } returns ban

            // when
            val result = playerBanService.getById(1L)

            // then
            assertThat(result.playerId).isEqualTo(10L)
        }

        @Test
        fun `존재하지 않는 ID 조회 시 PlayerBanNotFoundException 발생`() {
            // given
            every { playerBanRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<PlayerBanNotFoundException> {
                playerBanService.getById(999L)
            }
        }
    }

    @Nested
    @DisplayName("getAll")
    inner class GetAll {
        @Test
        fun `모든 제재를 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(id = 1L), createBan(id = 2L, playerId = 20L))
            every { playerBanRepository.findAll() } returns bans

            // when
            val result = playerBanService.getAll()

            // then
            assertThat(result).hasSize(2)
        }
    }

    @Nested
    @DisplayName("getBansByPlayer")
    inner class GetBansByPlayer {
        @Test
        fun `선수 ID로 제재를 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(playerId = 10L))
            every { playerBanRepository.findByPlayerId(10L) } returns bans

            // when
            val result = playerBanService.getBansByPlayer(10L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].playerId).isEqualTo(10L)
        }
    }

    @Nested
    @DisplayName("getBansByCompetition")
    inner class GetBansByCompetition {
        @Test
        fun `대회 ID로 제재를 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(competitionId = 100L))
            every { playerBanRepository.findByCompetitionId(100L) } returns bans

            // when
            val result = playerBanService.getBansByCompetition(100L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].competitionId).isEqualTo(100L)
        }
    }

    @Nested
    @DisplayName("getBansByPlayerAndCompetition")
    inner class GetBansByPlayerAndCompetition {
        @Test
        fun `선수 ID와 대회 ID로 제재를 조회할 수 있다`() {
            // given
            val bans = listOf(createBan(playerId = 10L, competitionId = 100L))
            every {
                playerBanRepository.findByPlayerIdAndCompetitionId(10L, 100L)
            } returns bans

            // when
            val result = playerBanService.getBansByPlayerAndCompetition(10L, 100L)

            // then
            assertThat(result).hasSize(1)
        }
    }

    @Nested
    @DisplayName("canPlayerPlay")
    inner class CanPlayerPlay {
        @Test
        fun `제재가 없는 선수는 출장 가능하다`() {
            // given
            every {
                playerBanRepository.existsByPlayerIdAndCompetitionId(10L, 100L)
            } returns false

            // when
            val result = playerBanService.canPlayerPlay(10L, 100L)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `제재가 있는 선수는 출장 불가능하다`() {
            // given
            every {
                playerBanRepository.existsByPlayerIdAndCompetitionId(10L, 100L)
            } returns true

            // when
            val result = playerBanService.canPlayerPlay(10L, 100L)

            // then
            assertThat(result).isFalse()
        }
    }

    @Nested
    @DisplayName("deleteBan")
    inner class DeleteBan {
        @Test
        fun `제재를 정상적으로 삭제할 수 있다`() {
            // given
            val ban = createBan(id = 1L)
            every { playerBanRepository.findByIdOrNull(1L) } returns ban
            every { playerBanRepository.delete(ban) } returns Unit

            // when
            playerBanService.deleteBan(1L)

            // then
            verify(exactly = 1) { playerBanRepository.delete(ban) }
        }

        @Test
        fun `존재하지 않는 제재 삭제 시 PlayerBanNotFoundException 발생`() {
            // given
            every { playerBanRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThrows<PlayerBanNotFoundException> {
                playerBanService.deleteBan(999L)
            }
        }
    }
}
