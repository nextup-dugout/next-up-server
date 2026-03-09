package com.nextup.api.dto.election

import com.nextup.core.domain.election.ActingOwnerPermissions
import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.Election
import com.nextup.core.domain.election.ElectionStatus
import com.nextup.core.domain.election.ElectionType
import com.nextup.core.service.election.dto.CandidateVoteCount
import com.nextup.core.service.election.dto.ElectionResult
import java.time.Instant

/**
 * 선거 응답 DTO
 */
data class ElectionResponse(
    val id: Long,
    val teamId: Long,
    val title: String,
    val description: String?,
    val electionType: ElectionType,
    val startAt: Instant,
    val endAt: Instant,
    val status: ElectionStatus,
    val isVotingOpen: Boolean,
    val createdAt: Instant,
    /** 긴급 선거 발동 멤버 ID (EMERGENCY 타입만 사용) */
    val triggeredByMemberId: Long? = null,
    /** 임시 구단주 멤버 ID (EMERGENCY 타입만 사용) */
    val actingOwnerMemberId: Long? = null,
    /** 임시 구단주 권한 (EMERGENCY 타입만 사용) */
    val actingOwnerPermissions: ActingOwnerPermissions? = null,
    /** 정규 선거 마감 기한 (EMERGENCY 타입만 사용) */
    val regularElectionDeadline: Instant? = null,
)

/**
 * 후보자 응답 DTO
 */
data class CandidateResponse(
    val id: Long,
    val electionId: Long,
    val memberId: Long,
    val memberName: String,
    val statement: String?,
    val createdAt: Instant,
)

/**
 * 선거 결과 응답 DTO
 */
data class ElectionResultResponse(
    val election: ElectionResponse,
    val candidates: List<CandidateResultResponse>,
    val totalVotes: Long,
)

/**
 * 후보자 결과 응답 DTO
 */
data class CandidateResultResponse(
    val candidate: CandidateResponse,
    val voteCount: Long,
)

/**
 * Election을 ElectionResponse로 변환합니다.
 */
fun Election.toResponse(): ElectionResponse =
    ElectionResponse(
        id = this.id,
        teamId = this.teamId,
        title = this.title,
        description = this.description,
        electionType = this.electionType,
        startAt = this.startAt,
        endAt = this.endAt,
        status = this.status,
        isVotingOpen = this.isVotingOpen(),
        createdAt = this.createdAt,
        triggeredByMemberId = this.triggeredByMemberId,
        actingOwnerMemberId = this.actingOwnerMemberId,
        actingOwnerPermissions = this.actingOwnerPermissions,
        regularElectionDeadline = this.regularElectionDeadline,
    )

/**
 * Election 리스트를 ElectionResponse 리스트로 변환합니다.
 */
fun List<Election>.toElectionResponseList(): List<ElectionResponse> = this.map { it.toResponse() }

/**
 * Candidate를 CandidateResponse로 변환합니다.
 */
fun Candidate.toResponse(): CandidateResponse =
    CandidateResponse(
        id = this.id,
        electionId = this.electionId,
        memberId = this.memberId,
        memberName = this.memberName,
        statement = this.statement,
        createdAt = this.createdAt,
    )

/**
 * ElectionResult를 ElectionResultResponse로 변환합니다.
 */
fun ElectionResult.toResponse(): ElectionResultResponse =
    ElectionResultResponse(
        election = this.election.toResponse(),
        candidates =
            this.candidateVoteCounts.map { it.toResponse() },
        totalVotes = this.totalVotes,
    )

/**
 * CandidateVoteCount를 CandidateResultResponse로 변환합니다.
 */
fun CandidateVoteCount.toResponse(): CandidateResultResponse =
    CandidateResultResponse(
        candidate = this.candidate.toResponse(),
        voteCount = this.voteCount,
    )
