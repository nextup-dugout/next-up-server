package com.nextup.api.dto.lineup

import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.domain.player.Position
import java.time.Instant

/**
 * 라인업 제출 응답 DTO
 * NOTE: nextup-scorer의 LineupSubmissionResponse와 동일한 구조 유지
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

/**
 * 라인업 확정 상태 상세 응답 DTO
 *
 * 확정 여부뿐 아니라 선수 상세 정보(이름, 포지션, 타순)를 포함합니다.
 */
data class LineupDetailResponse(
    val id: Long,
    val gameId: Long,
    val teamId: Long,
    val teamName: String,
    val status: LineupSubmissionStatus,
    val statusDisplayName: String,
    val isConfirmed: Boolean,
    val starterCount: Int,
    val substituteCount: Int,
    val submittedAt: Instant?,
    val confirmedAt: Instant?,
    val entries: List<LineupEntryResponse>,
) {
    companion object {
        fun from(
            submission: LineupSubmission,
            entries: List<LineupEntry>,
        ): LineupDetailResponse {
            val starters = entries.count { it.isStarter }
            val substitutes = entries.size - starters

            return LineupDetailResponse(
                id = submission.id,
                gameId = submission.game.id,
                teamId = submission.team.id,
                teamName = submission.team.name,
                status = submission.status,
                statusDisplayName = submission.status.displayName,
                isConfirmed =
                    submission.status == LineupSubmissionStatus.CONFIRMED ||
                        submission.status == LineupSubmissionStatus.EXCHANGED,
                starterCount = starters,
                substituteCount = substitutes,
                submittedAt = submission.submittedAt,
                confirmedAt = submission.confirmedAt,
                entries = entries.map { LineupEntryResponse.from(it) },
            )
        }
    }
}
