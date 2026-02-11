package com.nextup.api.mapper.game

import com.nextup.core.service.game.dto.GameTimelineDto
import com.nextup.core.service.game.dto.TimelineEventDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("GameTimelineMapper 테스트")
class GameTimelineMapperTest {
    @Test
    fun `GameTimelineDto를 GameTimelineResponse로 변환한다`() {
        // given
        val dto =
            GameTimelineDto(
                gameId = 1L,
                events =
                    listOf(
                        TimelineEventDto(
                            eventId = 10L,
                            inning = 1,
                            isTopInning = true,
                            inningDisplay = "1회초",
                            eventType = "PLATE_APPEARANCE",
                            description = "홍길동 안타",
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
                        TimelineEventDto(
                            eventId = 11L,
                            inning = 1,
                            isTopInning = true,
                            inningDisplay = "1회초",
                            eventType = "PLATE_APPEARANCE",
                            description = "이영희 홈런",
                            batterId = 101L,
                            batterName = "이영희",
                            pitcherId = 200L,
                            pitcherName = "김철수",
                            plateAppearanceResult = "HOME_RUN",
                            runsScored = 2,
                            outCountBefore = 0,
                            outCountAfter = 0,
                            eventTimestamp = Instant.parse("2026-01-01T10:05:00Z"),
                        ),
                    ),
                totalEvents = 2,
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gameId).isEqualTo(1L)
        assertThat(response.totalEvents).isEqualTo(2)
        assertThat(response.events).hasSize(2)
        assertThat(response.events[0].eventId).isEqualTo(10L)
        assertThat(response.events[0].inningDisplay).isEqualTo("1회초")
        assertThat(response.events[0].batterName).isEqualTo("홍길동")
        assertThat(response.events[1].runsScored).isEqualTo(2)
    }

    @Test
    fun `TimelineEventDto를 TimelineEventResponse로 변환한다`() {
        // given
        val dto =
            TimelineEventDto(
                eventId = 5L,
                inning = 3,
                isTopInning = false,
                inningDisplay = "3회말",
                eventType = "PLATE_APPEARANCE",
                description = "삼진 아웃",
                batterId = 300L,
                batterName = "박선수",
                pitcherId = 400L,
                pitcherName = "최투수",
                plateAppearanceResult = "STRIKEOUT",
                runsScored = 0,
                outCountBefore = 1,
                outCountAfter = 2,
                eventTimestamp = Instant.parse("2026-01-01T11:00:00Z"),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.eventId).isEqualTo(5L)
        assertThat(response.inning).isEqualTo(3)
        assertThat(response.isTopInning).isFalse()
        assertThat(response.inningDisplay).isEqualTo("3회말")
        assertThat(response.eventType).isEqualTo("PLATE_APPEARANCE")
        assertThat(response.description).isEqualTo("삼진 아웃")
        assertThat(response.batterId).isEqualTo(300L)
        assertThat(response.batterName).isEqualTo("박선수")
        assertThat(response.pitcherId).isEqualTo(400L)
        assertThat(response.pitcherName).isEqualTo("최투수")
        assertThat(response.plateAppearanceResult).isEqualTo("STRIKEOUT")
        assertThat(response.runsScored).isEqualTo(0)
        assertThat(response.outCountBefore).isEqualTo(1)
        assertThat(response.outCountAfter).isEqualTo(2)
    }

    @Test
    fun `null 값이 포함된 TimelineEventDto를 변환한다`() {
        // given
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

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.batterId).isNull()
        assertThat(response.batterName).isNull()
        assertThat(response.pitcherId).isNull()
        assertThat(response.pitcherName).isNull()
        assertThat(response.plateAppearanceResult).isNull()
    }
}
