package com.nextup.core.domain.attendance

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamMember
import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 활동 점수 엔티티
 *
 * 팀원의 경기 참여율, 연습 참석률, 기여도 점수 등을 관리합니다.
 */
@Entity
@Table(
    name = "activity_scores",
    indexes = [
        Index(name = "idx_as_team_id", columnList = "team_id"),
        Index(name = "idx_as_member_id", columnList = "member_id"),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_as_team_member",
            columnNames = ["team_id", "member_id"],
        ),
    ],
)
class ActivityScore private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: TeamMember,
    @Column(name = "game_participation_rate", nullable = false, precision = 5, scale = 2)
    var gameParticipationRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "practice_attendance_rate", nullable = false, precision = 5, scale = 2)
    var practiceAttendanceRate: BigDecimal = BigDecimal.ZERO,
    @Column(name = "contribution_score", nullable = false, precision = 5, scale = 2)
    var contributionScore: BigDecimal = BigDecimal.ZERO,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 전체 활동 점수를 계산합니다.
     * 경기 참여율 40% + 연습 참석률 40% + 기여도 20%
     */
    fun calculateTotalScore(): BigDecimal =
        (
            gameParticipationRate.multiply(BigDecimal("0.4"))
                .add(practiceAttendanceRate.multiply(BigDecimal("0.4")))
                .add(contributionScore.multiply(BigDecimal("0.2")))
        ).setScale(2, RoundingMode.HALF_UP)

    /**
     * 경기 참여율을 업데이트합니다.
     */
    fun updateGameParticipationRate(rate: BigDecimal) {
        require(rate >= BigDecimal.ZERO && rate <= BigDecimal(100)) {
            "경기 참여율은 0~100 범위여야 합니다."
        }
        this.gameParticipationRate = rate.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * 연습 참석률을 업데이트합니다.
     */
    fun updatePracticeAttendanceRate(rate: BigDecimal) {
        require(rate >= BigDecimal.ZERO && rate <= BigDecimal(100)) {
            "연습 참석률은 0~100 범위여야 합니다."
        }
        this.practiceAttendanceRate = rate.setScale(2, RoundingMode.HALF_UP)
    }

    /**
     * 기여도 점수를 업데이트합니다.
     */
    fun updateContributionScore(score: BigDecimal) {
        require(score >= BigDecimal.ZERO && score <= BigDecimal(100)) {
            "기여도 점수는 0~100 범위여야 합니다."
        }
        this.contributionScore = score.setScale(2, RoundingMode.HALF_UP)
    }

    companion object {
        /**
         * 활동 점수를 생성합니다.
         *
         * @param team 팀
         * @param member 팀원
         * @return 생성된 ActivityScore
         */
        fun create(
            team: Team,
            member: TeamMember,
        ): ActivityScore =
            ActivityScore(
                team = team,
                member = member,
                gameParticipationRate = BigDecimal.ZERO,
                practiceAttendanceRate = BigDecimal.ZERO,
                contributionScore = BigDecimal.ZERO,
            )
    }
}
