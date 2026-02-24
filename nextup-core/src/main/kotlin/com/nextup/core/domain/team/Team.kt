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
    var name: String,
    @Column(length = 20)
    var abbreviation: String? = null,
    @Column(nullable = false, length = 100)
    var city: String,
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

    fun updateBasicInfo(
        name: String? = null,
        city: String? = null,
        abbreviation: String? = null,
    ) {
        name?.let { this.name = it }
        city?.let { this.city = it }
        if (abbreviation != null) {
            this.abbreviation = abbreviation
        }
    }

    /**
     * 팀이 추가 멤버를 수용할 수 있는지 확인합니다.
     *
     * 사회인 야구 팀의 일반적인 최대 인원은 30명입니다.
     *
     * @param currentMemberCount 현재 활성 멤버 수
     * @return 멤버 추가 가능 여부
     */
    fun canAcceptMember(currentMemberCount: Int): Boolean = currentMemberCount < MAX_MEMBER_COUNT

    /**
     * 팀 가입 자격을 검증합니다.
     *
     * 비활성화된 팀에는 가입할 수 없습니다.
     *
     * @throws IllegalStateException 팀이 비활성 상태인 경우
     */
    fun validateJoinEligibility() {
        check(isActive) {
            "비활성화된 팀에는 가입할 수 없습니다. (팀: $fullName)"
        }
    }

    companion object {
        /** 팀 최대 멤버 수 (사회인 야구 일반 기준) */
        const val MAX_MEMBER_COUNT = 30
    }
}
