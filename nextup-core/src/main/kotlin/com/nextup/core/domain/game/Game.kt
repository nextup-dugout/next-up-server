package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.competition.Competition
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 경기 엔티티
 *
 * 대회에 속한 개별 경기를 나타냅니다.
 * 경기에 참여하는 팀(GameTeam)과 선수(GamePlayer)는
 * 각각 ManyToOne으로 Game을 참조합니다.
 */
@Entity
@Table(
    name = "games",
    indexes = [
        Index(name = "idx_games_competition", columnList = "competition_id"),
        Index(name = "idx_games_scheduled_at", columnList = "scheduled_at"),
        Index(name = "idx_games_status", columnList = "status"),
        Index(name = "idx_games_competition_date", columnList = "competition_id, scheduled_at")
    ]
)
class Game(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    val competition: Competition,

    @Column(name = "scheduled_at", nullable = false)
    var scheduledAt: LocalDateTime,

    @Column(length = 100)
    var location: String? = null,

    @Column(name = "field_name", length = 100)
    var fieldName: String? = null,

    @Column(name = "game_number")
    var gameNumber: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: GameStatus = GameStatus.SCHEDULED,

    @Column(name = "current_inning")
    var currentInning: Int = 0,

    @Column(name = "is_top_inning")
    var isTopInning: Boolean = true,

    @Column(name = "total_innings")
    var totalInnings: Int = 9,

    @Column(name = "started_at")
    var startedAt: LocalDateTime? = null,

    @Column(name = "ended_at")
    var endedAt: LocalDateTime? = null,

    @Column(length = 500)
    var note: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
) : BaseTimeEntity() {

    /**
     * 경기를 시작합니다.
     */
    fun start() {
        require(status.canStart()) { "예정 상태의 경기만 시작할 수 있습니다. 현재 상태: ${status.displayName}" }
        status = GameStatus.IN_PROGRESS
        currentInning = 1
        isTopInning = true
        startedAt = LocalDateTime.now()
    }

    /**
     * 다음 이닝으로 진행합니다.
     */
    fun nextHalfInning() {
        require(status.isOngoing()) { "진행 중인 경기만 이닝을 진행할 수 있습니다." }

        if (isTopInning) {
            isTopInning = false
        } else {
            currentInning++
            isTopInning = true
        }
    }

    /**
     * 경기를 정상 종료합니다.
     */
    fun finish() {
        require(status.isOngoing()) { "진행 중인 경기만 종료할 수 있습니다." }
        status = GameStatus.FINISHED
        endedAt = LocalDateTime.now()
    }

    /**
     * 콜드게임으로 종료합니다.
     */
    fun callGame(reason: String? = null) {
        require(status.isOngoing()) { "진행 중인 경기만 콜드게임 처리할 수 있습니다." }
        status = GameStatus.CALLED
        endedAt = LocalDateTime.now()
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "콜드게임 사유: $reason"
        }
    }

    /**
     * 경기를 취소합니다.
     */
    fun cancel(reason: String? = null) {
        require(status == GameStatus.SCHEDULED || status == GameStatus.POSTPONED) {
            "예정 또는 연기 상태의 경기만 취소할 수 있습니다."
        }
        status = GameStatus.CANCELLED
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "취소 사유: $reason"
        }
    }

    /**
     * 경기를 연기합니다.
     */
    fun postpone(newScheduledAt: LocalDateTime, reason: String? = null) {
        require(status == GameStatus.SCHEDULED) { "예정 상태의 경기만 연기할 수 있습니다." }
        status = GameStatus.POSTPONED
        scheduledAt = newScheduledAt
        if (reason != null) {
            note = (note?.let { "$it\n" } ?: "") + "연기 사유: $reason"
        }
    }

    /**
     * 몰수패 처리합니다.
     */
    fun forfeit(reason: String) {
        require(status == GameStatus.SCHEDULED || status.isOngoing()) {
            "예정 또는 진행 중인 경기만 몰수 처리할 수 있습니다."
        }
        status = GameStatus.FORFEITED
        endedAt = LocalDateTime.now()
        note = (note?.let { "$it\n" } ?: "") + "몰수 사유: $reason"
    }

    /**
     * 경기 일정을 변경합니다.
     */
    fun reschedule(newScheduledAt: LocalDateTime) {
        require(status == GameStatus.SCHEDULED || status == GameStatus.POSTPONED) {
            "예정 또는 연기 상태의 경기만 일정을 변경할 수 있습니다."
        }
        if (status == GameStatus.POSTPONED) {
            status = GameStatus.SCHEDULED
        }
        scheduledAt = newScheduledAt
    }

    /**
     * 현재 이닝 표시를 반환합니다 (예: "5회초", "7회말").
     */
    val currentInningDisplay: String
        get() = if (currentInning == 0) "경기 전" else "${currentInning}회${if (isTopInning) "초" else "말"}"

    /**
     * 경기가 연장전인지 확인합니다.
     */
    val isExtraInning: Boolean
        get() = currentInning > totalInnings
}
