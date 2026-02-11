package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.NotFoundException
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.service.stats.PlayerRecordService
import com.nextup.core.service.stats.PlayerStatsService
import com.nextup.core.service.stats.dto.BattingStatsDto
import com.nextup.core.service.stats.dto.PitchingStatsDto
import com.nextup.core.service.stats.dto.PlayerRecordDto
import com.nextup.core.service.stats.dto.RecordScope
import com.nextup.core.service.stats.dto.RecordType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * 선수 기록 서비스 구현체
 */
@Service
@Transactional(readOnly = true)
class PlayerRecordServiceImpl(
    private val playerRepository: PlayerRepositoryPort,
    private val playerStatsService: PlayerStatsService,
) : PlayerRecordService {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun getPlayerRecord(
        playerId: Long,
        scope: RecordScope,
        type: RecordType,
        year: Int?,
        competitionId: Long?,
    ): PlayerRecordDto {
        // 선수 존재 여부 확인
        val player =
            playerRepository.findByIdOrNull(playerId)
                ?: throw NotFoundException("PLAYER_NOT_FOUND", "선수를 찾을 수 없습니다. (ID: $playerId)")

        return when (scope) {
            RecordScope.SEASON -> getSeasonRecord(player.id, player.name, type, year)
            RecordScope.CAREER -> getCareerRecord(player.id, player.name, type)
            RecordScope.COMPETITION -> throw InvalidInputException(
                "NOT_IMPLEMENTED",
                "대회별 통계는 추후 구현 예정입니다.",
            )
        }
    }

    private fun getSeasonRecord(
        playerId: Long,
        playerName: String,
        type: RecordType,
        year: Int?,
    ): PlayerRecordDto {
        val targetYear = year ?: LocalDate.now().year

        val battingStats =
            if (type == RecordType.BATTING || type == RecordType.ALL) {
                try {
                    val stats = playerStatsService.getSeasonBattingStats(playerId, targetYear)
                    BattingStatsDto(
                        gamesPlayed = stats.gamesPlayed,
                        plateAppearances = stats.plateAppearances,
                        atBats = stats.atBats,
                        hits = stats.hits,
                        doubles = stats.doubles,
                        triples = stats.triples,
                        homeRuns = stats.homeRuns,
                        runs = stats.runs,
                        runsBattedIn = stats.runsBattedIn,
                        walks = stats.walks,
                        strikeouts = stats.strikeouts,
                        stolenBases = stats.stolenBases,
                        battingAverage = stats.battingAverage,
                        onBasePercentage = stats.onBasePercentage,
                        sluggingPercentage = stats.sluggingPercentage,
                        ops = stats.ops,
                    )
                } catch (e: IllegalArgumentException) {
                    log.debug("선수 시즌 타격 기록 없음: playerId={}, year={}, message={}", playerId, targetYear, e.message)
                    null
                }
            } else {
                null
            }

        val pitchingStats =
            if (type == RecordType.PITCHING || type == RecordType.ALL) {
                try {
                    val stats = playerStatsService.getSeasonPitchingStats(playerId, targetYear)
                    PitchingStatsDto(
                        gamesPlayed = stats.gamesPlayed,
                        gamesStarted = stats.gamesStarted,
                        inningsPitched = stats.inningsPitchedDisplay,
                        wins = stats.wins,
                        losses = stats.losses,
                        saves = stats.saves,
                        holds = stats.holds,
                        earnedRuns = stats.earnedRuns,
                        hitsAllowed = stats.hitsAllowed,
                        walksAllowed = stats.walksAllowed,
                        strikeouts = stats.strikeouts,
                        homeRunsAllowed = stats.homeRunsAllowed,
                        era = stats.earnedRunAverage,
                        whip = stats.whip,
                    )
                } catch (e: IllegalArgumentException) {
                    log.debug("선수 시즌 투수 기록 없음: playerId={}, year={}, message={}", playerId, targetYear, e.message)
                    null
                }
            } else {
                null
            }

        return PlayerRecordDto(
            playerId = playerId,
            playerName = playerName,
            scope = RecordScope.SEASON,
            type = type,
            year = targetYear,
            competitionId = null,
            competitionName = null,
            battingStats = battingStats,
            pitchingStats = pitchingStats,
        )
    }

    private fun getCareerRecord(
        playerId: Long,
        playerName: String,
        type: RecordType,
    ): PlayerRecordDto {
        val battingStats =
            if (type == RecordType.BATTING || type == RecordType.ALL) {
                try {
                    val stats = playerStatsService.getCareerBattingStats(playerId)
                    BattingStatsDto(
                        gamesPlayed = stats.gamesPlayed,
                        plateAppearances = stats.plateAppearances,
                        atBats = stats.atBats,
                        hits = stats.hits,
                        doubles = stats.doubles,
                        triples = stats.triples,
                        homeRuns = stats.homeRuns,
                        runs = stats.runs,
                        runsBattedIn = stats.runsBattedIn,
                        walks = stats.walks,
                        strikeouts = stats.strikeouts,
                        stolenBases = stats.stolenBases,
                        battingAverage = stats.battingAverage,
                        onBasePercentage = stats.onBasePercentage,
                        sluggingPercentage = stats.sluggingPercentage,
                        ops = stats.ops,
                    )
                } catch (e: IllegalArgumentException) {
                    log.debug("선수 통산 타격 기록 없음: playerId={}, message={}", playerId, e.message)
                    null
                }
            } else {
                null
            }

        val pitchingStats =
            if (type == RecordType.PITCHING || type == RecordType.ALL) {
                try {
                    val stats = playerStatsService.getCareerPitchingStats(playerId)
                    PitchingStatsDto(
                        gamesPlayed = stats.gamesPlayed,
                        gamesStarted = stats.gamesStarted,
                        inningsPitched = stats.inningsPitchedDisplay,
                        wins = stats.wins,
                        losses = stats.losses,
                        saves = stats.saves,
                        holds = stats.holds,
                        earnedRuns = stats.earnedRuns,
                        hitsAllowed = stats.hitsAllowed,
                        walksAllowed = stats.walksAllowed,
                        strikeouts = stats.strikeouts,
                        homeRunsAllowed = stats.homeRunsAllowed,
                        era = stats.earnedRunAverage,
                        whip = stats.whip,
                    )
                } catch (e: IllegalArgumentException) {
                    log.debug("선수 통산 투수 기록 없음: playerId={}, message={}", playerId, e.message)
                    null
                }
            } else {
                null
            }

        return PlayerRecordDto(
            playerId = playerId,
            playerName = playerName,
            scope = RecordScope.CAREER,
            type = type,
            year = null,
            competitionId = null,
            competitionName = null,
            battingStats = battingStats,
            pitchingStats = pitchingStats,
        )
    }
}
