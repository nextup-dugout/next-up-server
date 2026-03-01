package com.nextup.infrastructure.listener

import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GameCancelEventListener 테스트")
class GameCancelEventListenerTest {
    private val seasonBattingStatsRepository = mockk<SeasonBattingStatsRepositoryPort>()
    private val seasonPitchingStatsRepository = mockk<SeasonPitchingStatsRepositoryPort>()
    private val battingRecordRepository = mockk<BattingRecordRepositoryPort>()
    private val pitchingRecordRepository = mockk<PitchingRecordRepositoryPort>()

    private val listener =
        GameCancelEventListener(
            seasonBattingStatsRepository = seasonBattingStatsRepository,
            seasonPitchingStatsRepository = seasonPitchingStatsRepository,
            battingRecordRepository = battingRecordRepository,
            pitchingRecordRepository = pitchingRecordRepository,
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
    private val mockBatterGamePlayer = mockk<GamePlayer>()
    private val mockPitcherGamePlayer = mockk<GamePlayer>()

    private val gameId = 100L
    private val cancelEvent = GameCancelledEvent(gameId = gameId)

    @BeforeEach
    fun setUp() {
        every { mockBattingRecord.gamePlayer } returns mockBatterGamePlayer
        every { mockBatterGamePlayer.player } returns testBatter
        every { mockPitchingRecord.gamePlayer } returns mockPitcherGamePlayer
        every { mockPitcherGamePlayer.player } returns testPitcher
    }

    @Nested
    @DisplayName("onGameCancelled - 경기 취소 이벤트 처리")
    inner class OnGameCancelled {
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
        fun `타격 및 투구 기록이 없는 경우 아무것도 저장하지 않음`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameCancelled(cancelEvent)

            // then
            verify(exactly = 0) { seasonBattingStatsRepository.save(any()) }
            verify(exactly = 0) { seasonPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `시즌 타격 통계가 없는 선수는 건너뜀`() {
            // given
            every { battingRecordRepository.findAllByGameId(gameId) } returns listOf(mockBattingRecord)
            every { pitchingRecordRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonBattingStatsRepository.findAllByGameId(gameId) } returns emptyList()
            every { seasonPitchingStatsRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            listener.onGameCancelled(cancelEvent)

            // then: 통계가 없으므로 save 호출 없음
            verify(exactly = 0) { seasonBattingStatsRepository.save(any()) }
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
}
