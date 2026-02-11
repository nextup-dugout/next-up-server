package com.nextup.backoffice.controller.discipline

import com.nextup.backoffice.dto.common.ApiResponse
import com.nextup.backoffice.dto.discipline.DisciplineAdminResponse
import com.nextup.backoffice.dto.discipline.IssueDisciplineRequest
import com.nextup.core.domain.discipline.DisciplineStatus
import com.nextup.core.domain.discipline.DisciplineType
import com.nextup.core.service.discipline.DisciplineService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 징계 관리 API Controller (관리자용)
 *
 * 전체 권한: 생성, 조회, 취소
 */
@RestController
@RequestMapping("/api/backoffice/disciplines")
class DisciplineAdminController(
    private val disciplineService: DisciplineService,
) {
    /**
     * 모든 징계 목록을 조회합니다.
     */
    @GetMapping
    fun getAllDisciplines(
        @RequestParam(required = false) playerId: Long?,
        @RequestParam(required = false) competitionId: Long?,
        @RequestParam(required = false) status: DisciplineStatus?,
    ): ApiResponse<List<DisciplineAdminResponse>> {
        val disciplines =
            when {
                playerId != null && competitionId != null ->
                    disciplineService.getDisciplinesByPlayerAndCompetition(playerId, competitionId)

                playerId != null -> disciplineService.getDisciplinesByPlayer(playerId)
                competitionId != null -> disciplineService.getDisciplinesByCompetition(competitionId)
                status != null -> disciplineService.getDisciplinesByStatus(status)
                else -> disciplineService.getAll()
            }

        return ApiResponse.success(
            disciplines.map { DisciplineAdminResponse.from(it) }
        )
    }

    /**
     * 징계 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getDiscipline(
        @PathVariable id: Long,
    ): ApiResponse<DisciplineAdminResponse> {
        val discipline = disciplineService.getById(id)
        return ApiResponse.success(DisciplineAdminResponse.from(discipline))
    }

    /**
     * 선수의 활성 징계를 조회합니다.
     */
    @GetMapping("/active")
    fun getActiveDisciplines(
        @RequestParam playerId: Long,
        @RequestParam competitionId: Long,
    ): ApiResponse<List<DisciplineAdminResponse>> {
        val disciplines =
            disciplineService.getActiveDisciplines(playerId, competitionId)

        return ApiResponse.success(
            disciplines.map { DisciplineAdminResponse.from(it) }
        )
    }

    /**
     * 선수가 출장 가능한지 확인합니다.
     */
    @GetMapping("/eligibility")
    fun checkPlayerEligibility(
        @RequestParam playerId: Long,
        @RequestParam competitionId: Long,
    ): ApiResponse<Map<String, Boolean>> {
        val canPlay = disciplineService.canPlayerPlay(playerId, competitionId)
        return ApiResponse.success(mapOf("canPlay" to canPlay))
    }

    /**
     * 징계를 발급합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun issueDiscipline(
        @Valid @RequestBody request: IssueDisciplineRequest,
    ): ApiResponse<DisciplineAdminResponse> {
        val discipline =
            when (request.type) {
                DisciplineType.WARNING ->
                    disciplineService.issueWarning(
                        playerId = request.playerId,
                        competitionId = request.competitionId,
                        reason = request.reason,
                        issuedBy = request.issuedBy,
                        expiresAt = request.expiresAt,
                    )

                DisciplineType.SUSPENSION -> {
                    require(request.suspensionGames != null && request.suspensionGames > 0) {
                        "출장 정지 경기 수는 1 이상이어야 합니다"
                    }
                    disciplineService.issueSuspension(
                        playerId = request.playerId,
                        competitionId = request.competitionId,
                        reason = request.reason,
                        suspensionGames = request.suspensionGames,
                        issuedBy = request.issuedBy,
                    )
                }

                DisciplineType.BAN ->
                    disciplineService.issueBan(
                        playerId = request.playerId,
                        competitionId = request.competitionId,
                        reason = request.reason,
                        issuedBy = request.issuedBy,
                    )
            }

        return ApiResponse.success(DisciplineAdminResponse.from(discipline))
    }

    /**
     * 징계를 취소합니다.
     */
    @PutMapping("/{id}/cancel")
    fun cancelDiscipline(
        @PathVariable id: Long,
    ): ApiResponse<DisciplineAdminResponse> {
        val discipline = disciplineService.cancelDiscipline(id)
        return ApiResponse.success(DisciplineAdminResponse.from(discipline))
    }

    /**
     * 출장 정지 징계의 경기를 소화합니다.
     */
    @PutMapping("/{id}/increment-served")
    fun incrementServedGames(
        @PathVariable id: Long,
    ): ApiResponse<DisciplineAdminResponse> {
        val discipline = disciplineService.incrementServedGames(id)
        return ApiResponse.success(DisciplineAdminResponse.from(discipline))
    }
}
