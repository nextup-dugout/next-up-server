package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.domain.event.FieldingEventType
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.player.Player
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SeasonFieldingStats")
class SeasonFieldingStatsTest {
    private lateinit var player: Player
    private lateinit var seasonFieldingStats: SeasonFieldingStats

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        seasonFieldingStats = SeasonFieldingStats.create(player, 2026)
    }

    @Nested
    @DisplayName("생성")
    inner class CreateTest {
        @Test
        fun `정상적인 연도로 생성하면 초기값이 0이다`() {
            assertThat(seasonFieldingStats.year).isEqualTo(2026)
            assertThat(seasonFieldingStats.gamesPlayed).isEqualTo(0)
            assertThat(seasonFieldingStats.putOuts).isEqualTo(0)
            assertThat(seasonFieldingStats.assists).isEqualTo(0)
            assertThat(seasonFieldingStats.errors).isEqualTo(0)
            assertThat(seasonFieldingStats.doublePlays).isEqualTo(0)
            assertThat(seasonFieldingStats.passedBalls).isEqualTo(0)
        }

        @Test
        fun `연도가 0 이하이면 StatsValidationException이 발생한다`() {
            assertThatThrownBy { SeasonFieldingStats.create(player, 0) }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `연도가 음수이면 StatsValidationException이 발생한다`() {
            assertThatThrownBy { SeasonFieldingStats.create(player, -1) }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `teamId를 포함하여 생성할 수 있다`() {
            val stats = SeasonFieldingStats.create(player, 2026, teamId = 3L)
            assertThat(stats.teamId).isEqualTo(3L)
            assertThat(stats.year).isEqualTo(2026)
        }
    }

    @Nested
    @DisplayName("경기 기록 누적")
    inner class AddGameRecordTest {
        @Test
        fun `경기 기록을 누적하면 gamesPlayed와 수비 기록이 증가한다`() {
            // given
            val record = mockk<FieldingRecord>()
            every { record.putOuts } returns 3
            every { record.assists } returns 2
            every { record.errors } returns 1
            every { record.doublePlays } returns 1
            every { record.passedBalls } returns 0
            every { record.triplePlays } returns 0
            every { record.caughtStealing } returns 0
            every { record.stolenBasesAllowed } returns 0

            // when
            seasonFieldingStats.addGameRecord(record)

            // then
            assertThat(seasonFieldingStats.gamesPlayed).isEqualTo(1)
            assertThat(seasonFieldingStats.putOuts).isEqualTo(3)
            assertThat(seasonFieldingStats.assists).isEqualTo(2)
            assertThat(seasonFieldingStats.errors).isEqualTo(1)
            assertThat(seasonFieldingStats.doublePlays).isEqualTo(1)
            assertThat(seasonFieldingStats.passedBalls).isEqualTo(0)
        }

        @Test
        fun `여러 경기 기록을 누적하면 합산된다`() {
            // given
            val record1 = mockk<FieldingRecord>()
            every { record1.putOuts } returns 2
            every { record1.assists } returns 1
            every { record1.errors } returns 0
            every { record1.doublePlays } returns 0
            every { record1.passedBalls } returns 0
            every { record1.triplePlays } returns 0
            every { record1.caughtStealing } returns 0
            every { record1.stolenBasesAllowed } returns 0

            val record2 = mockk<FieldingRecord>()
            every { record2.putOuts } returns 3
            every { record2.assists } returns 2
            every { record2.errors } returns 1
            every { record2.doublePlays } returns 1
            every { record2.passedBalls } returns 1
            every { record2.triplePlays } returns 0
            every { record2.caughtStealing } returns 0
            every { record2.stolenBasesAllowed } returns 0

            // when
            seasonFieldingStats.addGameRecord(record1)
            seasonFieldingStats.addGameRecord(record2)

            // then
            assertThat(seasonFieldingStats.gamesPlayed).isEqualTo(2)
            assertThat(seasonFieldingStats.putOuts).isEqualTo(5)
            assertThat(seasonFieldingStats.assists).isEqualTo(3)
            assertThat(seasonFieldingStats.errors).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("수비율 계산")
    inner class FieldingPercentageTest {
        @Test
        fun `수비 기회가 0이면 수비율은 null이다`() {
            assertThat(seasonFieldingStats.fieldingPercentage).isNull()
        }

        @Test
        fun `수비 기회가 있으면 수비율을 반환한다`() {
            // given: PO=9, A=3, E=1 -> TC=13, FPCT=12/13
            val record = mockk<FieldingRecord>()
            every { record.putOuts } returns 9
            every { record.assists } returns 3
            every { record.errors } returns 1
            every { record.doublePlays } returns 0
            every { record.passedBalls } returns 0
            every { record.triplePlays } returns 0
            every { record.caughtStealing } returns 0
            every { record.stolenBasesAllowed } returns 0

            seasonFieldingStats.addGameRecord(record)

            // then
            assertThat(seasonFieldingStats.totalChances).isEqualTo(13)
            assertThat(seasonFieldingStats.fieldingPercentage).isNotNull
        }

        @Test
        fun `실책이 없으면 수비율은 1이다`() {
            // given: PO=5, A=3, E=0 -> TC=8, FPCT=8/8=1.000
            val record = mockk<FieldingRecord>()
            every { record.putOuts } returns 5
            every { record.assists } returns 3
            every { record.errors } returns 0
            every { record.doublePlays } returns 0
            every { record.passedBalls } returns 0
            every { record.triplePlays } returns 0
            every { record.caughtStealing } returns 0
            every { record.stolenBasesAllowed } returns 0

            seasonFieldingStats.addGameRecord(record)

            // then
            assertThat(seasonFieldingStats.fieldingPercentage)
                .isEqualByComparingTo(java.math.BigDecimal("1.000"))
        }

        @Test
        fun `수비율은 소수점 3자리로 계산된다`() {
            // given: PO=2, A=1, E=1 -> TC=4, FPCT=3/4=0.750
            val record = mockk<FieldingRecord>()
            every { record.putOuts } returns 2
            every { record.assists } returns 1
            every { record.errors } returns 1
            every { record.doublePlays } returns 0
            every { record.passedBalls } returns 0
            every { record.triplePlays } returns 0
            every { record.caughtStealing } returns 0
            every { record.stolenBasesAllowed } returns 0

            seasonFieldingStats.addGameRecord(record)

            // then
            assertThat(seasonFieldingStats.fieldingPercentage)
                .isEqualByComparingTo(java.math.BigDecimal("0.750"))
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class ValidateTest {
        @Test
        fun `정상 상태에서 validate는 예외를 발생시키지 않는다`() {
            val record = mockk<FieldingRecord>()
            every { record.putOuts } returns 3
            every { record.assists } returns 1
            every { record.errors } returns 0
            every { record.doublePlays } returns 0
            every { record.passedBalls } returns 0
            every { record.triplePlays } returns 0
            every { record.caughtStealing } returns 0
            every { record.stolenBasesAllowed } returns 0
            seasonFieldingStats.addGameRecord(record)

            seasonFieldingStats.validate()
        }

        @Test
        fun `초기 상태에서 validate는 예외를 발생시키지 않는다`() {
            seasonFieldingStats.validate()
        }

        @Test
        fun `gamesPlayed가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(seasonFieldingStats, "gamesPlayed", -1)
            assertThatThrownBy { seasonFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `putOuts가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(seasonFieldingStats, "putOuts", -1)
            assertThatThrownBy { seasonFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `assists가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(seasonFieldingStats, "assists", -1)
            assertThatThrownBy { seasonFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `errors가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(seasonFieldingStats, "errors", -1)
            assertThatThrownBy { seasonFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `doublePlays가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(seasonFieldingStats, "doublePlays", -1)
            assertThatThrownBy { seasonFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `passedBalls가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(seasonFieldingStats, "passedBalls", -1)
            assertThatThrownBy { seasonFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }
    }

    @Nested
    @DisplayName("L-6: 실시간 수비 통계 갱신")
    inner class LiveFieldingUpdateTest {
        @Test
        fun `PUT_OUT 이벤트로 자살이 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            assertThat(seasonFieldingStats.putOuts).isEqualTo(1)
        }

        @Test
        fun `ASSIST 이벤트로 보살이 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ASSIST)
            assertThat(seasonFieldingStats.assists).isEqualTo(1)
        }

        @Test
        fun `ERROR 이벤트로 실책이 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ERROR)
            assertThat(seasonFieldingStats.errors).isEqualTo(1)
        }

        @Test
        fun `DOUBLE_PLAY 이벤트로 병살 관여가 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.DOUBLE_PLAY)
            assertThat(seasonFieldingStats.doublePlays).isEqualTo(1)
        }

        @Test
        fun `TRIPLE_PLAY 이벤트로 삼중살 관여가 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.TRIPLE_PLAY)
            assertThat(seasonFieldingStats.triplePlays).isEqualTo(1)
        }

        @Test
        fun `PASSED_BALL 이벤트로 포일이 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PASSED_BALL)
            assertThat(seasonFieldingStats.passedBalls).isEqualTo(1)
        }

        @Test
        fun `CAUGHT_STEALING 이벤트로 도루 저지가 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.CAUGHT_STEALING)
            assertThat(seasonFieldingStats.caughtStealing).isEqualTo(1)
        }

        @Test
        fun `STOLEN_BASE_ALLOWED 이벤트로 도루 허용이 증가한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.STOLEN_BASE_ALLOWED)
            assertThat(seasonFieldingStats.stolenBasesAllowed).isEqualTo(1)
        }

        @Test
        fun `여러 이벤트를 순차 적용하면 합산된다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ASSIST)
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ERROR)

            assertThat(seasonFieldingStats.putOuts).isEqualTo(2)
            assertThat(seasonFieldingStats.assists).isEqualTo(1)
            assertThat(seasonFieldingStats.errors).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("L-6: 실시간 수비 통계 역산")
    inner class RevertLiveFieldingUpdateTest {
        @Test
        fun `PUT_OUT 역산으로 자살이 감소한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            seasonFieldingStats.revertLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            assertThat(seasonFieldingStats.putOuts).isEqualTo(0)
        }

        @Test
        fun `ASSIST 역산으로 보살이 감소한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ASSIST)
            seasonFieldingStats.revertLiveFieldingUpdate(FieldingEventType.ASSIST)
            assertThat(seasonFieldingStats.assists).isEqualTo(0)
        }

        @Test
        fun `ERROR 역산으로 실책이 감소한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ERROR)
            seasonFieldingStats.revertLiveFieldingUpdate(FieldingEventType.ERROR)
            assertThat(seasonFieldingStats.errors).isEqualTo(0)
        }

        @Test
        fun `0에서 역산해도 음수가 되지 않는다`() {
            seasonFieldingStats.revertLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            assertThat(seasonFieldingStats.putOuts).isEqualTo(0)

            seasonFieldingStats.revertLiveFieldingUpdate(FieldingEventType.ASSIST)
            assertThat(seasonFieldingStats.assists).isEqualTo(0)

            seasonFieldingStats.revertLiveFieldingUpdate(FieldingEventType.ERROR)
            assertThat(seasonFieldingStats.errors).isEqualTo(0)
        }

        @Test
        fun `모든 유형의 역산이 음수를 방지한다`() {
            for (type in FieldingEventType.entries) {
                seasonFieldingStats.revertLiveFieldingUpdate(type)
            }
            seasonFieldingStats.validate()
        }
    }

    @Nested
    @DisplayName("L-7: 정합성 교차 검증")
    inner class VerifyConsistencyTest {
        @Test
        fun `통계가 일치하면 빈 목록을 반환한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ASSIST)
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.ERROR)

            val mismatches =
                seasonFieldingStats.verifyConsistency(
                    totalPutOuts = 2,
                    totalAssists = 1,
                    totalErrors = 1,
                )
            assertThat(mismatches).isEmpty()
        }

        @Test
        fun `자살이 불일치하면 해당 항목을 반환한다`() {
            seasonFieldingStats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)

            val mismatches =
                seasonFieldingStats.verifyConsistency(
                    totalPutOuts = 2,
                    totalAssists = 0,
                    totalErrors = 0,
                )
            assertThat(mismatches).hasSize(1)
            assertThat(mismatches[0]).contains("자살")
        }

        @Test
        fun `여러 항목이 불일치하면 모든 항목을 반환한다`() {
            val mismatches =
                seasonFieldingStats.verifyConsistency(
                    totalPutOuts = 1,
                    totalAssists = 2,
                    totalErrors = 3,
                )
            assertThat(mismatches).hasSize(3)
        }

        @Test
        fun `모든 값이 0이면 빈 목록을 반환한다`() {
            val mismatches =
                seasonFieldingStats.verifyConsistency(
                    totalPutOuts = 0,
                    totalAssists = 0,
                    totalErrors = 0,
                )
            assertThat(mismatches).isEmpty()
        }
    }

    private fun setFieldDirectly(
        obj: Any,
        fieldName: String,
        value: Int,
    ) {
        val clazz = obj.javaClass
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
