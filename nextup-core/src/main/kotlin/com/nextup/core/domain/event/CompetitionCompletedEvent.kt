package com.nextup.core.domain.event

/**
 * L-9: 대회 완료 이벤트
 *
 * 대회의 모든 경기가 종료되어 대회가 완료되었을 때 발행됩니다.
 * 대회 완료 알림 발송에 사용됩니다.
 */
data class CompetitionCompletedEvent(
    val competitionId: Long,
    val competitionName: String,
    val leagueId: Long,
)
