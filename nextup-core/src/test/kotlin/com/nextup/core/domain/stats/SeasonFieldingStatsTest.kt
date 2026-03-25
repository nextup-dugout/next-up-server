package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.event.FieldingEventType
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
import java.math.RoundingMode

@DisplayName("SeasonFieldingStats 추가 테스트")
class SeasonFieldingStatsTest {
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        player = mockk(relaxed = true)
        every { player.id } returns 1L
    }

    private fun setField(
        obj: Any,
        fieldName: String,
        value: Int,
    ) {
        val field = obj.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }

    @Nested
    @DisplayName("create() 팩토리 메서드")
    inner class CreateTest {
        @Test
        fun `정상적으로 시즌 수비 통계를 생성한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)

            assertThat(stats.player).isEqualTo(player)
            assertThat(stats.year).isEqualTo(2026)
            assertThat(stats.teamId).isNull()
            assertThat(stats.competitionType).isEqualTo(CompetitionType.LEAGUE)
            assertThat(stats.gamesPlayed).isEqualTo(0)
        }

        @Test
        fun `팀 ID와 대회 유형을 지정하여 생성할 수 있다`() {
            val stats =
                SeasonFieldingStats.create(
                    player = player,
                    year = 2026,
                    teamId = 5L,
                    competitionType = CompetitionType.FRIENDLY,
                )

            assertThat(stats.teamId).isEqualTo(5L)
            assertThat(stats.competitionType).isEqualTo(CompetitionType.FRIENDLY)
        }

        @Test
        fun `연도가 0 이하이면 예외가 발생한다`() {
            assertThatThrownBy {
                SeasonFieldingStats.create(player, 0)
            }.isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("연도는 양수여야 합니다")
        }

        @Test
        fun `음수 연도이면 예외가 발생한다`() {
            assertThatThrownBy {
                SeasonFieldingStats.create(player, -1)
            }.isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("연도는 양수여야 합니다")
        }
    }

    @Nested
    @DisplayName("totalChances 계산")
    inner class TotalChancesTest {
        @Test
        fun `수비 기회 = 자살 + 보살 + 실책`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "putOuts", 5)
            setField(stats, "assists", 3)
            setField(stats, "errors", 2)

            assertThat(stats.totalChances).isEqualTo(10)
        }

        @Test
        fun `모두 0이면 수비 기회도 0`() {
            val stats = SeasonFieldingStats.create(player, 2026)

            assertThat(stats.totalChances).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("fieldingPercentage 계산")
    inner class FieldingPercentageTest {
        @Test
        fun `수비 기회가 0이면 null 반환`() {
            val stats = SeasonFieldingStats.create(player, 2026)

            assertThat(stats.fieldingPercentage).isNull()
        }

        @Test
        fun `수비율을 정확하게 계산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "putOuts", 7)
            setField(stats, "assists", 2)
            setField(stats, "errors", 1)

            // (7 + 2) / (7 + 2 + 1) = 9/10 = 0.900
            assertThat(stats.fieldingPercentage).isEqualTo(
                BigDecimal(9).divide(BigDecimal(10), 3, RoundingMode.HALF_UP),
            )
        }

        @Test
        fun `실책이 없으면 수비율 1_000`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "putOuts", 5)
            setField(stats, "assists", 3)
            setField(stats, "errors", 0)

            assertThat(stats.fieldingPercentage).isEqualByComparingTo(BigDecimal("1.000"))
        }
    }

    @Nested
    @DisplayName("applyLiveFieldingUpdate")
    inner class ApplyLiveFieldingUpdateTest {
        @Test
        fun `PUT_OUT 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            assertThat(stats.putOuts).isEqualTo(1)
        }

        @Test
        fun `ASSIST 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.ASSIST)
            assertThat(stats.assists).isEqualTo(1)
        }

        @Test
        fun `ERROR 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.ERROR)
            assertThat(stats.errors).isEqualTo(1)
        }

        @Test
        fun `DOUBLE_PLAY 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.DOUBLE_PLAY)
            assertThat(stats.doublePlays).isEqualTo(1)
        }

        @Test
        fun `TRIPLE_PLAY 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.TRIPLE_PLAY)
            assertThat(stats.triplePlays).isEqualTo(1)
        }

        @Test
        fun `PASSED_BALL 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.PASSED_BALL)
            assertThat(stats.passedBalls).isEqualTo(1)
        }

        @Test
        fun `CAUGHT_STEALING 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.CAUGHT_STEALING)
            assertThat(stats.caughtStealing).isEqualTo(1)
        }

        @Test
        fun `STOLEN_BASE_ALLOWED 이벤트를 적용한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.applyLiveFieldingUpdate(FieldingEventType.STOLEN_BASE_ALLOWED)
            assertThat(stats.stolenBasesAllowed).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("revertLiveFieldingUpdate")
    inner class RevertLiveFieldingUpdateTest {
        @Test
        fun `PUT_OUT 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "putOuts", 3)
            stats.revertLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            assertThat(stats.putOuts).isEqualTo(2)
        }

        @Test
        fun `ASSIST 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "assists", 2)
            stats.revertLiveFieldingUpdate(FieldingEventType.ASSIST)
            assertThat(stats.assists).isEqualTo(1)
        }

        @Test
        fun `ERROR 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "errors", 1)
            stats.revertLiveFieldingUpdate(FieldingEventType.ERROR)
            assertThat(stats.errors).isEqualTo(0)
        }

        @Test
        fun `DOUBLE_PLAY 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "doublePlays", 1)
            stats.revertLiveFieldingUpdate(FieldingEventType.DOUBLE_PLAY)
            assertThat(stats.doublePlays).isEqualTo(0)
        }

        @Test
        fun `TRIPLE_PLAY 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "triplePlays", 1)
            stats.revertLiveFieldingUpdate(FieldingEventType.TRIPLE_PLAY)
            assertThat(stats.triplePlays).isEqualTo(0)
        }

        @Test
        fun `PASSED_BALL 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "passedBalls", 1)
            stats.revertLiveFieldingUpdate(FieldingEventType.PASSED_BALL)
            assertThat(stats.passedBalls).isEqualTo(0)
        }

        @Test
        fun `CAUGHT_STEALING 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "caughtStealing", 2)
            stats.revertLiveFieldingUpdate(FieldingEventType.CAUGHT_STEALING)
            assertThat(stats.caughtStealing).isEqualTo(1)
        }

        @Test
        fun `STOLEN_BASE_ALLOWED 이벤트를 역산한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "stolenBasesAllowed", 1)
            stats.revertLiveFieldingUpdate(FieldingEventType.STOLEN_BASE_ALLOWED)
            assertThat(stats.stolenBasesAllowed).isEqualTo(0)
        }

        @Test
        fun `이미 0인 값에서 역산하면 0을 유지한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            stats.revertLiveFieldingUpdate(FieldingEventType.PUT_OUT)
            assertThat(stats.putOuts).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("validate")
    inner class ValidateTest {
        @Test
        fun `정상 상태에서는 예외가 발생하지 않는다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "gamesPlayed", 5)
            setField(stats, "putOuts", 10)
            setField(stats, "assists", 5)

            stats.validate() // no exception
        }

        @Test
        fun `gamesPlayed가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "gamesPlayed", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("출전 경기 수는 0 이상")
        }

        @Test
        fun `putOuts가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "putOuts", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("자살")
        }

        @Test
        fun `assists가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "assists", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("보살")
        }

        @Test
        fun `errors가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "errors", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("실책")
        }

        @Test
        fun `doublePlays가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "doublePlays", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("병살 관여")
        }

        @Test
        fun `passedBalls가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "passedBalls", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("포일")
        }

        @Test
        fun `triplePlays가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "triplePlays", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("삼중살 관여")
        }

        @Test
        fun `caughtStealing이 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "caughtStealing", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("도루 저지")
        }

        @Test
        fun `stolenBasesAllowed가 음수이면 예외가 발생한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "stolenBasesAllowed", -1)

            assertThatThrownBy { stats.validate() }
                .isInstanceOf(StatsValidationException::class.java)
                .hasMessageContaining("도루 허용")
        }
    }

    @Nested
    @DisplayName("addGamePlayedOnly")
    inner class AddGamePlayedOnlyTest {
        @Test
        fun `gamesPlayed만 1 증가한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            assertThat(stats.gamesPlayed).isEqualTo(0)

            stats.addGamePlayedOnly()

            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.putOuts).isEqualTo(0)
            assertThat(stats.assists).isEqualTo(0)
        }

        @Test
        fun `여러 번 호출하면 그만큼 증가한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)

            stats.addGamePlayedOnly()
            stats.addGamePlayedOnly()
            stats.addGamePlayedOnly()

            assertThat(stats.gamesPlayed).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("applyFieldCorrection - triplePlays, caughtStealing, stolenBasesAllowed")
    inner class AdditionalFieldCorrectionTest {
        @Test
        fun `triplePlays 필드 정정이 동작한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "triplePlays", 2)

            stats.applyFieldCorrection("triplePlays", 3)

            assertThat(stats.triplePlays).isEqualTo(5)
        }

        @Test
        fun `caughtStealing 필드 정정이 동작한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "caughtStealing", 4)

            stats.applyFieldCorrection("caughtStealing", -2)

            assertThat(stats.caughtStealing).isEqualTo(2)
        }

        @Test
        fun `stolenBasesAllowed 필드 정정이 동작한다`() {
            val stats = SeasonFieldingStats.create(player, 2026)
            setField(stats, "stolenBasesAllowed", 3)

            stats.applyFieldCorrection("stolenBasesAllowed", 1)

            assertThat(stats.stolenBasesAllowed).isEqualTo(4)
        }
    }
}
