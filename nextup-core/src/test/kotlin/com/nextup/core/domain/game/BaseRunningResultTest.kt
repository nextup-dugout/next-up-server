package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("BaseRunningResult")
class BaseRunningResultTest {

    @Nested
    @DisplayName("열거형 값 검증")
    inner class EnumValueTest {

        @Test
        fun `STOLEN_BASE의 displayName은 도루 성공이다`() {
            assertThat(BaseRunningResult.STOLEN_BASE.displayName).isEqualTo("도루 성공")
        }

        @Test
        fun `CAUGHT_STEALING의 displayName은 도루 실패이다`() {
            assertThat(BaseRunningResult.CAUGHT_STEALING.displayName).isEqualTo("도루 실패")
        }

        @Test
        fun `PICKED_OFF의 displayName은 견제사이다`() {
            assertThat(BaseRunningResult.PICKED_OFF.displayName).isEqualTo("견제사")
        }

        @Test
        fun `ADVANCED_ON_ERROR의 displayName은 실책으로 진루이다`() {
            assertThat(BaseRunningResult.ADVANCED_ON_ERROR.displayName).isEqualTo("실책으로 진루")
        }

        @Test
        fun `ADVANCED_ON_WILD_PITCH의 displayName은 폭투 진루이다`() {
            assertThat(BaseRunningResult.ADVANCED_ON_WILD_PITCH.displayName).isEqualTo("폭투 진루")
        }

        @Test
        fun `ADVANCED_ON_PASSED_BALL의 displayName은 포일 진루이다`() {
            assertThat(BaseRunningResult.ADVANCED_ON_PASSED_BALL.displayName).isEqualTo("포일 진루")
        }

        @Test
        fun `ADVANCED_ON_BALK의 displayName은 보크 진루이다`() {
            assertThat(BaseRunningResult.ADVANCED_ON_BALK.displayName).isEqualTo("보크 진루")
        }

        @Test
        fun `모든 BaseRunningResult 값이 7개이다`() {
            assertThat(BaseRunningResult.entries).hasSize(7)
        }
    }
}

@DisplayName("BattingRecord - 주루 기록")
class BattingRecordBaseRunningTest {

    private lateinit var gamePlayer: GamePlayer
    private lateinit var battingRecord: BattingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        battingRecord = BattingRecord.create(gamePlayer)
    }

    @Nested
    @DisplayName("도루 기록")
    inner class StolenBaseTest {

        @Test
        fun `도루를 기록하면 stolenBases가 1 증가한다`() {
            // when
            battingRecord.recordStolenBase()

            // then
            assertThat(battingRecord.stolenBases).isEqualTo(1)
        }

        @Test
        fun `도루를 여러 번 기록하면 stolenBases가 누적된다`() {
            // when
            battingRecord.recordStolenBase()
            battingRecord.recordStolenBase()
            battingRecord.recordStolenBase()

            // then
            assertThat(battingRecord.stolenBases).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("도루 실패 기록")
    inner class CaughtStealingTest {

        @Test
        fun `도루 실패를 기록하면 caughtStealing이 1 증가한다`() {
            // when
            battingRecord.recordCaughtStealing()

            // then
            assertThat(battingRecord.caughtStealing).isEqualTo(1)
        }

        @Test
        fun `도루 실패를 여러 번 기록하면 caughtStealing이 누적된다`() {
            // when
            battingRecord.recordCaughtStealing()
            battingRecord.recordCaughtStealing()

            // then
            assertThat(battingRecord.caughtStealing).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("도루 성공률 계산")
    inner class StolenBasePercentageTest {

        @Test
        fun `도루 시도가 없으면 성공률은 0이다`() {
            assertThat(battingRecord.stolenBasePercentage).isEqualByComparingTo("0.000")
        }

        @Test
        fun `도루 1회 성공 시 성공률은 1_000이다`() {
            battingRecord.recordStolenBase()

            assertThat(battingRecord.stolenBasePercentage).isEqualByComparingTo("1.000")
        }

        @Test
        fun `도루 2회 시도 중 1회 성공 시 성공률은 0_500이다`() {
            battingRecord.recordStolenBase()
            battingRecord.recordCaughtStealing()

            assertThat(battingRecord.stolenBasePercentage).isEqualByComparingTo("0.500")
        }

        @Test
        fun `도루 3회 시도 중 2회 성공 시 성공률은 0_667이다`() {
            battingRecord.recordStolenBase()
            battingRecord.recordStolenBase()
            battingRecord.recordCaughtStealing()

            assertThat(battingRecord.stolenBasePercentage).isEqualByComparingTo("0.667")
        }
    }
}
