package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.service.stats.MatchupService
import com.nextup.core.service.stats.dto.MatchupDto
import com.nextup.core.service.stats.dto.MatchupHistoryDto
import com.nextup.core.service.stats.dto.MatchupStatsDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

@Service
@Transactional(readOnly = true)
class MatchupServiceImpl(
    private val gameEventRepositoryPort: GameEventRepositoryPort,
    private val playerRepositoryPort: PlayerRepositoryPort,
) : MatchupService {
    override fun getMatchup(
        pitcherId: Long,
        batterId: Long,
        year: Int?,
        competitionId: Long?,
    ): MatchupDto {
        // 선수 존재 여부 확인
        val pitcher =
            playerRepositoryPort.findByIdOrNull(pitcherId)
                ?: throw PlayerNotFoundException(pitcherId)
        val batter =
            playerRepositoryPort.findByIdOrNull(batterId)
                ?: throw PlayerNotFoundException(batterId)

        // 매치업 이벤트 조회
        val events =
            if (year != null) {
                gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatterAndYear(
                    pitcherId,
                    batterId,
                    year,
                )
            } else {
                gameEventRepositoryPort.findPlateAppearancesByPitcherAndBatter(
                    pitcherId,
                    batterId,
                )
            }

        // 통계 계산
        val stats = calculateStats(events)

        // 히스토리 생성
        val history =
            events.map { event ->
                MatchupHistoryDto(
                    gameId = event.game.id,
                    gameDate = event.game.scheduledAt.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    result = event.plateAppearanceResult?.displayName ?: "기록 없음",
                    description = event.description,
                )
            }

        return MatchupDto(
            pitcherId = pitcherId,
            pitcherName = pitcher.name,
            batterId = batterId,
            batterName = batter.name,
            year = year,
            stats = stats,
            history = history,
        )
    }

    private fun calculateStats(events: List<com.nextup.core.domain.game.GameEvent>): MatchupStatsDto {
        var plateAppearances = 0
        var atBats = 0
        var hits = 0
        var doubles = 0
        var triples = 0
        var homeRuns = 0
        var walks = 0
        var strikeouts = 0
        var hitByPitch = 0
        var sacrificeFlies = 0
        var runsBattedIn = 0
        var totalBases = 0

        events.forEach { event ->
            val result = event.plateAppearanceResult ?: return@forEach

            plateAppearances++
            runsBattedIn += event.rbis

            when {
                result.isAtBat -> {
                    atBats++
                    if (result.isHit) {
                        hits++
                        totalBases += result.totalBases
                        when (result) {
                            PlateAppearanceResult.DOUBLE -> doubles++
                            PlateAppearanceResult.TRIPLE -> triples++
                            PlateAppearanceResult.HOME_RUN -> homeRuns++
                            else -> {} // SINGLE
                        }
                    } else if (result.isStrikeout) {
                        strikeouts++
                    }
                }
                result.isWalk -> walks++
                result.isHitByPitch -> hitByPitch++
                result == PlateAppearanceResult.SACRIFICE_FLY -> sacrificeFlies++
            }
        }

        // 타율 계산: hits / atBats
        val battingAverage =
            if (atBats > 0) {
                BigDecimal(hits).divide(BigDecimal(atBats), 3, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

        // 출루율 계산: (hits + walks + hbp) / (atBats + walks + hbp + sf)
        val onBaseDenominator = atBats + walks + hitByPitch + sacrificeFlies
        val onBasePercentage =
            if (onBaseDenominator > 0) {
                BigDecimal(hits + walks + hitByPitch)
                    .divide(BigDecimal(onBaseDenominator), 3, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

        // 장타율 계산: totalBases / atBats
        val sluggingPercentage =
            if (atBats > 0) {
                BigDecimal(totalBases).divide(BigDecimal(atBats), 3, RoundingMode.HALF_UP)
            } else {
                BigDecimal.ZERO
            }

        return MatchupStatsDto(
            plateAppearances = plateAppearances,
            atBats = atBats,
            hits = hits,
            doubles = doubles,
            triples = triples,
            homeRuns = homeRuns,
            walks = walks,
            strikeouts = strikeouts,
            hitByPitch = hitByPitch,
            sacrificeFlies = sacrificeFlies,
            runsBattedIn = runsBattedIn,
            battingAverage = battingAverage,
            onBasePercentage = onBasePercentage,
            sluggingPercentage = sluggingPercentage,
        )
    }
}
