package com.nextup.infrastructure.listener

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerFieldingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonFieldingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerFieldingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonFieldingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import com.nextup.infrastructure.config.CacheConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager

@DisplayName("GameCancelEventListener 테스트")
class GameCancelEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val seasonFieldingStatsRepository = mockk<SeasonFieldingStatsRepositoryPort>()
    private val careerBattingStatsRepository = mockk<CareerBattingStatsRepositoryPort>()
    private val careerPitchingStatsRepository = mockk<CareerPitchingStatsRepositoryPort>()
    private val careerFieldingStatsRepository = mockk<CareerFieldingStatsRepositoryPort>()
    private val battingRecordRepository = mockk<BattingRecordRepositoryPort>()
    private val pitchingRecordRepository = mockk<PitchingRecordRepositoryPort>()
    private val fieldingRecordRepository = mockk<FieldingRecordRepositoryPort>()
    private val gameRepository = mockk<GameRepositoryPort>()
    private val cacheManager = mockk<CacheManager>()
    private val standingsCache = mockk<Cache>(relaxed = true)

    private val listener =
        GameCancelEventListener(
            seasonBattingStatsRepository = seasonBattingStatsRepository,
            seasonPitchingStatsRepository = seasonPitchingStatsRepository,
            seasonFieldingStatsRepository = seasonFieldingStatsRepository,
            careerBattingStatsRepository = careerBattingStatsRepository,
            careerPitchingStatsRepository = careerPitchingStatsRepository,
            careerFieldingStatsRepository = careerFieldingStatsRepository,
            battingRecordRepository = battingRecordRepository,
            pitchingRecordRepository = pitchingRecordRepository,
            fieldingRecordRepository = fieldingRecordRepository,
            gameRepository = gameRepository,
            cacheManager = cacheManager,
        )

    private val testBatter =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private val testPitcher =
        Player(
            name = "박투수",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 2L,
        )

    private val mockBattingRecord = mockk<BattingRecord>(relaxed = true)
    private val mockPitchingRecord = mockk<PitchingRecord>(relaxed = true)
    private val mockFieldingRecord = mockk<FieldingRecord>(relaxed = true)
    private val mockBatterGamePlayer = mockk<GamePlayer>()
    private val mockPitcherGamePlayer = mockk<GamePlayer>()
    private val mockFielderGamePlayer = mockk<GamePlayer>()

    private val gameId = 100L
    private val cancelEvent = GameCancelledEvent(gameId = gameId)
    private val mockGame = mockk<Game>()
    private val mockCompetition = mockk<Competition>()

    @BeforeEach
    fun setUp() {
        every { mockBattingRecord.gamePlayer } returns mockBatterGamePlayer
        every { mockBatterGamePlayer.player } returns testBatter
        every { mockPitchingRecord.gamePlayer } returns mockPitcherGamePlayer
        every { mockPitcherGamePlayer.player } returns testPitcher
        every { mockFieldingRecord.gamePlayer } returns mockFielderGamePlayer
        every { mockFielderGamePlayer.player } returns testBatter

        every { gameRepository.findByIdOrNull(gameId) } returns mockGame
        every { mockGame.competition } returns mockCompetition
        every { mockCompetition.id } returns 200L
        every { cacheManager.getCache(CacheConfig.STANDINGS_CACHE) } returns standingsCache
        every { cacheManager.getCache(CacheConfig.LEADERBOARD_CACHE) } returns mockk(relaxed = true)
        every { cacheManager.getCache(CacheConfig.TEAM_STATS_CACHE) } returns mockk(relaxed = true)
    }

    @Nested
    @DisplayName("onGameCancelled - 경기 취소 이벤트 처리")
    inner class OnGameCancelled {
        @BeforeEach
        fun setUpFieldingDefault() {
            // 기본적으로 수비 기록 없음 (수비 테스트에서만 오버라이드)
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonFieldingStatsRepository.findAllByGameId(gameId) } returns emptyList()
        }

        @Test
        fun `타격 기록이 있는 경우 시즌 타격 통계가 롤백됨`() {
            // given
            val stats = SeasonBattingStats.create(testBatter, 2024)
            // 경기 기여분을 수동으로 추가 (gamesPlayed 1, plateAppearances 1)
            stats.addGameRecord(
                BattingRecord(mockBatterGamePlayer).also {
                    it.applyPlateAppearanceResult(
                        com.nextup.core.domain.game.PlateAppearanceResult.SINGLE,
                    )
                },
            )
            val initialGamesPlayed = stats.gamesPlayed
            val initialHits = stats.hits

            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns listOf(stats)
            every { seasonBattingStatsRepository.save(any()) } returns stats
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerBattingStatsRepository.findByPlayerId(testBatter.id) } returns null

            every { mockBattingRecord.plateAppearances } returns 1
            every { mockBattingRecord.atBats } returns 1
            every { mockBattingRecord.hits } returns 1
            every { mockBattingRecord.doubles } returns 0
            every { mockBattingRecord.triples } returns 0
            every { mockBattingRecord.homeRuns } returns 0
            every { mockBattingRecord.runs } returns 0
            every { mockBattingRecord.runsBattedIn } returns 0
            every { mockBattingRecord.walks } returns 0
            every { mockBattingRecord.intentionalWalks } returns 0
            every { mockBattingRecord.hitByPitch } returns 0
            every { mockBattingRecord.strikeouts } returns 0
            every { mockBattingRecord.sacrificeBunts } returns 0
            every { mockBattingRecord.sacrificeFlies } returns 0
            every { mockBattingRecord.stolenBases } returns 0
            every { mockBattingRecord.caughtStealing } returns 0
            every { mockBattingRecord.groundedIntoDoublePlays } returns 0

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(initialGamesPlayed - 1)
            assertThat(stats.hits).isEqualTo(initialHits - 1)
            verify { seasonBattingStatsRepository.save(stats) }
        }

        @Test
        fun `모든 기록이 없는 경우 아무것도 저장하지 않음`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify(exactly = 0) { seasonBattingStatsRepository.save(any()) }
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
            verify(exactly = 0) { seasonFieldingStatsRepository.save(any()) }
            verify(exactly = 0) { careerBattingStatsRepository.save(any()) }
            verify(exactly = 0) { careerPitchingStatsRepository.save(any()) }
            verify(exactly = 0) { careerFieldingStatsRepository.save(any()) }
        }

        @Test
        fun `시즌 타격 통계가 없는 선수는 건너뜀`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerBattingStatsRepository.findByPlayerId(testBatter.id) } returns null

            // when
            listener.onGameCancelled(cancelEvent)

            // then: 통계가 없으므로 save 호출 없음
            verify(exactly = 0) { seasonBattingStatsRepository.save(any()) }
        }

        @Test
        fun `시즌 투구 통계가 없는 선수는 건너뜀`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns null

            // when
            listener.onGameCancelled(cancelEvent)

            // then: 통계가 없으므로 save 호출 없음
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `투구 기록이 있는 경우 시즌 투구 통계가 롤백됨`() {
            // given
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns listOf(pitchingStats)
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns null

            every { mockPitchingRecord.isStartingPitcher } returns false
            every { mockPitchingRecord.inningsPitchedOuts } returns 0
            every { mockPitchingRecord.earnedRuns } returns 0
            every { mockPitchingRecord.runsAllowed } returns 0
            every { mockPitchingRecord.hitsAllowed } returns 0
            every { mockPitchingRecord.walksAllowed } returns 0
            every { mockPitchingRecord.strikeouts } returns 0
            every { mockPitchingRecord.homeRunsAllowed } returns 0
            every { mockPitchingRecord.hitBatsmen } returns 0
            every { mockPitchingRecord.wildPitches } returns 0
            every { mockPitchingRecord.balks } returns 0
            every { mockPitchingRecord.battersFaced } returns 0
            every { mockPitchingRecord.pitchesThrown } returns null
            every { mockPitchingRecord.strikesThrown } returns null
            every { mockPitchingRecord.decision } returns PitchingDecision.NONE

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify { seasonPitchingStatsRepository.save(pitchingStats) }
        }

        @Test
        fun `수비 기록이 있는 경우 시즌 수비 통계가 롤백됨`() {
            // given
            val fieldingStats = SeasonFieldingStats.create(testBatter, 2024)
            // 경기 기여분을 추가
            fieldingStats.addGameRecord(
                FieldingRecord(mockFielderGamePlayer).also {
                    it.recordPutOut()
                    it.recordAssist()
                },
            )
            val initialGamesPlayed = fieldingStats.gamesPlayed
            val initialPutOuts = fieldingStats.putOuts

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every { seasonFieldingStatsRepository.findAllByGameId(gameId) } returns listOf(fieldingStats)
            every { seasonFieldingStatsRepository.save(any()) } returns fieldingStats
            every { careerFieldingStatsRepository.findByPlayerId(testBatter.id) } returns null

            every { mockFieldingRecord.putOuts } returns 1
            every { mockFieldingRecord.assists } returns 1
            every { mockFieldingRecord.errors } returns 0
            every { mockFieldingRecord.doublePlays } returns 0
            every { mockFieldingRecord.passedBalls } returns 0

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            assertThat(fieldingStats.gamesPlayed).isEqualTo(initialGamesPlayed - 1)
            assertThat(fieldingStats.putOuts).isEqualTo(initialPutOuts - 1)
            verify { seasonFieldingStatsRepository.save(fieldingStats) }
        }

        @Test
        fun `시즌 수비 통계가 없는 선수는 건너뜀`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every { seasonFieldingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerFieldingStatsRepository.findByPlayerId(testBatter.id) } returns null

            // when
            listener.onGameCancelled(cancelEvent)

            // then: 통계가 없으므로 save 호출 없음
            verify(exactly = 0) { seasonFieldingStatsRepository.save(any()) }
        }

        @Test
        fun `타격과 투구 기록이 모두 있는 경우 모두 롤백됨`() {
            // given
            val battingStats = SeasonBattingStats.create(testBatter, 2024)
            val pitchingStats = SeasonPitchingStats.create(testPitcher, 2024)

            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns listOf(battingStats)
            every { seasonBattingStatsRepository.save(any()) } returns battingStats
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns listOf(pitchingStats)
            every { seasonPitchingStatsRepository.save(any()) } returns pitchingStats
            every { careerBattingStatsRepository.findByPlayerId(testBatter.id) } returns null
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns null

            every { mockBattingRecord.plateAppearances } returns 0
            every { mockBattingRecord.atBats } returns 0
            every { mockBattingRecord.hits } returns 0
            every { mockBattingRecord.doubles } returns 0
            every { mockBattingRecord.triples } returns 0
            every { mockBattingRecord.homeRuns } returns 0
            every { mockBattingRecord.runs } returns 0
            every { mockBattingRecord.runsBattedIn } returns 0
            every { mockBattingRecord.walks } returns 0
            every { mockBattingRecord.intentionalWalks } returns 0
            every { mockBattingRecord.hitByPitch } returns 0
            every { mockBattingRecord.strikeouts } returns 0
            every { mockBattingRecord.sacrificeBunts } returns 0
            every { mockBattingRecord.sacrificeFlies } returns 0
            every { mockBattingRecord.stolenBases } returns 0
            every { mockBattingRecord.caughtStealing } returns 0
            every { mockBattingRecord.groundedIntoDoublePlays } returns 0

            every { mockPitchingRecord.isStartingPitcher } returns false
            every { mockPitchingRecord.inningsPitchedOuts } returns 0
            every { mockPitchingRecord.earnedRuns } returns 0
            every { mockPitchingRecord.runsAllowed } returns 0
            every { mockPitchingRecord.hitsAllowed } returns 0
            every { mockPitchingRecord.walksAllowed } returns 0
            every { mockPitchingRecord.strikeouts } returns 0
            every { mockPitchingRecord.homeRunsAllowed } returns 0
            every { mockPitchingRecord.hitBatsmen } returns 0
            every { mockPitchingRecord.wildPitches } returns 0
            every { mockPitchingRecord.balks } returns 0
            every { mockPitchingRecord.battersFaced } returns 0
            every { mockPitchingRecord.pitchesThrown } returns null
            every { mockPitchingRecord.strikesThrown } returns null
            every { mockPitchingRecord.decision } returns PitchingDecision.NONE

            // when
            listener.onGameCancelled(cancelEvent)

            // then: 타격과 투구 모두 저장됨
            verify { seasonBattingStatsRepository.save(battingStats) }
            verify { seasonPitchingStatsRepository.save(pitchingStats) }
        }
    }

    @Nested
    @DisplayName("onGameCancelled - 커리어 스탯 롤백")
    inner class CareerStatsRollback {
        @BeforeEach
        fun setUpDefault() {
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonFieldingStatsRepository.findAllByGameId(gameId) } returns emptyList()
        }

        @Test
        fun `타격 기록이 있는 경우 커리어 타격 통계도 롤백됨`() {
            // given
            val careerStats = CareerBattingStats.create(testBatter)
            careerStats.addGameRecord(
                BattingRecord(mockBatterGamePlayer).also {
                    it.applyPlateAppearanceResult(
                        com.nextup.core.domain.game.PlateAppearanceResult.SINGLE,
                    )
                },
            )
            val initialGamesPlayed = careerStats.gamesPlayed
            val initialHits = careerStats.hits

            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerBattingStatsRepository.findByPlayerId(testBatter.id) } returns careerStats
            every { careerBattingStatsRepository.save(any()) } returns careerStats

            every { mockBattingRecord.plateAppearances } returns 1
            every { mockBattingRecord.atBats } returns 1
            every { mockBattingRecord.hits } returns 1
            every { mockBattingRecord.doubles } returns 0
            every { mockBattingRecord.triples } returns 0
            every { mockBattingRecord.homeRuns } returns 0
            every { mockBattingRecord.runs } returns 0
            every { mockBattingRecord.runsBattedIn } returns 0
            every { mockBattingRecord.walks } returns 0
            every { mockBattingRecord.intentionalWalks } returns 0
            every { mockBattingRecord.hitByPitch } returns 0
            every { mockBattingRecord.strikeouts } returns 0
            every { mockBattingRecord.sacrificeBunts } returns 0
            every { mockBattingRecord.sacrificeFlies } returns 0
            every { mockBattingRecord.stolenBases } returns 0
            every { mockBattingRecord.caughtStealing } returns 0
            every { mockBattingRecord.groundedIntoDoublePlays } returns 0

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            assertThat(careerStats.gamesPlayed).isEqualTo(initialGamesPlayed - 1)
            assertThat(careerStats.hits).isEqualTo(initialHits - 1)
            verify { careerBattingStatsRepository.save(careerStats) }
        }

        @Test
        fun `커리어 타격 통계가 없는 선수는 건너뜀`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerBattingStatsRepository.findByPlayerId(testBatter.id) } returns null

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify(exactly = 0) { careerBattingStatsRepository.save(any()) }
        }

        @Test
        fun `투구 기록이 있는 경우 커리어 투구 통계도 롤백됨`() {
            // given
            val careerStats = CareerPitchingStats.create(testPitcher)
            careerStats.addGameRecord(mockPitchingRecord)

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns careerStats
            every { careerPitchingStatsRepository.save(any()) } returns careerStats

            every { mockPitchingRecord.isStartingPitcher } returns false
            every { mockPitchingRecord.inningsPitchedOuts } returns 9
            every { mockPitchingRecord.earnedRuns } returns 2
            every { mockPitchingRecord.runsAllowed } returns 3
            every { mockPitchingRecord.hitsAllowed } returns 5
            every { mockPitchingRecord.walksAllowed } returns 1
            every { mockPitchingRecord.strikeouts } returns 4
            every { mockPitchingRecord.homeRunsAllowed } returns 1
            every { mockPitchingRecord.hitBatsmen } returns 0
            every { mockPitchingRecord.wildPitches } returns 0
            every { mockPitchingRecord.balks } returns 0
            every { mockPitchingRecord.battersFaced } returns 20
            every { mockPitchingRecord.pitchesThrown } returns null
            every { mockPitchingRecord.strikesThrown } returns null
            every { mockPitchingRecord.decision } returns PitchingDecision.NONE

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            assertThat(careerStats.gamesPlayed).isEqualTo(0)
            assertThat(careerStats.inningsPitchedOuts).isEqualTo(0)
            verify { careerPitchingStatsRepository.save(careerStats) }
        }

        @Test
        fun `커리어 투구 통계가 없는 선수는 건너뜀`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns listOf(mockPitchingRecord)
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerPitchingStatsRepository.findByPlayerId(testPitcher.id) } returns null

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify(exactly = 0) { careerPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `수비 기록이 있는 경우 커리어 수비 통계도 롤백됨`() {
            // given
            val careerStats = CareerFieldingStats.create(testBatter)
            careerStats.addGameRecord(
                FieldingRecord(mockFielderGamePlayer).also {
                    it.recordPutOut()
                    it.recordAssist()
                },
            )
            val initialGamesPlayed = careerStats.gamesPlayed
            val initialPutOuts = careerStats.putOuts

            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every { seasonFieldingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerFieldingStatsRepository.findByPlayerId(testBatter.id) } returns careerStats
            every { careerFieldingStatsRepository.save(any()) } returns careerStats

            every { mockFieldingRecord.putOuts } returns 1
            every { mockFieldingRecord.assists } returns 1
            every { mockFieldingRecord.errors } returns 0
            every { mockFieldingRecord.doublePlays } returns 0
            every { mockFieldingRecord.passedBalls } returns 0

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            assertThat(careerStats.gamesPlayed).isEqualTo(initialGamesPlayed - 1)
            assertThat(careerStats.putOuts).isEqualTo(initialPutOuts - 1)
            verify { careerFieldingStatsRepository.save(careerStats) }
        }

        @Test
        fun `커리어 수비 통계가 없는 선수는 건너뜀`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)
            every { seasonFieldingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerFieldingStatsRepository.findByPlayerId(testBatter.id) } returns null

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify(exactly = 0) { careerFieldingStatsRepository.save(any()) }
        }

        @Test
        fun `시즌과 커리어 통계가 모두 있으면 둘 다 롤백됨`() {
            // given
            val seasonStats = SeasonBattingStats.create(testBatter, 2024)
            val careerStats = CareerBattingStats.create(testBatter)

            // 각각 경기 기여분 추가
            val realRecord =
                BattingRecord(mockBatterGamePlayer).also {
                    it.applyPlateAppearanceResult(
                        com.nextup.core.domain.game.PlateAppearanceResult.SINGLE,
                    )
                }
            seasonStats.addGameRecord(realRecord)
            careerStats.addGameRecord(realRecord)

            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns listOf(seasonStats)
            every { seasonBattingStatsRepository.save(any()) } returns seasonStats
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { careerBattingStatsRepository.findByPlayerId(testBatter.id) } returns careerStats
            every { careerBattingStatsRepository.save(any()) } returns careerStats

            every { mockBattingRecord.plateAppearances } returns 1
            every { mockBattingRecord.atBats } returns 1
            every { mockBattingRecord.hits } returns 1
            every { mockBattingRecord.doubles } returns 0
            every { mockBattingRecord.triples } returns 0
            every { mockBattingRecord.homeRuns } returns 0
            every { mockBattingRecord.runs } returns 0
            every { mockBattingRecord.runsBattedIn } returns 0
            every { mockBattingRecord.walks } returns 0
            every { mockBattingRecord.intentionalWalks } returns 0
            every { mockBattingRecord.hitByPitch } returns 0
            every { mockBattingRecord.strikeouts } returns 0
            every { mockBattingRecord.sacrificeBunts } returns 0
            every { mockBattingRecord.sacrificeFlies } returns 0
            every { mockBattingRecord.stolenBases } returns 0
            every { mockBattingRecord.caughtStealing } returns 0
            every { mockBattingRecord.groundedIntoDoublePlays } returns 0

            // when
            listener.onGameCancelled(cancelEvent)

            // then: 시즌과 커리어 모두 롤백됨
            verify { seasonBattingStatsRepository.save(seasonStats) }
            verify { careerBattingStatsRepository.save(careerStats) }
            assertThat(seasonStats.gamesPlayed).isEqualTo(0)
            assertThat(careerStats.gamesPlayed).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("onGameCancelled - 순위 캐시 무효화")
    inner class StandingsCacheEviction {
        @BeforeEach
        fun setUpDefault() {
            every { fieldingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonFieldingStatsRepository.findAllByGameId(gameId) } returns emptyList()
        }

        @Test
        fun `경기 취소 시 해당 대회의 순위 캐시가 무효화된다`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify { cacheManager.getCache(CacheConfig.STANDINGS_CACHE) }
            verify { standingsCache.evict(200L) }
        }

        @Test
        fun `경기를 찾을 수 없으면 캐시 무효화를 건너뛴다`() {
            // given
            every { gameRepository.findByIdOrNull(gameId) } returns null
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify(exactly = 0) { standingsCache.evict(any()) }
        }
    }
}
