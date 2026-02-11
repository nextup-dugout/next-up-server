package com.nextup.backoffice.dto.stadium

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive

/**
 * 구장 생성 요청 DTO (백오피스용)
 */
data class CreateStadiumRequest(
    @field:NotBlank(message = "Stadium name is required")
    val name: String,
    @field:NotBlank(message = "Address is required")
    val address: String,
    @field:Min(value = -90, message = "Latitude must be between -90 and 90")
    @field:Max(value = 90, message = "Latitude must be between -90 and 90")
    val latitude: Double,
    @field:Min(value = -180, message = "Longitude must be between -180 and 180")
    @field:Max(value = 180, message = "Longitude must be between -180 and 180")
    val longitude: Double,
    @field:Positive(message = "Capacity must be positive")
    val capacity: Int? = null,
    val facilities: String? = null,
    val contactInfo: String? = null,
    val imageUrls: String? = null,
)
