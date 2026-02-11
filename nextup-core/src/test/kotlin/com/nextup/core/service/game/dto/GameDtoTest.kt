package com.nextup.core.service.game.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("Game DTO 테스트")
class GameDtoTest {
    @Nested
    @DisplayName("GameTimelineDto")
    inner class GameTimelineDtoTest {
        @Test
        fun `GameTimelineDto 생성 및 속성 접근`() {
            val dto =
                GameTimelineDto(
                    gameId = 1L,
                    events = emptyList(),
                    totalEvents = 0,
                )

            assertThat(dto.gameId).isEqualTo(1L)
            assertThat(dto.events).isEmpty()
            assertThat(dto.totalEvents).isEqualTo(0)
        }

        @Test
        fun `GameTimelineDto with events`() {
            val events =
                listOf(
                    TimelineEventDto(
                        eventId = 1L,
                        inning = 1,
                        isTopInning = true,
                        inningDisplay = "1회초",
                        eventType = "PLATE_APPEARANCE",
                        description = "안타",
                        batterId = 100L,
                        batterName = "홍길동",
                        pitcherId = 200L,
                        pitcherName = "김철수",
                        plateAppearanceResult = "SINGLE",
                        runsScored = 0,
                        outCountBefore = 0,
                        outCountAfter = 0,
                        eventTimestamp = Instant.parse("2026-01-01T10:00:00Z"),
                    ),
                )

            val dto =
                GameTimelineDto(
                    gameId = 1L,
                    events = events,
                    totalEvents = 1,
                )

            assertThat(dto.events).hasSize(1)
            assertThat(dto.totalEvents).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("TimelineEventDto")
    inner class TimelineEventDtoTest {
        @Test
        fun `TimelineEventDto 생성 및 속성 접근`() {
            val dto =
                TimelineEventDto(
                    eventId = 10L,
                    inning = 3,
                    isTopInning = false,
                    inningDisplay = "3회말",
                    eventType = "PLATE_APPEARANCE",
                    description = "홈런",
                    batterId = 100L,
                    batterName = "홍길동",
                    pitcherId = 200L,
                    pitcherName = "김철수",
                    plateAppearanceResult = "HOME_RUN",
                    runsScored = 2,
                    outCountBefore = 1,
                    outCountAfter = 1,
                    eventTimestamp = Instant.parse("2026-01-01T11:00:00Z"),
                )

            assertThat(dto.eventId).isEqualTo(10L)
            assertThat(dto.inning).isEqualTo(3)
            assertThat(dto.isTopInning).isFalse()
            assertThat(dto.inningDisplay).isEqualTo("3회말")
            assertThat(dto.eventType).isEqualTo("PLATE_APPEARANCE")
            assertThat(dto.description).isEqualTo("홈런")
            assertThat(dto.batterId).isEqualTo(100L)
            assertThat(dto.batterName).isEqualTo("홍길동")
            assertThat(dto.pitcherId).isEqualTo(200L)
            assertThat(dto.pitcherName).isEqualTo("김철수")
            assertThat(dto.plateAppearanceResult).isEqualTo("HOME_RUN")
            assertThat(dto.runsScored).isEqualTo(2)
            assertThat(dto.outCountBefore).isEqualTo(1)
            assertThat(dto.outCountAfter).isEqualTo(1)
        }

        @Test
        fun `TimelineEventDto with null values`() {
            val dto =
                TimelineEventDto(
                    eventId = 20L,
                    inning = 5,
                    isTopInning = true,
                    inningDisplay = "5회초",
                    eventType = "INNING_START",
                    description = "5회초 시작",
                    batterId = null,
                    batterName = null,
                    pitcherId = null,
                    pitcherName = null,
                    plateAppearanceResult = null,
                    runsScored = 0,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventTimestamp = Instant.parse("2026-01-01T12:00:00Z"),
                )

            assertThat(dto.batterId).isNull()
            assertThat(dto.batterName).isNull()
            assertThat(dto.pitcherId).isNull()
            assertThat(dto.pitcherName).isNull()
            assertThat(dto.plateAppearanceResult).isNull()
        }

        @Test
        fun `TimelineEventDto copy 테스트`() {
            val original =
                TimelineEventDto(
                    eventId = 1L,
                    inning = 1,
                    isTopInning = true,
                    inningDisplay = "1회초",
                    eventType = "PLATE_APPEARANCE",
                    description = "안타",
                    batterId = 100L,
                    batterName = "홍길동",
                    pitcherId = 200L,
                    pitcherName = "김철수",
                    plateAppearanceResult = "SINGLE",
                    runsScored = 0,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventTimestamp = Instant.parse("2026-01-01T10:00:00Z"),
                )

            val copied = original.copy(runsScored = 1)

            assertThat(copied.runsScored).isEqualTo(1)
            assertThat(copied.eventId).isEqualTo(original.eventId)
        }

        @Test
        fun `TimelineEventDto equals 테스트`() {
            val dto1 =
                TimelineEventDto(
                    eventId = 1L,
                    inning = 1,
                    isTopInning = true,
                    inningDisplay = "1회초",
                    eventType = "PLATE_APPEARANCE",
                    description = "안타",
                    batterId = 100L,
                    batterName = "홍길동",
                    pitcherId = 200L,
                    pitcherName = "김철수",
                    plateAppearanceResult = "SINGLE",
                    runsScored = 0,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventTimestamp = Instant.parse("2026-01-01T10:00:00Z"),
                )

            val dto2 =
                TimelineEventDto(
                    eventId = 1L,
                    inning = 1,
                    isTopInning = true,
                    inningDisplay = "1회초",
                    eventType = "PLATE_APPEARANCE",
                    description = "안타",
                    batterId = 100L,
                    batterName = "홍길동",
                    pitcherId = 200L,
                    pitcherName = "김철수",
                    plateAppearanceResult = "SINGLE",
                    runsScored = 0,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventTimestamp = Instant.parse("2026-01-01T10:00:00Z"),
                )

            assertThat(dto1).isEqualTo(dto2)
            assertThat(dto1.hashCode()).isEqualTo(dto2.hashCode())
        }

        @Test
        fun `TimelineEventDto toString 테스트`() {
            val dto =
                TimelineEventDto(
                    eventId = 1L,
                    inning = 1,
                    isTopInning = true,
                    inningDisplay = "1회초",
                    eventType = "PLATE_APPEARANCE",
                    description = "안타",
                    batterId = 100L,
                    batterName = "홍길동",
                    pitcherId = 200L,
                    pitcherName = "김철수",
                    plateAppearanceResult = "SINGLE",
                    runsScored = 0,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventTimestamp = Instant.parse("2026-01-01T10:00:00Z"),
                )

            val str = dto.toString()

            assertThat(str).contains("eventId=1")
            assertThat(str).contains("inning=1")
            assertThat(str).contains("홍길동")
        }
    }
}
