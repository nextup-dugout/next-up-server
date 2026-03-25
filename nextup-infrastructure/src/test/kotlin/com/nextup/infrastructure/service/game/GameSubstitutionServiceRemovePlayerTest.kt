package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.PlayerShortageDetectedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlayerShortageResult
import com.nextup.core.domain.game.ScorerLock
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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

@DisplayName("GameSubstitutionServiceImpl - removePlayerWithoutSubstitution")
class GameSubstitutionServiceRemovePlayerTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private val eventPublisher: ApplicationEventPublisher = mockk(relaxed = true)
    private lateinit var service: GameSubstitutionServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        gameEventRepository = mockk(relaxed = true)
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()

        every { pitchingRecordRepository.findByGamePlayerId(any()) } returns null
        every { pitchingRecordRepository.save(any()) } answers { firstArg() }

        service =
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
    @DisplayName("정상 퇴장 시나리오")
    inner class NormalExit {
        @Test
        fun `인원 충분 시 noShortage 결과를 반환한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val gamePlayer = createStartingFieldPlayer(50L, gameTeam, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns gamePlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            // 퇴장 후에도 9명 이상 남음
            val remainingPlayers = createRemainingPlayers(gameTeam, 9)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            val result = service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            assertThat(result.isShortage).isFalse()
            assertThat(result.activePlayerCount).isEqualTo(9)
            assertThat(result.gameTeamId).isEqualTo(100L)
            assertThat(result.teamId).isEqualTo(10L)
            assertThat(result.minimumRequired).isEqualTo(PlayerShortageResult.DEFAULT_MINIMUM_PLAYERS)

            // 퇴장 처리 확인
            assertThat(gamePlayer.isCurrentlyPlaying).isFalse()
            assertThat(gamePlayer.exitInning).isEqualTo(5)

            // 이벤트 저장 확인
            verify { gameEventRepository.save(any()) }
        }

        @Test
        fun `퇴장 이벤트 설명에 이닝 정보가 포함된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val gamePlayer = createStartingFieldPlayer(50L, gameTeam, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns gamePlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val remainingPlayers = createRemainingPlayers(gameTeam, 9)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            verify {
                gameEventRepository.save(
                    match { event ->
                        event.description.contains("퇴장") &&
                            event.description.contains("교체 선수 없음")
                    },
                )
            }
        }

        @Test
        fun `하위 이닝(말)에서 퇴장 시 설명에 말이 포함된다`() {
            // given
            val game =
                createGame(1L, GameStatus.IN_PROGRESS).apply {
                    isTopInning = false
                }
            val gameTeam = createGameTeam(100L, 10L)
            val gamePlayer = createStartingFieldPlayer(50L, gameTeam, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns gamePlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val remainingPlayers = createRemainingPlayers(gameTeam, 9)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            verify {
                gameEventRepository.save(match { event -> event.description.contains("말") })
            }
        }
    }

    @Nested
    @DisplayName("인원 부족 감지")
    inner class ShortageDetection {
        @Test
        fun `퇴장 후 9명 미만이면 shortage 결과를 반환한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val gamePlayer = createStartingFieldPlayer(50L, gameTeam, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns gamePlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            // 퇴장 후 8명만 남음
            val remainingPlayers = createRemainingPlayers(gameTeam, 8)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            val result = service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            assertThat(result.isShortage).isTrue()
            assertThat(result.activePlayerCount).isEqualTo(8)
            assertThat(result.minimumRequired).isEqualTo(9)
        }

        @Test
        fun `인원 부족 시 PlayerShortageDetectedEvent가 발행된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val gamePlayer = createStartingFieldPlayer(50L, gameTeam, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns gamePlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val remainingPlayers = createRemainingPlayers(gameTeam, 8)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            val eventSlot = slot<PlayerShortageDetectedEvent>()

            // when
            service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            verify { eventPublisher.publishEvent(capture(eventSlot)) }
            assertThat(eventSlot.captured.gameId).isEqualTo(1L)
            assertThat(eventSlot.captured.gameTeamId).isEqualTo(100L)
            assertThat(eventSlot.captured.teamId).isEqualTo(10L)
            assertThat(eventSlot.captured.activePlayerCount).isEqualTo(8)
            assertThat(eventSlot.captured.minimumRequired).isEqualTo(9)
        }

        @Test
        fun `인원 충분 시 PlayerShortageDetectedEvent가 발행되지 않는다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val gamePlayer = createStartingFieldPlayer(50L, gameTeam, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns gamePlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val remainingPlayers = createRemainingPlayers(gameTeam, 9)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            verify(exactly = 0) { eventPublisher.publishEvent(any<PlayerShortageDetectedEvent>()) }
        }
    }

    @Nested
    @DisplayName("투수 퇴장 시 이닝 마감")
    inner class PitcherExit {
        @Test
        fun `투수 퇴장 시 PitchingRecord의 closeInning이 호출된다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val pitcher = createStartingFieldPlayer(50L, gameTeam, Position.STARTING_PITCHER, 1)

            val pitchingRecord = PitchingRecord.create(pitcher, isStartingPitcher = true)
            repeat(3) { pitchingRecord.recordOut() }

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns pitcher
            every { gamePlayerRepository.save(any()) } answers { firstArg() }
            every { pitchingRecordRepository.findByGamePlayerId(50L) } returns pitchingRecord

            val remainingPlayers = createRemainingPlayers(gameTeam, 9)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            verify { pitchingRecordRepository.save(pitchingRecord) }
        }

        @Test
        fun `투수 퇴장 시 PitchingRecord가 없으면 저장을 건너뛴다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val pitcher = createStartingFieldPlayer(50L, gameTeam, Position.STARTING_PITCHER, 1)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns pitcher
            every { gamePlayerRepository.save(any()) } answers { firstArg() }
            every { pitchingRecordRepository.findByGamePlayerId(50L) } returns null

            val remainingPlayers = createRemainingPlayers(gameTeam, 9)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            verify(exactly = 0) { pitchingRecordRepository.save(any()) }
        }

        @Test
        fun `비투수 퇴장 시에는 PitchingRecord 조회하지 않는다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val fieldPlayer = createStartingFieldPlayer(50L, gameTeam, Position.LEFT_FIELD, 5)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns fieldPlayer
            every { gamePlayerRepository.save(any()) } answers { firstArg() }

            val remainingPlayers = createRemainingPlayers(gameTeam, 9)
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(1L) } returns remainingPlayers

            // when
            service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)

            // then
            verify(exactly = 0) { pitchingRecordRepository.findByGamePlayerId(50L) }
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    inner class ExceptionCases {
        @Test
        fun `경기를 찾을 수 없으면 예외가 발생한다`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                service.removePlayerWithoutSubstitution(999L, 50L, 5, 999L)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `진행 중이 아닌 경기에서 퇴장을 시도하면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.SCHEDULED)
            every { gameRepository.findByIdOrNull(1L) } returns game

            // when & then
            assertThatThrownBy {
                service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("진행 중인 경기만 선수 퇴장 처리")
        }

        @Test
        fun `선수를 찾을 수 없으면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns null

            // when & then
            assertThatThrownBy {
                service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)
            }.isInstanceOf(GamePlayerNotFoundException::class.java)
        }

        @Test
        fun `현재 출전 중이 아닌 선수를 퇴장시키려 하면 예외가 발생한다`() {
            // given
            val game = createGame(1L, GameStatus.IN_PROGRESS)
            val gameTeam = createGameTeam(100L, 10L)
            val benchPlayer = createBenchPlayer(50L, gameTeam, Position.LEFT_FIELD)

            every { gameRepository.findByIdOrNull(1L) } returns game
            every { gamePlayerRepository.findByIdOrNull(50L) } returns benchPlayer

            // when & then
            assertThatThrownBy {
                service.removePlayerWithoutSubstitution(1L, 50L, 5, 999L)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만 퇴장")
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
            scorerLock = ScorerLock(scorerId = 999L),
            id = id,
        )
    }

    private fun createGameTeam(
        gameTeamId: Long,
        teamId: Long,
    ): GameTeam {
        val gameTeam = mockk<GameTeam>(relaxed = true)
        val team = mockk<Team>(relaxed = true)
        every { team.id } returns teamId
        every { gameTeam.id } returns gameTeamId
        every { gameTeam.team } returns team
        return gameTeam
    }

    private fun createStartingFieldPlayer(
        id: Long,
        gameTeam: GameTeam,
        position: Position,
        battingOrder: Int,
    ): GamePlayer {
        val player = mockk<Player>(relaxed = true)
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
        gameTeam: GameTeam,
        position: Position,
    ): GamePlayer {
        val player = mockk<Player>(relaxed = true)
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

    private fun createRemainingPlayers(
        gameTeam: GameTeam,
        count: Int,
    ): List<GamePlayer> {
        val positions =
            listOf(
                Position.CATCHER,
                Position.FIRST_BASE,
                Position.SECOND_BASE,
                Position.THIRD_BASE,
                Position.SHORTSTOP,
                Position.LEFT_FIELD,
                Position.CENTER_FIELD,
                Position.RIGHT_FIELD,
                Position.STARTING_PITCHER,
                Position.RELIEF_PITCHER,
            )
        return (1..count).map { i ->
            val player = mockk<Player>(relaxed = true)
            val gp =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = positions[i % positions.size],
                    battingOrder = i,
                )
            gp
        }
    }
}
