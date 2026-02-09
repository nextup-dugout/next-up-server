package com.nextup.api.controller.attendance

import com.nextup.api.dto.attendance.ActivityScoreResponse
import com.nextup.api.dto.attendance.UpdateActivityScoreRequest
import com.nextup.api.dto.attendance.toResponse
import com.nextup.api.dto.common.ApiResponse
import com.nextup.core.service.attendance.ActivityService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 팀 활동 점수 관리 컨트롤러
 *
 * 팀원의 활동 점수(경기 참여율, 연습 참석률, 기여도)를 관리합니다.
 */
@RestController
@RequestMapping("/api/v1/teams/{teamId}/members")
class TeamActivityController(
    private val activityService: ActivityService,
) {
    /**
     * 팀원의 활동 점수를 조회합니다.
     */
    @GetMapping("/{memberId}/activity")
    fun getActivity(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
    ): ApiResponse<ActivityScoreResponse> {
        val activityScore = activityService.getActivityScore(teamId, memberId)
        return ApiResponse.success(activityScore.toResponse())
    }

    /**
     * 팀의 모든 활동 점수를 조회합니다.
     */
    @GetMapping("/activity")
    fun listActivities(
        @PathVariable teamId: Long,
    ): ApiResponse<List<ActivityScoreResponse>> {
        val activityScores = activityService.listActivityScores(teamId)
        return ApiResponse.success(activityScores.map { it.toResponse() })
    }

    /**
     * 경기 참여율을 업데이트합니다.
     */
    @PutMapping("/{memberId}/activity/game-participation")
    fun updateGameParticipation(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @RequestBody @Valid request: UpdateActivityScoreRequest,
    ): ApiResponse<ActivityScoreResponse> {
        val activityScore =
            activityService.updateGameParticipationRate(
                teamId = teamId,
                memberId = memberId,
                rate = request.score,
            )
        return ApiResponse.success(activityScore.toResponse())
    }

    /**
     * 연습 참석률을 업데이트합니다.
     */
    @PutMapping("/{memberId}/activity/practice-attendance")
    fun updatePracticeAttendance(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @RequestBody @Valid request: UpdateActivityScoreRequest,
    ): ApiResponse<ActivityScoreResponse> {
        val activityScore =
            activityService.updatePracticeAttendanceRate(
                teamId = teamId,
                memberId = memberId,
                rate = request.score,
            )
        return ApiResponse.success(activityScore.toResponse())
    }

    /**
     * 기여도 점수를 업데이트합니다.
     */
    @PutMapping("/{memberId}/activity/contribution")
    fun updateContribution(
        @PathVariable teamId: Long,
        @PathVariable memberId: Long,
        @RequestBody @Valid request: UpdateActivityScoreRequest,
    ): ApiResponse<ActivityScoreResponse> {
        val activityScore =
            activityService.updateContributionScore(
                teamId = teamId,
                memberId = memberId,
                score = request.score,
            )
        return ApiResponse.success(activityScore.toResponse())
    }
}
