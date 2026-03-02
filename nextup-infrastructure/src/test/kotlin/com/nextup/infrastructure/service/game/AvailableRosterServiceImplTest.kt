package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.CompetitionPlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AvailableRosterServiceImplTest {
    private val gameRepository: GameRepositoryPort = mockk()
    private val teamRepository: TeamRepositoryPort = mockk()
    private val competitionPlayerRepository: CompetitionPlayerRepositoryPort = mockk()

    private lateinit var service: AvailableRosterServiceImpl

    @BeforeEach
    fun setUp() {
        service =
            AvailableRosterServiceImpl(
                gameRepository,
                teamRepository,
                competitionPlayerRepository,
            )
    }

    @Nested
    @DisplayName("getAvailableRoster")
    inner class GetAvailableRoster {
        @Test
        @DisplayName("경기의 대회에 등록된 팀 소속 선수 목록을 반환한다")
        fun returnAvailableRoster() {
            // given
            val competition = mockk<Competition>()
            every { competition.id } returns 10L

            val game = mockk<Game>()
            every { game.competition } returns competition

            val team = mockk<Team>()

            val player = mockk<Player>()
            every { player.id } returns 1L
            every { player.name } returns "홍길동"
            every { player.primaryPosition } returns Position.PITCHER
            every { player.profileImageUrl } returns "http://img.png"

            val competitionPlayer = mockk<CompetitionPlayer>()
            every { competitionPlayer.player } returns player
            every { competitionPlayer.status } returns CompetitionPlayerStatus.ACTIVE
            every { competitionPlayer.isEligible } returns true

            every { gameRepository.findByIdWithTeams(1L) } returns game
            every { teamRepository.findByIdOrNull(2L) } returns team
            every {
                competitionPlayerRepository.findByCompetitionIdAndTeamId(
                    competitionId = 10L,
                    teamId = 2L,
                )
            } returns listOf(competitionPlayer)

            // when
            val result = service.getAvailableRoster(1L, 2L)

            // then
            assertThat(result.players).hasSize(1)
            assertThat(result.players[0].playerId).isEqualTo(1L)
            assertThat(result.players[0].playerName).isEqualTo("홍길동")
            assertThat(result.players[0].primaryPosition).isEqualTo(Position.PITCHER)
            assertThat(result.players[0].profileImageUrl).isEqualTo("http://img.png")
            assertThat(result.players[0].competitionPlayerStatus)
                .isEqualTo(CompetitionPlayerStatus.ACTIVE)
            assertThat(result.players[0].isEligible).isTrue()
        }

        @Test
        @DisplayName("경기가 존재하지 않으면 GameNotFoundException을 던진다")
        fun throwWhenGameNotFound() {
            every { gameRepository.findByIdWithTeams(999L) } returns null

            assertThatThrownBy { service.getAvailableRoster(999L, 1L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        @DisplayName("팀이 존재하지 않으면 TeamNotFoundException을 던진다")
        fun throwWhenTeamNotFound() {
            val competition = mockk<Competition>()
            every { competition.id } returns 10L

            val game = mockk<Game>()
            every { game.competition } returns competition

            every { gameRepository.findByIdWithTeams(1L) } returns game
            every { teamRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy { service.getAvailableRoster(1L, 999L) }
                .isInstanceOf(TeamNotFoundException::class.java)
        }

        @Test
        @DisplayName("대회에 등록된 선수가 없으면 빈 목록을 반환한다")
        fun returnEmptyRosterWhenNoPlayers() {
            val competition = mockk<Competition>()
            every { competition.id } returns 10L

            val game = mockk<Game>()
            every { game.competition } returns competition

            val team = mockk<Team>()

            every { gameRepository.findByIdWithTeams(1L) } returns game
            every { teamRepository.findByIdOrNull(2L) } returns team
            every {
                competitionPlayerRepository.findByCompetitionIdAndTeamId(
                    competitionId = 10L,
                    teamId = 2L,
                )
            } returns emptyList()

            val result = service.getAvailableRoster(1L, 2L)

            assertThat(result.players).isEmpty()
        }

        @Test
        @DisplayName("출전 정지 선수도 목록에 포함되지만 isEligible이 false이다")
        fun suspendedPlayerIncludedButNotEligible() {
            val competition = mockk<Competition>()
            every { competition.id } returns 10L

            val game = mockk<Game>()
            every { game.competition } returns competition

            val team = mockk<Team>()

            val player = mockk<Player>()
            every { player.id } returns 1L
            every { player.name } returns "정지선수"
            every { player.primaryPosition } returns Position.CATCHER
            every { player.profileImageUrl } returns null

            val competitionPlayer = mockk<CompetitionPlayer>()
            every { competitionPlayer.player } returns player
            every { competitionPlayer.status } returns CompetitionPlayerStatus.SUSPENDED
            every { competitionPlayer.isEligible } returns false

            every { gameRepository.findByIdWithTeams(1L) } returns game
            every { teamRepository.findByIdOrNull(2L) } returns team
            every {
                competitionPlayerRepository.findByCompetitionIdAndTeamId(
                    competitionId = 10L,
                    teamId = 2L,
                )
            } returns listOf(competitionPlayer)

            val result = service.getAvailableRoster(1L, 2L)

            assertThat(result.players).hasSize(1)
            assertThat(result.players[0].isEligible).isFalse()
            assertThat(result.players[0].competitionPlayerStatus)
                .isEqualTo(CompetitionPlayerStatus.SUSPENDED)
        }

        @Test
        @DisplayName("여러 선수를 정확히 매핑하여 반환한다")
        fun returnMultiplePlayers() {
            val competition = mockk<Competition>()
            every { competition.id } returns 10L

            val game = mockk<Game>()
            every { game.competition } returns competition

            val team = mockk<Team>()

            val players =
                listOf(
                    createMockCompetitionPlayer(1L, "선수1", Position.PITCHER, true),
                    createMockCompetitionPlayer(2L, "선수2", Position.CATCHER, true),
                    createMockCompetitionPlayer(3L, "선수3", Position.FIRST_BASE, false),
                )

            every { gameRepository.findByIdWithTeams(1L) } returns game
            every { teamRepository.findByIdOrNull(2L) } returns team
            every {
                competitionPlayerRepository.findByCompetitionIdAndTeamId(
                    competitionId = 10L,
                    teamId = 2L,
                )
            } returns players

            val result = service.getAvailableRoster(1L, 2L)

            assertThat(result.players).hasSize(3)
            assertThat(result.players[0].playerName).isEqualTo("선수1")
            assertThat(result.players[1].playerName).isEqualTo("선수2")
            assertThat(result.players[2].playerName).isEqualTo("선수3")
            assertThat(result.players[2].isEligible).isFalse()
        }

        private fun createMockCompetitionPlayer(
            playerId: Long,
            playerName: String,
            position: Position,
            eligible: Boolean,
        ): CompetitionPlayer {
            val player = mockk<Player>()
            every { player.id } returns playerId
            every { player.name } returns playerName
            every { player.primaryPosition } returns position
            every { player.profileImageUrl } returns null

            val cp = mockk<CompetitionPlayer>()
            every { cp.player } returns player
            every { cp.status } returns
                if (eligible) CompetitionPlayerStatus.ACTIVE else CompetitionPlayerStatus.SUSPENDED
            every { cp.isEligible } returns eligible
            return cp
        }
    }
}
