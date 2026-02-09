package com.nextup.core.domain.recruitment

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.team.Team
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 팀 모집 공고 엔티티
 *
 * 팀의 선수 모집 공고를 관리합니다.
 */
@Entity
@Table(
    name = "team_recruitments",
    indexes = [
        Index(name = "idx_team_recruitments_team_id", columnList = "team_id"),
        Index(name = "idx_team_recruitments_status", columnList = "status"),
        Index(name = "idx_team_recruitments_deadline", columnList = "deadline"),
        Index(
            name = "idx_team_recruitments_team_status",
            columnList = "team_id, status",
        ),
    ],
)
class TeamRecruitment private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,
    @Column(nullable = false, length = 200)
    var title: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,
    @Column(name = "positions_needed", nullable = false, length = 255)
    var positionsNeeded: String,
    @Column(name = "age_range", length = 50)
    var ageRange: String? = null,
    @Column(name = "skill_level", length = 50)
    var skillLevel: String? = null,
    @Column(length = 255)
    var location: String? = null,
    @Column(nullable = false)
    var deadline: LocalDate,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: RecruitmentStatus = RecruitmentStatus.OPEN,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 모집 공고를 마감합니다.
     */
    fun close() {
        require(status == RecruitmentStatus.OPEN) {
            "진행 중인 모집 공고만 마감할 수 있습니다"
        }
        this.status = RecruitmentStatus.CLOSED
    }

    /**
     * 모집 기한이 지났는지 확인합니다.
     */
    fun isExpired(): Boolean = LocalDate.now().isAfter(deadline)

    /**
     * 모집 공고 정보를 업데이트합니다.
     */
    fun update(
        title: String? = null,
        description: String? = null,
        positionsNeeded: String? = null,
        deadline: LocalDate? = null,
    ) {
        require(status == RecruitmentStatus.OPEN) {
            "진행 중인 모집 공고만 수정할 수 있습니다"
        }
        title?.let {
            require(it.isNotBlank()) { "제목은 필수입니다" }
            this.title = it
        }
        description?.let {
            require(it.isNotBlank()) { "설명은 필수입니다" }
            this.description = it
        }
        positionsNeeded?.let {
            require(it.isNotBlank()) { "모집 포지션은 필수입니다" }
            this.positionsNeeded = it
        }
        deadline?.let {
            require(it.isAfter(LocalDate.now())) {
                "마감일은 현재 날짜보다 이후여야 합니다"
            }
            this.deadline = it
        }
    }

    companion object {
        /**
         * 팀 모집 공고를 생성합니다.
         */
        fun create(
            team: Team,
            title: String,
            description: String,
            positionsNeeded: String,
            ageRange: String?,
            skillLevel: String?,
            location: String?,
            deadline: LocalDate,
        ): TeamRecruitment {
            require(title.isNotBlank()) { "제목은 필수입니다" }
            require(description.isNotBlank()) { "설명은 필수입니다" }
            require(positionsNeeded.isNotBlank()) { "모집 포지션은 필수입니다" }
            require(deadline.isAfter(LocalDate.now())) {
                "마감일은 현재 날짜보다 이후여야 합니다"
            }

            return TeamRecruitment(
                team = team,
                title = title,
                description = description,
                positionsNeeded = positionsNeeded,
                ageRange = ageRange,
                skillLevel = skillLevel,
                location = location,
                deadline = deadline,
            )
        }
    }
}
