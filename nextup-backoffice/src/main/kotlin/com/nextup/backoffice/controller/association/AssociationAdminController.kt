package com.nextup.backoffice.controller.association

import com.nextup.backoffice.dto.association.AssociationAdminResponse
import com.nextup.backoffice.dto.association.CreateAssociationRequest
import com.nextup.backoffice.dto.association.UpdateAssociationRequest
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.association.AssociationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/**
 * 협회 관리 API Controller (관리자용)
 */
@RestController
@RequestMapping("/api/backoffice/associations")
class AssociationAdminController(
    private val associationService: AssociationService,
) {
    /**
     * 모든 협회 목록을 조회합니다 (비활성화 포함).
     */
    @GetMapping
    fun getAllAssociations(): ApiResponse<List<AssociationAdminResponse>> {
        val associations = associationService.getAll()
        return ApiResponse.success(
            associations.map { AssociationAdminResponse.from(it) },
        )
    }

    /**
     * 협회 상세 정보를 조회합니다.
     */
    @GetMapping("/{id}")
    fun getAssociation(
        @PathVariable id: Long,
    ): ApiResponse<AssociationAdminResponse> {
        val association = associationService.getById(id)
        return ApiResponse.success(AssociationAdminResponse.from(association))
    }

    /**
     * 협회를 생성합니다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createAssociation(
        @Valid @RequestBody request: CreateAssociationRequest,
    ): ApiResponse<AssociationAdminResponse> {
        val association =
            associationService.create(
                name = request.name,
                abbreviation = request.abbreviation,
                region = request.region,
                description = request.description,
                logoUrl = request.logoUrl,
                websiteUrl = request.websiteUrl,
            )
        return ApiResponse.success(AssociationAdminResponse.from(association))
    }

    /**
     * 협회 정보를 수정합니다.
     */
    @PutMapping("/{id}")
    fun updateAssociation(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAssociationRequest,
    ): ApiResponse<AssociationAdminResponse> {
        val association =
            associationService.update(
                id = id,
                description = request.description,
                logoUrl = request.logoUrl,
                websiteUrl = request.websiteUrl,
            )
        return ApiResponse.success(AssociationAdminResponse.from(association))
    }

    /**
     * 협회를 비활성화합니다.
     */
    @DeleteMapping("/{id}")
    fun deactivateAssociation(
        @PathVariable id: Long,
    ): ApiResponse<AssociationAdminResponse> {
        val association = associationService.deactivate(id)
        return ApiResponse.success(AssociationAdminResponse.from(association))
    }

    /**
     * 협회를 활성화합니다.
     */
    @PostMapping("/{id}/activate")
    fun activateAssociation(
        @PathVariable id: Long,
    ): ApiResponse<AssociationAdminResponse> {
        val association = associationService.activate(id)
        return ApiResponse.success(AssociationAdminResponse.from(association))
    }
}
