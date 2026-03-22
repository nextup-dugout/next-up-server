package com.nextup.core.service.recruitment

import com.nextup.common.exception.ForbiddenException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.RecruitmentNotFoundException
import com.nextup.common.exception.TeamNotFoundException
import com.nextup.core.domain.recruitment.RecruitmentStatus
import com.nextup.core.domain.recruitment.TeamRecruitment
import com.nextup.core.port.repository.TeamRecruitmentRepositoryPort
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.service.recruitment.dto.CreateRecruitmentRequest
import com.nextup.core.service.recruitment.dto.UpdateRecruitmentRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 팀 모집 공고 서비스
 *
 * 비즈니스 로직은 Entity에 위임하고, 서비스는 조율(orchestration)만 수행합니다.
 */
@Service
@Transactional(readOnly = true)
class TeamRecruitmentService(
    private val recruitmentRepository: TeamRecruitmentRepositoryPort,
    private val teamRepository: TeamRepositoryPort,
) {
    /**
     * 팀 모집 공고를 생성합니다.
     */
    @Transactional
    fun createRecruitment(request: CreateRecruitmentRequest): TeamRecruitment {
        val team =
            teamRepository.findByIdOrNull(request.teamId)
                ?: throw TeamNotFoundException(request.teamId)

        val recruitment =
            TeamRecruitment.create(
                team = team,
                title = request.title,
                description = request.description,
                positionsNeeded = request.positionsNeeded,
                ageRange = request.ageRange,
                skillLevel = request.skillLevel,
                location = request.location,
                deadline = request.deadline,
            )

        return recruitmentRepository.save(recruitment)
    }

    /**
     * 팀 모집 공고를 수정합니다.
     */
    @Transactional
    fun updateRecruitment(
        id: Long,
        teamId: Long,
        request: UpdateRecruitmentRequest,
    ): TeamRecruitment {
        val recruitment = getById(id)
        verifyTeamOwnership(recruitment, teamId)

        try {
            recruitment.update(
                title = request.title,
                description = request.description,
                positionsNeeded = request.positionsNeeded,
                deadline = request.deadline,
            )
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_RECRUITMENT_STATE",
                e.message ?: "Cannot update recruitment",
            )
        }

        return recruitment
    }

    /**
     * 팀 모집 공고를 마감합니다.
     */
    @Transactional
    fun closeRecruitment(id: Long): TeamRecruitment {
        val recruitment = getById(id)

        try {
            recruitment.close()
        } catch (e: IllegalArgumentException) {
            throw InvalidStateException(
                "INVALID_RECRUITMENT_STATE",
                e.message ?: "Cannot close recruitment",
            )
        }

        return recruitment
    }

    /**
     * ID로 모집 공고를 조회합니다.
     */
    fun getById(id: Long): TeamRecruitment =
        recruitmentRepository.findByIdOrNull(id)
            ?: throw RecruitmentNotFoundException(id)

    /**
     * 팀별 모집 공고 목록을 조회합니다.
     */
    fun getByTeam(teamId: Long): List<TeamRecruitment> = recruitmentRepository.findByTeamId(teamId)

    /**
     * 모든 진행 중인 모집 공고를 조회합니다.
     */
    fun getAllOpen(): List<TeamRecruitment> = recruitmentRepository.findAllOpen()

    /**
     * 특정 상태의 모집 공고를 조회합니다.
     */
    fun getByStatus(status: RecruitmentStatus): List<TeamRecruitment> = recruitmentRepository.findByStatus(status)

    /**
     * 모집 공고를 삭제합니다.
     */
    @Transactional
    fun deleteRecruitment(
        id: Long,
        teamId: Long
    ) {
        val recruitment = getById(id)
        verifyTeamOwnership(recruitment, teamId)
        recruitmentRepository.delete(recruitment)
    }

    private fun verifyTeamOwnership(
        recruitment: TeamRecruitment,
        teamId: Long,
    ) {
        if (recruitment.team.id != teamId) {
            throw ForbiddenException(
                "RECRUITMENT_NOT_OWNED",
                "해당 팀의 모집 공고가 아닙니다",
            )
        }
    }
}
