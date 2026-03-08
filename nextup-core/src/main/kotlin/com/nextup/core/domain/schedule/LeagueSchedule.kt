package com.nextup.core.domain.schedule

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.team.Team
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime

/**
 * 대진표 엔티티
 *
 * 대회(Competition) 내의 라운드별 경기 일정을 나타냅니다.
 * 대진표 항목은 실제 경기(Game)와 연결될 수 있습니다.
 */
@Entity
@Table(
    name = "league_schedules",
    indexes = [
        Index(name = "idx_league_schedules_competition", columnList = "competition_id"),
        Index(name = "idx_league_schedules_date", columnList = "scheduled_date"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_league_schedules_competition_round_match",
            columnNames = ["competition_id", "round", "match_number"],
        ),
    ],
)
class LeagueSchedule private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    val competition: Competition,
    @Column(nullable = false)
    val round: Int,
    @Column(name = "match_number", nullable = false)
    val matchNumber: Int,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id", nullable = false)
    val homeTeam: Team,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id", nullable = false)
    val awayTeam: Team,
    @Column(name = "scheduled_date", nullable = false)
    var scheduledDate: LocalDate,
    @Column(name = "scheduled_time")
    var scheduledTime: LocalTime? = null,
    @Column(length = 255)
    var venue: String? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ScheduleStatus = ScheduleStatus.SCHEDULED
        protected set

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    var game: Game? = null
        protected set

    @Column(name = "postponed_reason", length = 500)
    var postponedReason: String? = null
        protected set

    @Column(name = "original_date")
    var originalDate: LocalDate? = null
        protected set

    /**
     * 경기 연결이 가능한 상태인지 확인합니다.
     */
    fun canLinkGame(): Boolean = status == ScheduleStatus.SCHEDULED || status == ScheduleStatus.POSTPONED

    /**
     * 경기를 연결합니다.
     */
    fun linkGame(game: Game) {
        require(status == ScheduleStatus.SCHEDULED || status == ScheduleStatus.POSTPONED) {
            "예정 또는 연기 상태의 대진표만 경기를 연결할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        this.game = game
        this.status = ScheduleStatus.GAME_CREATED
    }

    /**
     * 대진표를 연기합니다.
     */
    fun postpone(reason: String) {
        require(status == ScheduleStatus.SCHEDULED) {
            "예정 상태의 대진표만 연기할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        require(reason.isNotBlank()) {
            "연기 사유는 필수입니다."
        }
        if (this.originalDate == null) {
            this.originalDate = this.scheduledDate
        }
        this.postponedReason = reason
        this.status = ScheduleStatus.POSTPONED
    }

    /**
     * 대진표를 취소합니다.
     */
    fun cancel() {
        require(status != ScheduleStatus.COMPLETED) {
            "완료된 대진표는 취소할 수 없습니다."
        }
        this.status = ScheduleStatus.CANCELLED
    }

    /**
     * 대진표를 완료합니다.
     */
    fun complete() {
        require(status == ScheduleStatus.GAME_CREATED) {
            "경기가 생성된 대진표만 완료할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        this.status = ScheduleStatus.COMPLETED
    }

    /**
     * 일정을 변경합니다.
     */
    fun reschedule(
        newDate: LocalDate,
        newTime: LocalTime? = this.scheduledTime,
        newVenue: String? = this.venue,
    ) {
        require(status == ScheduleStatus.SCHEDULED || status == ScheduleStatus.POSTPONED) {
            "예정 또는 연기 상태의 대진표만 일정을 변경할 수 있습니다. 현재 상태: ${status.displayName}"
        }
        this.scheduledDate = newDate
        this.scheduledTime = newTime
        this.venue = newVenue
        if (status == ScheduleStatus.POSTPONED) {
            this.status = ScheduleStatus.SCHEDULED
            this.postponedReason = null
        }
    }

    companion object {
        fun create(
            competition: Competition,
            round: Int,
            matchNumber: Int,
            homeTeam: Team,
            awayTeam: Team,
            scheduledDate: LocalDate,
            scheduledTime: LocalTime? = null,
            venue: String? = null,
        ): LeagueSchedule {
            require(round > 0) { "라운드는 1 이상이어야 합니다." }
            require(matchNumber > 0) { "경기 번호는 1 이상이어야 합니다." }
            require(homeTeam.id != awayTeam.id) { "홈팀과 원정팀은 같을 수 없습니다." }

            return LeagueSchedule(
                competition = competition,
                round = round,
                matchNumber = matchNumber,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledDate = scheduledDate,
                scheduledTime = scheduledTime,
                venue = venue,
            )
        }
    }
}

/**
 * 대진표 상태
 */
enum class ScheduleStatus(
    val displayName: String,
) {
    SCHEDULED("예정"),
    GAME_CREATED("경기 생성"),
    POSTPONED("연기"),
    CANCELLED("취소"),
    COMPLETED("완료"),
}
