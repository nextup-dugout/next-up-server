package com.nextup.core.domain.event

import com.nextup.core.domain.election.ElectionType

/**
 * 선거 완료 이벤트
 *
 * 선거가 완료되었을 때 발행됩니다.
 * OWNER_ELECTION인 경우, 리스너에서 당선자에게 OWNER 권한을 이양합니다.
 */
data class ElectionCompletedEvent(
    val electionId: Long,
    val teamId: Long,
    val electionType: ElectionType,
)
