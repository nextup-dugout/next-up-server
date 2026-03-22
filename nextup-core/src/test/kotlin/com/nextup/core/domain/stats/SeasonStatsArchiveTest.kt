package com.nextup.core.domain.stats

import com.nextup.common.exception.FrozenStatsException
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * L-8: 시즌 통계 아카이브/확정 메커니즘 - Frozen Guard 테스트
 *
 * 확정된(frozen) 시즌 통계에 대한 수정 시도가 FrozenStatsException을 던지는지 검증합니다.
 */
@DisplayName("시즌 통계 아카이브/확정 — Frozen Guard 테스트")
class SeasonStatsArchiveTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    // === SeasonBattingStats Frozen Guard ===

    @Nested
    @DisplayName("SeasonBattingStats — 확정된 통계 수정 거부")
    inner class BattingStatsFrozenGuard {
        @Test
        fun `확정된 타격 통계에 applyLiveUpdate 시 FrozenStatsException`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.applyLiveUpdate(PlateAppearanceResult.SINGLE) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정된 타격 통계에 revertLiveUpdate 시 FrozenStatsException`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.revertLiveUpdate(PlateAppearanceResult.SINGLE) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정된 타격 통계에 applyFieldCorrection 시 FrozenStatsException`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.applyFieldCorrection("hits", 1) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정 해제 후에는 수정이 가능하다`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, 2026)
            stats.finalize()
            stats.unfinalize()

            // when & then (예외 없이 통과)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            assertThat(stats.hits).isEqualTo(1)
        }
    }

    // === SeasonPitchingStats Frozen Guard ===

    @Nested
    @DisplayName("SeasonPitchingStats — 확정된 통계 수정 거부")
    inner class PitchingStatsFrozenGuard {
        @Test
        fun `확정된 투수 통계에 applyLiveUpdate 시 FrozenStatsException`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정된 투수 통계에 revertLiveUpdate 시 FrozenStatsException`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.revertLiveUpdate(PlateAppearanceResult.STRIKEOUT) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정된 투수 통계에 applyFieldCorrection 시 FrozenStatsException`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.applyFieldCorrection("strikeouts", 1) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정 해제 후에는 수정이 가능하다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2026)
            stats.finalize()
            stats.unfinalize()

            // when & then (예외 없이 통과)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            assertThat(stats.strikeouts).isEqualTo(1)
        }
    }

    // === SeasonFieldingStats Finalize/Unfinalize + Frozen Guard ===

    @Nested
    @DisplayName("SeasonFieldingStats — 확정/해제 및 수정 거부")
    inner class FieldingStatsFinalizeAndFrozenGuard {
        @Test
        fun `수비 통계를 확정할 수 있다`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)

            // when
            stats.finalize()

            // then
            assertThat(stats.isFinalized).isTrue()
        }

        @Test
        fun `이미 확정된 수비 통계를 다시 확정하면 예외가 발생한다`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.finalize() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이미 확정")
        }

        @Test
        fun `확정된 수비 통계를 해제할 수 있다`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)
            stats.finalize()

            // when
            stats.unfinalize()

            // then
            assertThat(stats.isFinalized).isFalse()
        }

        @Test
        fun `확정되지 않은 수비 통계를 해제하면 예외가 발생한다`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)

            // when & then
            assertThatThrownBy { stats.unfinalize() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("확정되지 않은")
        }

        @Test
        fun `확정된 수비 통계에 addGameRecord 시 FrozenStatsException`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)
            stats.finalize()
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record = FieldingRecord.create(gamePlayer)

            // when & then
            assertThatThrownBy { stats.addGameRecord(record) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정된 수비 통계에 revertGameRecord 시 FrozenStatsException`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)
            stats.finalize()
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record = FieldingRecord.create(gamePlayer)

            // when & then
            assertThatThrownBy { stats.revertGameRecord(record) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정된 수비 통계에 applyFieldCorrection 시 FrozenStatsException`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.applyFieldCorrection("putOuts", 1) }
                .isInstanceOf(FrozenStatsException::class.java)
        }

        @Test
        fun `확정 해제 후에는 수정이 가능하다`() {
            // given
            val stats = SeasonFieldingStats.create(testPlayer, 2026)
            stats.finalize()
            stats.unfinalize()
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record = FieldingRecord.create(gamePlayer)

            // when & then (예외 없이 통과)
            stats.addGameRecord(record)
            assertThat(stats.gamesPlayed).isEqualTo(1)
        }
    }
}
