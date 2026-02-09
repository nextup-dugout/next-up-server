package com.nextup.core.service.stadium.dto

/**
 * 구장 생성 요청 DTO
 */
data class CreateStadiumRequest(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val capacity: Int? = null,
    val facilities: String? = null,
    val contactInfo: String? = null,
    val imageUrls: String? = null,
)
