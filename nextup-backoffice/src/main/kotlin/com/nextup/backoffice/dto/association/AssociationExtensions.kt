package com.nextup.backoffice.dto.association

import com.nextup.core.domain.association.Association

/**
 * Association Entity를 AssociationAdminResponse DTO로 변환하는 Extension Function
 */
fun Association.toAdminResponse(): AssociationAdminResponse =
    AssociationAdminResponse(
        id = this.id,
        name = this.name,
        abbreviation = this.abbreviation,
        region = this.region,
        description = this.description,
        logoUrl = this.logoUrl,
        websiteUrl = this.websiteUrl,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
