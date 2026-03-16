package com.nextup.core.service.recruitment

import com.nextup.common.exception.AlreadyTeamMemberApplicationException
import com.nextup.common.exception.DuplicateApplicationException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.RecruitmentApplicationNotFoundException
import com.nextup.common.exception.RecruitmentNotOpenException
import com.nextup.core.domain.event.RecruitmentApplicationAcceptedEvent
import com.nextup.core.domain.recruitment.ApplicationStatus
import com.nextup.core.domain.recruitment.RecruitmentApplication
import com.nextup.core.domain.recruitment.RecruitmentStatus
import com.nextup.core.port.repository.RecruitmentApplicationRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.port.repository.TeamRecruitmentRepositoryPort
import com.nextup.core.service.recruitment.dto.ApplyRecruitmentRequest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 모집 공고 지원 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class RecruitmentApplicationService(
    private val applicationRepository: RecruitmentApplicationRepositoryPort,
    private val recruitmentRepository: TeamRecruitmentRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
    private val eventPublisher: ApplicationEventPublisher,
) {
    /**
     * 모집 공고에 지원합니다.
     *
     * @param request 지원 요청
     * @return 생성된 RecruitmentApplication
     * @throws RecruitmentNotOpenException 모집 공고가 진행 중이 아닌 경우
     * @throws DuplicateApplicationException 이미 지원한 경우
     * @throws AlreadyTeamMemberApplicationException 이미 해당 팀의 멤버인 경우
     */
    @Transactional
    fun apply(request: ApplyRecruitmentRequest): RecruitmentApplication {
        val recruitment =
            recruitmentRepository.findByIdOrNull(request.recruitmentId)
                ?: throw com.nextup.common.exception.RecruitmentNotFoundException(request.recruitmentId)

        // 모집 공고가 진행 중인지 확인
        if (recruitment.status != RecruitmentStatus.OPEN) {
            throw RecruitmentNotOpenException(request.recruitmentId)
        }

        // 마감일이 지났는지 확인
        if (recruitment.isExpired()) {
            throw RecruitmentNotOpenException(request.recruitmentId)
        }

        // 중복 지원 방지 (PENDING 또는 ACCEPTED 상태)
        val hasActiveApplication =
            applicationRepository.existsByRecruitmentIdAndApplicantIdAndStatusIn(
                recruitmentId = request.recruitmentId,
                applicantId = request.applicantId,
                statuses = setOf(ApplicationStatus.PENDING, ApplicationStatus.ACCEPTED),
            )
        if (hasActiveApplication) {
            throw DuplicateApplicationException(request.recruitmentId, request.applicantId)
        }

        // 이미 해당 팀의 멤버인지 확인
        val isAlreadyMember =
            teamMemberRepository.existsByTeamIdAndUserId(
                teamId = recruitment.team.id,
                userId = request.applicantId,
            )
        if (isAlreadyMember) {
            throw AlreadyTeamMemberApplicationException(request.applicantId, recruitment.team.id)
        }

        val application =
            RecruitmentApplication.create(
                recruitment = recruitment,
                applicantId = request.applicantId,
                message = request.message,
                preferredPositions = request.preferredPositions,
            )

        return applicationRepository.save(application)
    }

    /**
     * 지원을 수락합니다.
     *
     * @param applicationId 지원 ID
     * @param processorId 처리자 ID
     * @return 수락된 RecruitmentApplication
     * @throws RecruitmentApplicationNotFoundException 지원을 찾을 수 없는 경우
     * @throws InvalidStateException PENDING 상태가 아닌 경우
     */
    @Transactional
    fun acceptApplication(
        applicationId: Long,
        processorId: Long,
    ): RecruitmentApplication {
        val application = getApplicationById(applicationId)

        try {
            application.accept(processorId)
        } catch (e: IllegalStateException) {
            throw InvalidStateException(
                "INVALID_APPLICATION_STATE",
                e.message ?: "지원을 수락할 수 없습니다",
            )
        }

        // 지원 수락 이벤트 발행 (팀 자동 합류)
        eventPublisher.publishEvent(
            RecruitmentApplicationAcceptedEvent(
                applicationId = application.id,
                recruitmentId = application.recruitment.id,
                teamId = application.recruitment.team.id,
                applicantId = application.applicantId,
                teamName = application.recruitment.team.name,
            ),
        )

        return application
    }

    /**
     * 지원을 거절합니다.
     *
     * @param applicationId 지원 ID
     * @param processorId 처리자 ID
     * @return 거절된 RecruitmentApplication
     * @throws RecruitmentApplicationNotFoundException 지원을 찾을 수 없는 경우
     * @throws InvalidStateException PENDING 상태가 아닌 경우
     */
    @Transactional
    fun rejectApplication(
        applicationId: Long,
        processorId: Long,
    ): RecruitmentApplication {
        val application = getApplicationById(applicationId)

        try {
            application.reject(processorId)
        } catch (e: IllegalStateException) {
            throw InvalidStateException(
                "INVALID_APPLICATION_STATE",
                e.message ?: "지원을 거절할 수 없습니다",
            )
        }

        return application
    }

    /**
     * 지원을 취소합니다. (지원자 본인)
     *
     * @param applicationId 지원 ID
     * @param applicantId 지원자 ID (본인 확인용)
     * @throws RecruitmentApplicationNotFoundException 지원을 찾을 수 없는 경우
     * @throws InvalidStateException PENDING 상태가 아닌 경우
     */
    @Transactional
    fun withdrawApplication(
        applicationId: Long,
        applicantId: Long,
    ) {
        val application = getApplicationById(applicationId)

        require(application.applicantId == applicantId) {
            "본인의 지원만 취소할 수 있습니다"
        }

        try {
            application.withdraw()
        } catch (e: IllegalStateException) {
            throw InvalidStateException(
                "INVALID_APPLICATION_STATE",
                e.message ?: "지원을 취소할 수 없습니다",
            )
        }
    }

    /**
     * 모집 공고별 지원자 목록을 조회합니다.
     *
     * @param recruitmentId 모집 공고 ID
     * @return 지원 목록
     */
    fun getApplicationsByRecruitment(recruitmentId: Long): List<RecruitmentApplication> =
        applicationRepository.findByRecruitmentId(recruitmentId)

    /**
     * 사용자의 지원 현황을 조회합니다.
     *
     * @param applicantId 지원자 ID
     * @return 지원 목록
     */
    fun getApplicationsByApplicant(applicantId: Long): List<RecruitmentApplication> =
        applicationRepository.findByApplicantId(applicantId)

    /**
     * ID로 지원을 조회합니다.
     *
     * @param id 지원 ID
     * @return RecruitmentApplication
     * @throws RecruitmentApplicationNotFoundException 지원을 찾을 수 없는 경우
     */
    fun getApplicationById(id: Long): RecruitmentApplication =
        applicationRepository.findByIdOrNull(id)
            ?: throw RecruitmentApplicationNotFoundException(id)
}
