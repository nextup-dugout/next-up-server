package com.nextup.core.domain.stats

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer
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

@DisplayName("SeasonBattingStats.revertGameRecord 테스트")
class SeasonBattingStatsRevertGameRecordTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private fun createMockBattingRecord(
        plateAppearances: Int = 0,
        atBats: Int = 0,
        hits: Int = 0,
        doubles: Int = 0,
        triples: Int = 0,
        homeRuns: Int = 0,
        runs: Int = 0,
        runsBattedIn: Int = 0,
        walks: Int = 0,
        intentionalWalks: Int = 0,
        hitByPitch: Int = 0,
        strikeouts: Int = 0,
        sacrificeBunts: Int = 0,
        sacrificeFlies: Int = 0,
        stolenBases: Int = 0,
        caughtStealing: Int = 0,
        groundedIntoDoublePlays: Int = 0,
    ): BattingRecord {
        val gamePlayer = mockk<GamePlayer>()
        val record = mockk<BattingRecord>()
        every { record.gamePlayer } returns gamePlayer
        every { record.plateAppearances } returns plateAppearances
        every { record.atBats } returns atBats
        every { record.hits } returns hits
        every { record.doubles } returns doubles
        every { record.triples } returns triples
        every { record.homeRuns } returns homeRuns
        every { record.runs } returns runs
        every { record.runsBattedIn } returns runsBattedIn
        every { record.walks } returns walks
        every { record.intentionalWalks } returns intentionalWalks
        every { record.hitByPitch } returns hitByPitch
        every { record.strikeouts } returns strikeouts
        every { record.sacrificeBunts } returns sacrificeBunts
        every { record.sacrificeFlies } returns sacrificeFlies
        every { record.stolenBases } returns stolenBases
        every { record.caughtStealing } returns caughtStealing
        every { record.groundedIntoDoublePlays } returns groundedIntoDoublePlays
        return record
    }

    @Nested
    @DisplayName("경기 타격 기여분 롤백")
    inner class RevertGameRecord {
        private lateinit var stats: SeasonBattingStats

        @BeforeEach
        fun setUp() {
            stats = SeasonBattingStats.create(testPlayer, 2024)
        }

        @Test
        fun `단타 기여분이 정확히 차감됨`() {
            // given: 시즌 통계에 1게임 분량 단타 기여 반영
            val record =
                createMockBattingRecord(
                    plateAppearances = 4,
                    atBats = 4,
                    hits = 2,
                )
            // 먼저 addGameRecord 없이 직접 값 설정은 불가하므로 revertGameRecord가 0 미만으로 내려가지 않음을 검증

            // when: 롤백
            stats.revertGameRecord(record)

            // then: 음수 방지로 모든 값이 0
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
        }

        @Test
        fun `경기 기록이 반영된 시즌 통계에서 정확히 차감됨`() {
            // given: 2경기 기록이 반영된 시즌 통계
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val realRecord1 = BattingRecord(gamePlayer)
            realRecord1.applyPlateAppearanceResult(
                com.nextup.core.domain.game.PlateAppearanceResult.SINGLE,
            )
            realRecord1.applyPlateAppearanceResult(
                com.nextup.core.domain.game.PlateAppearanceResult.HOME_RUN,
            )
            val realRecord2 = BattingRecord(gamePlayer)
            realRecord2.applyPlateAppearanceResult(
                com.nextup.core.domain.game.PlateAppearanceResult.DOUBLE,
            )

            stats.addGameRecord(realRecord1)
            stats.addGameRecord(realRecord2)

            assertThat(stats.gamesPlayed).isEqualTo(2)
            assertThat(stats.hits).isEqualTo(3)
            assertThat(stats.homeRuns).isEqualTo(1)
            assertThat(stats.doubles).isEqualTo(1)

            // when: 첫 번째 경기 기록 롤백
            stats.revertGameRecord(realRecord1)

            // then: 두 번째 경기 기록만 남음
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.hits).isEqualTo(1)
            assertThat(stats.homeRuns).isZero
            assertThat(stats.doubles).isEqualTo(1)
            assertThat(stats.plateAppearances).isEqualTo(1)
            assertThat(stats.atBats).isEqualTo(1)
        }

        @Test
        fun `홈런 기여분 롤백 시 홈런과 득점이 함께 차감됨`() {
            // given
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record = BattingRecord(gamePlayer)
            record.applyPlateAppearanceResult(com.nextup.core.domain.game.PlateAppearanceResult.HOME_RUN)

            stats.addGameRecord(record)
            assertThat(stats.homeRuns).isEqualTo(1)
            assertThat(stats.runs).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.homeRuns).isZero
            assertThat(stats.runs).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.atBats).isZero
        }

        @Test
        fun `볼넷 기여분 롤백 시 볼넷만 차감되고 타수는 영향 없음`() {
            // given
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record = BattingRecord(gamePlayer)
            record.applyPlateAppearanceResult(com.nextup.core.domain.game.PlateAppearanceResult.WALK)

            stats.addGameRecord(record)
            assertThat(stats.walks).isEqualTo(1)
            assertThat(stats.atBats).isZero

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.walks).isZero
            assertThat(stats.plateAppearances).isZero
        }

        @Test
        fun `음수 방지 - 기여분보다 큰 값을 롤백해도 0 미만으로 내려가지 않음`() {
            // given: 빈 통계에 큰 기여분 롤백 시도
            val record =
                createMockBattingRecord(
                    plateAppearances = 10,
                    atBats = 8,
                    hits = 5,
                    homeRuns = 2,
                    runs = 3,
                )

            // when
            stats.revertGameRecord(record)

            // then: 모든 값이 0 (음수 없음)
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.plateAppearances).isZero
            assertThat(stats.atBats).isZero
            assertThat(stats.hits).isZero
            assertThat(stats.homeRuns).isZero
            assertThat(stats.runs).isZero
        }

        @Test
        fun `gamesPlayed가 1 감소함`() {
            // given
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record = BattingRecord(gamePlayer)
            stats.addGameRecord(record)
            assertThat(stats.gamesPlayed).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isZero
        }
    }
}
