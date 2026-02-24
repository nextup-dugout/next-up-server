package com.nextup.core.service.standings.dto

/**
 * 시뮬레이션 요청 DTO
 *
 * @param gameResults 가상 경기 결과 목록
 */
data class SimulationRequest(
    val gameResults: List<SimulatedGameResult>,
)

/**
 * 시뮬레이션할 경기 결과
 *
 * @param gameId 경기 ID
 * @param homeScore 홈팀 점수
 * @param awayScore 원정팀 점수
 */
data class SimulatedGameResult(
    val gameId: Long,
    val homeScore: Int,
    val awayScore: Int,
)

/**
 * 순위 변동 정보
 *
 * @param teamId 팀 ID
 * @param teamName 팀명
 * @param previousRank 시뮬레이션 전 순위
 * @param projectedRank 시뮬레이션 후 예상 순위
 * @param rankChange 순위 변동 (양수 = 상승, 음수 = 하락)
 */
data class RankChange(
    val teamId: Long,
    val teamName: String,
    val previousRank: Int,
    val projectedRank: Int,
    val rankChange: Int,
)

/**
 * 시뮬레이션 결과 DTO
 *
 * @param standings 시뮬레이션 후 예상 순위표
 * @param changes 순위 변동 목록
 */
data class SimulationResult(
    val standings: List<TeamStandingDto>,
    val changes: List<RankChange>,
)

/**
 * 플레이오프 진출 시나리오 결과
 *
 * @param totalScenarios 총 시나리오 수
 * @param qualifyingScenarios 플레이오프 진출 시나리오 수
 * @param probability 플레이오프 진출 확률 (0.0 ~ 1.0)
 * @param magicNumber 플레이오프 진출 확정을 위한 매직넘버 (null이면 이미 확정 또는 불가)
 */
data class PlayoffScenarioResult(
    val totalScenarios: Int,
    val qualifyingScenarios: Int,
    val probability: Double,
    val magicNumber: Int?,
)
