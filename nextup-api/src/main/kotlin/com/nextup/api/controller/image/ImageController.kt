package com.nextup.api.controller.image

import com.nextup.api.dto.image.ImageUploadResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.EmptyImageFileException
import com.nextup.core.service.image.ImageUploadService
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 이미지 업로드 API Controller (일반 사용자용)
 *
 * - POST /api/v1/images/upload : 범용 이미지 업로드
 * - PUT /api/v1/teams/{teamId}/logo : 팀 로고 설정
 * - PUT /api/v1/players/me/profile-image : 선수 프로필 이미지 설정
 */
@RestController
@RequestMapping("/api/v1")
class ImageController(
    private val imageUploadService: ImageUploadService,
) {
    /**
     * 범용 이미지를 업로드합니다.
     *
     * POST /api/v1/images/upload
     */
    @PostMapping("/images/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam(required = false, defaultValue = "general") directory: String,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<ImageUploadResponse> {
        validateMultipartFile(file)
        val imageUrl =
            imageUploadService.uploadImage(
                directory = directory,
                originalFileName = file.originalFilename ?: "image.png",
                content = file.bytes,
                contentType = file.contentType ?: "image/png",
            )
        return ApiResponse.success(ImageUploadResponse(imageUrl))
    }

    /**
     * 팀 로고를 업로드합니다. 팀 OWNER 또는 MANAGER만 가능합니다.
     *
     * PUT /api/v1/teams/{teamId}/logo
     */
    @PutMapping("/teams/{teamId}/logo", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("@teamSecurity.isOwnerOrManager(#teamId, authentication.principal)")
    fun uploadTeamLogo(
        @PathVariable teamId: Long,
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<ImageUploadResponse> {
        validateMultipartFile(file)
        val imageUrl =
            imageUploadService.uploadTeamLogo(
                teamId = teamId,
                originalFileName = file.originalFilename ?: "logo.png",
                content = file.bytes,
                contentType = file.contentType ?: "image/png",
            )
        return ApiResponse.success(ImageUploadResponse(imageUrl))
    }

    /**
     * 선수 프로필 이미지를 업로드합니다. 인증된 사용자 본인의 프로필만 변경 가능합니다.
     *
     * PUT /api/v1/players/me/profile-image
     */
    @PutMapping("/players/me/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun uploadPlayerProfileImage(
        @RequestParam("file") file: MultipartFile,
        @AuthenticationPrincipal userId: Long,
    ): ApiResponse<ImageUploadResponse> {
        validateMultipartFile(file)
        val imageUrl =
            imageUploadService.uploadPlayerProfileImage(
                userId = userId,
                originalFileName = file.originalFilename ?: "profile.png",
                content = file.bytes,
                contentType = file.contentType ?: "image/png",
            )
        return ApiResponse.success(ImageUploadResponse(imageUrl))
    }

    private fun validateMultipartFile(file: MultipartFile) {
        if (file.isEmpty) {
            throw EmptyImageFileException()
        }
    }
}
