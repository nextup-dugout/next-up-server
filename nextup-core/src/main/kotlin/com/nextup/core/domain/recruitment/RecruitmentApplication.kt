package com.nextup.core.domain.recruitment

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant

/**
 * 모집 공고 지원 엔티티
 *
 * 팀 모집 공고에 대한 지원 정보를 관리합니다.
 * Rich Domain Model 원칙에 따라 비즈니스 로직을 Entity 내부에 캡슐화합니다.
 */
@Entity
@Table(
    name = "recruitment_applications",
    indexes = [
        Index(name = "idx_ra_recruitment_id", columnList = "recruitment_id"),
        Index(name = "idx_ra_applicant_id", columnList = "applicant_id"),
        Index(name = "idx_ra_status", columnList = "status"),
        Index(
            name = "idx_ra_recruitment_applicant",
            columnList = "recruitment_id, applicant_id",
        ),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_ra_recruitment_applicant_active",
            columnNames = ["recruitment_id", "applicant_id"],
        ),
    ],
)
class RecruitmentApplication private constructor(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id", nullable = false)
    val recruitment: TeamRecruitment,
    @Column(name = "applicant_id", nullable = false)
    val applicantId: Long,
    @Column(nullable = false, length = 500)
    val message: String,
    @Column(name = "preferred_positions", nullable = false, length = 255)
    val preferredPositions: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ApplicationStatus = ApplicationStatus.PENDING,
    @Column(name = "applied_at", nullable = false)
    val appliedAt: Instant = Instant.now(),
    @Column(name = "processed_at")
    var processedAt: Instant? = null,
    @Column(name = "processed_by")
    var processedBy: Long? = null,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 지원을 수락합니다.
     *
     * @param processorId 처리자 ID
     * @throws IllegalStateException PENDING 상태가 아닌 경우
     */
    fun accept(processorId: Long) {
        check(status == ApplicationStatus.PENDING) {
            "대기 중인 지원만 수락할 수 있습니다"
        }
        this.status = ApplicationStatus.ACCEPTED
        this.processedAt = Instant.now()
        this.processedBy = processorId
    }

    /**
     * 지원을 거절합니다.
     *
     * @param processorId 처리자 ID
     * @throws IllegalStateException PENDING 상태가 아닌 경우
     */
    fun reject(processorId: Long) {
        check(status == ApplicationStatus.PENDING) {
            "대기 중인 지원만 거절할 수 있습니다"
        }
        this.status = ApplicationStatus.REJECTED
        this.processedAt = Instant.now()
        this.processedBy = processorId
    }

    /**
     * 지원을 취소합니다.
     *
     * @throws IllegalStateException PENDING 상태가 아닌 경우
     */
    fun withdraw() {
        check(status == ApplicationStatus.PENDING) {
            "대기 중인 지원만 취소할 수 있습니다"
        }
        this.status = ApplicationStatus.WITHDRAWN
        this.processedAt = Instant.now()
    }

    companion object {
        /**
         * 모집 공고 지원을 생성합니다.
         *
         * @param recruitment 모집 공고
         * @param applicantId 지원자 ID
         * @param message 지원 메시지
         * @param preferredPositions 선호 포지션 (쉼표 구분)
         * @return 생성된 RecruitmentApplication
         */
        fun create(
            recruitment: TeamRecruitment,
            applicantId: Long,
            message: String,
            preferredPositions: String,
        ): RecruitmentApplication {
            require(message.isNotBlank()) { "지원 메시지는 필수입니다" }
            require(preferredPositions.isNotBlank()) { "선호 포지션은 필수입니다" }
            require(recruitment.status == RecruitmentStatus.OPEN) {
                "진행 중인 모집 공고에만 지원할 수 있습니다"
            }

            return RecruitmentApplication(
                recruitment = recruitment,
                applicantId = applicantId,
                message = message,
                preferredPositions = preferredPositions,
            )
        }
    }
}
