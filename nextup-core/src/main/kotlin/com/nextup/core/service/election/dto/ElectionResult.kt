package com.nextup.core.service.election.dto

import com.nextup.core.domain.election.Candidate
import com.nextup.core.domain.election.Election

/**
 * 선거 결과 도메인 읽기 모델
 *
 * 선거와 후보자별 득표 현황을 담는 서비스 계층 반환 객체입니다.
 */
data class ElectionResult(
    val election: Election,
    val candidateVoteCounts: List<CandidateVoteCount>,
    val totalVotes: Long,
)

/**
 * 후보자별 득표 현황
 */
data class CandidateVoteCount(
    val candidate: Candidate,
    val voteCount: Long,
)
