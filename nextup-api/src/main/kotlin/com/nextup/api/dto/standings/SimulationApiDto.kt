package com.nextup.api.dto.standings

import com.nextup.core.service.standings.dto.MagicNumber
import com.nextup.core.service.standings.dto.PlayoffScenarioResult
import com.nextup.core.service.standings.dto.RankChange
import com.nextup.core.service.standings.dto.SimulationResult
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull

// ============================================================
// Request DTOs
// ============================================================

/**
 * 순위 시뮬레이션 요청
 */
data class SimulationApiRequest(
    @field:NotNull
    @field:Valid
    val gameResults: List<SimulatedGameResultRequest>,
)

/**
 * 시뮬레이션용 경기 결과 요청
 */
data class SimulatedGameResultRequest(
    @field:NotNull
    val gameId: Long,
    @field:NotNull
    @field:Min(0)
    val homeScore: Int,
    @field:NotNull
    @field:Min(0)
    val awayScore: Int,
)

// ============================================================
// Response DTOs
// ============================================================

/**
 * 매직넘버 응답
 */
data class MagicNumberResponse(
    val teamId: Long,
    val targetRank: Int,
    val magicNumber: Int,
    val isClinched: Boolean,
    val isEliminated: Boolean,
) {
    companion object {
        fun from(dto: MagicNumber): MagicNumberResponse =
            MagicNumberResponse(
                teamId = dto.teamId,
                targetRank = dto.targetRank,
                magicNumber = dto.magicNumber,
                isClinched = dto.isClinched,
                isEliminated = dto.isEliminated,
            )
    }
}

/**
 * 순위 변동 응답
 */
data class RankChangeResponse(
    val teamId: Long,
    val teamName: String,
    val previousRank: Int,
    val projectedRank: Int,
    val rankChange: Int,
) {
    companion object {
        fun from(dto: RankChange): RankChangeResponse =
            RankChangeResponse(
                teamId = dto.teamId,
                teamName = dto.teamName,
                previousRank = dto.previousRank,
                projectedRank = dto.projectedRank,
                rankChange = dto.rankChange,
            )
    }
}

/**
 * 시뮬레이션 결과 응답
 */
data class SimulationResultResponse(
    val standings: List<TeamStandingResponse>,
    val changes: List<RankChangeResponse>,
) {
    companion object {
        fun from(dto: SimulationResult): SimulationResultResponse =
            SimulationResultResponse(
                standings = dto.standings.map { TeamStandingResponse.from(it) },
                changes = dto.changes.map { RankChangeResponse.from(it) },
            )
    }
}

/**
 * 플레이오프 시나리오 결과 응답
 */
data class PlayoffScenarioResponse(
    val totalScenarios: Int,
    val qualifyingScenarios: Int,
    val probability: Double,
    val magicNumber: Int?,
) {
    companion object {
        fun from(dto: PlayoffScenarioResult): PlayoffScenarioResponse =
            PlayoffScenarioResponse(
                totalScenarios = dto.totalScenarios,
                qualifyingScenarios = dto.qualifyingScenarios,
                probability = dto.probability,
                magicNumber = dto.magicNumber,
            )
    }
}
