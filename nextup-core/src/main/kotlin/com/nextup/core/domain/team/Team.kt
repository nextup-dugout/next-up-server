package com.nextup.core.domain.team

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.league.League
import jakarta.persistence.*

/**
 * 팀 엔티티
 *
 * 리그(League)에 소속된 야구 팀을 나타냅니다.
 */
@Entity
@Table(
    name = "teams",
    indexes = [
        Index(name = "idx_teams_league_id", columnList = "league_id"),
        Index(name = "idx_teams_is_active", columnList = "is_active"),
    ],
)
class Team(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    val league: League,
    @Column(nullable = false, length = 100)
    val name: String,
    @Column(length = 20)
    val abbreviation: String? = null,
    @Column(nullable = false, length = 100)
    val city: String,
    @Column(nullable = false)
    val foundedYear: Int,
    @Column(length = 255)
    var logoUrl: String? = null,
    @Column(length = 50)
    var primaryColor: String? = null,
    @Column(length = 50)
    var secondaryColor: String? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    val fullName: String
        get() = "$city $name"

    fun deactivate() {
        this.isActive = false
    }

    fun activate() {
        this.isActive = true
    }

    fun updateInfo(
        logoUrl: String? = this.logoUrl,
        primaryColor: String? = this.primaryColor,
        secondaryColor: String? = this.secondaryColor,
    ) {
        this.logoUrl = logoUrl
        this.primaryColor = primaryColor
        this.secondaryColor = secondaryColor
    }
}
