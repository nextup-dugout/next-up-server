package com.nextup.core.domain.stats

import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SeasonPitchingStats.revertGameRecord 테스트")
class SeasonPitchingStatsRevertGameRecordTest {
    private val testPlayer =
        Player(
            name = "박투수",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private fun createMockPitchingRecord(
        isStartingPitcher: Boolean = false,
        inningsPitchedOuts: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        hitsAllowed: Int = 0,
        walksAllowed: Int = 0,
        strikeouts: Int = 0,
        homeRunsAllowed: Int = 0,
        hitBatsmen: Int = 0,
        wildPitches: Int = 0,
        balks: Int = 0,
        battersFaced: Int = 0,
        pitchesThrown: Int? = null,
        strikesThrown: Int? = null,
        decision: PitchingDecision = PitchingDecision.NONE,
    ): PitchingRecord {
        val record = mockk<PitchingRecord>()
        every { record.isStartingPitcher } returns isStartingPitcher
        every { record.inningsPitchedOuts } returns inningsPitchedOuts
        every { record.earnedRuns } returns earnedRuns
        every { record.runsAllowed } returns runsAllowed
        every { record.hitsAllowed } returns hitsAllowed
        every { record.walksAllowed } returns walksAllowed
        every { record.strikeouts } returns strikeouts
        every { record.homeRunsAllowed } returns homeRunsAllowed
        every { record.hitBatsmen } returns hitBatsmen
        every { record.wildPitches } returns wildPitches
        every { record.balks } returns balks
        every { record.battersFaced } returns battersFaced
        every { record.pitchesThrown } returns pitchesThrown
        every { record.strikesThrown } returns strikesThrown
        every { record.decision } returns decision
        return record
    }

    @Nested
    @DisplayName("경기 투수 기여분 롤백")
    inner class RevertGameRecord {
        private lateinit var stats: SeasonPitchingStats

        @BeforeEach
        fun setUp() {
            stats = SeasonPitchingStats.create(testPlayer, 2024)
        }

        @Test
        fun `기본 투구 기여분이 정확히 차감됨`() {
            // given: 시즌 통계에 1경기 분량 추가
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val realRecord =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    recordOut(isStrikeout = true)
                    recordOut(isStrikeout = true)
                    recordOut(isStrikeout = false)
                    recordHit(isHomeRun = false, runsScored = 1, earnedRuns = 1)
                    recordWalk()
                    recordHitByPitch()
                    recordWildPitch()
                    recordBalk()
                }
            stats.addGameRecord(realRecord)

            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isEqualTo(1)
            assertThat(stats.inningsPitchedOuts).isEqualTo(3)
            assertThat(stats.strikeouts).isEqualTo(2)
            assertThat(stats.hitsAllowed).isEqualTo(1)
            assertThat(stats.walksAllowed).isEqualTo(1)
            assertThat(stats.hitBatsmen).isEqualTo(1)
            assertThat(stats.wildPitches).isEqualTo(1)
            assertThat(stats.balks).isEqualTo(1)

            // when: 롤백
            stats.revertGameRecord(realRecord)

            // then: 모든 값이 0으로 복원
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.inningsPitchedOuts).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.hitsAllowed).isZero
            assertThat(stats.walksAllowed).isZero
            assertThat(stats.hitBatsmen).isZero
            assertThat(stats.wildPitches).isZero
            assertThat(stats.balks).isZero
            assertThat(stats.earnedRuns).isZero
            assertThat(stats.runsAllowed).isZero
        }

        @Test
        fun `선발 투수가 아닌 경우 gamesStarted는 차감되지 않음`() {
            // given
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val realRecord = PitchingRecord.create(gamePlayer, isStartingPitcher = false)
            stats.addGameRecord(realRecord)

            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isZero

            // when
            stats.revertGameRecord(realRecord)

            // then
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.gamesStarted).isZero
        }

        @Test
        fun `선발 투수인 경우 gamesStarted도 차감됨`() {
            // given
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record1 = PitchingRecord.create(gamePlayer, isStartingPitcher = true)
            val record2 = PitchingRecord.create(gamePlayer, isStartingPitcher = true)
            stats.addGameRecord(record1)
            stats.addGameRecord(record2)

            assertThat(stats.gamesStarted).isEqualTo(2)

            // when
            stats.revertGameRecord(record1)

            // then
            assertThat(stats.gamesStarted).isEqualTo(1)
        }

        @Test
        fun `승리 결정 롤백`() {
            // given
            val record =
                createMockPitchingRecord(
                    decision = PitchingDecision.WIN,
                )
            stats.addGameRecord(record)
            assertThat(stats.wins).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.wins).isZero
        }

        @Test
        fun `패배 결정 롤백`() {
            // given
            val record =
                createMockPitchingRecord(
                    decision = PitchingDecision.LOSS,
                )
            stats.addGameRecord(record)
            assertThat(stats.losses).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.losses).isZero
        }

        @Test
        fun `세이브 결정 롤백`() {
            // given
            val record =
                createMockPitchingRecord(
                    decision = PitchingDecision.SAVE,
                )
            stats.addGameRecord(record)
            assertThat(stats.saves).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.saves).isZero
        }

        @Test
        fun `홀드 결정 롤백`() {
            // given
            val record =
                createMockPitchingRecord(
                    decision = PitchingDecision.HOLD,
                )
            stats.addGameRecord(record)
            assertThat(stats.holds).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.holds).isZero
        }

        @Test
        fun `블론세이브 결정 롤백`() {
            // given
            val record =
                createMockPitchingRecord(
                    decision = PitchingDecision.BLOWN_SAVE,
                )
            stats.addGameRecord(record)
            assertThat(stats.blownSaves).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.blownSaves).isZero
        }

        @Test
        fun `NONE 결정은 승패에 영향 없음`() {
            // given
            val record =
                createMockPitchingRecord(
                    decision = PitchingDecision.NONE,
                )
            stats.addGameRecord(record)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.wins).isZero
            assertThat(stats.losses).isZero
            assertThat(stats.saves).isZero
            assertThat(stats.holds).isZero
            assertThat(stats.blownSaves).isZero
        }

        @Test
        fun `투구 수가 있는 경우 투구 수도 차감됨`() {
            // given
            val record =
                createMockPitchingRecord(
                    pitchesThrown = 80,
                    strikesThrown = 50,
                )
            stats.addGameRecord(record)
            assertThat(stats.pitchesThrown).isEqualTo(80)
            assertThat(stats.strikesThrown).isEqualTo(50)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.pitchesThrown).isZero
            assertThat(stats.strikesThrown).isZero
        }

        @Test
        fun `투구 수가 null인 경우 투구 수에 영향 없음`() {
            // given
            val record =
                createMockPitchingRecord(
                    pitchesThrown = null,
                    strikesThrown = null,
                )
            stats.addGameRecord(record)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.pitchesThrown).isNull()
            assertThat(stats.strikesThrown).isNull()
        }

        @Test
        fun `음수 방지 - 기여분보다 큰 값을 롤백해도 0 미만으로 내려가지 않음`() {
            // given: 빈 통계에 큰 기여분 롤백 시도
            val record =
                createMockPitchingRecord(
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    earnedRuns = 3,
                    runsAllowed = 4,
                    hitsAllowed = 8,
                    walksAllowed = 2,
                    strikeouts = 10,
                    homeRunsAllowed = 1,
                    hitBatsmen = 1,
                    wildPitches = 2,
                    balks = 1,
                    battersFaced = 35,
                    pitchesThrown = 100,
                    strikesThrown = 65,
                    decision = PitchingDecision.WIN,
                )

            // when
            stats.revertGameRecord(record)

            // then: 모든 값이 0 (음수 없음)
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.inningsPitchedOuts).isZero
            assertThat(stats.earnedRuns).isZero
            assertThat(stats.runsAllowed).isZero
            assertThat(stats.hitsAllowed).isZero
            assertThat(stats.walksAllowed).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.homeRunsAllowed).isZero
            assertThat(stats.hitBatsmen).isZero
            assertThat(stats.wildPitches).isZero
            assertThat(stats.balks).isZero
            assertThat(stats.battersFaced).isZero
            assertThat(stats.pitchesThrown).isZero
            assertThat(stats.strikesThrown).isZero
            assertThat(stats.wins).isZero
        }

        @Test
        fun `2경기 중 1경기 롤백 시 나머지 경기 기록만 남음`() {
            // given: 2경기 기록이 반영된 시즌 통계
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record1 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    recordOut(isStrikeout = true)
                    recordOut(isStrikeout = true)
                    recordOut(isStrikeout = true)
                    recordHit(isHomeRun = false, runsScored = 1, earnedRuns = 1)
                }
            val record2 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    recordOut(isStrikeout = false)
                    recordOut(isStrikeout = false)
                    recordWalk()
                }

            stats.addGameRecord(record1)
            stats.addGameRecord(record2)

            assertThat(stats.gamesPlayed).isEqualTo(2)
            assertThat(stats.gamesStarted).isEqualTo(1)
            assertThat(stats.strikeouts).isEqualTo(3)
            assertThat(stats.inningsPitchedOuts).isEqualTo(5)

            // when: 첫 번째 경기 롤백
            stats.revertGameRecord(record1)

            // then: 두 번째 경기 기록만 남음
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.inningsPitchedOuts).isEqualTo(2)
            assertThat(stats.walksAllowed).isEqualTo(1)
            assertThat(stats.earnedRuns).isZero
            assertThat(stats.hitsAllowed).isZero
        }

        @Test
        fun `gamesPlayed가 1 감소함`() {
            // given
            val record = createMockPitchingRecord()
            stats.addGameRecord(record)
            assertThat(stats.gamesPlayed).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isZero
        }
    }
}
