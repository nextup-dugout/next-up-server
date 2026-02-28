package com.nextup.core.domain.league

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.association.Association
import jakarta.persistence.*
import java.time.LocalDate

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
    @Column(name = "max_team_count")
    val maxTeamCount: Int? = null,
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

    /**
     * 리그가 추가 팀을 수용할 수 있는지 확인합니다.
     *
     * maxTeamCount가 설정되지 않은 경우 제한 없이 수용합니다.
     *
     * @param currentTeamCount 현재 등록된 팀 수
     * @return 팀 추가 가능 여부
     */
    fun canAcceptTeam(currentTeamCount: Int): Boolean {
        val limit = maxTeamCount ?: return true
        return currentTeamCount < limit
    }

    /**
     * 리그 등록 기간 내인지 확인합니다.
     *
     * @param registrationStartDate 등록 시작일
     * @param registrationEndDate 등록 종료일
     * @param today 기준 날짜 (기본값: 오늘)
     * @return 등록 기간 내 여부
     */
    fun isRegistrationOpen(
        registrationStartDate: LocalDate,
        registrationEndDate: LocalDate,
        today: LocalDate = LocalDate.now(),
    ): Boolean =
        isActive &&
            !today.isBefore(registrationStartDate) &&
            !today.isAfter(registrationEndDate)

    /**
     * 시즌 날짜 유효성을 검증합니다.
     *
     * @param startDate 시즌 시작일
     * @param endDate 시즌 종료일
     * @throws IllegalArgumentException 시작일이 종료일 이후이거나 같은 경우
     */
    fun validateSeasonDates(
        startDate: LocalDate,
        endDate: LocalDate,
    ) {
        require(startDate.isBefore(endDate)) {
            "시즌 시작일은 종료일보다 이전이어야 합니다. (시작일: $startDate, 종료일: $endDate)"
        }
    }
}
