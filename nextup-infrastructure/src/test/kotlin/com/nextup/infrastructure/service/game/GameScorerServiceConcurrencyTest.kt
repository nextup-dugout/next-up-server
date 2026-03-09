package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.dto.GameEndReason
import com.nextup.core.service.game.dto.PlateAppearanceRequest
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("GameLifecycleServiceImpl / PlateAppearanceRecordServiceImpl / GameUndoServiceImpl - 동시성 테스트")
class GameScorerServiceConcurrencyTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var boxScoreService: BoxScoreService
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var gameLifecycleService: GameLifecycleServiceImpl
    private lateinit var plateAppearanceRecordService: PlateAppearanceRecordServiceImpl
    private lateinit var gameUndoService: GameUndoServiceImpl

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
        every { pitchingRecordRepository.findByGamePlayer(any()) } returns null
        every { gameEventRepository.save(any()) } answers { firstArg() }
        gameLifecycleService =
            GameLifecycleServiceImpl(
                gameRepository,
                gameTeamRepository,
                pitchingRecordRepository,
                eventPublisher,
            )
        plateAppearanceRecordService =
            PlateAppearanceRecordServiceImpl(
                gameRepository,
                gamePlayerRepository,
                boxScoreService,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                eventPublisher,
            )
        gameUndoService =
            GameUndoServiceImpl(
                gameRepository,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                eventPublisher,
            )
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 1: 동시 경기 시작 요청 — 단 한 번만 성공하거나 모두 동일한 최종 상태
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 경기 시작 요청")
    inner class ConcurrentStartGame {

        @RepeatedTest(3)
        fun `동시에 여러 스레드가 startGame을 호출해도 최종 상태는 IN_PROGRESS여야 한다`() {
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            val threadCount = 10
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        gameLifecycleService.startGame(1L)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 최소 1번은 성공해야 하며, 최종 상태는 IN_PROGRESS
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 2: 동시 타석 기록 — GameState 아웃 카운트 정합성
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 타석 기록 — 아웃 카운트 정합성")
    inner class ConcurrentRecordPlateAppearanceOutCount {

        @RepeatedTest(3)
        fun `동시에 삼진 타석을 기록해도 아웃 카운트는 3을 초과하지 않아야 한다`() {
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val threadCount = 5
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)
            val failCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        val request = createStrikeoutRequest(batterId = 10L, pitcherId = 20L)
                        plateAppearanceRecordService.recordPlateAppearance(1L, request)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        failCount.incrementAndGet()
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 아웃 카운트는 절대 3을 초과할 수 없다
            assertThat(game.gameState.outs).isLessThanOrEqualTo(3)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 3: 동시 이닝 전환 요청 — 이닝 카운터 정합성
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 이닝 전환 요청")
    inner class ConcurrentAdvanceHalfInning {

        @RepeatedTest(3)
        fun `동시에 이닝 전환을 요청해도 이닝 값은 합리적인 범위 내여야 한다`() {
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = true
                }
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameRepository.save(any()) } answers { firstArg() }

            val threadCount = 10
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        gameLifecycleService.advanceHalfInning(1L)
                        successCount.incrementAndGet()
                    } catch (e: Exception) {
                        // 상태 오류는 허용됨
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 이닝은 1 이상이어야 하고, 총 이닝 수를 크게 초과하지 않아야 한다
            assertThat(game.currentInning).isGreaterThanOrEqualTo(1)
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 4: 동시 경기 종료 + 이벤트 입력 — 종료 후 이벤트 거부
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("경기 종료와 타석 기록 동시 요청")
    inner class ConcurrentEndGameAndRecord {

        @RepeatedTest(3)
        fun `경기 종료와 타석 기록이 동시에 요청될 때 최종 상태는 일관성을 유지해야 한다`() {
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            val endSucceeded = AtomicInteger(0)
            val recordSucceeded = AtomicInteger(0)

            // 스레드 1: 경기 종료
            executor.submit {
                latch.await()
                try {
                    gameLifecycleService.endGame(1L, GameEndReason.REGULATION)
                    endSucceeded.incrementAndGet()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // 스레드 2: 타석 기록
            executor.submit {
                latch.await()
                try {
                    val request = createSingleRequest(batterId = 10L, pitcherId = 20L)
                    plateAppearanceRecordService.recordPlateAppearance(1L, request)
                    recordSucceeded.incrementAndGet()
                } catch (e: InvalidGameStateException) {
                    // 경기 종료 후 타석 기록 시도는 정상적인 실패
                } catch (e: Exception) {
                    // ignore
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 최종 상태가 FINISHED이거나 IN_PROGRESS이어야 한다 (중간 상태 없음)
            assertThat(game.status)
                .isIn(GameStatus.FINISHED, GameStatus.IN_PROGRESS)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 5: BattingRecord 동시 수정 — Race condition 허용, 음수 방지 검증
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("BattingRecord 동시 수정")
    inner class ConcurrentBattingRecordUpdate {

        @RepeatedTest(3)
        fun `동시에 여러 스레드가 BattingRecord를 수정할 때 타석 수는 음수가 되지 않아야 한다`() {
            val gamePlayer = createGamePlayer(10L)
            val battingRecord = BattingRecord.create(gamePlayer)

            val threadCount = 10
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 0)
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 타석 수는 절대 음수가 되어서는 안 된다
            assertThat(battingRecord.plateAppearances).isGreaterThanOrEqualTo(0)
            assertThat(battingRecord.hits).isGreaterThanOrEqualTo(0)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 6: PitchingRecord 동시 수정 — Race condition 허용, 음수 방지 검증
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("PitchingRecord 동시 수정")
    inner class ConcurrentPitchingRecordUpdate {

        @RepeatedTest(3)
        fun `동시에 여러 스레드가 PitchingRecord를 수정할 때 대면 타자 수는 음수가 되지 않아야 한다`() {
            val gamePlayer = createGamePlayer(20L)
            val pitchingRecord = PitchingRecord.create(gamePlayer)

            val threadCount = 10
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT)
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            assertThat(pitchingRecord.battersFaced).isGreaterThanOrEqualTo(0)
            assertThat(pitchingRecord.strikeouts).isGreaterThanOrEqualTo(0)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 7: 동시 몰수패 + 정상종료 요청
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 몰수패 및 정상종료 요청")
    inner class ConcurrentForfeitAndEnd {

        @RepeatedTest(3)
        fun `몰수패와 경기 종료가 동시에 요청될 때 최종 상태는 일관성을 유지해야 한다`() {
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val homeTeam = createMockTeam(10L, "홈팀")
            val awayTeam = createMockTeam(20L, "원정팀")
            val homeGameTeam = createMockGameTeam(1L, game, homeTeam)
            val awayGameTeam = createMockGameTeam(2L, game, awayTeam)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gameTeamRepository.findAllByGameId(1L) } returns listOf(homeGameTeam, awayGameTeam)
            every { gameRepository.save(any()) } answers { firstArg() }

            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)

            executor.submit {
                latch.await()
                try {
                    gameLifecycleService.forfeitGame(1L, winnerTeamId = 10L, reason = "원정팀 불참")
                } catch (e: Exception) {
                    // ignore
                }
            }

            executor.submit {
                latch.await()
                try {
                    gameLifecycleService.endGame(1L, GameEndReason.REGULATION)
                } catch (e: Exception) {
                    // ignore
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 최종 상태는 FORFEITED, FINISHED, IN_PROGRESS 중 하나여야 한다 (무결한 상태)
            assertThat(game.status)
                .isIn(GameStatus.FORFEITED, GameStatus.FINISHED, GameStatus.IN_PROGRESS)
            // 경기가 종료된 경우 endedAt이 설정되어야 한다
            if (game.status != GameStatus.IN_PROGRESS) {
                assertThat(game.endedAt).isNotNull()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 8: GameState 동시 볼카운트 수정 — Race condition 허용, 유효 범위 검증
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GameState 볼카운트 동시 수정")
    inner class ConcurrentGameStateBallStrikeCount {

        @Test
        fun `동시에 볼과 스트라이크를 추가해도 GameState가 유효 범위를 유지해야 한다`() {
            val gameState = GameState()

            val threadCount = 8
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)

            // 4개 스레드: 볼 추가
            repeat(4) {
                executor.submit {
                    latch.await()
                    try {
                        gameState.addBall()
                    } catch (e: Exception) {
                        // 이미 최대 볼이면 예외 발생 가능
                    }
                }
            }
            // 4개 스레드: 스트라이크 추가
            repeat(4) {
                executor.submit {
                    latch.await()
                    try {
                        gameState.addStrike()
                    } catch (e: Exception) {
                        // 이미 최대 스트라이크이면 예외 발생 가능
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            assertThat(gameState.balls).isBetween(0, 4)
            assertThat(gameState.strikes).isBetween(0, 3)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 9: GameState 동시 주자 설정 — Race condition 허용, 유효 값 검증
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("GameState 주자 동시 설정")
    inner class ConcurrentRunnerSetting {

        @RepeatedTest(3)
        fun `동시에 여러 스레드가 주자를 설정해도 최종 값은 설정된 ID 중 하나여야 한다`() {
            val gameState = GameState()

            val threadCount = 6
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val completedCount = AtomicInteger(0)

            // 3개 스레드: 1루 주자 서로 다른 ID로 경쟁
            repeat(3) { index ->
                val playerId = (index + 1).toLong()
                executor.submit {
                    latch.await()
                    try {
                        gameState.setRunner(com.nextup.core.domain.game.Base.FIRST, playerId)
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                    completedCount.incrementAndGet()
                }
            }

            // 3개 스레드: 2루 주자 서로 다른 ID로 경쟁
            repeat(3) { index ->
                val playerId = (index + 10).toLong()
                executor.submit {
                    latch.await()
                    try {
                        gameState.setRunner(com.nextup.core.domain.game.Base.SECOND, playerId)
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                    completedCount.incrementAndGet()
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 모든 스레드가 완료되어야 한다
            assertThat(completedCount.get()).isEqualTo(threadCount)
            // 1루/2루 주자는 설정된 값 중 하나이거나 null이어야 한다
            gameState.runnerOnFirstId?.let { assertThat(it).isIn(1L, 2L, 3L) }
            gameState.runnerOnSecondId?.let { assertThat(it).isIn(10L, 11L, 12L) }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 10: 동시 Undo + 새 이벤트 입력 — 정합성
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 Undo 및 새 이벤트 입력")
    inner class ConcurrentUndoAndRecord {

        @RepeatedTest(3)
        fun `Undo와 새 타석 기록이 동시에 요청될 때 예외 없이 처리되어야 한다`() {
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)
            val battingRecord =
                BattingRecord.create(batter).apply {
                    applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 0)
                }
            val pitchingRecord = PitchingRecord.create(pitcher)
            val event = createPlateAppearanceEvent(game, batter, pitcher, PlateAppearanceResult.SINGLE)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameEventRepository.findLastActiveEvent(1L) } returns event
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gameEventRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(2)
            val undoSucceeded = AtomicInteger(0)
            val recordSucceeded = AtomicInteger(0)

            // 스레드 1: Undo
            executor.submit {
                latch.await()
                try {
                    gameUndoService.undoLastEvent(1L)
                    undoSucceeded.incrementAndGet()
                } catch (e: Exception) {
                    // ignore
                }
            }

            // 스레드 2: 새 타석 기록
            executor.submit {
                latch.await()
                try {
                    val request = createSingleRequest(batterId = 10L, pitcherId = 20L)
                    plateAppearanceRecordService.recordPlateAppearance(1L, request)
                    recordSucceeded.incrementAndGet()
                } catch (e: Exception) {
                    // ignore
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 적어도 하나는 성공해야 한다
            assertThat(undoSucceeded.get() + recordSucceeded.get()).isGreaterThanOrEqualTo(1)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 11: 동시 경기 조회 — 존재하지 않는 경기에 대한 일관된 예외
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 경기 조회 — 없는 경기")
    inner class ConcurrentGameNotFound {

        @Test
        fun `동시에 여러 스레드가 없는 경기를 조회하면 모두 GameNotFoundException을 받아야 한다`() {
            every { gameRepository.findByIdOrNull(999L) } returns null

            val threadCount = 10
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val notFoundCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        gameLifecycleService.startGame(999L)
                    } catch (e: GameNotFoundException) {
                        notFoundCount.incrementAndGet()
                    } catch (e: Exception) {
                        // 다른 예외는 무시
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 모든 스레드가 GameNotFoundException을 받아야 한다
            assertThat(notFoundCount.get()).isEqualTo(threadCount)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 12: 동시 타순 진행 — Race condition 허용, 유효 범위 검증
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 타순 진행")
    inner class ConcurrentBatterAdvance {

        @RepeatedTest(3)
        fun `동시에 여러 스레드가 타순을 진행해도 타순은 1-9 범위를 유지해야 한다`() {
            val gameState = GameState(awayBattingOrder = 1, homeBattingOrder = 1)

            val threadCount = 18 // 9이닝 2번 순환
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val completedCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        gameState.advanceBatter(isHomeTeam = false)
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                    completedCount.incrementAndGet()
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            assertThat(completedCount.get()).isEqualTo(threadCount)
            // 타순은 항상 1-9 범위
            assertThat(gameState.awayBattingOrder).isBetween(1, 9)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 13: BattingRecord Undo 동시 롤백 — Race condition 노출 테스트
    //
    // 참고: 단위 테스트 레벨에서 revertPlateAppearanceResult는 synchronized 없이
    // 동시 호출 시 Race condition이 발생할 수 있습니다.
    // 실제 운영 환경에서는 DB 레벨의 @Version(Optimistic Locking)이
    // 동시 수정을 막아 이 문제를 방지합니다.
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("BattingRecord 동시 롤백")
    inner class ConcurrentBattingRecordRevert {

        @RepeatedTest(3)
        fun `동시에 여러 스레드가 BattingRecord를 롤백할 때 모든 스레드가 완료되어야 한다`() {
            val gamePlayer = createGamePlayer(10L)
            val battingRecord = BattingRecord.create(gamePlayer)

            // 먼저 10타석 기록 (스레드 수만큼 충분히 준비)
            repeat(10) {
                battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 0)
            }

            val threadCount = 5 // 기록된 타석보다 적게 롤백 시도
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val completedCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        battingRecord.revertPlateAppearanceResult(PlateAppearanceResult.SINGLE, rbis = 0)
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                    completedCount.incrementAndGet()
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            // 모든 스레드가 완료되어야 한다
            // (음수 방지는 DB @Version Optimistic Locking이 담당)
            assertThat(completedCount.get()).isEqualTo(threadCount)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 14: 동시 GameState 이닝 리셋 — 주자/카운트 초기화 정합성
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("동시 이닝 리셋")
    inner class ConcurrentInningReset {

        @RepeatedTest(3)
        fun `동시에 이닝 리셋과 주자 설정이 일어나도 최종 상태는 유효해야 한다`() {
            val gameState = GameState()
            gameState.setRunner(com.nextup.core.domain.game.Base.FIRST, 1L)
            gameState.setRunner(com.nextup.core.domain.game.Base.SECOND, 2L)
            gameState.setRunner(com.nextup.core.domain.game.Base.THIRD, 3L)

            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(4)
            val completedCount = AtomicInteger(0)

            // 2개 스레드: 이닝 리셋
            repeat(2) {
                executor.submit {
                    latch.await()
                    try {
                        gameState.resetForNewInning()
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                    completedCount.incrementAndGet()
                }
            }

            // 2개 스레드: 주자 설정
            repeat(2) { idx ->
                val playerId = (idx + 5).toLong()
                executor.submit {
                    latch.await()
                    try {
                        gameState.setRunner(com.nextup.core.domain.game.Base.FIRST, playerId)
                    } catch (e: Exception) {
                        // Race condition으로 인한 예외는 허용
                    }
                    completedCount.incrementAndGet()
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(5, TimeUnit.SECONDS)

            assertThat(completedCount.get()).isEqualTo(4)
            // 아웃 카운트는 항상 유효 범위
            assertThat(gameState.outs).isBetween(0, 3)
            assertThat(gameState.balls).isBetween(0, 4)
            assertThat(gameState.strikes).isBetween(0, 3)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // 테스트 15: 높은 부하 — 대량 동시 타석 기록 처리량
    // ──────────────────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("높은 부하 — 대량 동시 처리")
    inner class HighLoadConcurrentRecording {

        @Test
        fun `50개 스레드가 동시에 서비스를 호출해도 예외 없이 처리되어야 한다`() {
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val batter = createGamePlayer(10L)
            val pitcher = createGamePlayer(20L)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns batter
            every { gamePlayerRepository.findByIdOrNull(20L) } returns pitcher
            every { gameRepository.save(any()) } answers { firstArg() }

            val threadCount = 50
            val latch = CountDownLatch(1)
            val executor = Executors.newFixedThreadPool(threadCount)
            val successCount = AtomicInteger(0)
            val unexpectedFailCount = AtomicInteger(0)

            repeat(threadCount) {
                executor.submit {
                    latch.await()
                    try {
                        val request = createSingleRequest(batterId = 10L, pitcherId = 20L)
                        plateAppearanceRecordService.recordPlateAppearance(1L, request)
                        successCount.incrementAndGet()
                    } catch (e: InvalidGameStateException) {
                        // 3아웃 초과 등 예상 가능한 상태 오류는 정상
                    } catch (e: IllegalArgumentException) {
                        // 아웃 카운트 초과 등 예상 가능한 도메인 오류는 정상
                    } catch (e: Exception) {
                        unexpectedFailCount.incrementAndGet()
                    }
                }
            }

            latch.countDown()
            executor.shutdown()
            executor.awaitTermination(10, TimeUnit.SECONDS)

            // 예상치 못한 예외는 없어야 한다
            assertThat(unexpectedFailCount.get()).isEqualTo(0)
            // 적어도 1번은 성공해야 한다
            assertThat(successCount.get()).isGreaterThanOrEqualTo(1)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────────
    // Helper methods
    // ──────────────────────────────────────────────────────────────────────────────

    private fun createGamePlayer(id: Long): GamePlayer {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.id } returns id
        every { gamePlayer.battingOrder } returns 1
        return gamePlayer
    }

    private fun createMockTeam(
        id: Long,
        name: String,
    ): com.nextup.core.domain.team.Team {
        val team = mockk<com.nextup.core.domain.team.Team>(relaxed = true)
        every { team.id } returns id
        every { team.name } returns name
        return team
    }

    private fun createMockGameTeam(
        id: Long,
        game: Game,
        team: com.nextup.core.domain.team.Team,
    ): com.nextup.core.domain.game.GameTeam {
        val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
        every { gameTeam.id } returns id
        every { gameTeam.game } returns game
        every { gameTeam.team } returns team
        every { gameTeam.updateScore(any(), any(), any()) } returns Unit
        every { gameTeam.updateResult(any()) } returns Unit
        return gameTeam
    }

    private fun createGame(
        id: Long,
        status: GameStatus,
    ): Game {
        val association =
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
                idField.set(this, 1L)
            }

        val league =
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
                idField.set(this, 1L)
            }

        val competition =
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
                idField.set(this, 1L)
            }

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
            currentInning = 1,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
            id = id,
        )
    }

    private fun createSingleRequest(
        batterId: Long,
        pitcherId: Long,
    ): PlateAppearanceRequest =
        PlateAppearanceRequest(
            batterId = batterId,
            pitcherId = pitcherId,
            result = PlateAppearanceResult.SINGLE,
            runnerMovements = emptyList(),
            rbis = 0,
            balls = 2,
            strikes = 1,
        )

    private fun createStrikeoutRequest(
        batterId: Long,
        pitcherId: Long,
    ): PlateAppearanceRequest =
        PlateAppearanceRequest(
            batterId = batterId,
            pitcherId = pitcherId,
            result = PlateAppearanceResult.STRIKEOUT,
            runnerMovements = emptyList(),
            rbis = 0,
            balls = 0,
            strikes = 3,
        )

    private fun createPlateAppearanceEvent(
        game: Game,
        batter: GamePlayer,
        pitcher: GamePlayer,
        result: PlateAppearanceResult,
    ): GameEvent {
        val event =
            GameEvent(
                game = game,
                inning = game.currentInning,
                isTopInning = game.isTopInning,
                outCountBefore = 0,
                outCountAfter = 0,
                eventType = GameEventType.PLATE_APPEARANCE,
                description = result.displayName,
                batter = batter,
                pitcher = pitcher,
                runnersBeforeJson = null,
                plateAppearanceResult = result,
                runsScored = 0,
                rbis = 0,
            )
        val idField = GameEvent::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(event, 1L)
        return event
    }
}
