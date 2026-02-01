package com.nextup.backoffice.dto.association

import com.nextup.core.domain.association.Association
import java.time.Instant

/**
 * 협회 관리자 응답 DTO
 *
 * 일반 사용자 응답과 동일하지만 backoffice 모듈에 독립적으로 존재
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
    val updatedAt: Instant
) {
    companion object {
        fun from(association: Association): AssociationAdminResponse {
            return AssociationAdminResponse(
                id = association.id,
                name = association.name,
                abbreviation = association.abbreviation,
                region = association.region,
                description = association.description,
                logoUrl = association.logoUrl,
                websiteUrl = association.websiteUrl,
                isActive = association.isActive,
                createdAt = association.createdAt,
                updatedAt = association.updatedAt
            )
        }
    }
}
