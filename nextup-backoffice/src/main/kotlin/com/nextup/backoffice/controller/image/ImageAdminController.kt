package com.nextup.backoffice.controller.image

import com.nextup.backoffice.dto.image.ImageUploadAdminResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.EmptyImageFileException
import com.nextup.core.service.image.ImageUploadService
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 이미지 관리 API Controller (관리자용)
 *
 * - PUT /api/backoffice/leagues/{leagueId}/logo : 리그 로고 설정
 * - PUT /api/backoffice/associations/{associationId}/logo : 협회 로고 설정
 */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice")
class ImageAdminController(
    private val imageUploadService: ImageUploadService,
) {
    /**
     * 리그 로고를 업로드합니다.
     *
     * PUT /api/backoffice/leagues/{leagueId}/logo
     */
    @PutMapping("/leagues/{leagueId}/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadLeagueLogo(
        @PathVariable leagueId: Long,
        @RequestParam("file") file: MultipartFile,
    ): ApiResponse<ImageUploadAdminResponse> {
        validateMultipartFile(file)
        val imageUrl =
            imageUploadService.uploadLeagueLogo(
                leagueId = leagueId,
                originalFileName = file.originalFilename ?: "logo.png",
                content = file.bytes,
                contentType = file.contentType ?: "image/png",
            )
        return ApiResponse.success(ImageUploadAdminResponse(imageUrl))
    }

    /**
     * 협회 로고를 업로드합니다.
     *
     * PUT /api/backoffice/associations/{associationId}/logo
     */
    @PutMapping("/associations/{associationId}/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun uploadAssociationLogo(
        @PathVariable associationId: Long,
        @RequestParam("file") file: MultipartFile,
    ): ApiResponse<ImageUploadAdminResponse> {
        validateMultipartFile(file)
        val imageUrl =
            imageUploadService.uploadAssociationLogo(
                associationId = associationId,
                originalFileName = file.originalFilename ?: "logo.png",
                content = file.bytes,
                contentType = file.contentType ?: "image/png",
            )
        return ApiResponse.success(ImageUploadAdminResponse(imageUrl))
    }

    private fun validateMultipartFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw EmptyImageFileException()
        }
    }
}
