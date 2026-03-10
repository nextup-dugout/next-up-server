package com.nextup.core.service.election

import com.nextup.common.exception.CandidateNotFoundException
import com.nextup.common.exception.DuplicateVoteException
import com.nextup.common.exception.ElectionNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.domain.election.ElectionVote
import com.nextup.core.domain.team.TeamMemberRole
import com.nextup.core.port.repository.CandidateRepositoryPort
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.port.repository.TeamMemberRepositoryPort
import com.nextup.core.service.election.dto.CandidateResult
import com.nextup.core.service.election.dto.CastVoteRequest
import com.nextup.core.service.election.dto.CreateElectionRequest
import com.nextup.core.service.election.dto.ElectionResult
import com.nextup.core.service.election.dto.RegisterCandidateRequest
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
    private val teamMemberRepository: TeamMemberRepositoryPort,
) {
    /**
     * 선거를 생성합니다.
     */
    @Transactional
    fun createElection(request: CreateElectionRequest): Election {
        val election =
            Election.create(
                teamId = request.teamId,
                title = request.title,
                description = request.description,
                electionType = request.electionType,
                startAt = request.startAt,
                endAt = request.endAt,
            )
        return electionRepository.save(election)
    }

    /**
     * 선거를 시작합니다.
     */
    @Transactional
    fun startElection(electionId: Long): Election {
        val election = findElection(electionId)
        election.start()
        return election
    }

    /**
     * 선거를 완료합니다.
     * OWNER_ELECTION인 경우, 단독 최다 득표자에게 자동으로 OWNER 권한을 이양합니다.
     * 동률이거나 투표가 없으면 자동 이양하지 않습니다.
     */
    @Transactional
    fun completeElection(electionId: Long): Election {
        val election = findElection(electionId)
        election.complete()

        if (election.electionType == ElectionType.OWNER_ELECTION) {
            transferOwnershipToWinner(election)
        }

        return election
    }

    /**
     * OWNER 선거 완료 후 당선자에게 OWNER 권한을 자동 이양합니다.
     * 동률이거나 투표가 없으면 이양하지 않습니다.
     */
    private fun transferOwnershipToWinner(election: Election) {
        val voteCounts =
            electionVoteRepository.countByElectionIdGroupByCandidateId(election.id)

        // 투표가 없으면 이양하지 않음
        if (voteCounts.isEmpty()) return

        // 최다 득표수 확인
        val maxVotes = voteCounts.values.max()
        val topCandidates = voteCounts.filter { it.value == maxVotes }

        // 동률이면 이양하지 않음
        if (topCandidates.size > 1) return

        val winnerCandidateId = topCandidates.keys.first()
        val winnerCandidate =
            candidateRepository.findById(winnerCandidateId) ?: return

        val winner =
            teamMemberRepository.findByIdOrNull(winnerCandidate.memberId) ?: return

        // 당선자가 이미 OWNER이면 이양 불필요
        if (winner.role == TeamMemberRole.OWNER) return

        // 기존 OWNER를 MEMBER로 강등
        val teamMembers =
            teamMemberRepository.findByTeamId(election.teamId)
        val currentOwner =
            teamMembers.firstOrNull { it.role == TeamMemberRole.OWNER }

        if (currentOwner != null) {
            currentOwner.role = TeamMemberRole.MEMBER
            teamMemberRepository.save(currentOwner)
        }

        // 당선자를 OWNER로 승격
        winner.role = TeamMemberRole.OWNER
        teamMemberRepository.save(winner)
    }

    /**
     * 선거를 취소합니다.
     */
    @Transactional
    fun cancelElection(electionId: Long): Election {
        val election = findElection(electionId)
        election.cancel()
        return election
    }

    /**
     * 후보자를 등록합니다.
     */
    @Transactional
    fun registerCandidate(request: RegisterCandidateRequest): Candidate {
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
        return candidateRepository.save(candidate)
    }

    /**
     * 투표합니다.
     */
    @Transactional
    fun vote(request: CastVoteRequest): Candidate {
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

        return candidate
    }

    /**
     * 선거 결과를 조회합니다.
     */
    fun getResults(electionId: Long): ElectionResult {
        val election = findElection(electionId)
        val candidates = candidateRepository.findAllByElectionId(electionId)
        val voteCounts = electionVoteRepository.countByElectionIdGroupByCandidateId(electionId)

        val candidateResults =
            candidates.map { candidate ->
                CandidateResult(
                    candidate = candidate,
                    voteCount = voteCounts[candidate.id] ?: 0,
                )
            }.sortedByDescending { it.voteCount }

        val totalVotes = voteCounts.values.sum()

        return ElectionResult(
            election = election,
            candidateResults = candidateResults,
            totalVotes = totalVotes,
        )
    }

    /**
     * 팀의 모든 선거를 조회합니다.
     */
    fun getElectionsByTeam(teamId: Long): List<Election> = electionRepository.findAllByTeamId(teamId)

    /**
     * 선거를 ID로 조회합니다.
     */
    fun getElectionById(electionId: Long): Election = findElection(electionId)

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
