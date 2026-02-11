package com.nextup.backoffice.dto.stadium

import com.nextup.core.domain.stadium.Stadium
import java.time.Instant

/**
 * 구장 응답 DTO (백오피스용)
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
) {
    companion object {
        fun from(stadium: Stadium): StadiumResponse =
            StadiumResponse(
                id = stadium.id,
                name = stadium.name,
                address = stadium.address,
                latitude = stadium.latitude,
                longitude = stadium.longitude,
                capacity = stadium.capacity,
                facilities = stadium.facilities,
                contactInfo = stadium.contactInfo,
                imageUrls = stadium.imageUrls,
                isActive = stadium.isActive,
                createdAt = stadium.createdAt,
                updatedAt = stadium.updatedAt,
            )
    }
}
