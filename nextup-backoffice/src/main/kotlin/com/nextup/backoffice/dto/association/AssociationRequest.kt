package com.nextup.backoffice.dto.association

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * 협회 생성 요청 DTO
 */
data class CreateAssociationRequest(
    @field:NotBlank(message = "협회 이름은 필수입니다")
    @field:Size(max = 100, message = "협회 이름은 100자를 초과할 수 없습니다")
    val name: String,
    @field:Size(max = 20, message = "약어는 20자를 초과할 수 없습니다")
    val abbreviation: String? = null,
    @field:Size(max = 50, message = "지역은 50자를 초과할 수 없습니다")
    val region: String? = null,
    @field:Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    val description: String? = null,
    @field:Size(max = 255, message = "로고 URL은 255자를 초과할 수 없습니다")
    val logoUrl: String? = null,
    @field:Size(max = 255, message = "웹사이트 URL은 255자를 초과할 수 없습니다")
    val websiteUrl: String? = null,
)

/**
 * 협회 수정 요청 DTO
 */
data class UpdateAssociationRequest(
    @field:Size(max = 500, message = "설명은 500자를 초과할 수 없습니다")
    val description: String? = null,
    @field:Size(max = 255, message = "로고 URL은 255자를 초과할 수 없습니다")
    val logoUrl: String? = null,
    @field:Size(max = 255, message = "웹사이트 URL은 255자를 초과할 수 없습니다")
    val websiteUrl: String? = null,
)
