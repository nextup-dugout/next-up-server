package com.nextup.scorer.dto.lineup

import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.player.Position
import java.time.Instant

/**
 * 라인업 제출 응답 DTO
 */
data class LineupSubmissionResponse(
    val id: Long,
    val gameId: Long,
    val teamId: Long,
    val teamName: String,
    val submittedById: Long,
    val submittedByName: String,
    val status: LineupSubmissionStatus,
    val statusDisplayName: String,
    val submittedAt: Instant?,
    val confirmedAt: Instant?,
    val confirmedById: Long?,
    val confirmedByName: String?,
    val rejectionReason: String?,
    val rejectedById: Long?,
    val rejectedByName: String?,
    val exchangePendingAt: Instant?,
    val exchangeRejectionReason: String?,
    val exchangeRejectedById: Long?,
    val exchangeRejectedByName: String?,
    val entries: List<LineupEntryResponse>,
) {
    companion object {
        fun from(
            submission: LineupSubmission,
            entries: List<LineupEntry>,
        ): LineupSubmissionResponse =
            LineupSubmissionResponse(
                id = submission.id,
                gameId = submission.game.id,
                teamId = submission.team.id,
                teamName = submission.team.name,
                submittedById = submission.submittedBy.id,
                submittedByName = submission.submittedBy.nickname,
                status = submission.status,
                statusDisplayName = submission.status.displayName,
                submittedAt = submission.submittedAt,
                confirmedAt = submission.confirmedAt,
                confirmedById = submission.confirmedBy?.id,
                confirmedByName = submission.confirmedBy?.nickname,
                rejectionReason = submission.rejectionReason,
                rejectedById = submission.rejectedBy?.id,
                rejectedByName = submission.rejectedBy?.nickname,
                exchangePendingAt = submission.exchangePendingAt,
                exchangeRejectionReason = submission.exchangeRejectionReason,
                exchangeRejectedById = submission.exchangeRejectedBy?.id,
                exchangeRejectedByName = submission.exchangeRejectedBy?.nickname,
                entries = entries.map { LineupEntryResponse.from(it) },
            )
    }
}

/**
 * 라인업 엔트리 응답 DTO
 */
data class LineupEntryResponse(
    val id: Long,
    val playerId: Long,
    val playerName: String,
    val position: Position,
    val positionDisplayName: String,
    val battingOrder: Int?,
    val backNumber: Int?,
    val isStarter: Boolean,
) {
    companion object {
        fun from(entry: LineupEntry): LineupEntryResponse =
            LineupEntryResponse(
                id = entry.id,
                playerId = entry.player.id,
                playerName = entry.player.name,
                position = entry.position,
                positionDisplayName = entry.position.displayName,
                battingOrder = entry.battingOrder,
                backNumber = entry.backNumber,
                isStarter = entry.isStarter,
            )
    }
}

/**
 * 라인업 제출 요약 응답 DTO (목록 조회용)
 */
data class LineupSubmissionSummaryResponse(
    val id: Long,
    val gameId: Long,
    val teamId: Long,
    val teamName: String,
    val status: LineupSubmissionStatus,
    val statusDisplayName: String,
    val starterCount: Int,
    val submittedAt: Instant?,
) {
    companion object {
        fun from(
            submission: LineupSubmission,
            starterCount: Int,
        ): LineupSubmissionSummaryResponse =
            LineupSubmissionSummaryResponse(
                id = submission.id,
                gameId = submission.game.id,
                teamId = submission.team.id,
                teamName = submission.team.name,
                status = submission.status,
                statusDisplayName = submission.status.displayName,
                starterCount = starterCount,
                submittedAt = submission.submittedAt,
            )
    }
}
