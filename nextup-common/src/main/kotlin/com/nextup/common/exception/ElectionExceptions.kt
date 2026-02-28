package com.nextup.common.exception

/**
 * 선거를 찾을 수 없을 때 발생하는 예외
 */
class ElectionNotFoundException(
    electionId: Long,
) : NotFoundException(
        code = "ELECTION_NOT_FOUND",
        message = "Election not found: $electionId",
    )

/**
 * 후보자를 찾을 수 없을 때 발생하는 예외
 */
class CandidateNotFoundException(
    candidateId: Long,
) : NotFoundException(
        code = "CANDIDATE_NOT_FOUND",
        message = "Candidate not found: $candidateId",
    )

/**
 * 중복 투표 시 발생하는 예외
 */
class DuplicateVoteException(
    code: String = "DUPLICATE_VOTE",
    message: String = "User has already voted in this election",
) : BusinessException(code, message)

/**
 * 긴급 선거 발동 권한이 없을 때 발생하는 예외
 * MANAGER 역할이 아닌 멤버가 긴급 선거를 발동하려 할 때 발생합니다.
 */
class UnauthorizedEmergencyElectionException(
    memberId: Long,
) : ForbiddenException(
        code = "UNAUTHORIZED_EMERGENCY_ELECTION",
        message = "Member $memberId does not have MANAGER role to trigger emergency election",
    )

/**
 * 이미 진행 중인 선거가 있을 때 발생하는 예외
 */
class ActiveElectionAlreadyExistsException(
    teamId: Long,
) : InvalidStateException(
        code = "ACTIVE_ELECTION_ALREADY_EXISTS",
        message = "Team $teamId already has an active election in progress",
    )

/**
 * 임시 구단주 지정 대상이 유효하지 않을 때 발생하는 예외
 * MANAGER 역할이 아닌 멤버를 임시 구단주로 지정하려 할 때 발생합니다.
 */
class InvalidActingOwnerException(
    memberId: Long,
) : InvalidInputException(
        code = "INVALID_ACTING_OWNER",
        message = "Member $memberId is not eligible to be designated as acting owner (must be MANAGER role)",
    )
