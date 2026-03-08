package com.nextup.core.domain.player

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 선수 엔티티
 *
 * 야구 선수의 기본 정보를 관리합니다.
 * 팀 소속 이력은 PlayerTeamHistory를 통해 관리됩니다.
 */
@Entity
@Table(
    name = "players",
    indexes = [
        Index(name = "idx_players_name", columnList = "name"),
        Index(name = "idx_players_birth_date", columnList = "birth_date"),
    ],
)
class Player(
    @Column(nullable = false, length = 50)
    val name: String,
    @Column(name = "birth_date")
    val birthDate: LocalDate? = null,
    @Column(length = 50)
    val birthPlace: String? = null,
    @Column(length = 50)
    val nationality: String? = null,
    @Column
    var height: Int? = null,
    @Column
    var weight: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "throwing_hand", length = 10)
    var throwingHand: ThrowingHand? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "batting_hand", length = 10)
    var battingHand: BattingHand? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_position", nullable = false, length = 30)
    var primaryPosition: Position,
    @Column(name = "debut_year")
    var debutYear: Int? = null,
    @Column(name = "retirement_year")
    var retirementYear: Int? = null,
    @Column(name = "profile_image_url", length = 255)
    var profileImageUrl: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    val isActive: Boolean
        get() = retirementYear == null

    fun retire(year: Int) {
        require(retirementYear == null) { "이미 은퇴한 선수입니다." }
        require(year >= (debutYear ?: 0)) { "은퇴 연도는 데뷔 연도 이후여야 합니다." }
        this.retirementYear = year
    }

    fun updatePhysicalInfo(
        height: Int? = this.height,
        weight: Int? = this.weight,
    ) {
        this.height = height
        this.weight = weight
    }

    fun updateProfile(profileImageUrl: String?) {
        this.profileImageUrl = profileImageUrl
    }

    /**
     * 선수 프로필 정보를 수정합니다.
     * (포지션, 투타, 신체정보)
     */
    fun updatePlayerProfile(
        primaryPosition: Position = this.primaryPosition,
        throwingHand: ThrowingHand? = this.throwingHand,
        battingHand: BattingHand? = this.battingHand,
        height: Int? = this.height,
        weight: Int? = this.weight,
    ) {
        this.primaryPosition = primaryPosition
        this.throwingHand = throwingHand
        this.battingHand = battingHand
        this.height = height
        this.weight = weight
    }

    /**
     * 선수의 나이를 계산합니다.
     */
    fun calculateAge(baseDate: LocalDate = LocalDate.now()): Int? =
        birthDate?.let {
            var age = baseDate.year - it.year
            if (baseDate.dayOfYear < it.dayOfYear) {
                age--
            }
            age
        }
}
