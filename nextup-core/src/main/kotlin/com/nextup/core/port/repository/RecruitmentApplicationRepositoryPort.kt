package com.nextup.core.port.repository

import com.nextup.core.domain.recruitment.ApplicationStatus
import com.nextup.core.domain.recruitment.RecruitmentApplication

/**
 * RecruitmentApplication Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface RecruitmentApplicationRepositoryPort {
    fun save(application: RecruitmentApplication): RecruitmentApplication

    fun findByIdOrNull(id: Long): RecruitmentApplication?

    fun findByRecruitmentId(recruitmentId: Long): List<RecruitmentApplication>

    fun findByRecruitmentIdAndStatus(
        recruitmentId: Long,
        status: ApplicationStatus,
    ): List<RecruitmentApplication>

    fun findByApplicantId(applicantId: Long): List<RecruitmentApplication>

    fun findByRecruitmentIdAndApplicantId(
        recruitmentId: Long,
        applicantId: Long,
    ): RecruitmentApplication?

    fun existsByRecruitmentIdAndApplicantIdAndStatusIn(
        recruitmentId: Long,
        applicantId: Long,
        statuses: Set<ApplicationStatus>,
    ): Boolean

    fun delete(application: RecruitmentApplication)
}
