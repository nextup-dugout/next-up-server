package com.nextup.core.service.team

import com.nextup.core.service.team.dto.TeamDashboardDto

/**
 * 팀 대시보드 통합 서비스 인터페이스
 *
 * 팀 홈 화면에 필요한 데이터(다음 경기, 최근 결과, 순위, 출석 현황 등)를
 * 한 번에 제공합니다.
 */
interface TeamDashboardService {
    /**
     * 팀 대시보드 데이터를 조회합니다.
     *
     * @param teamId 팀 ID
     * @return 팀 대시보드 통합 데이터
     * @throws TeamNotFoundException 팀을 찾을 수 없는 경우
     */
    fun getTeamDashboard(teamId: Long): TeamDashboardDto
}
