package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.dto.SubstitutionRequest
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

@DisplayName("GameSubstitutionServiceImpl - substitutePlayer")
class GameScorerServiceSubstitutionTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private lateinit var gameSubstitutionService: GameSubstitutionServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        gameEventRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()

        // M-10/M-11: 기본 stub - 기록 없음으로 설정
        every { pitchingRecordRepository.findByGamePlayerId(any()) } returns null
        every { battingRecordRepository.findByGamePlayerId(any()) } returns null
        every { battingRecordRepository.save(any()) } answers { firstArg() }
        every { pitchingRecordRepository.save(any()) } answers { firstArg() }

        gameSubstitutionService =
            GameSubstitutionServiceImpl(
                gameRepository,
                gamePlayerRepository,
                gameEventRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                eventPublisher,
            )
    }

    @Nested
    @DisplayName("정상 교체 시나리오")
    inner class NormalSubstitution {
        @Test
        fun `벤치 선수가 출전 중인 선수를 교체할 수 있다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val outgoingPlayer = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            val incomingPlayer = createBenchPlayer(20L, Position.CENTER_FIELD)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns outgoingPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns incomingPlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val savedEvent = mockk<GameEvent>(relaxed = true)
            every { savedEvent.eventType } returns GameEventType.SUBSTITUTION
            every { gameEventRepository.save(any()) } returns savedEvent

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when
            val result = gameSubstitutionService.substitutePlayer(1L, request, 999L)

            // then
            assertThat(result).isNotNull()
            // 교체 나가는 선수가 퇴장 처리됨
            assertThat(outgoingPlayer.isCurrentlyPlaying).isFalse()
            assertThat(outgoingPlayer.exitInning).isEqualTo(game.currentInning)
            // 교체 들어오는 선수가 출전 처리됨
            assertThat(incomingPlayer.isCurrentlyPlaying).isTrue()
            assertThat(incomingPlayer.position).isEqualTo(Position.LEFT_FIELD)
            assertThat(incomingPlayer.battingOrder).isEqualTo(5)
            // 이벤트 저장 확인
            verify { gameEventRepository.save(any()) }
        }

        @Test
        fun `교체 이벤트가 SUBSTITUTION 타입으로 저장된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val outgoingPlayer = createStartingPlayer(10L, Position.RIGHT_FIELD, 3)
            val incomingPlayer = createBenchPlayer(20L, Position.RIGHT_FIELD)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns outgoingPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns incomingPlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val savedEvent = mockk<GameEvent>(relaxed = true)
            every { savedEvent.eventType } returns GameEventType.SUBSTITUTION
            every { gameEventRepository.save(any()) } returns savedEvent

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.RIGHT_FIELD,
                    newBattingOrder = 3,
                )

            // when
            val result = gameSubstitutionService.substitutePlayer(1L, request, 999L)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.SUBSTITUTION)
        }
    }

    @Nested
    @DisplayName("재출전 방지")
    inner class ReentryPrevention {
        @Test
        fun `이미 퇴장한 선수가 교체 들어오려 하면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val outgoingPlayer = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            // 이미 퇴장한 선수 (exitInning != null)
            val exitedPlayer = createStartingPlayer(20L, Position.RIGHT_FIELD, 7)
            exitedPlayer.exitGame(2)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns outgoingPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns exitedPlayer

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            assertThatThrownBy {
                gameSubstitutionService.substitutePlayer(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("이미 퇴장한 선수")
        }
    }

    @Nested
    @DisplayName("경기 상태 검증")
    inner class GameStateValidation {
        @Test
        fun `진행 중이 아닌 경기에서 교체를 시도하면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            assertThatThrownBy {
                gameSubstitutionService.substitutePlayer(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("진행 중인 경기만 선수 교체")
        }

        @Test
        fun `경기를 찾을 수 없으면 예외가 발생한다`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            assertThatThrownBy {
                gameSubstitutionService.substitutePlayer(999L, request, 999L)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `교체 나가는 선수를 찾을 수 없으면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns null

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            assertThatThrownBy {
                gameSubstitutionService.substitutePlayer(1L, request, 999L)
            }.isInstanceOf(GamePlayerNotFoundException::class.java)
        }

        @Test
        fun `교체 들어오는 선수를 찾을 수 없으면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val outgoingPlayer = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns outgoingPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns null

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            assertThatThrownBy {
                gameSubstitutionService.substitutePlayer(1L, request, 999L)
            }.isInstanceOf(GamePlayerNotFoundException::class.java)
        }

        @Test
        fun `현재 출전 중이 아닌 선수를 교체 내보내려 하면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val notPlayingPlayer = createBenchPlayer(10L, Position.LEFT_FIELD) // isCurrentlyPlaying = false
            val incomingPlayer = createBenchPlayer(20L, Position.CENTER_FIELD)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns notPlayingPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns incomingPlayer

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when & then
            assertThatThrownBy {
                gameSubstitutionService.substitutePlayer(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만 교체")
        }
    }

    @Nested
    @DisplayName("DH 해제 규칙")
    inner class DhReleaseRules {
        @Test
        fun `투수가 DH 타순으로 들어오면 DH 규칙이 해제된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val dhPlayer = createDhPlayer(10L, battingOrder = 4)
            val reliefPitcher = createBenchPlayer(20L, Position.RELIEF_PITCHER)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns dhPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns reliefPitcher
            every { gamePlayerRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            val savedEvent = mockk<GameEvent>(relaxed = true)
            every { savedEvent.eventType } returns GameEventType.SUBSTITUTION
            every { gameEventRepository.save(any()) } returns savedEvent

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.RELIEF_PITCHER,
                    newBattingOrder = 4, // DH 타순과 일치
                )

            // when
            gameSubstitutionService.substitutePlayer(1L, request, 999L)

            // then - DH 해제 후 퇴장 처리
            assertThat(dhPlayer.isDesignatedHitter).isFalse()
            verify { gamePlayerRepository.save(dhPlayer) }
        }

        @Test
        fun `하위 이닝(말)에서 교체하면 설명에 '말'이 포함된다`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    isTopInning = false
                }
            val outgoingPlayer = createStartingPlayer(10L, Position.LEFT_FIELD, 5)
            val incomingPlayer = createBenchPlayer(20L, Position.CENTER_FIELD)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns outgoingPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns incomingPlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            var capturedEvent: GameEvent? = null
            every { gameEventRepository.save(any()) } answers {
                capturedEvent = firstArg()
                firstArg()
            }

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.LEFT_FIELD,
                    newBattingOrder = 5,
                )

            // when
            gameSubstitutionService.substitutePlayer(1L, request, 999L)

            // then
            assertThat(capturedEvent).isNotNull()
            assertThat(capturedEvent!!.description).contains("말")
        }

        @Test
        fun `DH 해제 시 설명에 DH 규칙 해제가 포함된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val dhPlayer = createDhPlayer(10L, battingOrder = 4)
            val reliefPitcher = createBenchPlayer(20L, Position.RELIEF_PITCHER)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns dhPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns reliefPitcher
            every { gamePlayerRepository.save(any()) } answers { firstArg() }
            every { gameRepository.save(any()) } answers { firstArg() }

            var capturedEvent: GameEvent? = null
            every { gameEventRepository.save(any()) } answers {
                capturedEvent = firstArg()
                firstArg()
            }

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.RELIEF_PITCHER,
                    newBattingOrder = 4, // DH 타순과 일치
                )

            // when
            gameSubstitutionService.substitutePlayer(1L, request, 999L)

            // then
            assertThat(capturedEvent).isNotNull()
            assertThat(capturedEvent!!.description).contains("DH 규칙 해제")
        }

        @Test
        fun `투수가 DH 타순이 아닌 타순으로 교체되면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val dhPlayer = createDhPlayer(10L, battingOrder = 4)
            val reliefPitcher = createBenchPlayer(20L, Position.RELIEF_PITCHER)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns dhPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns reliefPitcher

            val request =
                SubstitutionRequest(
                    gameTeamId = 1L,
                    outgoingPlayerId = 10L,
                    incomingPlayerId = 20L,
                    newPosition = Position.RELIEF_PITCHER,
                    newBattingOrder = 5, // DH 타순(4번)과 불일치
                )

            // when & then
            assertThatThrownBy {
                gameSubstitutionService.substitutePlayer(1L, request, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("DH 타순")
        }
    }

    // ──────────────────────────── helpers ────────────────────────────

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
            )
        val league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = null,
                foundedYear = 2020,
                divisionLevel = 1,
                description = null,
                logoUrl = null,
            )
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
            )
        val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
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

    private fun createStartingPlayer(
        id: Long,
        position: Position,
        battingOrder: Int,
    ): GamePlayer {
        val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
        val player = mockk<com.nextup.core.domain.player.Player>(relaxed = true)
        every { player.name } returns "선수$id"
        return GamePlayer.createStarter(
            gameTeam = gameTeam,
            player = player,
            position = position,
            battingOrder = battingOrder,
        ).apply {
            val f = GamePlayer::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.set(this, id)
        }
    }

    private fun createBenchPlayer(
        id: Long,
        position: Position,
    ): GamePlayer {
        val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
        val player = mockk<com.nextup.core.domain.player.Player>(relaxed = true)
        every { player.name } returns "선수$id"
        return GamePlayer.createBench(
            gameTeam = gameTeam,
            player = player,
            position = position,
        ).apply {
            val f = GamePlayer::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.set(this, id)
        }
    }

    private fun createDhPlayer(
        id: Long,
        battingOrder: Int,
    ): GamePlayer {
        val gameTeam = mockk<com.nextup.core.domain.game.GameTeam>(relaxed = true)
        val player = mockk<com.nextup.core.domain.player.Player>(relaxed = true)
        every { player.name } returns "DH선수$id"
        return GamePlayer.createStarter(
            gameTeam = gameTeam,
            player = player,
            position = Position.DESIGNATED_HITTER,
            battingOrder = battingOrder,
        ).apply {
            val f = GamePlayer::class.java.getDeclaredField("id")
            f.isAccessible = true
            f.set(this, id)
            setAsDesignatedHitter(pitcherOrder = 9)
        }
    }
}
