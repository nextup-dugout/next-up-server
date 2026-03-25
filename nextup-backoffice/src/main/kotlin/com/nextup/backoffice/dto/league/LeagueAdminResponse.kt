package com.nextup.backoffice.dto.league

import java.time.Instant

/**
 * 리그 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 * 변환 로직은 LeagueExtensions.kt의 Extension Function을 사용합니다.
 */
data class LeagueAdminResponse(
    val id: Long,
    val associationId: Long,
    val associationName: String,
    val name: String,
    val abbreviation: String?,
    val foundedYear: Int,
    val divisionLevel: Int?,
    val description: String?,
    val logoUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
