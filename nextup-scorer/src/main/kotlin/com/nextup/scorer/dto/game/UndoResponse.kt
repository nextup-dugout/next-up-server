package com.nextup.scorer.dto.game

/**
 * Undo 응답 DTO
 */
data class UndoResponse(
    val undoneEventId: Long,
    val eventType: String,
    val restoredState: GameStateResponse,
    val message: String,
)
