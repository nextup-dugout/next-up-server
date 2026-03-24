package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode

@DisplayName("PitchingStatCalculator")
class PitchingStatCalculatorTest {
    private fun createCalculator(
        inningsPitchedOuts: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        hitsAllowed: Int = 0,
        walksAllowed: Int = 0,
        strikeouts: Int = 0,
        pitchesThrown: Int? = null,
        strikesThrown: Int? = null,
        isStartingPitcher: Boolean = false,
        decision: PitchingDecision = PitchingDecision.NONE,
    ): PitchingStatCalculator =
        PitchingStatCalculator(
            inningsPitchedOuts = inningsPitchedOuts,
            earnedRuns = earnedRuns,
            runsAllowed = runsAllowed,
            hitsAllowed = hitsAllowed,
            walksAllowed = walksAllowed,
            strikeouts = strikeouts,
            pitchesThrown = pitchesThrown,
            strikesThrown = strikesThrown,
            isStartingPitcher = isStartingPitcher,
            decision = decision,
        )

    @Nested
    @DisplayName("이닝 계산")
    inner class InningsCalculationTest {
        @Test
        fun `0 아웃이면 0이닝이다`() {
            val calc = createCalculator(inningsPitchedOuts = 0)

            assertThat(calc.completeInnings).isEqualTo(0)
            assertThat(calc.remainingOuts).isEqualTo(0)
            assertThat(calc.inningsPitchedDisplay).isEqualTo("0.0")
        }

        @Test
        fun `3 아웃이면 1이닝이다`() {
            val calc = createCalculator(inningsPitchedOuts = 3)

            assertThat(calc.completeInnings).isEqualTo(1)
            assertThat(calc.remainingOuts).isEqualTo(0)
            assertThat(calc.inningsPitchedDisplay).isEqualTo("1.0")
        }

        @Test
        fun `16 아웃이면 5점1이닝이다`() {
            val calc = createCalculator(inningsPitchedOuts = 16)

            assertThat(calc.completeInnings).isEqualTo(5)
            assertThat(calc.remainingOuts).isEqualTo(1)
            assertThat(calc.inningsPitchedDisplay).isEqualTo("5.1")
        }
    }

    @Nested
    @DisplayName("ERA 계산")
    inner class EarnedRunAverageTest {
        @Test
        fun `이닝 0이고 자책점 0이면 ERA는 0이다`() {
            val calc = createCalculator(inningsPitchedOuts = 0, earnedRuns = 0)

            assertThat(calc.earnedRunAverage).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        }

        @Test
        fun `이닝 0이고 자책점이 있으면 ERA는 null이다`() {
            val calc = createCalculator(inningsPitchedOuts = 0, earnedRuns = 3)

            assertThat(calc.earnedRunAverage).isNull()
        }

        @Test
        fun `9이닝에 자책점 3이면 ERA는 3점00이다`() {
            val calc = createCalculator(inningsPitchedOuts = 27, earnedRuns = 3)

            assertThat(calc.earnedRunAverage).isEqualTo(BigDecimal("3.00"))
        }
    }

    @Nested
    @DisplayName("WHIP 계산")
    inner class WhipTest {
        @Test
        fun `이닝 0이면 WHIP는 0이다`() {
            val calc = createCalculator(inningsPitchedOuts = 0)

            assertThat(calc.whip).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        }

        @Test
        fun `9이닝에 피안타 9 볼넷 0이면 WHIP는 1점00이다`() {
            val calc = createCalculator(inningsPitchedOuts = 27, hitsAllowed = 9, walksAllowed = 0)

            assertThat(calc.whip).isEqualTo(BigDecimal("1.00"))
        }
    }

    @Nested
    @DisplayName("K/9 계산")
    inner class StrikeoutsPer9Test {
        @Test
        fun `이닝 0이면 K9는 0이다`() {
            val calc = createCalculator(inningsPitchedOuts = 0)

            assertThat(calc.strikeoutsPer9).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        }

        @Test
        fun `9이닝에 삼진 9면 K9는 9점00이다`() {
            val calc = createCalculator(inningsPitchedOuts = 27, strikeouts = 9)

            assertThat(calc.strikeoutsPer9).isEqualTo(BigDecimal("9.00"))
        }
    }

