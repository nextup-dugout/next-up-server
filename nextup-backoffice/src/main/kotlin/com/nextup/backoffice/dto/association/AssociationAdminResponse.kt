package com.nextup.backoffice.dto.association

import java.time.Instant

/**
 * 협회 관리자 응답 DTO
 *
 * 일반 사용자 응답과 동일하지만 backoffice 모듈에 독립적으로 존재
 * 변환 로직은 AssociationExtensions.kt의 Extension Function을 사용합니다.
 */
data class AssociationAdminResponse(
    val id: Long,
    val name: String,
    val abbreviation: String?,
    val region: String?,
    val description: String?,
    val logoUrl: String?,
    val websiteUrl: String?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
