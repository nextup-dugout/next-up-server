package com.nextup.core.domain.event

import com.nextup.core.domain.election.ElectionType

/**
 * 재선거(결선투표) 생성 이벤트
 *
 * 동률 발생으로 재선거가 자동 생성되었을 때 발행됩니다.
 * 리스너에서 팀 멤버에게 재선거 알림을 발송하는 데 활용됩니다.
 */
data class RunoffElectionCreatedEvent(
    val runoffElectionId: Long,
    val parentElectionId: Long,
    val teamId: Long,
    val electionType: ElectionType,
    val tiedCandidateCount: Int,
)
