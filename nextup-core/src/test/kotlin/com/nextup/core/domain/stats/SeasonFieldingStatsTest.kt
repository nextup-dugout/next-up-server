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

            val record2 = mockk<FieldingRecord>()
            every { record2.putOuts } returns 3
            every { record2.assists } returns 2
            every { record2.errors } returns 1
            every { record2.doublePlays } returns 1
            every { record2.passedBalls } returns 1

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

            seasonFieldingStats.addGameRecord(record)

            // then
            assertThat(seasonFieldingStats.totalChances).isEqualTo(13)
            assertThat(seasonFieldingStats.fieldingPercentage).isNotNull
        }
    }
}
