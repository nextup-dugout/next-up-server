package com.nextup.core.domain.match

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 매칭 요청 엔티티
 *
 * 팀 간 연습 경기 매칭을 위한 요청을 관리합니다.
 */
@Entity
@Table(
    name = "match_requests",
    indexes = [
        Index(name = "idx_match_requests_team_id", columnList = "team_id"),
        Index(name = "idx_match_requests_status", columnList = "status"),
        Index(name = "idx_match_requests_preferred_date", columnList = "preferred_date"),
        Index(name = "idx_match_requests_skill_level", columnList = "skill_level"),
    ],
)
class MatchRequest private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @Column(name = "preferred_date", nullable = false)
    val preferredDate: LocalDate,
    @Column(name = "preferred_time", length = 50)
    val preferredTime: String?,
    @Column(name = "preferred_location", length = 500)
    val preferredLocation: String?,
    @Column(columnDefinition = "TEXT")
    val message: String?,
    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", nullable = false, length = 20)
    val skillLevel: SkillLevel,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MatchRequestStatus = MatchRequestStatus.OPEN,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    @Version
    var version: Long = 0
        internal set

    /**
     * 매칭 요청을 취소합니다.
     */
    fun cancel() {
        require(status == MatchRequestStatus.OPEN) {
            "OPEN 상태의 요청만 취소할 수 있습니다"
        }
        this.status = MatchRequestStatus.CANCELLED
    }

    /**
     * 매칭을 완료합니다.
     */
    fun match() {
        require(status == MatchRequestStatus.OPEN) {
            "OPEN 상태의 요청만 매칭할 수 있습니다"
        }
        this.status = MatchRequestStatus.MATCHED
    }

    /**
     * 요청이 만료되었는지 확인합니다.
     * 요청 생성 후 30일이 지나면 만료됩니다.
     */
    fun isExpired(): Boolean {
        val daysSinceCreation = ChronoUnit.DAYS.between(createdAt, Instant.now())
        return daysSinceCreation >= 30
    }

    /**
     * 만료 상태로 변경합니다.
     */
    fun expire() {
        require(status == MatchRequestStatus.OPEN) {
            "OPEN 상태의 요청만 만료시킬 수 있습니다"
        }
        this.status = MatchRequestStatus.EXPIRED
    }

    companion object {
        /**
         * 매칭 요청을 생성합니다.
         */
        fun create(
            team: Team,
            preferredDate: LocalDate,
            preferredTime: String?,
            preferredLocation: String?,
            message: String?,
            skillLevel: SkillLevel,
        ): MatchRequest {
            require(preferredDate >= LocalDate.now()) {
                "선호 날짜는 오늘 이후여야 합니다"
            }

            return MatchRequest(
                team = team,
                preferredDate = preferredDate,
                preferredTime = preferredTime,
                preferredLocation = preferredLocation,
                message = message,
                skillLevel = skillLevel,
            )
        }
    }
}
