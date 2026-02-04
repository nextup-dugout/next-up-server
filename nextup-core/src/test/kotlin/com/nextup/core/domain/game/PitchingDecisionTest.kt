package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PitchingDecision")
class PitchingDecisionTest {
    @Nested
    @DisplayName("승리 결정")
    inner class WinTest {
        @Test
        fun `WIN은 isWin이 true이다`() {
            assertThat(PitchingDecision.WIN.isWin).isTrue()
        }

        @Test
        fun `WIN은 isLoss가 false이다`() {
            assertThat(PitchingDecision.WIN.isLoss).isFalse()
        }

        @Test
        fun `WIN은 hasNoDecision이 false이다`() {
            assertThat(PitchingDecision.WIN.hasNoDecision).isFalse()
        }
    }

    @Nested
    @DisplayName("패배 결정")
    inner class LossTest {
        @Test
        fun `LOSS는 isLoss가 true이다`() {
            assertThat(PitchingDecision.LOSS.isLoss).isTrue()
        }

        @Test
        fun `LOSS는 isWin이 false이다`() {
            assertThat(PitchingDecision.LOSS.isWin).isFalse()
        }
    }

    @Nested
    @DisplayName("세이브 결정")
    inner class SaveTest {
        @Test
        fun `SAVE는 isSave가 true이다`() {
            assertThat(PitchingDecision.SAVE.isSave).isTrue()
        }

        @Test
        fun `SAVE는 isReliefSuccess가 true이다`() {
            assertThat(PitchingDecision.SAVE.isReliefSuccess).isTrue()
        }
    }

    @Nested
    @DisplayName("홀드 결정")
    inner class HoldTest {
        @Test
        fun `HOLD는 isHold가 true이다`() {
            assertThat(PitchingDecision.HOLD.isHold).isTrue()
        }

        @Test
        fun `HOLD는 isReliefSuccess가 true이다`() {
            assertThat(PitchingDecision.HOLD.isReliefSuccess).isTrue()
        }
    }

    @Nested
    @DisplayName("블론세이브 결정")
    inner class BlownSaveTest {
        @Test
        fun `BLOWN_SAVE는 isBlownSave가 true이다`() {
            assertThat(PitchingDecision.BLOWN_SAVE.isBlownSave).isTrue()
        }

        @Test
        fun `BLOWN_SAVE는 isReliefSuccess가 false이다`() {
            assertThat(PitchingDecision.BLOWN_SAVE.isReliefSuccess).isFalse()
        }
    }

    @Nested
    @DisplayName("결정 없음")
    inner class NoneTest {
        @Test
        fun `NONE은 hasNoDecision이 true이다`() {
            assertThat(PitchingDecision.NONE.hasNoDecision).isTrue()
        }

        @Test
        fun `NONE은 isWin이 false이다`() {
            assertThat(PitchingDecision.NONE.isWin).isFalse()
        }

        @Test
        fun `NONE은 isLoss가 false이다`() {
            assertThat(PitchingDecision.NONE.isLoss).isFalse()
        }

        @Test
        fun `NONE은 isReliefSuccess가 false이다`() {
            assertThat(PitchingDecision.NONE.isReliefSuccess).isFalse()
        }
    }

    @Nested
    @DisplayName("구원 성공")
    inner class ReliefSuccessTest {
        @Test
        fun `WIN은 구원 성공이 아니다`() {
            assertThat(PitchingDecision.WIN.isReliefSuccess).isFalse()
        }

        @Test
        fun `LOSS는 구원 성공이 아니다`() {
            assertThat(PitchingDecision.LOSS.isReliefSuccess).isFalse()
        }
    }
}
