package com.nextup.infrastructure.service.attendance

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.team.TeamMember
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.math.RoundingMode

@DisplayName("ActivityServiceImpl 테스트")
class ActivityServiceImplTest {
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var teamMemberRepository: TeamMemberRepositoryPort
    private lateinit var gameRepositoryPort: GameRepositoryPort
    private lateinit var gamePlayerRepositoryPort: GamePlayerRepositoryPort
    private lateinit var activityService: ActivityServiceImpl

    @BeforeEach
    fun setUp() {
        teamRepository = mockk()
        teamMemberRepository = mockk()
        gameRepositoryPort = mockk()
        gamePlayerRepositoryPort = mockk()
        activityService =
            ActivityServiceImpl(
                teamRepository,
                teamMemberRepository,
                gameRepositoryPort,
                gamePlayerRepositoryPort,
            )
    }

    private fun createMockGame(id: Long): Game {
        val game = mockk<Game>(relaxed = true)
        every { game.id } returns id
        return game
    }

    private fun createMockGamePlayer(
        playerId: Long,
        gameId: Long,
    ): GamePlayer {
        val game = mockk<Game>(relaxed = true)
        every { game.id } returns gameId
        val gameTeam = mockk<GameTeam>(relaxed = true)
        every { gameTeam.game } returns game
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.gameTeam } returns gameTeam
        return gamePlayer
    }

    private fun createMockTeamMember(
        playerId: Long,
        playerName: String,
    ): TeamMember {
        val player = mockk<Player>(relaxed = true)
        every { player.id } returns playerId
        every { player.name } returns playerName
        val member = mockk<TeamMember>(relaxed = true)
        every { member.player } returns player
        return member
    }

    private fun mockTeamGames(
        teamId: Long,
        gameIds: List<Long>,
    ) {
        val games = gameIds.map { createMockGame(it) }
        every {
            gameRepositoryPort.findGames(
                date = null,
                teamId = teamId,
                competitionId = null,
                status = null,
                pageCommand = any<PageCommand>(),
            )
        } returns
            PageResult(
                content = games,
                page = 0,
                size = 10000,
                totalElements = games.size.toLong(),
                totalPages = 1,
            )
    }

    @Nested
    @DisplayName("getGameParticipationRate")
    inner class GetGameParticipationRate {
        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException 발생`() {
            // given
            every { teamRepository.existsById(999L) } returns false

            // when & then
            assertThrows<TeamNotFoundException> {
                activityService.getGameParticipationRate(999L, 1L)
            }
        }

        @Test
        fun `팀의 경기가 없으면 0을 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, emptyList())

            // when
            val result = activityService.getGameParticipationRate(1L, 1L)

            // then
            assertThat(result).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        }

        @Test
        fun `선수가 모든 경기에 참여한 경우 100을 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, listOf(1L, 2L, 3L))
            every { gamePlayerRepositoryPort.findAllByPlayerId(10L) } returns
                listOf(
                    createMockGamePlayer(10L, 1L),
                    createMockGamePlayer(10L, 2L),
                    createMockGamePlayer(10L, 3L),
                )

            // when
            val result = activityService.getGameParticipationRate(1L, 10L)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("100.00"))
        }

        @Test
        fun `선수가 일부 경기에만 참여한 경우 정확한 비율을 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, listOf(1L, 2L, 3L))
            every { gamePlayerRepositoryPort.findAllByPlayerId(10L) } returns
                listOf(
                    createMockGamePlayer(10L, 1L),
                    createMockGamePlayer(10L, 3L),
                )

            // when
            val result = activityService.getGameParticipationRate(1L, 10L)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("66.67"))
        }

        @Test
        fun `선수가 경기에 전혀 참여하지 않은 경우 0을 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, listOf(1L, 2L))
            every { gamePlayerRepositoryPort.findAllByPlayerId(10L) } returns emptyList()

            // when
            val result = activityService.getGameParticipationRate(1L, 10L)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `선수가 다른 팀 경기에만 참여한 경우 0을 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, listOf(1L, 2L))
            // 선수는 게임 ID 100, 200에만 참여 (다른 팀의 경기)
            every { gamePlayerRepositoryPort.findAllByPlayerId(10L) } returns
                listOf(
                    createMockGamePlayer(10L, 100L),
                    createMockGamePlayer(10L, 200L),
                )

            // when
            val result = activityService.getGameParticipationRate(1L, 10L)

            // then
            assertThat(result).isEqualByComparingTo(BigDecimal("0.00"))
        }
    }

    @Nested
    @DisplayName("listGameParticipationRates")
    inner class ListGameParticipationRates {
        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException 발생`() {
            // given
            every { teamRepository.existsById(999L) } returns false

            // when & then
            assertThrows<TeamNotFoundException> {
                activityService.listGameParticipationRates(999L)
            }
        }

        @Test
        fun `팀의 경기가 없으면 빈 목록을 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, emptyList())

            // when
            val result = activityService.listGameParticipationRates(1L)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        fun `팀의 모든 멤버별 경기참여율을 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, listOf(1L, 2L, 3L, 4L))

            val member1 = createMockTeamMember(10L, "홍길동")
            val member2 = createMockTeamMember(20L, "김철수")
            every { teamMemberRepository.findByTeamId(1L) } returns listOf(member1, member2)

            // 홍길동: 4경기 중 3경기 참여 (75%)
            every { gamePlayerRepositoryPort.findAllByPlayerId(10L) } returns
                listOf(
                    createMockGamePlayer(10L, 1L),
                    createMockGamePlayer(10L, 2L),
                    createMockGamePlayer(10L, 4L),
                )
            // 김철수: 4경기 중 1경기 참여 (25%)
            every { gamePlayerRepositoryPort.findAllByPlayerId(20L) } returns
                listOf(
                    createMockGamePlayer(20L, 3L),
                )

            // when
            val result = activityService.listGameParticipationRates(1L)

            // then
            assertThat(result).hasSize(2)

            val rate1 = result.find { it.playerId == 10L }!!
            assertThat(rate1.playerName).isEqualTo("홍길동")
            assertThat(rate1.gamesPlayed).isEqualTo(3)
            assertThat(rate1.totalTeamGames).isEqualTo(4)
            assertThat(rate1.participationRate).isEqualByComparingTo(BigDecimal("75.00"))

            val rate2 = result.find { it.playerId == 20L }!!
            assertThat(rate2.playerName).isEqualTo("김철수")
            assertThat(rate2.gamesPlayed).isEqualTo(1)
            assertThat(rate2.totalTeamGames).isEqualTo(4)
            assertThat(rate2.participationRate).isEqualByComparingTo(BigDecimal("25.00"))
        }

        @Test
        fun `경기에 참여하지 않은 멤버는 0%를 반환한다`() {
            // given
            every { teamRepository.existsById(1L) } returns true
            mockTeamGames(1L, listOf(1L))

            val member = createMockTeamMember(10L, "미참여선수")
            every { teamMemberRepository.findByTeamId(1L) } returns listOf(member)
            every { gamePlayerRepositoryPort.findAllByPlayerId(10L) } returns emptyList()

            // when
            val result = activityService.listGameParticipationRates(1L)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].gamesPlayed).isEqualTo(0)
            assertThat(result[0].participationRate).isEqualByComparingTo(BigDecimal("0.00"))
        }
    }
}
