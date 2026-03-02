package com.nextup.core.service.player

import com.nextup.core.service.player.dto.PlayerDashboardDto

/**
 * 선수 대시보드 통합 조회 서비스 인터페이스
 *
 * 프론트엔드 선수 프로필 화면에 필요한 모든 데이터를 한 번의 호출로 제공합니다.
 */
interface PlayerDashboardService {
    /**
     * 선수 대시보드 데이터를 조회합니다.
     *
     * @param playerId 선수 ID
     * @return 선수 대시보드 통합 데이터
     */
    fun getPlayerDashboard(playerId: Long): PlayerDashboardDto
}
