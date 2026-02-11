package com.nextup.scorer.service.websocket

import com.nextup.scorer.dto.websocket.GameEventMessage
import com.nextup.scorer.dto.websocket.GameStateMessage
import com.nextup.scorer.dto.websocket.ScoreboardMessage
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Service

/**
 * 경기 브로드캐스트 서비스
 *
 * WebSocket을 통해 경기 이벤트, 스코어보드, 상태를 실시간으로 브로드캐스트합니다.
 * 모든 브로드캐스트는 비동기로 처리됩니다.
 */
@Service
class GameBroadcastService(
    private val messagingTemplate: SimpMessagingTemplate
) {
    /**
     * 경기 이벤트를 브로드캐스트합니다.
     *
     * @param gameId 경기 ID
     * @param event 이벤트 메시지
     */
    fun broadcastEvent(
        gameId: Long,
        event: GameEventMessage
    ) {
        messagingTemplate.convertAndSend(
            "/topic/games/$gameId/events",
            event
        )
    }

    /**
     * 스코어보드를 브로드캐스트합니다.
     *
     * @param gameId 경기 ID
     * @param scoreboard 스코어보드 메시지
     */
    fun broadcastScoreboard(
        gameId: Long,
        scoreboard: ScoreboardMessage
    ) {
        messagingTemplate.convertAndSend(
            "/topic/games/$gameId/scoreboard",
            scoreboard
        )
    }

    /**
     * 경기 상태를 브로드캐스트합니다.
     *
     * @param gameId 경기 ID
     * @param state 상태 메시지
     */
    fun broadcastState(
        gameId: Long,
        state: GameStateMessage
    ) {
        messagingTemplate.convertAndSend(
            "/topic/games/$gameId/state",
            state
        )
    }
}
