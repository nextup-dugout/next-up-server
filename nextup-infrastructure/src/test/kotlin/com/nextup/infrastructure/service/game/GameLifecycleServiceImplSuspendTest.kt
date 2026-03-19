package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.PitchingDecisionService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameLifecycleServiceImpl - suspend/resume 테스트")
class GameLifecycleServiceImplSuspendTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var service: GameLifecycleServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gameTeamRepository = mockk()
        pitchingRecordRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        every { gameTeamRepository.findAllByGameId(any()) } returns emptyList()
        every { pitchingRecordRepository.findAllByGameId(any()) } returns emptyList()
        service =
            GameLifecycleServiceImpl(
                gameRepository,
                gameTeamRepository,
                pitchingRecordRepository,
                PitchingDecisionService(),
                eventPublisher,
            )
    }

    @Nested
    @DisplayName("suspendGame")
    inner class SuspendGame {
        @Test
        fun `should suspend game when status is IN_PROGRESS`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.suspendGame(1L, "우천 중단", 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.SUSPENDED)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should suspend game without reason`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.suspendGame(1L, null, 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.SUSPENDED)
        }

        @Test
        fun `should throw exception when game is SCHEDULED`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { service.suspendGame(1L, "중단 사유", 999L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만 중단할 수 있습니다")
        }

        @Test
        fun `should throw exception when game is FINISHED`() {
            // given
            val game = createGame(1L, GameStatus.FINISHED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { service.suspendGame(1L, "중단 사유", 999L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만 중단할 수 있습니다")
        }

        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy { service.suspendGame(999L, "중단 사유", 999L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("resumeGame")
    inner class ResumeGame {
        @Test
        fun `should resume game when status is SUSPENDED`() {
            // given
            val game = createGame(1L, GameStatus.SUSPENDED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.resumeGame(1L, 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.IN_PROGRESS)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should throw exception when game is IN_PROGRESS`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { service.resumeGame(1L, 999L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("중단된 경기만 재개할 수 있습니다")
        }

        @Test
        fun `should throw exception when game is SCHEDULED`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { service.resumeGame(1L, 999L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("중단된 경기만 재개할 수 있습니다")
        }

        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy { service.resumeGame(999L, 999L) }
                .isInstanceOf(GameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("cancelGame - SUSPENDED 상태 허용")
    inner class CancelGameSuspended {
        @Test
        fun `should cancel game when status is SUSPENDED`() {
            // given
            val game = createGame(1L, GameStatus.SUSPENDED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            // when
            val result = service.cancelGame(1L, "경기 취소", 999L)

            // then
            assertThat(result.status).isEqualTo(GameStatus.CANCELLED)
            verify { gameRepository.save(any()) }
        }

        @Test
        fun `should throw exception when game is IN_PROGRESS`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy { service.cancelGame(1L, "사유", 999L) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("예정, 연기, 또는 중단 상태의 경기만 취소할 수 있습니다")
        }
    }

    private fun createAssociation(id: Long): Association =
        Association(
            name = "서울시야구협회",
            abbreviation = null,
            region = "서울",
            description = null,
            logoUrl = null,
            websiteUrl = null,
        ).apply {
            val idField = Association::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createLeague(
        id: Long,
        association: Association,
    ): League =
        League(
            association = association,
            name = "1부 리그",
            abbreviation = null,
            foundedYear = 2020,
            divisionLevel = 1,
            description = null,
            logoUrl = null,
        ).apply {
            val idField = League::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createCompetition(
        id: Long,
        league: League,
    ): Competition =
        Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            endDate = LocalDate.of(2025, 6, 30),
            status = CompetitionStatus.IN_PROGRESS,
            description = null,
            maxTeams = null,
        ).apply {
            val idField = Competition::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createGame(
        id: Long,
        status: GameStatus,
    ): Game {
        val association = createAssociation(1L)
        val league = createLeague(1L, association)
        val competition = createCompetition(1L, league)
        val homeTeam =
            Team(
                league = league,
                name = "홈팀",
                city = "서울",
                foundedYear = 2020,
                id = 10L,
            )
        val awayTeam =
            Team(
                league = league,
                name = "원정팀",
                city = "부산",
                foundedYear = 2020,
                id = 11L,
            )

        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실구장",
            fieldName = "1구장",
            gameNumber = 1,
            status = status,
            currentInning = 5,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
            scorerId = 999L,
            id = id,
        )
    }
}
