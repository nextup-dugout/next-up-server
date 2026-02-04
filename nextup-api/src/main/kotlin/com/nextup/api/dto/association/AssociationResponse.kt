package com.nextup.api.dto.association

import com.nextup.core.domain.association.Association
import java.time.Instant

/**
 * 협회 응답 DTO
 */
data class AssociationResponse(
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
) {
    companion object {
        fun from(association: Association): AssociationResponse =
            AssociationResponse(
                id = association.id,
                name = association.name,
                abbreviation = association.abbreviation,
                region = association.region,
                description = association.description,
                logoUrl = association.logoUrl,
                websiteUrl = association.websiteUrl,
                isActive = association.isActive,
                createdAt = association.createdAt,
                updatedAt = association.updatedAt,
            )
    }
}

/**
 * 협회 목록 응답 DTO (간략 정보)
 */
data class AssociationSummaryResponse(
    val id: Long,
    val name: String,
    val abbreviation: String?,
    val region: String?,
    val logoUrl: String?,
) {
    companion object {
        fun from(association: Association): AssociationSummaryResponse =
            AssociationSummaryResponse(
                id = association.id,
                name = association.name,
                abbreviation = association.abbreviation,
                region = association.region,
                logoUrl = association.logoUrl,
            )
    }
}
