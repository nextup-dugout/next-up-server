package com.nextup.core.domain.competition

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.league.League
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 대회 엔티티
 *
 * 리그(League)에서 개최하는 시즌별 대회를 나타냅니다.
 * 예: 2025년 서울시야구협회 1부 리그 춘계대회
 */
@Entity
@Table(
    name = "competitions",
    indexes = [
        Index(name = "idx_competitions_league", columnList = "league_id"),
        Index(name = "idx_competitions_year_season", columnList = "year, season"),
        Index(name = "idx_competitions_dates", columnList = "start_date, end_date"),
        Index(name = "idx_competitions_status", columnList = "status"),
    ],
)
class Competition(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id", nullable = false)
    val league: League,
    @Column(nullable = false, length = 100)
    val name: String,
    @Column(nullable = false)
    val year: Int,
    @Column(nullable = false)
    val season: Int = 1,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val type: CompetitionType = CompetitionType.LEAGUE,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "end_date")
    var endDate: LocalDate? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CompetitionStatus = CompetitionStatus.SCHEDULED,
    @Column(length = 500)
    var description: String? = null,
    @Column(name = "max_teams")
    val maxTeams: Int? = null,
    @Column(name = "playoff_teams")
    var playoffTeams: Int? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 대회를 시작합니다.
     */
    fun start() {
        require(status == CompetitionStatus.SCHEDULED) { "예정된 대회만 시작할 수 있습니다." }
        this.status = CompetitionStatus.IN_PROGRESS
    }

    /**
     * 대회를 완료합니다.
     */
    fun complete(endDate: LocalDate = LocalDate.now()) {
        require(status == CompetitionStatus.IN_PROGRESS) { "진행 중인 대회만 완료할 수 있습니다." }
        require(!endDate.isBefore(startDate)) { "종료일은 시작일 이후여야 합니다." }
        this.status = CompetitionStatus.COMPLETED
        this.endDate = endDate
    }

    /**
     * 대회를 취소합니다.
     */
    fun cancel() {
        require(status != CompetitionStatus.COMPLETED) { "완료된 대회는 취소할 수 없습니다." }
        this.status = CompetitionStatus.CANCELLED
    }

    /**
     * 대회가 현재 진행 중인지 확인합니다.
     */
    val isActive: Boolean
        get() = status == CompetitionStatus.IN_PROGRESS

    /**
     * 대회가 특정 날짜에 활성 상태인지 확인합니다.
     */
    fun isActiveAt(date: LocalDate): Boolean {
        if (status != CompetitionStatus.IN_PROGRESS && status != CompetitionStatus.COMPLETED) {
            return false
        }
        val end = endDate ?: LocalDate.MAX
        return !date.isBefore(startDate) && !date.isAfter(end)
    }
}

/**
 * 대회 상태
 */
enum class CompetitionStatus(
    val displayName: String,
) {
    SCHEDULED("예정"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
    CANCELLED("취소"),
    POSTPONED("연기"),
}
