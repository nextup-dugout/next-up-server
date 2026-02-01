package com.nextup.api.controller.association

import com.nextup.api.dto.association.AssociationResponse
import com.nextup.api.dto.association.AssociationSummaryResponse
import com.nextup.api.dto.common.ApiResponse
import com.nextup.infrastructure.service.association.AssociationService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * 협회 조회 API Controller (일반 사용자용)
 */
@RestController
@RequestMapping("/api/associations")
class AssociationController(
    private val associationService: AssociationService
) {

    /**
     * 활성화된 협회 목록을 조회합니다.
     */
    @GetMapping
    fun getAssociations(
        @RequestParam(required = false) region: String?
    ): ApiResponse<List<AssociationSummaryResponse>> {
        val associations = if (region != null) {
            associationService.getActiveByRegion(region)
        } else {
            associationService.getAllActive()
        }

        return ApiResponse.success(
            associations.map { AssociationSummaryResponse.from(it) }
        )
    }

    /**
     * 협회 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getAssociation(
        @PathVariable id: Long
    ): ApiResponse<AssociationResponse> {
        val association = associationService.getById(id)
        return ApiResponse.success(AssociationResponse.from(association))
    }
}