    @Nested
    @DisplayName("BB/9 계산")
    inner class WalksPer9Test {
        @Test
        fun `이닝 0이면 BB9는 0이다`() {
            val calc = createCalculator(inningsPitchedOuts = 0)

            assertThat(calc.walksPer9).isEqualTo(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
        }

        @Test
        fun `9이닝에 볼넷 3이면 BB9는 3점00이다`() {
            val calc = createCalculator(inningsPitchedOuts = 27, walksAllowed = 3)

            assertThat(calc.walksPer9).isEqualTo(BigDecimal("3.00"))
        }
    }

    @Nested
    @DisplayName("K/BB 비율")
    inner class StrikeoutToWalkRatioTest {
        @Test
        fun `삼진 0 볼넷 0이면 비율 0이다`() {
            val calc = createCalculator(strikeouts = 0, walksAllowed = 0)

            assertThat(calc.strikeoutToWalkRatio).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `볼넷 0이고 삼진 있으면 삼진 수가 그대로 반환된다`() {
            val calc = createCalculator(strikeouts = 10, walksAllowed = 0)

            assertThat(calc.strikeoutToWalkRatio).isEqualTo(BigDecimal("10.00"))
        }

        @Test
        fun `삼진 6 볼넷 2이면 비율 3점00이다`() {
            val calc = createCalculator(strikeouts = 6, walksAllowed = 2)

            assertThat(calc.strikeoutToWalkRatio).isEqualTo(BigDecimal("3.00"))
        }
    }

    @Nested
    @DisplayName("스트라이크 비율")
    inner class StrikePercentageTest {
        @Test
        fun `투구 수가 null이면 null이다`() {
            val calc = createCalculator(pitchesThrown = null, strikesThrown = null)

            assertThat(calc.strikePercentage).isNull()
        }

        @Test
        fun `투구 수가 0이면 null이다`() {
            val calc = createCalculator(pitchesThrown = 0, strikesThrown = 0)

            assertThat(calc.strikePercentage).isNull()
        }

        @Test
        fun `100구에 70스트라이크면 0점700이다`() {
            val calc = createCalculator(pitchesThrown = 100, strikesThrown = 70)

            assertThat(calc.strikePercentage).isEqualTo(BigDecimal("0.700"))
        }
    }

    @Nested
    @DisplayName("비자책 실점")
    inner class UnearnedRunsTest {
        @Test
        fun `실점 5 자책점 3이면 비자책 2이다`() {
            val calc = createCalculator(runsAllowed = 5, earnedRuns = 3)

            assertThat(calc.unearnedRuns).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("선발 승리 자격")
    inner class QualifiedForWinTest {
        @Test
        fun `선발이고 5이닝 이상이면 자격이다`() {
            val calc = createCalculator(inningsPitchedOuts = 15, isStartingPitcher = true)

            assertThat(calc.isQualifiedForWin()).isTrue()
        }

        @Test
        fun `선발이 아니면 자격이 아니다`() {
            val calc = createCalculator(inningsPitchedOuts = 15, isStartingPitcher = false)

            assertThat(calc.isQualifiedForWin()).isFalse()
        }

        @Test
        fun `선발이지만 5이닝 미만이면 자격이 아니다`() {
            val calc = createCalculator(inningsPitchedOuts = 14, isStartingPitcher = true)

            assertThat(calc.isQualifiedForWin()).isFalse()
        }
    }

    @Nested
    @DisplayName("PitchingRecord로부터 생성")
    inner class FromRecordTest {
        @Test
        fun `PitchingRecord의 현재 상태를 정확히 반영한다`() {
            val gamePlayer = io.mockk.mockk<GamePlayer>(relaxed = true)
            val record = PitchingRecord.create(gamePlayer, isStartingPitcher = true)
            // record에 몇 가지 기록 추가
            record.recordOut(isStrikeout = true)
            record.recordOut(isStrikeout = true)
            record.recordOut(isStrikeout = false)
            record.recordHit(runsScored = 1, earnedRuns = 1)
            record.recordWalk()

            val calc = PitchingStatCalculator.from(record)

            assertThat(calc.inningsPitchedOuts).isEqualTo(3)
            assertThat(calc.strikeouts).isEqualTo(2)
            assertThat(calc.earnedRuns).isEqualTo(1)
            assertThat(calc.hitsAllowed).isEqualTo(1)
            assertThat(calc.walksAllowed).isEqualTo(1)
            assertThat(calc.isStartingPitcher).isTrue()
            assertThat(calc.completeInnings).isEqualTo(1)
        }
    }
}
