package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BaseRunningEvent enum 테스트")
class BaseRunningEventTest {

    @Nested
    @DisplayName("도루 관련 이벤트")
    inner class StealEvents {

        @Test
        fun `도루 시도 이벤트를 구분할 수 있다`() {
            assertThat(BaseRunningEvent.STOLEN_BASE.isStealAttempt).isTrue()
            assertThat(BaseRunningEvent.CAUGHT_STEALING.isStealAttempt).isTrue()
            assertThat(BaseRunningEvent.DOUBLE_STEAL.isStealAttempt).isTrue()
            assertThat(BaseRunningEvent.TRIPLE_STEAL.isStealAttempt).isTrue()
        }

        @Test
        fun `도루 성공 이벤트를 구분할 수 있다`() {
            assertThat(BaseRunningEvent.STOLEN_BASE.isSuccessfulSteal).isTrue()
            assertThat(BaseRunningEvent.DOUBLE_STEAL.isSuccessfulSteal).isTrue()
            assertThat(BaseRunningEvent.TRIPLE_STEAL.isSuccessfulSteal).isTrue()
            assertThat(BaseRunningEvent.CAUGHT_STEALING.isSuccessfulSteal).isFalse()
        }

        @Test
        fun `폭투는 도루 시도가 아니다`() {
            assertThat(BaseRunningEvent.WILD_PITCH.isStealAttempt).isFalse()
        }
    }

    @Nested
    @DisplayName("투수/포수 책임 이벤트")
    inner class BatteryErrorEvents {

        @Test
        fun `폭투, 포일, 보크는 배터리 책임 이벤트이다`() {
            assertThat(BaseRunningEvent.WILD_PITCH.isBatteryError).isTrue()
            assertThat(BaseRunningEvent.PASSED_BALL.isBatteryError).isTrue()
            assertThat(BaseRunningEvent.BALK.isBatteryError).isTrue()
        }

        @Test
        fun `도루는 배터리 책임 이벤트가 아니다`() {
            assertThat(BaseRunningEvent.STOLEN_BASE.isBatteryError).isFalse()
        }
    }

    @Nested
    @DisplayName("아웃/진루 구분")
    inner class OutOrAdvance {

        @Test
        fun `아웃 이벤트를 구분할 수 있다`() {
            assertThat(BaseRunningEvent.CAUGHT_STEALING.isOut).isTrue()
            assertThat(BaseRunningEvent.PICKOFF.isOut).isTrue()
            assertThat(BaseRunningEvent.OUT_ON_BASES.isOut).isTrue()
            assertThat(BaseRunningEvent.OUT_ON_APPEAL.isOut).isTrue()
            assertThat(BaseRunningEvent.OUT_PASSING_RUNNER.isOut).isTrue()
            assertThat(BaseRunningEvent.OUT_INTERFERENCE.isOut).isTrue()
        }

        @Test
        fun `진루 이벤트를 구분할 수 있다`() {
            assertThat(BaseRunningEvent.STOLEN_BASE.isAdvance).isTrue()
            assertThat(BaseRunningEvent.WILD_PITCH.isAdvance).isTrue()
            assertThat(BaseRunningEvent.PASSED_BALL.isAdvance).isTrue()
            assertThat(BaseRunningEvent.BALK.isAdvance).isTrue()
            assertThat(BaseRunningEvent.ADVANCE_ON_THROW.isAdvance).isTrue()
            assertThat(BaseRunningEvent.ADVANCE_ON_ERROR.isAdvance).isTrue()
            assertThat(BaseRunningEvent.ADVANCE_ON_FLYOUT.isAdvance).isTrue()
        }

        @Test
        fun `아웃 이벤트는 진루 이벤트가 아니다`() {
            assertThat(BaseRunningEvent.CAUGHT_STEALING.isAdvance).isFalse()
            assertThat(BaseRunningEvent.PICKOFF.isAdvance).isFalse()
        }
    }

    @Nested
    @DisplayName("이벤트 속성")
    inner class EventProperties {

        @Test
        fun `모든 이벤트는 표시 이름을 가진다`() {
            BaseRunningEvent.entries.forEach { event ->
                assertThat(event.displayName).isNotBlank()
            }
        }

        @Test
        fun `모든 이벤트는 설명을 가진다`() {
            BaseRunningEvent.entries.forEach { event ->
                assertThat(event.description).isNotBlank()
            }
        }
    }
}
