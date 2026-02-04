package com.nextup.api.controller.lineup

import com.nextup.api.dto.common.ApiResponse
import com.nextup.api.dto.lineup.AddLineupEntryRequest
import com.nextup.api.dto.lineup.CreateLineupRequest
import com.nextup.api.dto.lineup.LineupEntryResponse
import com.nextup.api.dto.lineup.LineupSubmissionResponse
import com.nextup.api.dto.lineup.LineupSubmissionSummaryResponse
import com.nextup.api.dto.lineup.SetLineupEntriesRequest
import com.nextup.core.service.lineup.LineupService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 팀 감독용 라인업 컨트롤러
 *
 * 팀 관리자(감독)가 경기 전 라인업을 작성하고 제출하는 API를 제공합니다.
 * - 라인업 생성, 수정, 제출은 이 컨트롤러에서 처리
 * - 제출 후 확인/반려는 nextup-scorer 모듈에서 처리
 */
@RestController
@RequestMapping("/api/v1/lineups")
class LineupController(
    private val lineupService: LineupService,
) {
    /**
     * 라인업 제출을 생성합니다.
     */
    @PostMapping
    fun createLineup(
        @Valid @RequestBody request: CreateLineupRequest,
        @RequestHeader("X-User-Id") userId: Long,
    ): ResponseEntity<ApiResponse<LineupSubmissionResponse>> {
        val submission =
            lineupService.createLineupSubmission(
                gameId = request.gameId,
                teamId = request.teamId,
                submittedByUserId = userId,
            )
        val entries = lineupService.getLineupEntries(submission.id)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(LineupSubmissionResponse.from(submission, entries)))
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
     * 경기의 모든 라인업 제출을 조회합니다.
     */
    @GetMapping
    fun getLineupsByGame(
        @RequestParam gameId: Long,
    ): ResponseEntity<ApiResponse<List<LineupSubmissionSummaryResponse>>> {
        val submissions = lineupService.getLineupSubmissionsByGame(gameId)
        val responses =
            submissions.map { submission ->
                val entries = lineupService.getLineupEntries(submission.id)
                val starterCount = entries.count { it.isStarter }
                LineupSubmissionSummaryResponse.from(submission, starterCount)
            }

        return ResponseEntity.ok(ApiResponse.success(responses))
    }

    /**
     * 라인업 엔트리를 추가합니다.
     */
    @PostMapping("/{submissionId}/entries")
    fun addLineupEntry(
        @PathVariable submissionId: Long,
        @Valid @RequestBody request: AddLineupEntryRequest,
    ): ResponseEntity<ApiResponse<LineupEntryResponse>> {
        val entry =
            lineupService.addLineupEntry(
                submissionId = submissionId,
                playerId = request.playerId,
                position = request.position,
                battingOrder = request.battingOrder,
                backNumber = request.backNumber,
                isStarter = request.isStarter,
            )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(LineupEntryResponse.from(entry)))
    }

    /**
     * 라인업 엔트리를 일괄 설정합니다.
     */
    @PutMapping("/{submissionId}/entries")
    fun setLineupEntries(
        @PathVariable submissionId: Long,
        @Valid @RequestBody request: SetLineupEntriesRequest,
    ): ResponseEntity<ApiResponse<List<LineupEntryResponse>>> {
        val entries =
            lineupService.setLineupEntries(
                submissionId = submissionId,
                entries = request.entries.map { it.toInput() },
            )

        return ResponseEntity.ok(ApiResponse.success(entries.map { LineupEntryResponse.from(it) }))
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
     * 라인업을 기록원에게 제출합니다.
     */
    @PostMapping("/{submissionId}/submit")
    fun submitLineup(
        @PathVariable submissionId: Long,
    ): ResponseEntity<ApiResponse<LineupSubmissionResponse>> {
        val submission = lineupService.submitLineup(submissionId)
        val entries = lineupService.getLineupEntries(submissionId)

        return ResponseEntity.ok(ApiResponse.success(LineupSubmissionResponse.from(submission, entries)))
    }
}
