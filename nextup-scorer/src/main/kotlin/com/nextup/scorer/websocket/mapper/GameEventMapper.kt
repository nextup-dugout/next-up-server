package com.nextup.scorer.websocket.mapper

import com.nextup.scorer.dto.websocket.GameEventMessage
import com.nextup.scorer.dto.websocket.PlayerBriefDto
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * GameEvent → GameEventMessage 매퍼
 *
 * 경기 이벤트 엔티티를 WebSocket 메시지로 변환합니다.
 * Note: GameEvent 엔티티가 구현되면 실제 변환 로직 추가 필요
 */
@Component
class GameEventMapper {

    /**
     * 타석 결과 이벤트 메시지를 생성합니다.
     *
     * @param eventId 이벤트 ID
     * @param eventType 이벤트 타입
     * @param inning 이닝
     * @param isTopInning 초/말 여부
     * @param description 설명
     * @param batter 타자 정보
     * @param pitcher 투수 정보
     * @param result 타석 결과
     * @param runsScored 득점
     * @return GameEventMessage
     */
    fun toPlateAppearanceMessage(
        eventId: Long,
        eventType: String,
        inning: Int,
        isTopInning: Boolean,
        description: String,
        batter: PlayerBriefDto?,
        pitcher: PlayerBriefDto?,
        result: String?,
        runsScored: Int
    ): GameEventMessage {
        return GameEventMessage(
            eventId = eventId,
            eventType = eventType,
            inning = inning,
            isTopInning = isTopInning,
            description = description,
            batter = batter,
            pitcher = pitcher,
            result = result,
            runsScored = runsScored,
            timestamp = Instant.now()
        )
    }

    /**
     * 선수 교체 이벤트 메시지를 생성합니다.
     *
     * @param eventId 이벤트 ID
     * @param inning 이닝
     * @param isTopInning 초/말 여부
     * @param description 교체 설명
     * @return GameEventMessage
     */
    fun toSubstitutionMessage(
        eventId: Long,
        inning: Int,
        isTopInning: Boolean,
        description: String
    ): GameEventMessage {
        return GameEventMessage(
            eventId = eventId,
            eventType = "SUBSTITUTION",
            inning = inning,
            isTopInning = isTopInning,
            description = description,
            batter = null,
            pitcher = null,
            result = null,
            runsScored = 0,
            timestamp = Instant.now()
        )
    }

    /**
     * 이닝 종료 이벤트 메시지를 생성합니다.
     *
     * @param eventId 이벤트 ID
     * @param inning 이닝
     * @param isTopInning 초/말 여부
     * @return GameEventMessage
     */
    fun toInningEndMessage(
        eventId: Long,
        inning: Int,
        isTopInning: Boolean
    ): GameEventMessage {
        val inningText = if (isTopInning) "${inning}회초" else "${inning}회말"
        return GameEventMessage(
            eventId = eventId,
            eventType = "INNING_END",
            inning = inning,
            isTopInning = isTopInning,
            description = "$inningText 종료",
            batter = null,
            pitcher = null,
            result = null,
            runsScored = 0,
            timestamp = Instant.now()
        )
    }
}
