package com.nextup.core.domain.league

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.association.Association
import jakarta.persistence.*

/**
 * 리그 엔티티
 *
 * 협회(Association) 소속의 리그를 나타냅니다.
 * 예: 서울시야구협회 1부 리그, 2부 리그 등
 */
@Entity
@Table(
    name = "leagues",
    indexes = [
        Index(name = "idx_leagues_association", columnList = "association_id"),
        Index(name = "idx_leagues_is_active", columnList = "is_active"),
    ],
)
class League(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "association_id", nullable = false)
    val association: Association,
    @Column(nullable = false, length = 100)
    val name: String,
    @Column(length = 20)
    val abbreviation: String? = null,
    @Column(nullable = false)
    val foundedYear: Int,
    @Column(name = "division_level")
    val divisionLevel: Int? = null,
    @Column(length = 500)
    var description: String? = null,
    @Column(length = 255)
    var logoUrl: String? = null,
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
    ) {
        this.description = description
        this.logoUrl = logoUrl
    }
}
