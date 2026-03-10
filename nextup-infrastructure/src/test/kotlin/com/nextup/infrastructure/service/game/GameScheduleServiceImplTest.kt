package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
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
import java.time.LocalDateTime
import java.time.LocalTime

@DisplayName("GameScheduleServiceImpl 테스트")
class GameScheduleServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var service: GameScheduleServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameTeamRepository = mockk()
        service = GameScheduleServiceImpl(gameRepository, gameTeamRepository)
    }

    @Nested
    @DisplayName("getGames")
    inner class GetGamesTest {
        @Test
        @DisplayName("competitionId 필터로 경기를 조회한다")
        fun getGamesWithCompetitionId() {
            // given
            val competitionId = 1L
            val games = listOf(createGame(1L), createGame(2L))
            val gameTeams = listOf(createGameTeam(1L), createGameTeam(2L))

            every { gameRepository.findByCompetitionId(competitionId) } returns games
            every { gameTeamRepository.findAllByGameIds(any()) } returns gameTeams

            // when
            val result =
                service.getGames(
                    date = null,
                    teamId = null,
                    competitionId = competitionId,
                    page = 0,
                    size = 10
                )

            // then
            assertThat(result).hasSize(2)
            verify { gameRepository.findByCompetitionId(competitionId) }
        }

        @Test
        @DisplayName("date 필터로 경기를 조회한다")
        fun getGamesWithDate() {
            // given
            val date = LocalDate.of(2026, 2, 9)
            val start = date.atStartOfDay()
            val end = date.atTime(LocalTime.MAX)
            val games = listOf(createGame(1L))
            val gameTeams = listOf(createGameTeam(1L))

            every { gameRepository.findByScheduledAtBetween(start, end) } returns games
            every { gameTeamRepository.findAllByGameIds(any()) } returns gameTeams

            // when
            val result = service.getGames(date = date, teamId = null, competitionId = null, page = 0, size = 10)

            // then
            assertThat(result).hasSize(1)
            verify { gameRepository.findByScheduledAtBetween(start, end) }
        }

        @Test
        @DisplayName("필터 없이 모든 경기를 조회한다")
        fun getGamesWithoutFilter() {
            // given
            val games = listOf(createGame(1L), createGame(2L), createGame(3L))
            val gameTeams = listOf(createGameTeam(1L), createGameTeam(2L), createGameTeam(3L))

            every { gameRepository.findAll() } returns games
            every { gameTeamRepository.findAllByGameIds(any()) } returns gameTeams

            // when
            val result = service.getGames(date = null, teamId = null, competitionId = null, page = 0, size = 10)

            // then
            assertThat(result).hasSize(3)
            verify { gameRepository.findAll() }
        }

        @Test
        @DisplayName("teamId 필터를 추가로 적용한다")
        fun getGamesWithTeamIdFilter() {
            // given
            val teamId = 10L
            val games = listOf(createGame(1L), createGame(2L), createGame(3L))
            val teamGames =
                listOf(
                    createGameTeam(1L, teamId = teamId),
                    createGameTeam(3L, teamId = teamId),
                )

            every { gameRepository.findAll() } returns games
            every { gameTeamRepository.findAllByTeamId(teamId) } returns teamGames
            every { gameTeamRepository.findAllByGameIds(listOf(1L, 3L)) } returns teamGames

            // when
            val result = service.getGames(date = null, teamId = teamId, competitionId = null, page = 0, size = 10)

            // then
            assertThat(result).hasSize(2)
            assertThat(result.map { it.gameId }).containsExactlyInAnyOrder(1L, 3L)
            verify { gameTeamRepository.findAllByTeamId(teamId) }
        }

        @Test
        @DisplayName("페이징이 적용된다")
        fun getGamesWithPaging() {
            // given
            val games = (1L..10L).map { createGame(it) }
            val gameTeams = (1L..10L).map { createGameTeam(it) }

            every { gameRepository.findAll() } returns games
            every { gameTeamRepository.findAllByGameIds(any()) } returns gameTeams

            // when
            val result = service.getGames(date = null, teamId = null, competitionId = null, page = 1, size = 3)

            // then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.gameId }).containsExactly(4L, 5L, 6L)
        }

        @Test
        @DisplayName("빈 리스트를 반환한다")
        fun getGamesReturnsEmptyList() {
            // given
            every { gameRepository.findAll() } returns emptyList()

            // when
            val result = service.getGames(date = null, teamId = null, competitionId = null, page = 0, size = 10)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("getGameDetail")
    inner class GetGameDetailTest {
        @Test
        @DisplayName("경기 상세 정보를 조회한다")
        fun getGameDetailSuccess() {
            // given
            val gameId = 1L
            val game = createGame(gameId)
            val homeTeam = createGameTeam(gameId, homeAway = HomeAway.HOME)
            val awayTeam = createGameTeam(gameId, homeAway = HomeAway.AWAY)

            every { gameRepository.findByIdWithTeams(gameId) } returns game
            every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam, awayTeam)

            // when
            val result = service.getGameDetail(gameId)

            // then
            assertThat(result.gameId).isEqualTo(gameId)
            assertThat(result.homeTeamId).isEqualTo(homeTeam.team.id)
            assertThat(result.awayTeamId).isEqualTo(awayTeam.team.id)
            assertThat(result.homeScore).isEqualTo(homeTeam.totalScore)
            assertThat(result.awayScore).isEqualTo(awayTeam.totalScore)
        }

        @Test
        @DisplayName("경기를 찾지 못하면 GameNotFoundException을 던진다")
        fun getGameDetailThrowsException() {
            // given
            val gameId = 999L
            every { gameRepository.findByIdWithTeams(gameId) } returns null

            // when & then
            assertThatThrownBy { service.getGameDetail(gameId) }
                .isInstanceOf(GameNotFoundException::class.java)
                .hasMessageContaining(gameId.toString())
        }
    }

    @Nested
    @DisplayName("getGamesByTeam")
    inner class GetGamesByTeamTest {
        @Test
        @DisplayName("팀의 경기 목록을 scheduledAt 순으로 정렬하여 반환한다")
        fun getGamesByTeamReturnsSortedGames() {
            // given
            val teamId = 1L
            val game1 = createGame(1L, scheduledAt = LocalDateTime.of(2026, 2, 10, 14, 0))
            val game2 = createGame(2L, scheduledAt = LocalDateTime.of(2026, 2, 9, 14, 0))
            val game3 = createGame(3L, scheduledAt = LocalDateTime.of(2026, 2, 11, 14, 0))

            val gameTeams =
                listOf(
                    createGameTeam(1L, teamId = teamId),
                    createGameTeam(2L, teamId = teamId),
                    createGameTeam(3L, teamId = teamId),
                )

            every { gameTeamRepository.findAllByTeamId(teamId) } returns gameTeams
            every { gameRepository.findAllByIds(listOf(1L, 2L, 3L)) } returns listOf(game1, game2, game3)
            every { gameTeamRepository.findAllByGameIds(any()) } returns gameTeams

            // when
            val result = service.getGamesByTeam(teamId)

            // then
            assertThat(result).hasSize(3)
            assertThat(result.map { it.gameId }).containsExactly(2L, 1L, 3L)
        }
    }

    @Nested
    @DisplayName("getUpcomingGamesByTeam")
    inner class GetUpcomingGamesByTeamTest {
        @Test
        @DisplayName("미래의 SCHEDULED 경기만 필터링한다")
        fun getUpcomingGamesFiltersCorrectly() {
            // given
            val teamId = 1L
            val now = LocalDateTime.now()
            val futureScheduled = createGame(1L, scheduledAt = now.plusDays(1), status = GameStatus.SCHEDULED)
            val futureInProgress = createGame(2L, scheduledAt = now.plusDays(2), status = GameStatus.IN_PROGRESS)
            val pastScheduled = createGame(3L, scheduledAt = now.minusDays(1), status = GameStatus.SCHEDULED)

            val gameTeams =
                listOf(
                    createGameTeam(1L, teamId = teamId),
                    createGameTeam(2L, teamId = teamId),
                    createGameTeam(3L, teamId = teamId),
                )

            every { gameTeamRepository.findAllByTeamId(teamId) } returns gameTeams
            every { gameRepository.findAllByIds(listOf(1L, 2L, 3L)) } returns
                listOf(futureScheduled, futureInProgress, pastScheduled)
            every { gameTeamRepository.findAllByGameIds(listOf(1L)) } returns listOf(gameTeams[0])

            // when
            val result = service.getUpcomingGamesByTeam(teamId, limit = 10)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].gameId).isEqualTo(1L)
        }

        @Test
        @DisplayName("limit을 적용한다")
        fun getUpcomingGamesRespectsLimit() {
            // given
            val teamId = 1L
            val now = LocalDateTime.now()
            val games = (1L..5L).map { createGame(it, scheduledAt = now.plusDays(it), status = GameStatus.SCHEDULED) }
            val gameTeams = (1L..5L).map { createGameTeam(it, teamId = teamId) }

            every { gameTeamRepository.findAllByTeamId(teamId) } returns gameTeams
            every { gameRepository.findAllByIds(any()) } returns games
            every { gameTeamRepository.findAllByGameIds(listOf(1L, 2L, 3L)) } returns gameTeams.take(3)

            // when
            val result = service.getUpcomingGamesByTeam(teamId, limit = 3)

            // then
            assertThat(result).hasSize(3)
        }
    }

    @Nested
    @DisplayName("getUpcomingGamesByTeamIds")
    inner class GetUpcomingGamesByTeamIdsTest {
        @Test
        @DisplayName("여러 팀의 다가오는 경기를 통합 조회한다")
        fun getUpcomingGamesByTeamIdsReturnsGamesFromMultipleTeams() {
            // given
            val teamIds = listOf(1L, 2L)
            val now = LocalDateTime.now()
            val game1 = createGame(10L, scheduledAt = now.plusDays(1), status = GameStatus.SCHEDULED)
            val game2 = createGame(20L, scheduledAt = now.plusDays(3), status = GameStatus.SCHEDULED)

            val gameTeam1 = createGameTeam(10L, teamId = 1L)
            val gameTeam2 = createGameTeam(20L, teamId = 2L)

            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(gameTeam1)
            every { gameTeamRepository.findAllByTeamId(2L) } returns listOf(gameTeam2)
            every { gameRepository.findAllByIds(listOf(10L, 20L)) } returns listOf(game1, game2)
            every { gameTeamRepository.findAllByGameIds(listOf(10L, 20L)) } returns listOf(gameTeam1, gameTeam2)

            // when
            val result = service.getUpcomingGamesByTeamIds(teamIds, limit = 10)

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0].gameId).isEqualTo(10L)
            assertThat(result[1].gameId).isEqualTo(20L)
        }

        @Test
        @DisplayName("빈 팀 목록이면 빈 리스트를 반환한다")
        fun getUpcomingGamesByTeamIdsReturnsEmptyForEmptyTeamIds() {
            // given & when
            val result = service.getUpcomingGamesByTeamIds(emptyList(), limit = 10)

            // then
            assertThat(result).isEmpty()
        }

        @Test
        @DisplayName("중복 경기를 제거한다")
        fun getUpcomingGamesByTeamIdsDeduplicatesGames() {
            // given
            val teamIds = listOf(1L, 2L)
            val now = LocalDateTime.now()
            // 같은 경기에 두 팀이 모두 참여하는 경우
            val game = createGame(10L, scheduledAt = now.plusDays(1), status = GameStatus.SCHEDULED)

            val gameTeam1 = createGameTeam(10L, teamId = 1L, homeAway = HomeAway.HOME)
            val gameTeam2 = createGameTeam(10L, teamId = 2L, homeAway = HomeAway.AWAY)

            every { gameTeamRepository.findAllByTeamId(1L) } returns listOf(gameTeam1)
            every { gameTeamRepository.findAllByTeamId(2L) } returns listOf(gameTeam2)
            every { gameRepository.findAllByIds(listOf(10L)) } returns listOf(game)
            every { gameTeamRepository.findAllByGameIds(listOf(10L)) } returns listOf(gameTeam1, gameTeam2)

            // when
            val result = service.getUpcomingGamesByTeamIds(teamIds, limit = 10)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].gameId).isEqualTo(10L)
        }

        @Test
        @DisplayName("limit을 적용한다")
        fun getUpcomingGamesByTeamIdsRespectsLimit() {
            // given
            val teamIds = listOf(1L)
            val now = LocalDateTime.now()
            val games =
                (1L..5L).map {
                    createGame(it, scheduledAt = now.plusDays(it), status = GameStatus.SCHEDULED)
                }
            val gameTeams = (1L..5L).map { createGameTeam(it, teamId = 1L) }

            every { gameTeamRepository.findAllByTeamId(1L) } returns gameTeams
            every { gameRepository.findAllByIds(any()) } returns games
            every { gameTeamRepository.findAllByGameIds(listOf(1L, 2L)) } returns gameTeams.take(2)

            // when
            val result = service.getUpcomingGamesByTeamIds(teamIds, limit = 2)

            // then
            assertThat(result).hasSize(2)
        }
    }

    // Helper methods
    private fun createGame(
        id: Long,
        scheduledAt: LocalDateTime = LocalDateTime.of(2026, 2, 9, 14, 0),
        status: GameStatus = GameStatus.SCHEDULED,
    ): Game {
        val game = mockk<Game>(relaxed = true)
        val competition = mockk<Competition>(relaxed = true)

        every { game.id } returns id
        every { game.competition } returns competition
        every { game.scheduledAt } returns scheduledAt
        every { game.status } returns status
        every { game.location } returns "서울"
        every { game.fieldName } returns "잠실구장"
        every { game.gameNumber } returns 1
        every { game.currentInningDisplay } returns "1회초"
        every { game.totalInnings } returns 9
        every { game.startedAt } returns null
        every { game.endedAt } returns null
        every { game.note } returns null
        every { game.forfeitReason } returns null

        every { competition.id } returns 100L
        every { competition.name } returns "서울시 리그"

        return game
    }

    private fun createGameTeam(
        gameId: Long,
        teamId: Long = 1L,
        homeAway: HomeAway = HomeAway.HOME,
    ): GameTeam {
        val gameTeam = mockk<GameTeam>(relaxed = true)
        val game = mockk<Game>(relaxed = true)
        val team = mockk<Team>(relaxed = true)

        every { gameTeam.game } returns game
        every { gameTeam.team } returns team
        every { gameTeam.homeAway } returns homeAway
        every { gameTeam.totalScore } returns 5

        every { game.id } returns gameId
        every { team.id } returns teamId
        every { team.name } returns "팀 $teamId"

        return gameTeam
    }
}
