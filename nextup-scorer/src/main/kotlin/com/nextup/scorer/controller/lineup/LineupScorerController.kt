package com.nextup.scorer.controller.lineup

import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.lineup.LineupService
import com.nextup.scorer.dto.lineup.LineupEntryResponse
import com.nextup.scorer.dto.lineup.LineupSubmissionResponse
import com.nextup.scorer.dto.lineup.RejectLineupRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 라인업 기록원 컨트롤러
 *
 * 기록원이 경기 전 제출된 라인업을 확인하고 승인/반려하는 API를 제공합니다.
 * 라인업 작성 및 제출은 nextup-api 모듈의 LineupController에서 처리합니다.
 */
@RestController
@RequestMapping("/api/v1/scorer/lineups")
class LineupScorerController(
    private val lineupService: LineupService,
) {
    /**
     * 경기의 제출된 라인업을 조회합니다. (기록원 확인용)
     */
    @GetMapping("/submitted")
    fun getSubmittedLineups(
        @RequestParam gameId: Long,
    ): ResponseEntity<ApiResponse<List<LineupSubmissionResponse>>> {
        val submissions = lineupService.getSubmittedLineupsByGame(gameId)
        val responses =
            submissions.map { submission ->
                val entries = lineupService.getLineupEntries(submission.id)
                LineupSubmissionResponse.from(submission, entries)
            }

        return ResponseEntity.ok(ApiResponse.success(responses))
    }

    /**
     * 라인업 제출을 조회합니다.
     */
    @GetMapping("/{submissionId}")
    fun getLineup(
        @PathVariable submissionId: Long,
    ): ResponseEntity<ApiResponse<LineupSubmissionResponse>> {
        val submission = lineupService.getLineupSubmission(submissionId)
        val entries = lineupService.getLineupEntries(submissionId)

        return ResponseEntity.ok(ApiResponse.success(LineupSubmissionResponse.from(submission, entries)))
    }

    /**
     * 라인업 엔트리를 조회합니다.
     */
    @GetMapping("/{submissionId}/entries")
    fun getLineupEntries(
        @PathVariable submissionId: Long,
    ): ResponseEntity<ApiResponse<List<LineupEntryResponse>>> {
        val entries = lineupService.getLineupEntries(submissionId)

        return ResponseEntity.ok(ApiResponse.success(entries.map { LineupEntryResponse.from(it) }))
    }

    /**
     * 상대팀 라인업을 조회합니다. (교환 완료 후에만 조회 가능)
     *
     * 양 팀 모두 라인업을 제출하여 EXCHANGED 상태가 된 경우에만 상대팀 라인업을 반환합니다.
     * 교환이 완료되지 않은 경우 403 응답을 반환합니다.
     *
     * @param gameId 경기 ID
     * @param myTeamId 요청하는 팀 ID (본인 팀)
     */
    @GetMapping("/games/{gameId}/opponent-lineup")
    fun getOpponentLineup(
        @PathVariable gameId: Long,
        @RequestParam myTeamId: Long,
    ): ResponseEntity<ApiResponse<LineupSubmissionResponse>> {
        val opponentSubmission = lineupService.getOpponentLineup(gameId, myTeamId)
        val entries = lineupService.getLineupEntries(opponentSubmission.id)

        return ResponseEntity.ok(
            ApiResponse.success(LineupSubmissionResponse.from(opponentSubmission, entries)),
        )
    }

    /**
     * 기록원이 라인업을 확인합니다.
     */
    @PostMapping("/{submissionId}/confirm")
    fun confirmLineup(
        @PathVariable submissionId: Long,
        @AuthenticationPrincipal scorerUserId: Long,
    ): ResponseEntity<ApiResponse<LineupSubmissionResponse>> {
        val submission = lineupService.confirmLineup(submissionId, scorerUserId)
        val entries = lineupService.getLineupEntries(submissionId)

        return ResponseEntity.ok(ApiResponse.success(LineupSubmissionResponse.from(submission, entries)))
    }

    /**
     * 기록원이 라인업을 반려합니다.
     */
    @PostMapping("/{submissionId}/reject")
    fun rejectLineup(
        @PathVariable submissionId: Long,
        @AuthenticationPrincipal scorerUserId: Long,
        @Valid @RequestBody request: RejectLineupRequest,
    ): ResponseEntity<ApiResponse<LineupSubmissionResponse>> {
        val submission = lineupService.rejectLineup(submissionId, scorerUserId, request.reason)
        val entries = lineupService.getLineupEntries(submissionId)

        return ResponseEntity.ok(ApiResponse.success(LineupSubmissionResponse.from(submission, entries)))
    }
}
