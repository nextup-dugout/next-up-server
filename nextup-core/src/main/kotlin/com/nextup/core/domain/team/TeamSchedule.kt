package com.nextup.core.domain.team

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 팀 일정 엔티티
 *
 * 경기 일정 외에 연습, 이벤트, 모임 등 팀 자체 일정을 관리합니다.
 */
@Entity
@Table(
    name = "team_schedules",
    indexes = [
        Index(name = "idx_team_schedules_team_id", columnList = "team_id"),
        Index(name = "idx_team_schedules_start_at", columnList = "start_at"),
        Index(name = "idx_team_schedules_type", columnList = "schedule_type"),
    ],
)
class TeamSchedule private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @Column(nullable = false, length = 200)
    var title: String,
    @Column(columnDefinition = "TEXT")
    var description: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 30)
    var scheduleType: TeamScheduleType,
    @Column(name = "start_at", nullable = false)
    var startAt: LocalDateTime,
    @Column(name = "end_at")
    var endAt: LocalDateTime?,
    @Column(length = 500)
    var location: String?,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 일정 정보를 수정합니다.
     */
    fun update(
        title: String? = null,
        description: String? = null,
        scheduleType: TeamScheduleType? = null,
        startAt: LocalDateTime? = null,
        endAt: LocalDateTime? = null,
        location: String? = null,
    ) {
        title?.let {
            require(it.isNotBlank()) { "제목은 비어있을 수 없습니다" }
            this.title = it
        }
        description?.let { this.description = it }
        scheduleType?.let { this.scheduleType = it }
        startAt?.let { this.startAt = it }
        endAt?.let { this.endAt = it }
        location?.let { this.location = it }

        validateDateRange()
    }

    private fun validateDateRange() {
        endAt?.let { end ->
            require(!end.isBefore(startAt)) {
                "종료 시간은 시작 시간 이후여야 합니다"
            }
        }
    }

    companion object {
        /**
         * 팀 일정을 생성합니다.
         */
        fun create(
            team: Team,
            title: String,
            description: String? = null,
            scheduleType: TeamScheduleType,
            startAt: LocalDateTime,
            endAt: LocalDateTime? = null,
            location: String? = null,
        ): TeamSchedule {
            require(title.isNotBlank()) { "제목은 필수입니다" }

            val schedule =
                TeamSchedule(
                    team = team,
                    title = title,
                    description = description,
                    scheduleType = scheduleType,
                    startAt = startAt,
                    endAt = endAt,
                    location = location,
                )

            schedule.validateDateRange()

            return schedule
        }
    }
}

/**
 * 팀 일정 유형
 */
enum class TeamScheduleType(
    val displayName: String,
) {
    PRACTICE("연습"),
    MEETING("모임"),
    EVENT("이벤트"),
    OTHER("기타"),
}
