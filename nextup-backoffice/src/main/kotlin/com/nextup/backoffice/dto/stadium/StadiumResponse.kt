package com.nextup.backoffice.dto.stadium

import java.time.Instant

/**
 * 구장 응답 DTO (백오피스용)
 *
 * 변환 로직은 StadiumExtensions.kt의 Extension Function을 사용합니다.
 */
data class StadiumResponse(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val capacity: Int?,
    val facilities: String?,
    val contactInfo: String?,
    val imageUrls: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
