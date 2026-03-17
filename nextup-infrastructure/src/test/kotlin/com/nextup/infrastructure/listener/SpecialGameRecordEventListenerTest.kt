package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.SpecialGameRecordDetectedEvent
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.SpecialGameRecord
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("SpecialGameRecordEventListener 테스트")
class SpecialGameRecordEventListenerTest {
    private val gameRepository = mockk<GameRepositoryPort>()
    private val gameTeamRepository = mockk<GameTeamRepositoryPort>()
    private val battingRecordRepository = mockk<BattingRecordRepositoryPort>()
    private val pitchingRecordRepository = mockk<PitchingRecordRepositoryPort>()
    private val fieldingRecordRepository = mockk<FieldingRecordRepositoryPort>()
    private val gameEventRepository = mockk<GameEventRepositoryPort>()
    private val eventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)

    private val listener =
        SpecialGameRecordEventListener(
            gameRepository = gameRepository,
            gameTeamRepository = gameTeamRepository,
            battingRecordRepository = battingRecordRepository,
            pitchingRecordRepository = pitchingRecordRepository,
            fieldingRecordRepository = fieldingRecordRepository,
            gameEventRepository = gameEventRepository,
            eventPublisher = eventPublisher,
        )

    private lateinit var competition: Competition
    private lateinit var homeTeam: Team
    private lateinit var awayTeam: Team
    private lateinit var game: Game

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)

        competition =
            Competition(
                league = league,
                name = "테스트 대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            )

        game =
            Game.createForTest(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                status = GameStatus.FINISHED,
                currentInning = 9,
                isTopInning = false,
                totalInnings = 9,
                id = 10L,
            )
    }

    private fun createGamePlayer(
        gameTeam: GameTeam,
        position: Position = Position.STARTING_PITCHER,
        id: Long = 0L,
    ): GamePlayer {
        val player =
            Player(
                name = "테스트선수",
                birthDate = LocalDate.of(1990, 1, 1),
                primaryPosition = position,
                id = id,
            )
        return GamePlayer(
            gameTeam = gameTeam,
            player = player,
            position = position,
            battingOrder = 1,
            id = id,
        )
    }

    private fun createEvent(gameId: Long = 10L) =
        GameResultConfirmedEvent(
            gameId = gameId,
            homeTeamId = 1L,
            awayTeamId = 2L,
            homeScore = 5,
            awayScore = 0,
        )

    @Nested
    @DisplayName("퍼펙트게임 감지 및 이벤트 발행")
    inner class PerfectGameDetection {
        @Test
        fun `퍼펙트게임 감지 시 GameEvent 저장 및 도메인 이벤트 발행`() {
            // given
            val homeGameTeam = game.gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayGameTeam = game.gameTeams.first { it.homeAway == HomeAway.AWAY }
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
            homeGameTeam.updateScore(totalScore = 5, totalHits = 8, totalErrors = 0)

            val homePitcher = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)

            every { gameRepository.findByIdOrNull(10L) } returns game
            every { gameTeamRepository.findAllByGameId(10L) } returns listOf(homeGameTeam, awayGameTeam)
            every { battingRecordRepository.findAllByGameId(10L) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(10L) } returns listOf(homePitchingRecord)
            every { fieldingRecordRepository.findAllByGameId(10L) } returns listOf(homeFieldingRecord)
            every { gameEventRepository.save(any()) } returnsArgument 0

            // when
            listener.onGameResultConfirmed(createEvent())

            // then
            val gameEventSlot = slot<GameEvent>()
            verify { gameEventRepository.save(capture(gameEventSlot)) }
            assertThat(gameEventSlot.captured.eventType).isEqualTo(GameEventType.PERFECT_GAME)

            val domainEventSlot = slot<SpecialGameRecordDetectedEvent>()
            verify { eventPublisher.publishEvent(capture(domainEventSlot)) }
            assertThat(domainEventSlot.captured.record).isEqualTo(SpecialGameRecord.PERFECT_GAME)
            assertThat(domainEventSlot.captured.teamId).isEqualTo(homeTeam.id)
            assertThat(domainEventSlot.captured.opponentTeamId).isEqualTo(awayTeam.id)
        }
    }

    @Nested
    @DisplayName("노히트노런 감지 및 이벤트 발행")
    inner class NoHitterDetection {
        @Test
        fun `노히트노런 감지 시 GameEvent 저장 및 도메인 이벤트 발행`() {
            // given
            val homeGameTeam = game.gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayGameTeam = game.gameTeams.first { it.homeAway == HomeAway.AWAY }
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
            homeGameTeam.updateScore(totalScore = 3, totalHits = 5, totalErrors = 0)

            val homePitcher = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)
            homePitchingRecord.recordWalk()

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)

            every { gameRepository.findByIdOrNull(10L) } returns game
            every { gameTeamRepository.findAllByGameId(10L) } returns listOf(homeGameTeam, awayGameTeam)
            every { battingRecordRepository.findAllByGameId(10L) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(10L) } returns listOf(homePitchingRecord)
            every { fieldingRecordRepository.findAllByGameId(10L) } returns listOf(homeFieldingRecord)
            every { gameEventRepository.save(any()) } returnsArgument 0

            // when
            listener.onGameResultConfirmed(createEvent())

            // then
            val gameEventSlot = slot<GameEvent>()
            verify { gameEventRepository.save(capture(gameEventSlot)) }
            assertThat(gameEventSlot.captured.eventType).isEqualTo(GameEventType.NO_HITTER)

            val domainEventSlot = slot<SpecialGameRecordDetectedEvent>()
            verify { eventPublisher.publishEvent(capture(domainEventSlot)) }
            assertThat(domainEventSlot.captured.record).isEqualTo(SpecialGameRecord.NO_HITTER)
        }
    }

    @Nested
    @DisplayName("미감지 케이스")
    inner class NoDetection {
        @Test
        fun `정상 경기(안타 있음)에서는 특수 기록 미감지`() {
            // given
            val homeGameTeam = game.gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayGameTeam = game.gameTeams.first { it.homeAway == HomeAway.AWAY }
            awayGameTeam.updateScore(totalScore = 2, totalHits = 5, totalErrors = 1)
            homeGameTeam.updateScore(totalScore = 5, totalHits = 8, totalErrors = 0)

            every { gameRepository.findByIdOrNull(10L) } returns game
            every { gameTeamRepository.findAllByGameId(10L) } returns listOf(homeGameTeam, awayGameTeam)
            every { battingRecordRepository.findAllByGameId(10L) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(10L) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(10L) } returns emptyList()

            // when
            listener.onGameResultConfirmed(createEvent())

            // then
            verify(exactly = 0) { gameEventRepository.save(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<SpecialGameRecordDetectedEvent>()) }
        }

        @Test
        fun `몰수승 경기에서는 특수 기록 미감지`() {
            // given
            val forfeitedGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.FORFEITED,
                    id = 20L,
                )

            every { gameRepository.findByIdOrNull(20L) } returns forfeitedGame
            every { gameTeamRepository.findAllByGameId(20L) } returns forfeitedGame.gameTeams
            every { battingRecordRepository.findAllByGameId(20L) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(20L) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(20L) } returns emptyList()

            val event =
                GameResultConfirmedEvent(
                    gameId = 20L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 7,
                    awayScore = 0,
                )

            // when
            listener.onGameResultConfirmed(event)

            // then
            verify(exactly = 0) { gameEventRepository.save(any()) }
            verify(exactly = 0) { eventPublisher.publishEvent(any<SpecialGameRecordDetectedEvent>()) }
        }
    }

    @Nested
    @DisplayName("에러 처리")
    inner class ErrorHandling {
        @Test
        fun `경기가 존재하지 않으면 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(999L) } returns null

            val event =
                GameResultConfirmedEvent(
                    gameId = 999L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 0,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onGameResultConfirmed(event)
            }
        }

        @Test
        fun `팀이 2개가 아니면 감지 건너뜀`() {
            // given
            val homeGameTeam = game.gameTeams.first { it.homeAway == HomeAway.HOME }

            every { gameRepository.findByIdOrNull(10L) } returns game
            every { gameTeamRepository.findAllByGameId(10L) } returns listOf(homeGameTeam)

            // when
            listener.onGameResultConfirmed(createEvent())

            // then
            verify(exactly = 0) { battingRecordRepository.findAllByGameId(any()) }
            verify(exactly = 0) { gameEventRepository.save(any()) }
        }
    }
}
