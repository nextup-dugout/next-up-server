package com.nextup.core.domain.event

import com.nextup.core.domain.game.PlateAppearanceResult
import java.time.Instant

/**
 * 타석 결과 기록 도메인 이벤트
 *
 * 경기 중 타석 결과가 기록될 때 발행됩니다.
 * 시즌 타격 통계와 투수 통계를 실시간으로 갱신하기 위해 사용됩니다.
 *
 * @param gameId 경기 ID
 * @param playerId 타자 선수 ID
 * @param pitcherId 투수 선수 ID (투수 시즌 통계 실시간 갱신용)
 * @param batterTeamId 타자 소속 팀 ID (팀별 시즌 통계 분리용)
 * @param pitcherTeamId 투수 소속 팀 ID (팀별 시즌 통계 분리용)
 * @param result 타석 결과
 * @param timestamp 이벤트 발생 시각
 */
data class PlateAppearanceRecordedEvent(
    val gameId: Long,
    val playerId: Long,
    val pitcherId: Long,
    val batterTeamId: Long,
    val pitcherTeamId: Long,
    val result: PlateAppearanceResult,
    val timestamp: Instant = Instant.now(),
)
