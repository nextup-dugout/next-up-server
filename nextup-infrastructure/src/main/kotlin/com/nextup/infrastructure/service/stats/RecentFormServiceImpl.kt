package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.service.stats.RecentFormService
import com.nextup.core.service.stats.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 최근 N경기 폼 분석 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class RecentFormServiceImpl(
    private val playerRepositoryPort: PlayerRepositoryPort,
    private val battingRecordRepositoryPort: BattingRecordRepositoryPort,
    private val pitchingRecordRepositoryPort: PitchingRecordRepositoryPort,
    private val gameRepositoryPort: GameRepositoryPort,
    private val gameTeamRepositoryPort: GameTeamRepositoryPort,
) : RecentFormService {
    companion object {
        private const val MAX_GAMES = 20
    }

    override fun getRecentForm(
        playerId: Long,
        games: Int,
        type: FormType,
    ): RecentFormDto {
        require(games in 1..MAX_GAMES) { "조회할 경기 수는 1~$MAX_GAMES 사이여야 합니다." }

        val player =
            playerRepositoryPort.findByIdOrNull(playerId)
                ?: throw PlayerNotFoundException(playerId)

        return when (type) {
            FormType.BATTING -> analyzeBattingForm(player.id, player.name, games)
            FormType.PITCHING -> analyzePitchingForm(player.id, player.name, games)
        }
    }

    private fun analyzeBattingForm(
        playerId: Long,
        playerName: String,
        gamesRequested: Int,
    ): RecentFormDto {
        val records = battingRecordRepositoryPort.findRecentByPlayerId(playerId, gamesRequested)

        if (records.isEmpty()) {
            throw InvalidInputException("NO_BATTING_RECORDS", "타격 기록이 없습니다.")
        }

        // 배치 로드: 모든 gameId를 수집하여 한 번에 조회 (N+1 방지)
        val gameIds = records.map { it.gamePlayer.gameTeam.game.id }.distinct()
        val gamesMap = gameRepositoryPort.findAllByIds(gameIds).associateBy { it.id }
        val gameTeamsMap = gameTeamRepositoryPort.findAllByGameIds(gameIds).groupBy { it.game.id }

        val gamesFound = records.size
        val gameBattingDtos = records.map { it.toGameBattingDto(gamesMap, gameTeamsMap) }

        val totalAtBats = records.sumOf { it.atBats }
        val totalHits = records.sumOf { it.hits }
        val totalHomeRuns = records.sumOf { it.homeRuns }
        val totalRbis = records.sumOf { it.runsBattedIn }
        val totalRuns = records.sumOf { it.runs }

        val recentAverage =
            if (totalAtBats == 0) {
                BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP)
            } else {
                BigDecimal(totalHits).divide(BigDecimal(totalAtBats), 3, RoundingMode.HALF_UP)
            }

        // 전체 통산 타율 계산 (비교용)
        val allRecords = battingRecordRepositoryPort.findAllByPlayerId(playerId)
        val overallAtBats = allRecords.sumOf { it.atBats }
        val overallHits = allRecords.sumOf { it.hits }
        val overallAverage =
            if (overallAtBats == 0) {
                null
            } else {
                BigDecimal(overallHits).divide(BigDecimal(overallAtBats), 3, RoundingMode.HALF_UP)
            }

        val trend = calculateBattingTrend(records)
        val trendDescription = generateBattingTrendDescription(trend, recentAverage, records)

        val battingForm =
            RecentBattingFormDto(
                games = gameBattingDtos,
                totalAtBats = totalAtBats,
                totalHits = totalHits,
                totalHomeRuns = totalHomeRuns,
                totalRbis = totalRbis,
                totalRuns = totalRuns,
                recentAverage = recentAverage,
                overallAverage = overallAverage,
            )

        return RecentFormDto(
            playerId = playerId,
            playerName = playerName,
            type = FormType.BATTING,
            gamesRequested = gamesRequested,
            gamesFound = gamesFound,
            trend = trend,
            trendDescription = trendDescription,
            batting = battingForm,
            pitching = null,
        )
    }

    private fun analyzePitchingForm(
        playerId: Long,
        playerName: String,
        gamesRequested: Int,
    ): RecentFormDto {
        val records = pitchingRecordRepositoryPort.findRecentByPlayerId(playerId, gamesRequested)

        if (records.isEmpty()) {
            throw InvalidInputException("NO_PITCHING_RECORDS", "투수 기록이 없습니다.")
        }

        // 배치 로드: 모든 gameId를 수집하여 한 번에 조회 (N+1 방지)
        val gameIds = records.map { it.gamePlayer.gameTeam.game.id }.distinct()
        val gamesMap = gameRepositoryPort.findAllByIds(gameIds).associateBy { it.id }
        val gameTeamsMap = gameTeamRepositoryPort.findAllByGameIds(gameIds).groupBy { it.game.id }

        val gamesFound = records.size
        val gamePitchingDtos = records.map { it.toGamePitchingDto(gamesMap, gameTeamsMap) }

        val totalInningsPitchedOuts = records.sumOf { it.inningsPitchedOuts }
        val totalEarnedRuns = records.sumOf { it.earnedRuns }
        val totalStrikeouts = records.sumOf { it.strikeouts }

        val inningsPitchedDisplay = formatInningsPitched(totalInningsPitchedOuts)

        val recentEra =
            if (totalInningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(totalInningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(totalEarnedRuns)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

        // 전체 통산 ERA 계산 (비교용)
        val allRecords = pitchingRecordRepositoryPort.findAllByPlayerId(playerId)
        val overallInningsPitchedOuts = allRecords.sumOf { it.inningsPitchedOuts }
        val overallEarnedRuns = allRecords.sumOf { it.earnedRuns }
        val overallEra =
            if (overallInningsPitchedOuts == 0) {
                null
            } else {
                val innings = BigDecimal(overallInningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(overallEarnedRuns)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

        val trend = calculatePitchingTrend(records)
        val trendDescription = generatePitchingTrendDescription(trend, recentEra, records)

        val pitchingForm =
            RecentPitchingFormDto(
                games = gamePitchingDtos,
                totalInningsPitchedOuts = totalInningsPitchedOuts,
                inningsPitchedDisplay = inningsPitchedDisplay,
                totalEarnedRuns = totalEarnedRuns,
                totalStrikeouts = totalStrikeouts,
                recentEra = recentEra,
                overallEra = overallEra,
            )

        return RecentFormDto(
            playerId = playerId,
            playerName = playerName,
            type = FormType.PITCHING,
            gamesRequested = gamesRequested,
            gamesFound = gamesFound,
            trend = trend,
            trendDescription = trendDescription,
            batting = null,
            pitching = pitchingForm,
        )
    }

    private fun calculateBattingTrend(records: List<BattingRecord>): FormTrend {
        if (records.size <= 1) {
            return FormTrend.STABLE
        }

        val halfSize = records.size / 2
        val recentHalf = records.take(halfSize)
        val earlierHalf = records.drop(halfSize)

        val recentAtBats = recentHalf.sumOf { it.atBats }
        val recentHits = recentHalf.sumOf { it.hits }
        val earlierAtBats = earlierHalf.sumOf { it.atBats }
        val earlierHits = earlierHalf.sumOf { it.hits }

        if (recentAtBats == 0 || earlierAtBats == 0) {
            return FormTrend.STABLE
        }

        val recentAvg = BigDecimal(recentHits).divide(BigDecimal(recentAtBats), 10, RoundingMode.HALF_UP)
        val earlierAvg = BigDecimal(earlierHits).divide(BigDecimal(earlierAtBats), 10, RoundingMode.HALF_UP)

        return when {
            recentAvg >= earlierAvg.multiply(BigDecimal("1.1")) -> FormTrend.UP
            recentAvg <= earlierAvg.multiply(BigDecimal("0.9")) -> FormTrend.DOWN
            else -> FormTrend.STABLE
        }
    }

    private fun calculatePitchingTrend(records: List<PitchingRecord>): FormTrend {
        if (records.size <= 1) {
            return FormTrend.STABLE
        }

        val halfSize = records.size / 2
        val recentHalf = records.take(halfSize)
        val earlierHalf = records.drop(halfSize)

        val recentInnings = recentHalf.sumOf { it.inningsPitchedOuts }
        val recentER = recentHalf.sumOf { it.earnedRuns }
        val earlierInnings = earlierHalf.sumOf { it.inningsPitchedOuts }
        val earlierER = earlierHalf.sumOf { it.earnedRuns }

        if (recentInnings == 0 || earlierInnings == 0) {
            return FormTrend.STABLE
        }

        val recentEra = calculateEra(recentER, recentInnings)
        val earlierEra = calculateEra(earlierER, earlierInnings)

        // ERA는 낮을수록 좋음
        return when {
            recentEra <= earlierEra.multiply(BigDecimal("0.9")) -> FormTrend.UP
            recentEra >= earlierEra.multiply(BigDecimal("1.1")) -> FormTrend.DOWN
            else -> FormTrend.STABLE
        }
    }

    private fun calculateEra(
        earnedRuns: Int,
        inningsPitchedOuts: Int,
    ): BigDecimal {
        if (inningsPitchedOuts == 0) {
            return BigDecimal.ZERO
        }
        val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
        return BigDecimal(earnedRuns)
            .multiply(BigDecimal(9))
            .divide(innings, 10, RoundingMode.HALF_UP)
    }

    private fun generateBattingTrendDescription(
        trend: FormTrend,
        recentAverage: BigDecimal,
        records: List<BattingRecord>,
    ): String {
        if (records.size <= 1) {
            return "최근 ${records.size}경기 기록입니다."
        }

        val halfSize = records.size / 2
        val earlierHalf = records.drop(halfSize)
        val earlierAtBats = earlierHalf.sumOf { it.atBats }
        val earlierHits = earlierHalf.sumOf { it.hits }
        val earlierAvg =
            if (earlierAtBats == 0) {
                BigDecimal.ZERO
            } else {
                BigDecimal(earlierHits).divide(BigDecimal(earlierAtBats), 3, RoundingMode.HALF_UP)
            }

        return when (trend) {
            FormTrend.UP -> "최근 ${records.size}경기 상승세입니다. 타율 $recentAverage (이전 $earlierAvg)"
            FormTrend.DOWN -> "최근 ${records.size}경기 하락세입니다. 타율 $recentAverage (이전 $earlierAvg)"
            FormTrend.STABLE -> "최근 ${records.size}경기 안정적인 폼을 유지하고 있습니다."
        }
    }

    private fun generatePitchingTrendDescription(
        trend: FormTrend,
        recentEra: BigDecimal,
        records: List<PitchingRecord>,
    ): String {
        if (records.size <= 1) {
            return "최근 ${records.size}경기 기록입니다."
        }

        val halfSize = records.size / 2
        val earlierHalf = records.drop(halfSize)
        val earlierInnings = earlierHalf.sumOf { it.inningsPitchedOuts }
        val earlierER = earlierHalf.sumOf { it.earnedRuns }
        val earlierEra = calculateEra(earlierER, earlierInnings)

        return when (trend) {
            FormTrend.UP -> "최근 ${records.size}경기 상승세입니다. ERA $recentEra (이전 $earlierEra)"
            FormTrend.DOWN -> "최근 ${records.size}경기 하락세입니다. ERA $recentEra (이전 $earlierEra)"
            FormTrend.STABLE -> "최근 ${records.size}경기 안정적인 폼을 유지하고 있습니다."
        }
    }

    private fun formatInningsPitched(outs: Int): String {
        val completeInnings = outs / 3
        val remainingOuts = outs % 3
        return "$completeInnings.$remainingOuts"
    }

    private fun BattingRecord.toGameBattingDto(
        gamesMap: Map<Long, Game>,
        gameTeamsMap: Map<Long, List<GameTeam>>,
    ): GameBattingDto {
        val gameId = this.gamePlayer.gameTeam.game.id
        val game = gamesMap[gameId]
        val gameDate = game?.scheduledAt?.toLocalDate()?.toString() ?: "Unknown"

        // 상대팀 정보 가져오기 (배치 로드된 데이터 사용)
        val myTeamId = this.gamePlayer.gameTeam.team.id
        val opponentName = getOpponentTeamName(gameTeamsMap[gameId], myTeamId)

        return GameBattingDto(
            gameId = gameId,
            gameDate = gameDate,
            opponentName = opponentName,
            atBats = this.atBats,
            hits = this.hits,
            homeRuns = this.homeRuns,
            rbis = this.runsBattedIn,
            runs = this.runs,
            walks = this.walks,
            strikeouts = this.strikeouts,
        )
    }

    private fun PitchingRecord.toGamePitchingDto(
        gamesMap: Map<Long, Game>,
        gameTeamsMap: Map<Long, List<GameTeam>>,
    ): GamePitchingDto {
        val gameId = this.gamePlayer.gameTeam.game.id
        val game = gamesMap[gameId]
        val gameDate = game?.scheduledAt?.toLocalDate()?.toString() ?: "Unknown"

        // 상대팀 정보 가져오기 (배치 로드된 데이터 사용)
        val myTeamId = this.gamePlayer.gameTeam.team.id
        val opponentName = getOpponentTeamName(gameTeamsMap[gameId], myTeamId)

        return GamePitchingDto(
            gameId = gameId,
            gameDate = gameDate,
            opponentName = opponentName,
            inningsPitched = this.inningsPitchedDisplay,
            earnedRuns = this.earnedRuns,
            strikeouts = this.strikeouts,
            walksAllowed = this.walksAllowed,
            hitsAllowed = this.hitsAllowed,
            decision = this.decision.name,
        )
    }

    private fun getOpponentTeamName(
        gameTeams: List<GameTeam>?,
        myTeamId: Long,
    ): String {
        val opponentTeam = gameTeams?.find { it.team.id != myTeamId }
        return opponentTeam?.team?.name ?: "상대팀"
    }
}
