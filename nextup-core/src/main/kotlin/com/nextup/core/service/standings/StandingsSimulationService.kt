package com.nextup.core.service.standings

import com.nextup.core.service.standings.dto.MagicNumber
import com.nextup.core.service.standings.dto.PlayoffScenarioResult
import com.nextup.core.service.standings.dto.SimulationRequest
import com.nextup.core.service.standings.dto.SimulationResult

/**
 * 순위 시뮬레이션 서비스
 */
interface StandingsSimulationService {
    /**
     * 대회의 각 팀별 매직넘버를 계산합니다.
     *
     * @param competitionId 대회 ID
     * @return 팀별 매직넘버 목록
     */
    fun calculateMagicNumbers(competitionId: Long): List<MagicNumber>

    /**
     * 가상의 경기 결과를 적용하여 예상 순위표를 반환합니다.
     *
     * @param competitionId 대회 ID
     * @param request 시뮬레이션 요청 (가상 경기 결과 목록)
     * @return 시뮬레이션 결과 (예상 순위표 및 순위 변동)
     */
    fun simulateStandings(
        competitionId: Long,
        request: SimulationRequest,
    ): SimulationResult

    /**
     * 특정 팀의 플레이오프 진출 시나리오를 계산합니다.
     *
     * 남은 경기가 15경기를 초과하면 몬테카를로 시뮬레이션(1000회 반복)을 사용합니다.
     *
     * @param competitionId 대회 ID
     * @param teamId 대상 팀 ID
     * @param playoffTeams 플레이오프 진출 팀 수
     * @return 플레이오프 진출 시나리오 결과
     */
    fun calculatePlayoffScenarios(
        competitionId: Long,
        teamId: Long,
        playoffTeams: Int,
    ): PlayoffScenarioResult
}
