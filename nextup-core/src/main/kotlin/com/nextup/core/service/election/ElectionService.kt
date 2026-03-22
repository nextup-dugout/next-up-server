package com.nextup.core.service.election

import com.nextup.common.exception.CandidateNotFoundException
import com.nextup.common.exception.DuplicateVoteException
import com.nextup.common.exception.ElectionNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionVote
import com.nextup.core.domain.event.ElectionCompletedEvent
import com.nextup.core.domain.event.RunoffElectionCreatedEvent
import com.nextup.core.port.repository.CandidateRepositoryPort
import com.nextup.core.port.repository.ElectionRepositoryPort
import com.nextup.core.port.repository.ElectionVoteRepositoryPort
import com.nextup.core.service.election.dto.CandidateResult
import com.nextup.core.service.election.dto.CastVoteRequest
import com.nextup.core.service.election.dto.CreateElectionRequest
import com.nextup.core.service.election.dto.ElectionResult
import com.nextup.core.service.election.dto.RegisterCandidateRequest
import org.springframework.context.ApplicationEventPublisher
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
    private val eventPublisher: ApplicationEventPublisher,
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
     * 도메인 이벤트를 발행하여 OWNER_ELECTION의 경우 리스너에서 권한 이양을 처리합니다.
     */
    @Transactional
    fun completeElection(electionId: Long): Election {
        val election = findElection(electionId)
        election.complete()

        eventPublisher.publishEvent(
            ElectionCompletedEvent(
                electionId = election.id,
                teamId = election.teamId,
                electionType = election.electionType,
            ),
        )

        return election
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
     * 재선거(결선투표)를 생성합니다.
     *
     * 동률이 발생한 원본 선거를 기반으로, 동률 후보들만 포함하는 재선거를 생성합니다.
     * 최대 재선거 횟수(Election.MAX_RUNOFF_COUNT)를 초과하면 예외가 발생합니다.
     *
     * @param parentElectionId 원본 선거 ID
     * @param tiedCandidateIds 동률 후보자 ID 목록
     * @return 생성된 재선거
     */
    @Transactional
    fun createRunoffElection(
        parentElectionId: Long,
        tiedCandidateIds: List<Long>,
    ): Election {
        val parentElection = findElection(parentElectionId)

        // 재선거 체인의 루트 선거를 찾아 총 재선거 횟수를 계산
        val rootElectionId = findRootElectionId(parentElection)
        val currentRunoffCount =
            electionRepository.countByParentElectionId(rootElectionId) +
                if (parentElection.isRunoff) {
                    electionRepository.countByParentElectionId(parentElectionId)
                } else {
                    0L
                }

        val runoffElection =
            Election.createRunoff(
                parentElection = parentElection,
                currentRunoffCount = currentRunoffCount,
            )
        val savedRunoff = electionRepository.save(runoffElection)

        // 동률 후보들을 재선거에 등록
        val originalCandidates =
            tiedCandidateIds.mapNotNull { candidateRepository.findById(it) }
        for (candidate in originalCandidates) {
            val runoffCandidate =
                Candidate.create(
                    electionId = savedRunoff.id,
                    memberId = candidate.memberId,
                    memberName = candidate.memberName,
                    statement = candidate.statement,
                )
            candidateRepository.save(runoffCandidate)
        }

        eventPublisher.publishEvent(
            RunoffElectionCreatedEvent(
                runoffElectionId = savedRunoff.id,
                parentElectionId = parentElectionId,
                teamId = savedRunoff.teamId,
                electionType = savedRunoff.electionType,
                tiedCandidateCount = originalCandidates.size,
            ),
        )

        return savedRunoff
    }

    /**
     * 재선거 체인의 루트(최초 원본) 선거 ID를 찾습니다.
     */
    private fun findRootElectionId(election: Election): Long {
        var current = election
        while (current.parentElectionId != null) {
            current =
                electionRepository.findById(current.parentElectionId!!)
                    ?: break
        }
        return current.id
    }

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
