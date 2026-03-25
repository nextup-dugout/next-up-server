package com.nextup.infrastructure.listener

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.PlateAppearanceRecordedEvent
import com.nextup.core.domain.event.PlateAppearanceUndoneEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("PitchingStatsEventListener 테스트")
class PitchingStatsEventListenerTest {
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val careerPitchingStatsRepository = mockk<CareerPitchingStatsRepositoryPort>()
    private val pitchingRecordRepository = mockk<PitchingRecordRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val playerRepository = mockk<PlayerRepositoryPort>()

    private val listener =
        PitchingStatsEventListener(
            seasonPitchingStatsRepository = seasonPitchingStatsRepository,
            careerPitchingStatsRepository = careerPitchingStatsRepository,
            pitchingRecordRepository = pitchingRecordRepository,
            gameRepository = gameRepository,
            playerRepository = playerRepository,
        )

    private val testPitcher =
        Player(
            name = "박투수",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 2L,
        )

    private val mockGame = mockk<Game>()
    private val mockCompetition = mockk<Competition>()

    @BeforeEach
    fun setUp() {
        every { mockGame.scheduledAt } returns LocalDateTime.of(2024, 5, 15, 18, 0)
        every { mockGame.competition } returns mockCompetition
        every { mockCompetition.type } returns CompetitionType.LEAGUE
        every { gameRepository.findByIdOrNull(any()) } returns mockGame
    }

    @Nested
    @DisplayName("onPlateAppearanceRecorded - 타석 결과 기록 이벤트 처리 (투수)")
    inner class OnPlateAppearanceRecorded {
        @Test
        fun `타석 결과가 투수 시즌 통계에 즉시 반영됨`() {
            // given
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.hitsAllowed).isEqualTo(1)
            verify { seasonPitchingStatsRepository.save(pitchingStats) }
        }

