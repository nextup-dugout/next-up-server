package com.nextup.infrastructure.service.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.TimeLimitWarningEvent
import com.nextup.core.domain.event.TimeLimitWarningType
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.game.ScorerLock
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import com.nextup.core.service.game.dto.RunnerMovement
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("PlateAppearanceRecordServiceImpl 테스트")
class PlateAppearanceRecordServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var service: PlateAppearanceRecordServiceImpl
    private val publishedEvents = mutableListOf<Any>()

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        boxScoreService = mockk(relaxed = true)
        gameEventRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        gameTeamRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        publishedEvents.clear()

        every { gameEventRepository.save(any()) } answers { firstArg() }
        every { pitchingRecordRepository.findByGamePlayer(any()) } returns null
        every { eventPublisher.publishEvent(capture(publishedEvents)) } returns Unit

        service =
            PlateAppearanceRecordServiceImpl(
                gameRepository,
                gamePlayerRepository,
                boxScoreService,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                gameTeamRepository,
                eventPublisher,
            )
    }

    @Nested
    @DisplayName("끝내기(Walk-off) 자동 감지")
    inner class WalkOffDetection {
        @Test
        @DisplayName("말 이닝 + 정규 이닝 이상 + 득점 + 홈팀 리드 → 끝내기 자동 종료")
        fun walkOffDetectedWhenBottomInningAndHomeLeads() {
            // given: 7회말, 홈팀 4-3 리드 상태에서 주자 득점
            val game = createGame(1L, currentInning = 7, isTopInning = false, totalInnings = 7)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(4)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(3)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 30L,
                                fromBase = Base.THIRD,
                                toBase = Base.HOME,
                            ),
                        ),
                    rbis = 1,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.status).isEqualTo(GameStatus.FINISHED)
        }

        @Test
        @DisplayName("끝내기 시 GameResultConfirmedEvent 발행")
        fun walkOffPublishesGameResultConfirmedEvent() {
            // given: 9회말, 홈팀 역전 5-4
            val game = createGame(1L, currentInning = 9, isTopInning = false, totalInnings = 9)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(5)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(4)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 30L,
                                fromBase = Base.THIRD,
                                toBase = Base.HOME,
                            ),
                        ),
                    rbis = 1,
                )

            // when
            service.recordPlateAppearance(1L, request, 999L)

            // then
            val gameResultEvents =
                publishedEvents.filterIsInstance<GameResultConfirmedEvent>()
            assertThat(gameResultEvents).hasSize(1)
        }

        @Test
        @DisplayName("끝내기 시 advanceBatter/resetCount 호출 안 됨")
        fun walkOffDoesNotAdvanceBatterOrResetCount() {
            // given: 9회말, 홈팀 6-5 리드에서 홈런으로 끝내기
            val game = createGame(1L, currentInning = 9, isTopInning = false, totalInnings = 9)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(6)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.HOME_RUN,
                    runnerMovements = emptyList(),
                    rbis = 1,
                )

            val stateBeforeHome = game.gameState.homeBattingOrder

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then: 끝내기이므로 경기 종료, 타순/카운트 상태가 진행되지 않음
            assertThat(result.game.status).isEqualTo(GameStatus.FINISHED)
            // advanceBatter가 호출 안 되므로 homeBattingOrder가 변하지 않음
            assertThat(game.gameState.homeBattingOrder).isEqualTo(stateBeforeHome)
        }

        @Test
        @DisplayName("말 이닝 + 정규 이닝 미만 + 득점 → 끝내기 아님")
        fun notWalkOffWhenBeforeRegulationInning() {
            // given: 5회말, 홈팀 리드 (9이닝 경기)
            val game = createGame(1L, currentInning = 5, isTopInning = false, totalInnings = 9)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(5)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(3)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 30L,
                                fromBase = Base.THIRD,
                                toBase = Base.HOME,
                            ),
                        ),
                    rbis = 1,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then: 정규 이닝 미만이므로 끝내기가 아님, 경기 계속
            assertThat(result.game.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(publishedEvents.filterIsInstance<GameResultConfirmedEvent>()).isEmpty()
        }

        @Test
        @DisplayName("초 이닝 + 득점 → 끝내기 아님")
        fun notWalkOffWhenTopInning() {
            // given: 9회초, 원정팀 공격 중 득점
            val game = createGame(1L, currentInning = 9, isTopInning = true, totalInnings = 9)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(3)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 30L,
                                fromBase = Base.THIRD,
                                toBase = Base.HOME,
                            ),
                        ),
                    rbis = 1,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then: 초 이닝이므로 끝내기 아님
            assertThat(result.game.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(publishedEvents.filterIsInstance<GameResultConfirmedEvent>()).isEmpty()
        }

        @Test
        @DisplayName("득점 없는 타석 → 끝내기 감지 로직 실행 안 됨")
        fun noWalkOffCheckWhenNoRunsScored() {
            // given: 9회말, 홈팀 리드이지만 득점 없는 안타
            val game = createGame(1L, currentInning = 9, isTopInning = false, totalInnings = 9)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(3)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(2)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then: 득점이 없으므로 끝내기 감지 안 함
            assertThat(result.game.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(publishedEvents.filterIsInstance<GameResultConfirmedEvent>()).isEmpty()
        }

        @Test
        @DisplayName("연장전 말에서 득점 + 홈팀 역전 → 끝내기 자동 종료")
        fun walkOffInExtraInnings() {
            // given: 10회말, 홈팀 5-4 역전
            val game = createGame(1L, currentInning = 10, isTopInning = false, totalInnings = 9)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(5)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(4)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements =
                        listOf(
                            RunnerMovement(
                                runnerId = 30L,
                                fromBase = Base.SECOND,
                                toBase = Base.HOME,
                            ),
                        ),
                    rbis = 1,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then
            assertThat(result.game.status).isEqualTo(GameStatus.FINISHED)
            assertThat(publishedEvents.filterIsInstance<GameResultConfirmedEvent>()).hasSize(1)
        }

        @Test
        @DisplayName("말 이닝 + 득점 + 동점 → 끝내기 아님")
        fun notWalkOffWhenTied() {
            // given: 9회말, 동점 상태에서 득점 없는 안타
            val game = createGame(1L, currentInning = 9, isTopInning = false, totalInnings = 9)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            val gameTeams = game.gameTeams
            gameTeams.first { it.homeAway == HomeAway.HOME }.addScore(3)
            gameTeams.first { it.homeAway == HomeAway.AWAY }.addScore(3)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameTeamRepository.findAllByGameId(1L) } returns gameTeams
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then: 동점이므로 끝내기 아님
            assertThat(result.game.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(publishedEvents.filterIsInstance<GameResultConfirmedEvent>()).isEmpty()
        }
    }

    @Nested
    @DisplayName("시간 제한 경고 감지 (M-5)")
    inner class TimeLimitWarningDetection {

        @Test
        @DisplayName("시간 제한 도달 시 TimeLimitWarningEvent(LIMIT_REACHED) 발행")
        fun timeLimitReachedPublishesEvent() {
            // given: 120분 제한, 시작 125분 전 → 제한 도달
            val game =
                createGameWithTimeLimit(
                    id = 1L,
                    currentInning = 5,
                    isTopInning = true,
                    timeLimitMinutes = 120,
                    startedAt = LocalDateTime.now().minusMinutes(125),
                )
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then
            val timeLimitEvents =
                publishedEvents.filterIsInstance<TimeLimitWarningEvent>()
            assertThat(timeLimitEvents).hasSize(1)
            assertThat(timeLimitEvents[0].warningType)
                .isEqualTo(TimeLimitWarningType.LIMIT_REACHED)
            assertThat(result.warnings).anyMatch { it.contains("시간 제한 경고") }
        }

        @Test
        @DisplayName("시간 제한 임박 시 TimeLimitWarningEvent(APPROACHING_LIMIT) 발행")
        fun timeLimitApproachingPublishesEvent() {
            // given: 120분 제한, 시작 112분 전 → 임박 (10분 이하 남음)
            val game =
                createGameWithTimeLimit(
                    id = 1L,
                    currentInning = 5,
                    isTopInning = true,
                    timeLimitMinutes = 120,
                    startedAt = LocalDateTime.now().minusMinutes(112),
                )
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            val result = service.recordPlateAppearance(1L, request, 999L)

            // then
            val timeLimitEvents =
                publishedEvents.filterIsInstance<TimeLimitWarningEvent>()
            assertThat(timeLimitEvents).hasSize(1)
            assertThat(timeLimitEvents[0].warningType)
                .isEqualTo(TimeLimitWarningType.APPROACHING_LIMIT)
            assertThat(result.warnings).anyMatch { it.contains("시간 제한 주의") }
        }

        @Test
        @DisplayName("시간 제한이 없는 경기에서는 경고 이벤트 미발행")
        fun noTimeLimitNoEvent() {
            // given: 시간 제한 없음 (기본 GameRules)
            val game = createGame(1L, currentInning = 5, isTopInning = true)
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            service.recordPlateAppearance(1L, request, 999L)

            // then
            val timeLimitEvents =
                publishedEvents.filterIsInstance<TimeLimitWarningEvent>()
            assertThat(timeLimitEvents).isEmpty()
        }

        @Test
        @DisplayName("시간 제한 정상 범위면 경고 이벤트 미발행")
        fun withinTimeLimitNoEvent() {
            // given: 120분 제한, 시작 60분 전 → 정상 범위
            val game =
                createGameWithTimeLimit(
                    id = 1L,
                    currentInning = 3,
                    isTopInning = true,
                    timeLimitMinutes = 120,
                    startedAt = LocalDateTime.now().minusMinutes(60),
                )
            val batter = createGamePlayer(10L, playerId = 100L, battingOrder = 1)
            val pitcher = createGamePlayer(20L, playerId = 200L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val request =
                PlateAppearanceRequest(
                    batterId = 10L,
                    pitcherId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                    runnerMovements = emptyList(),
                    rbis = 0,
                )

            // when
            service.recordPlateAppearance(1L, request, 999L)

            // then
            val timeLimitEvents =
                publishedEvents.filterIsInstance<TimeLimitWarningEvent>()
            assertThat(timeLimitEvents).isEmpty()
        }
    }

    // Helper methods

    private fun createGamePlayer(
        id: Long,
        playerId: Long = id * 10,
        battingOrder: Int = 0,
    ): GamePlayer {
        val player = mockk<com.nextup.core.domain.player.Player>(relaxed = true)
        every { player.id } returns playerId

        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns id
        every { gamePlayer.player } returns player
        every { gamePlayer.battingOrder } returns battingOrder
        return gamePlayer
    }

    private fun setEntityId(
        entity: Any,
        id: Long,
    ) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun createGame(
        id: Long,
        currentInning: Int = 1,
        isTopInning: Boolean = true,
        totalInnings: Int = 9,
    ): Game {
        val association =
            Association(
                name = "서울시야구협회",
                region = "서울",
            ).also { setEntityId(it, 1L) }
        val league =
            League(
                association = association,
                name = "1부 리그",
                foundedYear = 2020,
            ).also { setEntityId(it, 1L) }
        val competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
                gameRules = GameRules(defaultInnings = totalInnings),
            ).also { setEntityId(it, 1L) }
        val homeTeam =
            Team(
                league = league,
                name = "홈팀",
                city = "서울",
                foundedYear = 2020,
                id = 1L,
            )
        val awayTeam =
            Team(
                league = league,
                name = "원정팀",
                city = "부산",
                foundedYear = 2020,
                id = 2L,
            )

        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            status = GameStatus.IN_PROGRESS,
            currentInning = currentInning,
            isTopInning = isTopInning,
            totalInnings = totalInnings,
            gameState = GameState(),
            scorerLock = ScorerLock(scorerId = 999L),
            id = id,
        )
    }

    private fun createGameWithTimeLimit(
        id: Long,
        currentInning: Int = 1,
        isTopInning: Boolean = true,
        totalInnings: Int = 9,
        timeLimitMinutes: Int,
        startedAt: LocalDateTime,
    ): Game {
        val association =
            Association(
                name = "서울시야구협회",
                region = "서울",
            ).also { setEntityId(it, 1L) }
        val league =
            League(
                association = association,
                name = "1부 리그",
                foundedYear = 2020,
            ).also { setEntityId(it, 1L) }
        val competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
                gameRules =
                    GameRules(
                        defaultInnings = totalInnings,
                        timeLimitMinutes = timeLimitMinutes,
                    ),
            ).also { setEntityId(it, 1L) }
        val homeTeam =
            Team(
                league = league,
                name = "홈팀",
                city = "서울",
                foundedYear = 2020,
                id = 1L,
            )
        val awayTeam =
            Team(
                league = league,
                name = "원정팀",
                city = "부산",
                foundedYear = 2020,
                id = 2L,
            )

        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            status = GameStatus.IN_PROGRESS,
            currentInning = currentInning,
            isTopInning = isTopInning,
            totalInnings = totalInnings,
            startedAt = startedAt,
            gameState = GameState(),
            scorerLock = ScorerLock(scorerId = 999L),
            id = id,
        )
    }
}
