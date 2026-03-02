package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.BaseRunningResult
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.dto.BaseRunningRequest
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

@DisplayName("GameScorerServiceImpl - recordBaseRunning")
class GameScorerServiceBaseRunningTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var gameScorerService: GameScorerServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        gameTeamRepository = mockk()
        boxScoreService = mockk(relaxed = true)
        gameEventRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        every { gameTeamRepository.findAllByGameId(any()) } returns emptyList()
        every { pitchingRecordRepository.findAllByGameId(any()) } returns emptyList()
        every { pitchingRecordRepository.findByGamePlayer(any()) } returns null
        every { gameEventRepository.save(any()) } answers { firstArg() }
        every { gameRepository.save(any()) } answers { firstArg() }
        gameScorerService =
            GameScorerServiceImpl(
                gameRepository,
                gamePlayerRepository,
                gameTeamRepository,
                boxScoreService,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                eventPublisher,
            )
    }

    @Nested
    @DisplayName("도루 성공")
    inner class StolenBase {
        @Test
        fun `should record stolen base from first to second`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            val battingRecord: BattingRecord = mockk(relaxed = true)
            game.gameState.setRunner(Base.FIRST, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner
            every { battingRecordRepository.findByGamePlayer(runner) } returns battingRecord

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.FIRST,
                    toBase = Base.SECOND,
                    result = BaseRunningResult.STOLEN_BASE,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.BASE_RUNNING)
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.STOLEN_BASE)
            assertThat(result.fromBase).isEqualTo(Base.FIRST)
            assertThat(result.toBase).isEqualTo(Base.SECOND)
            verify { battingRecord.recordStolenBase() }
            verify { gameRepository.save(game) }
        }

        @Test
        fun `should record stolen base to home (scoring)`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            val battingRecord: BattingRecord = mockk(relaxed = true)
            game.gameState.setRunner(Base.THIRD, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner
            every { battingRecordRepository.findByGamePlayer(runner) } returns battingRecord

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.THIRD,
                    toBase = Base.HOME,
                    result = BaseRunningResult.STOLEN_BASE,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.BASE_RUNNING)
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.STOLEN_BASE)
            verify { battingRecord.recordStolenBase() }
            // 홈으로 도루 시 toBase가 HOME이므로 setRunner(HOME, runner)는 호출하지 않음
            assertThat(game.gameState.runnerOnThirdId).isNull()
        }
    }

    @Nested
    @DisplayName("도루 실패 (아웃)")
    inner class CaughtStealing {
        @Test
        fun `should record caught stealing and increase out count`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            val battingRecord: BattingRecord = mockk(relaxed = true)
            game.gameState.setRunner(Base.FIRST, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner
            every { battingRecordRepository.findByGamePlayer(runner) } returns battingRecord

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.FIRST,
                    toBase = Base.SECOND,
                    result = BaseRunningResult.CAUGHT_STEALING,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.BASE_RUNNING)
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.CAUGHT_STEALING)
            assertThat(result.outCountAfter).isEqualTo(1)
            verify { battingRecord.recordCaughtStealing() }
            assertThat(game.gameState.runnerOnFirstId).isNull()
        }
    }

    @Nested
    @DisplayName("견제사")
    inner class PickedOff {
        @Test
        fun `should record picked off and increase out count`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            game.gameState.setRunner(Base.SECOND, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.SECOND,
                    toBase = Base.SECOND,
                    result = BaseRunningResult.PICKED_OFF,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.BASE_RUNNING)
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.PICKED_OFF)
            assertThat(result.outCountAfter).isEqualTo(1)
            assertThat(game.gameState.runnerOnSecondId).isNull()
        }
    }

    @Nested
    @DisplayName("기타 진루")
    inner class AdvancedRunning {
        @Test
        fun `should record advanced on wild pitch`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            game.gameState.setRunner(Base.SECOND, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.SECOND,
                    toBase = Base.THIRD,
                    result = BaseRunningResult.ADVANCED_ON_WILD_PITCH,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.BASE_RUNNING)
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.ADVANCED_ON_WILD_PITCH)
            assertThat(game.gameState.runnerOnSecondId).isNull()
            assertThat(game.gameState.runnerOnThirdId).isEqualTo(10L)
        }

        @Test
        fun `should record advanced on error`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            game.gameState.setRunner(Base.FIRST, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.FIRST,
                    toBase = Base.SECOND,
                    result = BaseRunningResult.ADVANCED_ON_ERROR,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.ADVANCED_ON_ERROR)
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnSecondId).isEqualTo(10L)
        }

        @Test
        fun `should record advanced on passed ball`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            game.gameState.setRunner(Base.FIRST, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.FIRST,
                    toBase = Base.THIRD,
                    result = BaseRunningResult.ADVANCED_ON_PASSED_BALL,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.ADVANCED_ON_PASSED_BALL)
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnThirdId).isEqualTo(10L)
        }

        @Test
        fun `should record advanced on balk`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val runner = createGamePlayer(10L)
            game.gameState.setRunner(Base.SECOND, 10L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns runner

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.SECOND,
                    toBase = Base.THIRD,
                    result = BaseRunningResult.ADVANCED_ON_BALK,
                )

            // when
            val result = gameScorerService.recordBaseRunning(1L, request)

            // then
            assertThat(result.baseRunningResult).isEqualTo(BaseRunningResult.ADVANCED_ON_BALK)
            assertThat(game.gameState.runnerOnSecondId).isNull()
            assertThat(game.gameState.runnerOnThirdId).isEqualTo(10L)
        }
    }

    @Nested
    @DisplayName("예외 상황")
    inner class Exceptions {
        @Test
        fun `should throw exception when game not found`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.FIRST,
                    toBase = Base.SECOND,
                    result = BaseRunningResult.STOLEN_BASE,
                )

            // when & then
            assertThatThrownBy { gameScorerService.recordBaseRunning(999L, request) }
                .isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `should throw exception when game is not in progress`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            val request =
                BaseRunningRequest(
                    runnerId = 10L,
                    fromBase = Base.FIRST,
                    toBase = Base.SECOND,
                    result = BaseRunningResult.STOLEN_BASE,
                )

            // when & then
            assertThatThrownBy { gameScorerService.recordBaseRunning(1L, request) }
                .isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("진행 중인 경기만 주루 기록을 입력할 수 있습니다")
        }

        @Test
        fun `should throw exception when runner not found`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(999L) } returns null

            val request =
                BaseRunningRequest(
                    runnerId = 999L,
                    fromBase = Base.FIRST,
                    toBase = Base.SECOND,
                    result = BaseRunningResult.STOLEN_BASE,
                )

            // when & then
            assertThatThrownBy { gameScorerService.recordBaseRunning(1L, request) }
                .isInstanceOf(GamePlayerNotFoundException::class.java)
        }
    }

    private fun createGamePlayer(id: Long): GamePlayer {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns id
        every { gamePlayer.battingOrder } returns 1
        return gamePlayer
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

        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실구장",
            fieldName = "1구장",
            gameNumber = 1,
            status = status,
            currentInning = 5,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
        ).apply {
            val idField = Game::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }
    }
}
