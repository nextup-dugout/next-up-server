package com.nextup.core.domain.stats

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("SeasonAward 테스트")
class SeasonAwardTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.SHORTSTOP,
            id = 1L,
        )

    @Nested
    @DisplayName("create 팩토리 메서드")
    inner class Create {
        @Test
        fun `정상적으로 시즌 타이틀을 생성한다`() {
            // when
            val award =
                SeasonAward.create(
                    player = testPlayer,
                    year = 2026,
                    title = SeasonAwardTitle.BATTING_CHAMPION,
                    statValue = BigDecimal("0.350"),
                )

            // then
            assertThat(award.player).isEqualTo(testPlayer)
            assertThat(award.year).isEqualTo(2026)
            assertThat(award.title).isEqualTo(SeasonAwardTitle.BATTING_CHAMPION)
            assertThat(award.statValue).isEqualByComparingTo(BigDecimal("0.350"))
            assertThat(award.id).isEqualTo(0L)
        }

        @Test
        fun `statValue가 null인 시즌 타이틀을 생성한다`() {
            // when
            val award =
                SeasonAward.create(
                    player = testPlayer,
                    year = 2026,
                    title = SeasonAwardTitle.MVP,
                )

            // then
            assertThat(award.statValue).isNull()
        }

        @Test
        fun `연도가 0 이하이면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                SeasonAward.create(
                    player = testPlayer,
                    year = 0,
                    title = SeasonAwardTitle.BATTING_CHAMPION,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("연도는 양수여야 합니다")
        }

        @Test
        fun `음수 연도이면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                SeasonAward.create(
                    player = testPlayer,
                    year = -1,
                    title = SeasonAwardTitle.HOME_RUN_KING,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("연도는 양수여야 합니다")
        }

        @Test
        fun `모든 타이틀 종류로 생성할 수 있다`() {
            // when & then
            SeasonAwardTitle.entries.forEach { title ->
                val award =
                    SeasonAward.create(
                        player = testPlayer,
                        year = 2026,
                        title = title,
                        statValue = BigDecimal.ONE,
                    )
                assertThat(award.title).isEqualTo(title)
            }
        }
    }
}
