package com.nextup.scorer.websocket.mapper

import com.nextup.scorer.dto.websocket.PlayerBriefDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GameEventMapper")
class GameEventMapperTest {
    private lateinit var mapper: GameEventMapper

    @BeforeEach
    fun setUp() {
        mapper = GameEventMapper()
    }

    @Nested
    @DisplayName("toPlateAppearanceMessage")
    inner class ToPlateAppearanceMessage {
        @Test
        fun `should create message with all fields`() {
            // given
            val batter = PlayerBriefDto(id = 1L, name = "김타자", backNumber = 7)
            val pitcher = PlayerBriefDto(id = 2L, name = "박투수", backNumber = 18)

            // when
            val message =
                mapper.toPlateAppearanceMessage(
                    eventId = 100L,
                    eventType = "PLATE_APPEARANCE",
                    inning = 3,
                    isTopInning = true,
                    description = "우전 안타",
                    batter = batter,
                    pitcher = pitcher,
                    result = "SINGLE",
                    runsScored = 1
                )

            // then
            assertThat(message.eventId).isEqualTo(100L)
            assertThat(message.eventType).isEqualTo("PLATE_APPEARANCE")
            assertThat(message.inning).isEqualTo(3)
            assertThat(message.isTopInning).isTrue()
            assertThat(message.description).isEqualTo("우전 안타")
            assertThat(message.batter).isEqualTo(batter)
            assertThat(message.pitcher).isEqualTo(pitcher)
            assertThat(message.result).isEqualTo("SINGLE")
            assertThat(message.runsScored).isEqualTo(1)
            assertThat(message.timestamp).isNotNull()
        }

        @Test
        fun `should create message with null batter and pitcher`() {
            // when
            val message =
                mapper.toPlateAppearanceMessage(
                    eventId = 101L,
                    eventType = "PLATE_APPEARANCE",
                    inning = 5,
                    isTopInning = false,
                    description = "대타 교체",
                    batter = null,
                    pitcher = null,
                    result = null,
                    runsScored = 0
                )

            // then
            assertThat(message.batter).isNull()
            assertThat(message.pitcher).isNull()
            assertThat(message.result).isNull()
            assertThat(message.runsScored).isEqualTo(0)
        }

        @Test
        fun `should create message for home run with runs scored`() {
            // given
            val batter = PlayerBriefDto(id = 5L, name = "홈런왕", backNumber = 44)
            val pitcher = PlayerBriefDto(id = 6L, name = "피안타", backNumber = 21)

            // when
            val message =
                mapper.toPlateAppearanceMessage(
                    eventId = 102L,
                    eventType = "PLATE_APPEARANCE",
                    inning = 7,
                    isTopInning = true,
                    description = "3점 홈런",
                    batter = batter,
                    pitcher = pitcher,
                    result = "HOME_RUN",
                    runsScored = 3
                )

            // then
            assertThat(message.result).isEqualTo("HOME_RUN")
            assertThat(message.runsScored).isEqualTo(3)
        }

        @Test
        fun `should create message for bottom inning`() {
            // when
            val message =
                mapper.toPlateAppearanceMessage(
                    eventId = 103L,
                    eventType = "PLATE_APPEARANCE",
                    inning = 9,
                    isTopInning = false,
                    description = "끝내기 안타",
                    batter = PlayerBriefDto(id = 10L, name = "영웅", backNumber = 1),
                    pitcher = null,
                    result = "SINGLE",
                    runsScored = 1
                )

            // then
            assertThat(message.inning).isEqualTo(9)
            assertThat(message.isTopInning).isFalse()
        }
    }

    @Nested
    @DisplayName("toSubstitutionMessage")
    inner class ToSubstitutionMessage {
        @Test
        fun `should create substitution message`() {
            // when
            val message =
                mapper.toSubstitutionMessage(
                    eventId = 200L,
                    inning = 6,
                    isTopInning = true,
                    description = "대타 교체: 김대타 → 박타자"
                )

            // then
            assertThat(message.eventId).isEqualTo(200L)
            assertThat(message.eventType).isEqualTo("SUBSTITUTION")
            assertThat(message.inning).isEqualTo(6)
            assertThat(message.isTopInning).isTrue()
            assertThat(message.description).isEqualTo("대타 교체: 김대타 → 박타자")
            assertThat(message.batter).isNull()
            assertThat(message.pitcher).isNull()
            assertThat(message.result).isNull()
            assertThat(message.runsScored).isEqualTo(0)
            assertThat(message.timestamp).isNotNull()
        }

        @Test
        fun `should create substitution message for pitcher change`() {
            // when
            val message =
                mapper.toSubstitutionMessage(
                    eventId = 201L,
                    inning = 8,
                    isTopInning = false,
                    description = "투수 교체: 마무리 등판"
                )

            // then
            assertThat(message.eventType).isEqualTo("SUBSTITUTION")
            assertThat(message.description).isEqualTo("투수 교체: 마무리 등판")
        }
    }

    @Nested
    @DisplayName("toInningEndMessage")
    inner class ToInningEndMessage {
        @Test
        fun `should create top inning end message`() {
            // when
            val message =
                mapper.toInningEndMessage(
                    eventId = 300L,
                    inning = 5,
                    isTopInning = true
                )

            // then
            assertThat(message.eventId).isEqualTo(300L)
            assertThat(message.eventType).isEqualTo("INNING_END")
            assertThat(message.inning).isEqualTo(5)
            assertThat(message.isTopInning).isTrue()
            assertThat(message.description).isEqualTo("5회초 종료")
            assertThat(message.batter).isNull()
            assertThat(message.pitcher).isNull()
            assertThat(message.result).isNull()
            assertThat(message.runsScored).isEqualTo(0)
            assertThat(message.timestamp).isNotNull()
        }

        @Test
        fun `should create bottom inning end message`() {
            // when
            val message =
                mapper.toInningEndMessage(
                    eventId = 301L,
                    inning = 7,
                    isTopInning = false
                )

            // then
            assertThat(message.description).isEqualTo("7회말 종료")
            assertThat(message.isTopInning).isFalse()
        }

        @Test
        fun `should create first inning end message`() {
            // when
            val message =
                mapper.toInningEndMessage(
                    eventId = 302L,
                    inning = 1,
                    isTopInning = true
                )

            // then
            assertThat(message.description).isEqualTo("1회초 종료")
        }

        @Test
        fun `should create ninth inning end message`() {
            // when
            val message =
                mapper.toInningEndMessage(
                    eventId = 303L,
                    inning = 9,
                    isTopInning = false
                )

            // then
            assertThat(message.description).isEqualTo("9회말 종료")
        }
    }
}
