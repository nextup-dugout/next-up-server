package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
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
import java.math.BigDecimal

@DisplayName("CareerFieldingStats")
class CareerFieldingStatsTest {
    private lateinit var player: Player
    private lateinit var careerFieldingStats: CareerFieldingStats

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        careerFieldingStats = CareerFieldingStats.create(player)
    }

    private fun createFieldingRecordMock(
        putOuts: Int = 0,
        assists: Int = 0,
        errors: Int = 0,
        doublePlays: Int = 0,
        passedBalls: Int = 0,
        triplePlays: Int = 0,
        caughtStealing: Int = 0,
        stolenBasesAllowed: Int = 0,
    ): FieldingRecord {
        val record = mockk<FieldingRecord>()
        every { record.putOuts } returns putOuts
        every { record.assists } returns assists
        every { record.errors } returns errors
        every { record.doublePlays } returns doublePlays
        every { record.passedBalls } returns passedBalls
        every { record.triplePlays } returns triplePlays
        every { record.caughtStealing } returns caughtStealing
        every { record.stolenBasesAllowed } returns stolenBasesAllowed
        return record
    }

    @Nested
    @DisplayName("생성")
    inner class CreateTest {
        @Test
        fun `create로 생성하면 초기값이 모두 0이다`() {
            assertThat(careerFieldingStats.seasonsPlayed).isEqualTo(0)
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(0)
            assertThat(careerFieldingStats.putOuts).isEqualTo(0)
            assertThat(careerFieldingStats.assists).isEqualTo(0)
            assertThat(careerFieldingStats.errors).isEqualTo(0)
            assertThat(careerFieldingStats.doublePlays).isEqualTo(0)
            assertThat(careerFieldingStats.passedBalls).isEqualTo(0)
            assertThat(careerFieldingStats.triplePlays).isEqualTo(0)
            assertThat(careerFieldingStats.caughtStealing).isEqualTo(0)
            assertThat(careerFieldingStats.stolenBasesAllowed).isEqualTo(0)
        }

        @Test
        fun `생성 시 player가 올바르게 설정된다`() {
            assertThat(careerFieldingStats.player).isEqualTo(player)
        }
    }

    @Nested
    @DisplayName("시즌 추가")
    inner class AddSeasonTest {
        @Test
        fun `addSeason을 호출하면 seasonsPlayed가 1 증가한다`() {
            careerFieldingStats.addSeason()
            assertThat(careerFieldingStats.seasonsPlayed).isEqualTo(1)
        }

        @Test
        fun `addSeason을 여러 번 호출하면 누적된다`() {
            repeat(3) { careerFieldingStats.addSeason() }
            assertThat(careerFieldingStats.seasonsPlayed).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("경기 기록 누적")
    inner class AddGameRecordTest {
        @Test
        fun `addGameRecord 호출 시 gamesPlayed가 1 증가하고 수비 기록이 누적된다`() {
            // given
            val record =
                createFieldingRecordMock(
                    putOuts = 5,
                    assists = 3,
                    errors = 1,
                    doublePlays = 2,
                    passedBalls = 1,
                )

            // when
            careerFieldingStats.addGameRecord(record)

            // then
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(1)
            assertThat(careerFieldingStats.putOuts).isEqualTo(5)
            assertThat(careerFieldingStats.assists).isEqualTo(3)
            assertThat(careerFieldingStats.errors).isEqualTo(1)
            assertThat(careerFieldingStats.doublePlays).isEqualTo(2)
            assertThat(careerFieldingStats.passedBalls).isEqualTo(1)
        }

        @Test
        fun `여러 경기 기록을 누적하면 합산된다`() {
            // given
            val record1 = createFieldingRecordMock(putOuts = 3, assists = 1)
            val record2 =
                createFieldingRecordMock(
                    putOuts = 4,
                    assists = 2,
                    errors = 1,
                    doublePlays = 1,
                )

            // when
            careerFieldingStats.addGameRecord(record1)
            careerFieldingStats.addGameRecord(record2)

            // then
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(2)
            assertThat(careerFieldingStats.putOuts).isEqualTo(7)
            assertThat(careerFieldingStats.assists).isEqualTo(3)
            assertThat(careerFieldingStats.errors).isEqualTo(1)
            assertThat(careerFieldingStats.doublePlays).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("경기 기록 롤백")
    inner class RevertGameRecordTest {
        @Test
        fun `revertGameRecord 호출 시 gamesPlayed가 1 감소하고 수비 기록이 차감된다`() {
            // given
            val record =
                createFieldingRecordMock(
                    putOuts = 5,
                    assists = 3,
                    errors = 1,
                    doublePlays = 2,
                    passedBalls = 1,
                )

            careerFieldingStats.addGameRecord(record)
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(1)

            // when
            careerFieldingStats.revertGameRecord(record)

            // then
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(0)
            assertThat(careerFieldingStats.putOuts).isEqualTo(0)
            assertThat(careerFieldingStats.assists).isEqualTo(0)
            assertThat(careerFieldingStats.errors).isEqualTo(0)
            assertThat(careerFieldingStats.doublePlays).isEqualTo(0)
            assertThat(careerFieldingStats.passedBalls).isEqualTo(0)
        }

        @Test
        fun `초기 상태에서 revertGameRecord를 호출해도 음수가 되지 않는다`() {
            // given
            val record =
                createFieldingRecordMock(
                    putOuts = 5,
                    assists = 3,
                    errors = 1,
                    doublePlays = 2,
                    passedBalls = 1,
                )

            // when
            careerFieldingStats.revertGameRecord(record)

            // then
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(0)
            assertThat(careerFieldingStats.putOuts).isEqualTo(0)
            assertThat(careerFieldingStats.assists).isEqualTo(0)
            assertThat(careerFieldingStats.errors).isEqualTo(0)
            assertThat(careerFieldingStats.doublePlays).isEqualTo(0)
            assertThat(careerFieldingStats.passedBalls).isEqualTo(0)
        }

        @Test
        fun `여러 경기 중 하나를 롤백하면 나머지만 남는다`() {
            // given
            val record1 = createFieldingRecordMock(putOuts = 3, assists = 1)
            val record2 = createFieldingRecordMock(putOuts = 4, assists = 2, errors = 1, doublePlays = 1)

            careerFieldingStats.addGameRecord(record1)
            careerFieldingStats.addGameRecord(record2)
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(2)

            // when: revert record1 only
            careerFieldingStats.revertGameRecord(record1)

            // then: record2 values remain
            assertThat(careerFieldingStats.gamesPlayed).isEqualTo(1)
            assertThat(careerFieldingStats.putOuts).isEqualTo(4)
            assertThat(careerFieldingStats.assists).isEqualTo(2)
            assertThat(careerFieldingStats.errors).isEqualTo(1)
            assertThat(careerFieldingStats.doublePlays).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("수비 기회(TC) 계산")
    inner class TotalChancesTest {
        @Test
        fun `초기 상태에서 수비 기회는 0이다`() {
            assertThat(careerFieldingStats.totalChances).isEqualTo(0)
        }

        @Test
        fun `수비 기회는 자살 + 보살 + 실책이다`() {
            // given
            val record = createFieldingRecordMock(putOuts = 5, assists = 3, errors = 2)

            careerFieldingStats.addGameRecord(record)

            // then: TC = 5 + 3 + 2 = 10
            assertThat(careerFieldingStats.totalChances).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("수비율(FPCT) 계산")
    inner class FieldingPercentageTest {
        @Test
        fun `수비 기회가 0이면 수비율은 null이다`() {
            assertThat(careerFieldingStats.fieldingPercentage).isNull()
        }

        @Test
        fun `수비 기회가 있으면 수비율을 반환한다`() {
            // given: PO=9, A=3, E=1 -> TC=13, FPCT=12/13
            val record = createFieldingRecordMock(putOuts = 9, assists = 3, errors = 1)

            careerFieldingStats.addGameRecord(record)

            // then
            assertThat(careerFieldingStats.fieldingPercentage).isNotNull
        }

        @Test
        fun `실책이 없으면 수비율은 1이다`() {
            // given: PO=5, A=3, E=0 -> TC=8, FPCT=8/8=1.000
            val record = createFieldingRecordMock(putOuts = 5, assists = 3)

            careerFieldingStats.addGameRecord(record)

            // then
            assertThat(careerFieldingStats.fieldingPercentage)
                .isEqualByComparingTo(BigDecimal("1.000"))
        }

        @Test
        fun `수비율은 소수점 3자리로 계산된다`() {
            // given: PO=2, A=1, E=1 -> TC=4, FPCT=3/4=0.750
            val record = createFieldingRecordMock(putOuts = 2, assists = 1, errors = 1)

            careerFieldingStats.addGameRecord(record)

            // then
            assertThat(careerFieldingStats.fieldingPercentage)
                .isEqualByComparingTo(BigDecimal("0.750"))
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class ValidateTest {
        @Test
        fun `정상 상태에서 validate는 예외를 발생시키지 않는다`() {
            // given: 정상 상태
            val record = createFieldingRecordMock(putOuts = 3, assists = 1)
            careerFieldingStats.addGameRecord(record)
            careerFieldingStats.addSeason()

            // when & then (no exception)
            careerFieldingStats.validate()
        }

        @Test
        fun `초기 상태에서 validate는 예외를 발생시키지 않는다`() {
            careerFieldingStats.validate()
        }

        @Test
        fun `seasonsPlayed가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "seasonsPlayed", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `gamesPlayed가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "gamesPlayed", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `putOuts가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "putOuts", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `assists가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "assists", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `errors가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "errors", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `doublePlays가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "doublePlays", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `passedBalls가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "passedBalls", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `triplePlays가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "triplePlays", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `caughtStealing이 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "caughtStealing", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
        }

        @Test
        fun `stolenBasesAllowed가 음수이면 StatsValidationException이 발생한다`() {
            setFieldDirectly(careerFieldingStats, "stolenBasesAllowed", -1)
            assertThatThrownBy { careerFieldingStats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
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
