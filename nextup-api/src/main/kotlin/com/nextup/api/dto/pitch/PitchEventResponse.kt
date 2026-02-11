package com.nextup.api.dto.pitch

import com.nextup.core.domain.game.PitchEvent
import com.nextup.core.domain.game.PitchResult
import java.time.Instant

/**
 * 투구 이벤트 응답 DTO (Public API)
 */
data class PitchEventResponse(
    val id: Long,
    val gameId: Long,
    val inning: Int,
    val isTopInning: Boolean,
    val pitchNumber: Int,
    val result: PitchResult,
    val ballCount: Int,
    val strikeCount: Int,
    val description: String?,
    val countDisplay: String,
    val createdAt: Instant,
)

/**
 * Entity → Response DTO 변환
 */
fun PitchEvent.toResponse(): PitchEventResponse =
    PitchEventResponse(
        id = this.id,
        gameId = this.game.id,
        inning = this.inning,
        isTopInning = this.isTopInning,
        pitchNumber = this.pitchNumber,
        result = this.result,
        ballCount = this.ballCount,
        strikeCount = this.strikeCount,
        description = this.description,
        countDisplay = this.countDisplay,
        createdAt = this.createdAt,
    )

/**
 * List 변환
 */
fun List<PitchEvent>.toResponse(): List<PitchEventResponse> = this.map { it.toResponse() }