        @Test
        fun `홈런 기록 시 투수 통계에 피안타와 피홈런 모두 반영됨`() {
            // given
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.HOME_RUN,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.hitsAllowed).isEqualTo(1)
            assertThat(pitchingStats.homeRunsAllowed).isEqualTo(1)
        }

        @Test
        fun `삼진 기록 시 투수 삼진 통계 증가`() {
            // given
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.STRIKEOUT,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.strikeouts).isEqualTo(1)
        }

        @Test
        fun `볼넷 기록 시 투수 볼넷 통계 증가`() {
            // given
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.WALK,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then
            assertThat(pitchingStats.battersFaced).isEqualTo(1)
            assertThat(pitchingStats.walksAllowed).isEqualTo(1)
        }

        @Test
        fun `투수 시즌 통계가 없는 경우 자동 생성 후 투수 통계 갱신됨`() {
            // given
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns null
            every { playerRepository.findByIdOrNull(testPitcher.id) } returns testPitcher
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceRecorded(event)

            // then: 자동 생성 + 갱신 = 2회
            verify(exactly = 2) { seasonPitchingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPitcher.id) }
        }

        @Test
        fun `선수를 찾을 수 없는 경우 PlayerNotFoundException 발생`() {
            // given
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(999L, 2024, 20L, any())
            } returns null
            every { playerRepository.findByIdOrNull(999L) } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = 999L,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<PlayerNotFoundException> {
                listener.onPlateAppearanceRecorded(event)
            }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                PlateAppearanceRecordedEvent(
                    gameId = 999L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onPlateAppearanceRecorded(event)
            }
        }
    }

    @Nested
    @DisplayName("onPlateAppearanceUndone - 타석 결과 취소 이벤트 처리 (투수)")
    inner class OnPlateAppearanceUndone {
        @Test
        fun `Undo 시 투수 통계가 역산됨`() {
            // given
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)
            pitchingStats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns pitchingStats
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then
            assertThat(pitchingStats.battersFaced).isZero
            assertThat(pitchingStats.hitsAllowed).isZero
            verify { seasonPitchingStatsRepository.save(pitchingStats) }
        }

        @Test
        fun `시즌 투수 통계가 없는 경우 자동 생성 후 역산 처리`() {
            // given
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns null
            every { playerRepository.findByIdOrNull(testPitcher.id) } returns testPitcher
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 10L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when
            listener.onPlateAppearanceUndone(event)

            // then: 자동 생성 + 갱신 = 2회
            verify(exactly = 2) { seasonPitchingStatsRepository.save(any()) }
            verify { playerRepository.findByIdOrNull(testPitcher.id) }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val event =
                PlateAppearanceUndoneEvent(
                    gameId = 999L,
                    playerId = 1L,
                    pitcherId = testPitcher.id,
                    batterTeamId = 10L,
                    pitcherTeamId = 20L,
                    result = PlateAppearanceResult.SINGLE,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onPlateAppearanceUndone(event)
            }
        }
    }

    @Nested
    @DisplayName("onGameResultConfirmed - 경기 결과 확정 이벤트 처리 (투수)")
    inner class OnGameResultConfirmed {
        private val mockPitchingRecord = mockk<PitchingRecord>(relaxed = true)
        private val mockPitcherGamePlayer = mockk<GamePlayer>()
        private val mockPitcherGameTeam = mockk<GameTeam>()
        private val mockPitcherTeam = mockk<com.nextup.core.domain.team.Team>()

        private val gameId = 10L
        private val event =
            GameResultConfirmedEvent(
                gameId = gameId,
                homeTeamId = 1L,
                awayTeamId = 2L,
                homeScore = 3,
                awayScore = 1,
            )

        @BeforeEach
        fun setUpRecords() {
            every { mockPitchingRecord.gamePlayer } returns mockPitcherGamePlayer
            every { mockPitcherGamePlayer.player } returns testPitcher
            every { mockPitcherGamePlayer.gameTeam } returns mockPitcherGameTeam
            every { mockPitcherGameTeam.team } returns mockPitcherTeam
            every { mockPitcherTeam.id } returns 20L
        }

        @Test
        fun `경기 종료 시 SeasonPitchingStats가 신규 생성되고 기록이 누적됨`() {
            // given
            val careerPitching = CareerPitchingStats.create(testPitcher)

            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns null
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 새 SeasonPitchingStats 생성 및 저장 확인
            verify(exactly = 1) { seasonPitchingStatsRepository.save(any()) }
            verify(exactly = 1) { careerPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `경기 종료 시 기존 SeasonPitchingStats에 기록이 누적됨`() {
            // given
            val seasonPitching = SeasonPitchingStats.create(testPitcher, 2024)
            val careerPitching = CareerPitchingStats.create(testPitcher)
            careerPitching.addSeason()

            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns seasonPitching
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            val initialGames = seasonPitching.gamesPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: gamesPlayed 가 1 증가함
            assertThat(seasonPitching.gamesPlayed).isEqualTo(initialGames + 1)
            verify(exactly = 1) { seasonPitchingStatsRepository.save(seasonPitching) }
        }

        @Test
        fun `경기 종료 시 기존 SeasonPitchingStats에 실시간 갱신 필드가 중복 추가되지 않음`() {
            // given: 경기 중 실시간 갱신으로 피안타 3, 삼진 2, 볼넷 1이 이미 반영된 상태
            val seasonPitching = SeasonPitchingStats.create(testPitcher, 2024)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            seasonPitching.applyLiveUpdate(PlateAppearanceResult.WALK)

            val careerPitching = CareerPitchingStats.create(testPitcher)
            careerPitching.addSeason()

            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)
            every { pitchingRecord.gamePlayer } returns mockPitcherGamePlayer
            every { pitchingRecord.hitsAllowed } returns 3
            every { pitchingRecord.strikeouts } returns 2
            every { pitchingRecord.walksAllowed } returns 1
            every { pitchingRecord.homeRunsAllowed } returns 1
            every { pitchingRecord.hitBatsmen } returns 0
            every { pitchingRecord.battersFaced } returns 6
            every { pitchingRecord.inningsPitchedOuts } returns 9
            every { pitchingRecord.earnedRuns } returns 2
            every { pitchingRecord.runsAllowed } returns 3
            every { pitchingRecord.wildPitches } returns 1
            every { pitchingRecord.balks } returns 0
            every { pitchingRecord.isStartingPitcher } returns true
            every { pitchingRecord.pitchesThrown } returns null
            every { pitchingRecord.strikesThrown } returns null
            every { pitchingRecord.decision } returns com.nextup.core.domain.game.PitchingDecision.WIN

            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(pitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns seasonPitching
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 실시간 갱신 필드는 중복 추가되지 않음 (applyLiveUpdate 값 그대로)
            assertThat(seasonPitching.hitsAllowed).isEqualTo(3)
            assertThat(seasonPitching.strikeouts).isEqualTo(2)
            assertThat(seasonPitching.walksAllowed).isEqualTo(1)
            assertThat(seasonPitching.homeRunsAllowed).isEqualTo(1)
            assertThat(seasonPitching.battersFaced).isEqualTo(6)

            // then: 경기 종료 시에만 확정되는 필드는 정상 추가됨
            assertThat(seasonPitching.gamesPlayed).isEqualTo(1)
            assertThat(seasonPitching.gamesStarted).isEqualTo(1)
            assertThat(seasonPitching.inningsPitchedOuts).isEqualTo(9)
            assertThat(seasonPitching.earnedRuns).isEqualTo(2)
            assertThat(seasonPitching.runsAllowed).isEqualTo(3)
            assertThat(seasonPitching.wildPitches).isEqualTo(1)
            assertThat(seasonPitching.wins).isEqualTo(1)
        }

        @Test
        fun `첫 시즌 투수 기록 시 CareerPitchingStats에 addSeason 호출됨`() {
            // given: 시즌 통계 없음(첫 시즌)
            val careerPitching = CareerPitchingStats.create(testPitcher)

            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns null
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            listener.onGameResultConfirmed(event)

            // then: 첫 시즌이므로 seasonsPlayed = 1
            assertThat(careerPitching.seasonsPlayed).isEqualTo(1)
        }

        @Test
        fun `기존 시즌 투수 기록 시 CareerPitchingStats에 addSeason 호출되지 않음`() {
            // given: 시즌 통계 이미 존재
            val existingSeason = SeasonPitchingStats.create(testPitcher, 2024)
            val careerPitching = CareerPitchingStats.create(testPitcher)
            careerPitching.addSeason()

            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every {
                seasonPitchingStatsRepository.findByPlayerIdAndYearAndTeamIdAndCompetitionType(
                    testPitcher.id,
                    2024,
                    20L,
                    any()
                )
            } returns existingSeason
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerPitching
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            val initialSeasons = careerPitching.seasonsPlayed

            // when
            listener.onGameResultConfirmed(event)

            // then: addSeason 호출 안 됨 - seasonsPlayed 변화 없음
            assertThat(careerPitching.seasonsPlayed).isEqualTo(initialSeasons)
        }

        @Test
        fun `투수 기록이 없는 경우 아무것도 저장하지 않음`() {
            // given
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameResultConfirmed(event)

            // then
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
            verify(exactly = 0) { careerPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `경기를 찾을 수 없는 경우 GameNotFoundException 발생`() {
            // given
            every { gameRepository.findByIdOrNull(any()) } returns null

            val badEvent =
                GameResultConfirmedEvent(
                    gameId = 999L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 0,
                    awayScore = 0,
                )

            // when & then
            assertThrows<GameNotFoundException> {
                listener.onGameResultConfirmed(badEvent)
            }
        }
    }
}
