package com.nextup.infrastructure.service.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.Player
import com.nextup.core.port.repository.GameEventRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant

@DisplayName("GameTimelineServiceImpl 테스트")
class GameTimelineServiceImplTest {
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var service: GameTimelineServiceImpl

    @BeforeEach
    fun setUp() {
        gameEventRepository = mockk()
        service = GameTimelineServiceImpl(gameEventRepository)
    }

    @Nested
    @DisplayName("getTimeline")
    inner class GetTimelineTest {
        @Test
        @DisplayName("이닝 필터 없이 모든 이벤트를 조회한다")
        fun getTimelineWithoutInningFilter() {
            // given
            val gameId = 1L
            val events =
                listOf(
                    createGameEvent(1L, inning = 1, isTopInning = true),
                    createGameEvent(2L, inning = 1, isTopInning = false),
                    createGameEvent(3L, inning = 2, isTopInning = true),
                )

            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns events

            // when
            val result = service.getTimeline(gameId, fromInning = null, toInning = null)

            // then
            assertThat(result.gameId).isEqualTo(gameId)
            assertThat(result.events).hasSize(3)
            assertThat(result.totalEvents).isEqualTo(3)
        }

        @Test
        @DisplayName("fromInning만 지정하여 이벤트를 필터링한다")
        fun getTimelineWithFromInningOnly() {
            // given
            val gameId = 1L
            val events =
                listOf(
                    createGameEvent(1L, inning = 1, isTopInning = true),
                    createGameEvent(2L, inning = 2, isTopInning = true),
                    createGameEvent(3L, inning = 3, isTopInning = true),
                    createGameEvent(4L, inning = 4, isTopInning = true),
                )

            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns events

            // when
            val result = service.getTimeline(gameId, fromInning = 2, toInning = null)

            // then
            assertThat(result.events).hasSize(3)
            assertThat(result.events.map { it.inning }).containsExactly(2, 3, 4)
        }

        @Test
        @DisplayName("toInning만 지정하여 이벤트를 필터링한다")
        fun getTimelineWithToInningOnly() {
            // given
            val gameId = 1L
            val events =
                listOf(
                    createGameEvent(1L, inning = 1, isTopInning = true),
                    createGameEvent(2L, inning = 2, isTopInning = true),
                    createGameEvent(3L, inning = 3, isTopInning = true),
                    createGameEvent(4L, inning = 4, isTopInning = true),
                )

            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns events

            // when
            val result = service.getTimeline(gameId, fromInning = null, toInning = 3)

            // then
            assertThat(result.events).hasSize(3)
            assertThat(result.events.map { it.inning }).containsExactly(1, 2, 3)
        }

        @Test
        @DisplayName("fromInning과 toInning 범위로 이벤트를 필터링한다")
        fun getTimelineWithInningRange() {
            // given
            val gameId = 1L
            val events =
                listOf(
                    createGameEvent(1L, inning = 1, isTopInning = true),
                    createGameEvent(2L, inning = 2, isTopInning = true),
                    createGameEvent(3L, inning = 3, isTopInning = true),
                    createGameEvent(4L, inning = 4, isTopInning = true),
                    createGameEvent(5L, inning = 5, isTopInning = true),
                )

            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns events

            // when
            val result = service.getTimeline(gameId, fromInning = 2, toInning = 4)

            // then
            assertThat(result.events).hasSize(3)
            assertThat(result.events.map { it.inning }).containsExactly(2, 3, 4)
        }

        @Test
        @DisplayName("이벤트가 없으면 빈 리스트를 반환한다")
        fun getTimelineReturnsEmptyEvents() {
            // given
            val gameId = 1L
            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns emptyList()

            // when
            val result = service.getTimeline(gameId, fromInning = null, toInning = null)

            // then
            assertThat(result.events).isEmpty()
            assertThat(result.totalEvents).isEqualTo(0)
        }

        @Test
        @DisplayName("totalEvents 개수가 정확하다")
        fun getTimelineIncludesCorrectTotalEvents() {
            // given
            val gameId = 1L
            val events = (1..7).map { createGameEvent(it.toLong(), inning = it, isTopInning = true) }

            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns events

            // when
            val result = service.getTimeline(gameId, fromInning = null, toInning = null)

            // then
            assertThat(result.totalEvents).isEqualTo(7)
        }

        @Test
        @DisplayName("이벤트 필드가 정확하게 매핑된다")
        fun getTimelineMapsEventFieldsCorrectly() {
            // given
            val gameId = 1L
            val event =
                createGameEvent(
                    id = 1L,
                    inning = 3,
                    isTopInning = false,
                    eventType = GameEventType.PLATE_APPEARANCE,
                    description = "김철수 우전안타",
                    batterName = "김철수",
                    pitcherName = "박영수",
                    plateAppearanceResult = PlateAppearanceResult.SINGLE,
                    runsScored = 2,
                    outCountBefore = 1,
                    outCountAfter = 1,
                )

            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns listOf(event)

            // when
            val result = service.getTimeline(gameId, fromInning = null, toInning = null)

            // then
            val dto = result.events[0]
            assertThat(dto.eventId).isEqualTo(1L)
            assertThat(dto.inning).isEqualTo(3)
            assertThat(dto.isTopInning).isFalse()
            assertThat(dto.eventType).isEqualTo("타석 결과")
            assertThat(dto.description).isEqualTo("김철수 우전안타")
            assertThat(dto.batterName).isEqualTo("김철수")
            assertThat(dto.pitcherName).isEqualTo("박영수")
            assertThat(dto.plateAppearanceResult).isNotNull()
            assertThat(dto.runsScored).isEqualTo(2)
            assertThat(dto.outCountBefore).isEqualTo(1)
            assertThat(dto.outCountAfter).isEqualTo(1)
        }

        @Test
        @DisplayName("이닝 표시 형식이 정확하다")
        fun getTimelineFormatsInningDisplayCorrectly() {
            // given
            val gameId = 1L
            val events =
                listOf(
                    createGameEvent(1L, inning = 3, isTopInning = true),
                    createGameEvent(2L, inning = 5, isTopInning = false),
                    createGameEvent(3L, inning = 9, isTopInning = true),
                )

            every { gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId) } returns events

            // when
            val result = service.getTimeline(gameId, fromInning = null, toInning = null)

            // then
            assertThat(result.events[0].inningDisplay).isEqualTo("3회초")
            assertThat(result.events[1].inningDisplay).isEqualTo("5회말")
            assertThat(result.events[2].inningDisplay).isEqualTo("9회초")
        }
    }

    // Helper methods
    private fun createGameEvent(
        id: Long,
        inning: Int,
        isTopInning: Boolean,
        eventType: GameEventType = GameEventType.PLATE_APPEARANCE,
        description: String = "이벤트 설명",
        batterName: String? = null,
        pitcherName: String? = null,
        plateAppearanceResult: PlateAppearanceResult? = null,
        runsScored: Int = 0,
        outCountBefore: Int = 0,
        outCountAfter: Int = 0,
    ): GameEvent {
        val event = mockk<GameEvent>(relaxed = true)
        val batter = batterName?.let { createGamePlayer(it) }
        val pitcher = pitcherName?.let { createGamePlayer(it) }

        every { event.id } returns id
        every { event.inning } returns inning
        every { event.isTopInning } returns isTopInning
        every { event.eventType } returns eventType
        every { event.description } returns description
        every { event.batter } returns batter
        every { event.pitcher } returns pitcher
        every { event.plateAppearanceResult } returns plateAppearanceResult
        every { event.runsScored } returns runsScored
        every { event.outCountBefore } returns outCountBefore
        every { event.outCountAfter } returns outCountAfter
        every { event.eventTimestamp } returns Instant.now()

        return event
    }

    private fun createGamePlayer(playerName: String): GamePlayer {
        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        val player = mockk<Player>(relaxed = true)

        every { gamePlayer.id } returns 1L
        every { gamePlayer.player } returns player
        every { player.name } returns playerName

        return gamePlayer
    }
}
