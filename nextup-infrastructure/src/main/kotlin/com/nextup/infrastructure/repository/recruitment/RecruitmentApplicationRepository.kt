package com.nextup.infrastructure.repository.recruitment

import com.nextup.core.domain.recruitment.ApplicationStatus
import com.nextup.core.domain.recruitment.RecruitmentApplication
import com.nextup.core.port.repository.RecruitmentApplicationRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RecruitmentApplicationRepository :
    JpaRepository<RecruitmentApplication, Long>,
    RecruitmentApplicationRepositoryPort {
    override fun findByIdOrNull(id: Long): RecruitmentApplication? = findById(id).orElse(null)

    override fun findByRecruitmentId(recruitmentId: Long): List<RecruitmentApplication>

    override fun findByRecruitmentIdAndStatus(
        recruitmentId: Long,
        status: ApplicationStatus,
    ): List<RecruitmentApplication>

    override fun findByApplicantId(applicantId: Long): List<RecruitmentApplication>

    override fun findByRecruitmentIdAndApplicantId(
        recruitmentId: Long,
        applicantId: Long,
    ): RecruitmentApplication?

    @Query(
        """
        SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
        FROM RecruitmentApplication a
        WHERE a.recruitment.id = :recruitmentId
        AND a.applicantId = :applicantId
        AND a.status IN :statuses
        """,
    )
    override fun existsByRecruitmentIdAndApplicantIdAndStatusIn(
        recruitmentId: Long,
        applicantId: Long,
        statuses: Set<ApplicationStatus>,
    ): Boolean
}
