package com.nextup.backoffice.dto.stadium

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive

/**
 * 구장 수정 요청 DTO (백오피스용)
 */
data class UpdateStadiumRequest(
    val address: String? = null,
    @field:Min(value = -90, message = "Latitude must be between -90 and 90")
    @field:Max(value = 90, message = "Latitude must be between -90 and 90")
    val latitude: Double? = null,
    @field:Min(value = -180, message = "Longitude must be between -180 and 180")
    @field:Max(value = 180, message = "Longitude must be between -180 and 180")
    val longitude: Double? = null,
    @field:Positive(message = "Capacity must be positive")
    val capacity: Int? = null,
    val facilities: String? = null,
    val contactInfo: String? = null,
    val imageUrls: String? = null,
)
