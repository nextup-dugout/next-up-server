package com.nextup.core.service.stadium.dto

/**
 * 구장 검색 요청 DTO
 */
data class SearchStadiumRequest(
    val latitude: Double,
    val longitude: Double,
    val radiusKm: Double,
)
