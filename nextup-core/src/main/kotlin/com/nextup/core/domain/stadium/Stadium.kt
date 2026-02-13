package com.nextup.core.domain.stadium

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*

/**
 * 구장 엔티티
 *
 * 야구장 정보를 관리하며, PostGIS를 활용한 위치 기반 검색을 지원합니다.
 */
@Entity
@Table(
    name = "stadiums",
    indexes = [
        Index(name = "idx_stadiums_is_active", columnList = "is_active"),
        Index(name = "idx_stadiums_location", columnList = "latitude,longitude"),
    ],
)
class Stadium private constructor(
    @Column(nullable = false, length = 100)
    val name: String,
    @Column(nullable = false, length = 255)
    var address: String,
    @Column(nullable = false)
    var latitude: Double,
    @Column(nullable = false)
    var longitude: Double,
    @Column
    var capacity: Int? = null,
    @Column(length = 500)
    var facilities: String? = null,
    @Column(length = 255)
    var contactInfo: String? = null,
    @Column(length = 1000)
    var imageUrls: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        fun create(
            name: String,
            address: String,
            latitude: Double,
            longitude: Double,
            capacity: Int? = null,
            facilities: String? = null,
            contactInfo: String? = null,
            imageUrls: String? = null,
        ): Stadium {
            require(name.isNotBlank()) { "Stadium name cannot be blank" }
            require(address.isNotBlank()) { "Stadium address cannot be blank" }
            require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90" }
            require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180" }
            capacity?.let { require(it > 0) { "Capacity must be positive" } }

            return Stadium(
                name = name,
                address = address,
                latitude = latitude,
                longitude = longitude,
                capacity = capacity,
                facilities = facilities,
                contactInfo = contactInfo,
                imageUrls = imageUrls,
            )
        }
    }

    fun update(
        address: String? = this.address,
        latitude: Double? = this.latitude,
        longitude: Double? = this.longitude,
        capacity: Int? = this.capacity,
        facilities: String? = this.facilities,
        contactInfo: String? = this.contactInfo,
        imageUrls: String? = this.imageUrls,
    ) {
        address?.let {
            require(it.isNotBlank()) { "Address cannot be blank" }
            this.address = it
        }
        latitude?.let {
            require(it in -90.0..90.0) { "Latitude must be between -90 and 90" }
            this.latitude = it
        }
        longitude?.let {
            require(it in -180.0..180.0) { "Longitude must be between -180 and 180" }
            this.longitude = it
        }
        capacity?.let {
            require(it > 0) { "Capacity must be positive" }
            this.capacity = it
        }
        this.facilities = facilities
        this.contactInfo = contactInfo
        this.imageUrls = imageUrls
    }

    fun deactivate() {
        this.isActive = false
    }

    fun activate() {
        this.isActive = true
    }
}
