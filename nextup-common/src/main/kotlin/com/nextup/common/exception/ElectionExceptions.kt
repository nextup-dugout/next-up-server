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
