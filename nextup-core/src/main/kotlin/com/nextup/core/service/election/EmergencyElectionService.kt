package com.nextup.core.service.election

import com.nextup.common.exception.ActiveElectionAlreadyExistsException
import com.nextup.common.exception.ElectionNotFoundException
import com.nextup.common.exception.InvalidActingOwnerException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.TeamMemberNotFoundException
import com.nextup.common.exception.UnauthorizedEmergencyElectionException
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.election.dto.DesignateActingOwnerRequest
import com.nextup.core.service.election.dto.ElectionResponse
import com.nextup.core.service.election.dto.TriggerEmergencyElectionRequest
import com.nextup.core.service.election.dto.toResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 비상대책위원회 모드 (긴급 선거) 서비스
 *
 * 구단주 부재 시 MANAGER가 긴급 선거를 발동하고, 임시 구단주를 지정합니다.
 * 긴급 선거 발동 후 14일 이내 정규 구단주 선거를 자동 생성합니다.
 */
@Service
@Transactional(readOnly = true)
class EmergencyElectionService(
    private val electionRepository: ElectionRepositoryPort,
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    /**
     * 긴급 선거를 발동합니다.
     *
     * MANAGER 역할의 멤버만 발동 가능하며, 이미 진행 중인 선거가 없어야 합니다.
     * 긴급 선거는 발동 즉시 IN_PROGRESS 상태로 시작합니다.
     *
     * @param request 긴급 선거 발동 요청
     * @return 생성된 긴급 선거 응답
     * @throws UnauthorizedEmergencyElectionException 요청자가 MANAGER 역할이 아닌 경우
     * @throws ActiveElectionAlreadyExistsException 이미 진행 중인 선거가 있는 경우
     */
    @Transactional
    fun triggerEmergencyElection(request: TriggerEmergencyElectionRequest): ElectionResponse {
        val requester =
            teamMemberRepository.findByIdOrNull(request.requesterId)
                ?: throw TeamMemberNotFoundException(request.requesterId)

        if (requester.role != TeamMemberRole.MANAGER) {
            throw UnauthorizedEmergencyElectionException(request.requesterId)
        }

        val activeElections =
            electionRepository.findAllByTeamId(request.teamId)
                .filter {
                    it.status == ElectionStatus.IN_PROGRESS ||
                        it.status == ElectionStatus.SCHEDULED
                }
        if (activeElections.isNotEmpty()) {
            throw ActiveElectionAlreadyExistsException(request.teamId)
        }

        val emergencyElection =
            Election.createEmergency(
                teamId = request.teamId,
                triggeredByMemberId = request.requesterId,
                title = request.title,
                description = request.description,
                startAt = request.startAt,
                endAt = request.endAt,
            )

        return electionRepository.save(emergencyElection).toResponse()
    }

    /**
     * 임시 구단주를 지정합니다.
     *
     * 긴급 선거에서만 가능하며, 대상은 MANAGER 역할의 팀 멤버여야 합니다.
     * 임시 구단주는 제한된 권한(라인업/일정 관리 가능, 강퇴/해산/소유권 이전 불가)을 갖습니다.
     *
     * @param request 임시 구단주 지정 요청
     * @return 업데이트된 선거 응답
     * @throws ElectionNotFoundException 선거를 찾을 수 없는 경우
     * @throws InvalidStateException 긴급 선거가 아닌 경우
     * @throws InvalidActingOwnerException 대상 멤버가 MANAGER 역할이 아닌 경우
     */
    @Transactional
    fun designateActingOwner(request: DesignateActingOwnerRequest): ElectionResponse {
        val election =
            electionRepository.findById(request.electionId)
                ?: throw ElectionNotFoundException(request.electionId)

        if (!election.isEmergency()) {
            throw InvalidStateException(
                code = "NOT_EMERGENCY_ELECTION",
                message = "Election ${request.electionId} is not an emergency election",
            )
        }

        val actingOwnerCandidate =
            teamMemberRepository.findByIdOrNull(request.actingOwnerMemberId)
                ?: throw TeamMemberNotFoundException(request.actingOwnerMemberId)

        if (actingOwnerCandidate.role != TeamMemberRole.MANAGER) {
            throw InvalidActingOwnerException(request.actingOwnerMemberId)
        }

        election.designateActingOwner(request.actingOwnerMemberId)

        return electionRepository.save(election).toResponse()
    }

    /**
     * 긴급 선거 완료 후 정규 구단주 선거를 자동 생성합니다.
     *
     * 긴급 선거 완료 시 호출되며, regularElectionDeadline 내에 시작하는 정규 OWNER_ELECTION을 생성합니다.
     *
     * @param emergencyElection 완료된 긴급 선거
     * @return 생성된 정규 선거 응답
     * @throws InvalidStateException 긴급 선거가 아니거나 완료 상태가 아닌 경우
     */
    @Transactional
    fun createRegularElectionAfterEmergency(emergencyElection: Election): ElectionResponse {
        require(emergencyElection.isEmergency()) {
            "정규 선거 자동 생성은 긴급 선거 완료 후에만 가능합니다."
        }
        val deadline =
            checkNotNull(emergencyElection.regularElectionDeadline) {
                "긴급 선거에 정규 선거 마감 기한이 설정되어 있지 않습니다."
            }
        val regularElection =
            Election.create(
                teamId = emergencyElection.teamId,
                title = "[정규] 구단주 선출 선거",
                description =
                    "비상대책위원회 모드 해제 후 정규 구단주 선출 선거입니다. " +
                        "마감 기한: $deadline",
                electionType = ElectionType.OWNER_ELECTION,
                startAt = deadline.minusSeconds(7 * 24 * 60 * 60),
                endAt = deadline,
            )

        return electionRepository.save(regularElection).toResponse()
    }
}
