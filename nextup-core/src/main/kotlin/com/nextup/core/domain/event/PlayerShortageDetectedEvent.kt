package com.nextup.core.domain.event

/**
 * 인원 부족 감지 도메인 이벤트
 *
 * 선수 퇴장/부상 퇴장 후 팀의 활동 선수 수가 최소 인원 미만으로 떨어졌을 때 발행됩니다.
 * 기록원에게 몰수패 처리 여부를 판단하도록 알림을 전송합니다.
 *
 * @param gameId 경기 ID
 * @param gameTeamId 인원 부족 팀의 GameTeam ID
 * @param teamId 인원 부족 팀 ID
 * @param activePlayerCount 현재 활동 선수 수
 * @param minimumRequired 최소 필요 인원
 */
data class PlayerShortageDetectedEvent(
    val gameId: Long,
    val gameTeamId: Long,
    val teamId: Long,
    val activePlayerCount: Int,
    val minimumRequired: Int,
)
