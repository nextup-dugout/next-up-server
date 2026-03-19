package com.nextup.core.domain.event

import com.nextup.core.domain.election.ElectionType

/**
 * 선거 동률 이벤트
 *
 * 선거 결과가 동률로 당선자를 결정할 수 없을 때 발행됩니다.
 * 리스너에서 재선거 자동 생성 또는 관리자 알림 발행에 활용됩니다.
 */
data class ElectionTiedEvent(
    val electionId: Long,
    val teamId: Long,
    val electionType: ElectionType,
    val tiedCandidateCount: Int,
    val tiedVoteCount: Long,
)
