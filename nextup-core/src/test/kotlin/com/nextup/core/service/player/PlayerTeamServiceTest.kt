package com.nextup.core.service.player

import com.nextup.common.exception.*
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.*
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.PlayerTeamHistoryRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PlayerTeamService")
class PlayerTeamServiceTest {
    private lateinit var playerTeamHistoryRepository: PlayerTeamHistoryRepositoryPort
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var playerTeamService: PlayerTeamService

    @BeforeEach
    fun setUp() {
        playerTeamHistoryRepository = mockk()
        playerRepository = mockk()
        teamRepository = mockk()
        playerTeamService =
            PlayerTeamService(
                playerTeamHistoryRepository,
                playerRepository,
                teamRepository,
            )
    }

    @Nested
    @DisplayName("registerAffiliation")
    inner class RegisterAffiliation {
        @Test
        fun `should register player affiliation successfully`() {
            // given
            val playerId = 1L
            val teamId = 1L
            val leagueId = 1L
            val startDate = LocalDate.now()
            val position = Position.STARTING_PITCHER
            val uniformNumber = 10

            val player = createPlayer(playerId, "홍길동")
            val team = createTeam(teamId, "서울 타이거즈", leagueId)

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { teamRepository.findByIdWithLeague(teamId) } returns team
            every { playerTeamHistoryRepository.existsActiveByPlayerIdAndLeagueId(playerId, leagueId) } returns false
            every { playerTeamHistoryRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                playerTeamService.registerAffiliation(
                    playerId = playerId,
                    teamId = teamId,
                    startDate = startDate,
                    position = position,
                    uniformNumber = uniformNumber,
                    contractType = ContractType.REGULAR,
                )

            // then
            assertThat(result.player.id).isEqualTo(playerId)
            assertThat(result.team.id).isEqualTo(teamId)
            assertThat(result.position).isEqualTo(position)
            assertThat(result.uniformNumber).isEqualTo(uniformNumber)
            assertThat(result.status).isEqualTo(PlayerTeamStatus.ACTIVE)
            verify { playerTeamHistoryRepository.save(any()) }
        }

        @Test
        fun `should throw exception when player is already active in the same league`() {
            // given
            val playerId = 1L
            val teamId = 1L
            val leagueId = 1L

            val player = createPlayer(playerId, "홍길동")
            val team = createTeam(teamId, "서울 타이거즈", leagueId)

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { teamRepository.findByIdWithLeague(teamId) } returns team
            every { playerTeamHistoryRepository.existsActiveByPlayerIdAndLeagueId(playerId, leagueId) } returns true

            // when & then
            assertThatThrownBy {
                playerTeamService.registerAffiliation(
                    playerId = playerId,
                    teamId = teamId,
                    startDate = LocalDate.now(),
                    position = Position.STARTING_PITCHER,
                )
            }.isInstanceOf(PlayerAlreadyInLeagueException::class.java)
        }

        @Test
        fun `should allow registration in different league`() {
            // given
            val playerId = 1L
            val teamId = 1L
            val leagueId = 2L

            val player = createPlayer(playerId, "홍길동")
            val team = createTeam(teamId, "경기 타이거즈", leagueId)

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { teamRepository.findByIdWithLeague(teamId) } returns team
            every { playerTeamHistoryRepository.existsActiveByPlayerIdAndLeagueId(playerId, leagueId) } returns false
            every { playerTeamHistoryRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                playerTeamService.registerAffiliation(
                    playerId = playerId,
                    teamId = teamId,
                    startDate = LocalDate.now(),
                    position = Position.STARTING_PITCHER,
                )

            // then
            assertThat(result.status).isEqualTo(PlayerTeamStatus.ACTIVE)
            verify { playerTeamHistoryRepository.save(any()) }
        }

        @Test
        fun `should throw exception when player not found`() {
            // given
            val playerId = 999L
            every { playerRepository.findByIdOrNull(playerId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.registerAffiliation(
                    playerId = playerId,
                    teamId = 1L,
                    startDate = LocalDate.now(),
                    position = Position.STARTING_PITCHER,
                )
            }.isInstanceOf(PlayerNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when team not found`() {
            // given
            val playerId = 1L
            val teamId = 999L
            val player = createPlayer(playerId, "홍길동")

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { teamRepository.findByIdWithLeague(teamId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.registerAffiliation(
                    playerId = playerId,
                    teamId = teamId,
                    startDate = LocalDate.now(),
                    position = Position.STARTING_PITCHER,
                )
            }.isInstanceOf(TeamNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("endAffiliation")
    inner class EndAffiliation {
        @Test
        fun `should end affiliation successfully`() {
            // given
            val affiliationId = 1L
            val endDate = LocalDate.now()
            val affiliation = createPlayerTeamHistory(affiliationId)

            every { playerTeamHistoryRepository.findByIdOrNull(affiliationId) } returns affiliation

            // when
            val result = playerTeamService.endAffiliation(affiliationId, endDate)

            // then
            assertThat(result.endDate).isEqualTo(endDate)
            assertThat(result.status).isEqualTo(PlayerTeamStatus.INACTIVE)
        }

        @Test
        fun `should throw exception when affiliation not found`() {
            // given
            val affiliationId = 999L
            every { playerTeamHistoryRepository.findByIdOrNull(affiliationId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.endAffiliation(affiliationId, LocalDate.now())
            }.isInstanceOf(PlayerTeamHistoryNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("transferPlayer")
    inner class TransferPlayer {
        @Test
        fun `should transfer player within same league successfully`() {
            // given
            val playerId = 1L
            val fromTeamId = 1L
            val toTeamId = 2L
            val leagueId = 1L
            val transferDate = LocalDate.now()

            val player = createPlayer(playerId, "홍길동")
            val fromTeam = createTeam(fromTeamId, "서울 타이거즈", leagueId)
            val toTeam = createTeam(toTeamId, "서울 라이온즈", leagueId)
            val currentAffiliation = createPlayerTeamHistory(1L, player, fromTeam)

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { teamRepository.findByIdWithLeague(fromTeamId) } returns fromTeam
            every { teamRepository.findByIdWithLeague(toTeamId) } returns toTeam
            every { playerTeamHistoryRepository.findActiveByPlayerIdAndLeagueId(playerId, leagueId) } returns
                currentAffiliation
            every { playerTeamHistoryRepository.save(any()) } answers { firstArg() }

            // when
            val result =
                playerTeamService.transferPlayer(
                    playerId = playerId,
                    fromTeamId = fromTeamId,
                    toTeamId = toTeamId,
                    transferDate = transferDate,
                    newPosition = Position.LEFT_FIELD,
                    newUniformNumber = 20,
                    newContractType = ContractType.REGULAR,
                )

            // then
            assertThat(result.player.id).isEqualTo(playerId)
            assertThat(result.team.id).isEqualTo(toTeamId)
            assertThat(result.status).isEqualTo(PlayerTeamStatus.ACTIVE)
            assertThat(result.position).isEqualTo(Position.LEFT_FIELD)
            assertThat(result.uniformNumber).isEqualTo(20)
            assertThat(currentAffiliation.status).isEqualTo(PlayerTeamStatus.TRANSFERRED)
            assertThat(currentAffiliation.endDate).isEqualTo(transferDate)
            verify(exactly = 1) { playerTeamHistoryRepository.save(any()) }
        }

        @Test
        fun `should throw exception when transferring to different league`() {
            // given
            val playerId = 1L
            val fromTeamId = 1L
            val toTeamId = 2L
            val fromLeagueId = 1L
            val toLeagueId = 2L

            val player = createPlayer(playerId, "홍길동")
            val fromTeam = createTeam(fromTeamId, "서울 타이거즈", fromLeagueId)
            val toTeam = createTeam(toTeamId, "경기 라이온즈", toLeagueId)

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { teamRepository.findByIdWithLeague(fromTeamId) } returns fromTeam
            every { teamRepository.findByIdWithLeague(toTeamId) } returns toTeam

            // when & then
            assertThatThrownBy {
                playerTeamService.transferPlayer(
                    playerId = playerId,
                    fromTeamId = fromTeamId,
                    toTeamId = toTeamId,
                    transferDate = LocalDate.now(),
                    newPosition = Position.LEFT_FIELD,
                )
            }.isInstanceOf(PlayerTransferNotAllowedException::class.java)
                .hasMessageContaining("같은 리그 내에서만 이적")
        }

        @Test
        fun `should throw exception when player not in from team`() {
            // given
            val playerId = 1L
            val fromTeamId = 1L
            val toTeamId = 2L
            val leagueId = 1L

            val player = createPlayer(playerId, "홍길동")
            val fromTeam = createTeam(fromTeamId, "서울 타이거즈", leagueId)
            val toTeam = createTeam(toTeamId, "서울 라이온즈", leagueId)

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { teamRepository.findByIdWithLeague(fromTeamId) } returns fromTeam
            every { teamRepository.findByIdWithLeague(toTeamId) } returns toTeam
            every { playerTeamHistoryRepository.findActiveByPlayerIdAndLeagueId(playerId, leagueId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.transferPlayer(
                    playerId = playerId,
                    fromTeamId = fromTeamId,
                    toTeamId = toTeamId,
                    transferDate = LocalDate.now(),
                    newPosition = Position.LEFT_FIELD,
                )
            }.isInstanceOf(PlayerNotInTeamException::class.java)
        }
    }

    @Nested
    @DisplayName("getActiveAffiliationsByPlayer")
    inner class GetActiveAffiliationsByPlayer {
        @Test
        fun `should return active affiliations for player`() {
            // given
            val playerId = 1L
            val player = createPlayer(playerId, "홍길동")
            val team1 = createTeam(1L, "서울 타이거즈", 1L)
            val team2 = createTeam(2L, "경기 라이온즈", 2L)
            val affiliations =
                listOf(
                    createPlayerTeamHistory(1L, player, team1),
                    createPlayerTeamHistory(2L, player, team2),
                )

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { playerTeamHistoryRepository.findActiveByPlayerId(playerId) } returns affiliations

            // when
            val result = playerTeamService.getActiveAffiliationsByPlayer(playerId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.status == PlayerTeamStatus.ACTIVE }).isTrue()
        }

        @Test
        fun `should throw exception when player not found`() {
            // given
            val playerId = 999L
            every { playerRepository.findByIdOrNull(playerId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.getActiveAffiliationsByPlayer(playerId)
            }.isInstanceOf(PlayerNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("changeUniformNumber")
    inner class ChangeUniformNumber {
        @Test
        fun `should change uniform number successfully`() {
            // given
            val affiliationId = 1L
            val newNumber = 99
            val affiliation = createPlayerTeamHistory(affiliationId)

            every { playerTeamHistoryRepository.findByIdOrNull(affiliationId) } returns affiliation

            // when
            val result = playerTeamService.changeUniformNumber(affiliationId, newNumber)

            // then
            assertThat(result.uniformNumber).isEqualTo(newNumber)
        }

        @Test
        fun `should throw exception when affiliation not found`() {
            // given
            val affiliationId = 999L
            every { playerTeamHistoryRepository.findByIdOrNull(affiliationId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.changeUniformNumber(affiliationId, 99)
            }.isInstanceOf(PlayerTeamHistoryNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("changePosition")
    inner class ChangePosition {
        @Test
        fun `should change position successfully`() {
            // given
            val affiliationId = 1L
            val newPosition = Position.CENTER_FIELD
            val affiliation = createPlayerTeamHistory(affiliationId)

            every { playerTeamHistoryRepository.findByIdOrNull(affiliationId) } returns affiliation

            // when
            val result = playerTeamService.changePosition(affiliationId, newPosition)

            // then
            assertThat(result.position).isEqualTo(newPosition)
        }

        @Test
        fun `should throw exception when affiliation not found`() {
            // given
            val affiliationId = 999L
            every { playerTeamHistoryRepository.findByIdOrNull(affiliationId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.changePosition(affiliationId, Position.CENTER_FIELD)
            }.isInstanceOf(PlayerTeamHistoryNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getTeamRoster")
    inner class GetTeamRoster {
        @Test
        fun `should return team roster`() {
            // given
            val teamId = 1L
            val team = createTeam(teamId, "서울 타이거즈", 1L)
            val player1 = createPlayer(1L, "홍길동")
            val player2 = createPlayer(2L, "김철수")
            val affiliations =
                listOf(
                    createPlayerTeamHistory(1L, player1, team),
                    createPlayerTeamHistory(2L, player2, team),
                )

            every { teamRepository.findByIdWithLeague(teamId) } returns team
            every { playerTeamHistoryRepository.findActiveByTeamId(teamId) } returns affiliations

            // when
            val result = playerTeamService.getTeamRoster(teamId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.team.id == teamId }).isTrue()
        }

        @Test
        fun `should throw exception when team not found`() {
            // given
            val teamId = 999L
            every { teamRepository.findByIdWithLeague(teamId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.getTeamRoster(teamId)
            }.isInstanceOf(TeamNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getTeamRosterAtDate")
    inner class GetTeamRosterAtDate {
        @Test
        fun `should return team roster at specific date`() {
            // given
            val teamId = 1L
            val date = LocalDate.of(2023, 6, 15)
            val team = createTeam(teamId, "서울 타이거즈", 1L)
            val player1 = createPlayer(1L, "홍길동")
            val player2 = createPlayer(2L, "김철수")
            val affiliations =
                listOf(
                    createPlayerTeamHistory(1L, player1, team),
                    createPlayerTeamHistory(2L, player2, team),
                )

            every { teamRepository.findByIdWithLeague(teamId) } returns team
            every { playerTeamHistoryRepository.findByTeamIdAtDate(teamId, date) } returns affiliations

            // when
            val result = playerTeamService.getTeamRosterAtDate(teamId, date)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.team.id == teamId }).isTrue()
        }

        @Test
        fun `should throw exception when team not found`() {
            // given
            val teamId = 999L
            every { teamRepository.findByIdWithLeague(teamId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.getTeamRosterAtDate(teamId, LocalDate.now())
            }.isInstanceOf(TeamNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("getPlayerHistory")
    inner class GetPlayerHistory {
        @Test
        fun `should return player affiliation history`() {
            // given
            val playerId = 1L
            val player = createPlayer(playerId, "홍길동")
            val team1 = createTeam(1L, "서울 타이거즈", 1L)
            val team2 = createTeam(2L, "경기 라이온즈", 2L)
            val history =
                listOf(
                    createPlayerTeamHistory(1L, player, team1),
                    createPlayerTeamHistory(2L, player, team2),
                )

            every { playerRepository.findByIdOrNull(playerId) } returns player
            every { playerTeamHistoryRepository.findByPlayerIdWithDetails(playerId) } returns history

            // when
            val result = playerTeamService.getPlayerHistory(playerId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.all { it.player.id == playerId }).isTrue()
        }

        @Test
        fun `should throw exception when player not found`() {
            // given
            val playerId = 999L
            every { playerRepository.findByIdOrNull(playerId) } returns null

            // when & then
            assertThatThrownBy {
                playerTeamService.getPlayerHistory(playerId)
            }.isInstanceOf(PlayerNotFoundException::class.java)
        }
    }

    private fun createPlayer(
        id: Long,
        name: String,
    ): Player =
        Player(
            name = name,
            primaryPosition = Position.STARTING_PITCHER,
        ).apply {
            val idField = Player::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createTeam(
        id: Long,
        name: String,
        leagueId: Long,
    ): Team {
        val league = createLeague(leagueId)
        return Team(
            league = league,
            name = name,
            city = "서울",
            foundedYear = 2020,
        ).apply {
            val idField = Team::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createLeague(id: Long): League {
        val association = createAssociation(1L)
        return League(
            association = association,
            name = "서울리그",
            foundedYear = 2020,
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }

    private fun createAssociation(id: Long): Association =
        Association(
            name = "서울협회",
            region = "서울",
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createPlayerTeamHistory(
        id: Long,
        player: Player = createPlayer(1L, "홍길동"),
        team: Team = createTeam(1L, "서울 타이거즈", 1L),
    ): PlayerTeamHistory =
        PlayerTeamHistory(
            player = player,
            team = team,
            startDate = LocalDate.now(),
            position = Position.STARTING_PITCHER,
            contractType = ContractType.REGULAR,
            status = PlayerTeamStatus.ACTIVE,
        ).apply {
            val idField = PlayerTeamHistory::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
}
