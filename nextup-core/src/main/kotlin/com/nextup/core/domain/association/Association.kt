package com.nextup.core.domain.association

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*

/**
 * 사회인 야구 협회 엔티티
 *
 * 사회인 야구는 여러 협회(예: 서울시 야구협회, 경기도 야구협회)가 존재하며,
 * 각 협회는 독립적인 리그 체계를 운영합니다.
 */
@Entity
@Table(
    name = "associations",
    indexes = [
        Index(name = "idx_associations_region", columnList = "region"),
        Index(name = "idx_associations_is_active", columnList = "is_active"),
    ],
)
class Association(
    @Column(nullable = false, length = 100)
    val name: String,
    @Column(length = 20)
    val abbreviation: String? = null,
    @Column(length = 50)
    val region: String? = null,
    @Column(length = 500)
    var description: String? = null,
    @Column(length = 255)
    var logoUrl: String? = null,
    @Column(length = 255)
    var websiteUrl: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    fun deactivate() {
        this.isActive = false
    }

    fun activate() {
        this.isActive = true
    }

    fun updateInfo(
        description: String? = this.description,
        logoUrl: String? = this.logoUrl,
        websiteUrl: String? = this.websiteUrl,
    ) {
        this.description = description
        this.logoUrl = logoUrl
        this.websiteUrl = websiteUrl
    }
}
