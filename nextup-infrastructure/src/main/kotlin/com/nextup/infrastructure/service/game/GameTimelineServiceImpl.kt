package com.nextup.infrastructure.service.game

import com.nextup.core.domain.game.GameEvent
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.service.game.GameTimelineService
import com.nextup.core.service.game.dto.GameTimelineDto
import com.nextup.core.service.game.dto.TimelineEventDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 경기 타임라인 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class GameTimelineServiceImpl(
    private val gameEventRepository: GameEventRepositoryPort,
) : GameTimelineService {
    override fun getTimeline(
        gameId: Long,
        fromInning: Int?,
        toInning: Int?,
    ): GameTimelineDto {
        val allEvents = gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId)

        val filteredEvents =
            allEvents.filter { event ->
                val inningMatch =
                    when {
                        fromInning != null && toInning != null -> event.inning in fromInning..toInning
                        fromInning != null -> event.inning >= fromInning
                        toInning != null -> event.inning <= toInning
                        else -> true
                    }
                inningMatch
            }

        val eventDtos = filteredEvents.map { it.toTimelineEventDto() }

        return GameTimelineDto(
            gameId = gameId,
            events = eventDtos,
            totalEvents = eventDtos.size,
        )
    }

    /**
     * GameEvent를 TimelineEventDto로 변환합니다.
     */
    private fun GameEvent.toTimelineEventDto(): TimelineEventDto =
        TimelineEventDto(
            eventId = this.id,
            inning = this.inning,
            isTopInning = this.isTopInning,
            inningDisplay = formatInningDisplay(this.inning, this.isTopInning),
            eventType = this.eventType.displayName,
            description = this.description,
            batterId = this.batter?.id,
            batterName = this.batter?.player?.name,
            pitcherId = this.pitcher?.id,
            pitcherName = this.pitcher?.player?.name,
            plateAppearanceResult = this.plateAppearanceResult?.displayName,
            runsScored = this.runsScored,
            outCountBefore = this.outCountBefore,
            outCountAfter = this.outCountAfter,
            eventTimestamp = this.eventTimestamp,
        )

    /**
     * 이닝 표시 문자열을 생성합니다.
     */
    private fun formatInningDisplay(
        inning: Int,
        isTopInning: Boolean,
    ): String = "${inning}회${if (isTopInning) "초" else "말"}"
}
