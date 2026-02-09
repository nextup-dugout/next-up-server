package com.nextup.core.service.election

import com.nextup.common.exception.CandidateNotFoundException
import com.nextup.common.exception.DuplicateVoteException
import com.nextup.common.exception.ElectionNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionVote
import com.nextup.core.port.repository.CandidateRepositoryPort
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.service.election.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Election Service
 *
 * 팀 내 선거/투표 비즈니스 로직을 처리합니다.
 */
@Service
@Transactional(readOnly = true)
class ElectionService(
    private val electionRepository: ElectionRepositoryPort,
    private val candidateRepository: CandidateRepositoryPort,
    private val electionVoteRepository: ElectionVoteRepositoryPort,
) {
    /**
     * 선거를 생성합니다.
     */
    @Transactional
    fun createElection(request: CreateElectionRequest): ElectionResponse {
        val election =
            Election.create(
                teamId = request.teamId,
                title = request.title,
                description = request.description,
                electionType = request.electionType,
                startAt = request.startAt,
                endAt = request.endAt,
            )
        return electionRepository.save(election).toResponse()
    }

    /**
     * 선거를 시작합니다.
     */
    @Transactional
    fun startElection(electionId: Long): ElectionResponse {
        val election = findElection(electionId)
        election.start()
        return election.toResponse()
    }

    /**
     * 선거를 완료합니다.
     */
    @Transactional
    fun completeElection(electionId: Long): ElectionResponse {
        val election = findElection(electionId)
        election.complete()
        return election.toResponse()
    }

    /**
     * 선거를 취소합니다.
     */
    @Transactional
    fun cancelElection(electionId: Long): ElectionResponse {
        val election = findElection(electionId)
        election.cancel()
        return election.toResponse()
    }

    /**
     * 후보자를 등록합니다.
     */
    @Transactional
    fun registerCandidate(request: RegisterCandidateRequest): CandidateResponse {
        val election = findElection(request.electionId)

        // 선거가 진행 중이거나 완료된 경우 후보자 등록 불가
        if (election.status != com.nextup.core.domain.election.ElectionStatus.SCHEDULED) {
            throw InvalidStateException(
                code = "CANNOT_REGISTER_CANDIDATE",
                message = "Cannot register candidate: election status is ${election.status}",
            )
        }

        // 이미 등록된 후보자인지 확인
        val existingCandidate =
            candidateRepository.findByElectionIdAndMemberId(
                request.electionId,
                request.memberId,
            )
        if (existingCandidate != null) {
            throw InvalidStateException(
                code = "CANDIDATE_ALREADY_REGISTERED",
                message = "Member ${request.memberId} is already registered as a candidate",
            )
        }

        val candidate =
            Candidate.create(
                electionId = request.electionId,
                memberId = request.memberId,
                memberName = request.memberName,
                statement = request.statement,
            )
        return candidateRepository.save(candidate).toResponse()
    }

    /**
     * 투표합니다.
     */
    @Transactional
    fun vote(request: CastVoteRequest): CandidateResponse {
        val election = findElection(request.electionId)

        // 투표 가능 여부 확인
        if (!election.isVotingOpen()) {
            throw InvalidStateException(
                code = "VOTING_NOT_OPEN",
                message = "Voting is not open for this election",
            )
        }

        // 이미 투표했는지 확인
        val existingVote =
            electionVoteRepository.findByElectionIdAndVoterId(
                request.electionId,
                request.voterId,
            )
        if (existingVote != null) {
            throw DuplicateVoteException()
        }

        // 후보자 존재 여부 확인
        val candidate = findCandidate(request.candidateId)
        if (candidate.electionId != request.electionId) {
            throw InvalidStateException(
                code = "CANDIDATE_NOT_IN_ELECTION",
                message = "Candidate ${request.candidateId} is not in election ${request.electionId}",
            )
        }

        val vote =
            ElectionVote.create(
                electionId = request.electionId,
                voterId = request.voterId,
                candidateId = request.candidateId,
            )
        electionVoteRepository.save(vote)

        return candidate.toResponse()
    }

    /**
     * 선거 결과를 조회합니다.
     */
    fun getResults(electionId: Long): ElectionResultResponse {
        val election = findElection(electionId)
        val candidates = candidateRepository.findAllByElectionId(electionId)
        val voteCounts = electionVoteRepository.countByElectionIdGroupByCandidateId(electionId)

        val candidateResults =
            candidates.map { candidate ->
                CandidateResultResponse(
                    candidate = candidate.toResponse(),
                    voteCount = voteCounts[candidate.id] ?: 0,
                )
            }.sortedByDescending { it.voteCount }

        val totalVotes = voteCounts.values.sum()

        return ElectionResultResponse(
            election = election.toResponse(),
            candidates = candidateResults,
            totalVotes = totalVotes,
        )
    }

    /**
     * 팀의 모든 선거를 조회합니다.
     */
    fun getElectionsByTeam(teamId: Long): List<ElectionResponse> =
        electionRepository.findAllByTeamId(teamId).toResponse()

    /**
     * 선거를 ID로 조회합니다.
     */
    fun getElectionById(electionId: Long): ElectionResponse = findElection(electionId).toResponse()

    /**
     * Election을 조회하고 없으면 예외를 던집니다.
     */
    private fun findElection(electionId: Long): Election =
        electionRepository.findById(electionId)
            ?: throw ElectionNotFoundException(electionId)

    /**
     * Candidate를 조회하고 없으면 예외를 던집니다.
     */
    private fun findCandidate(candidateId: Long): Candidate =
        candidateRepository.findById(candidateId)
            ?: throw CandidateNotFoundException(candidateId)
}
