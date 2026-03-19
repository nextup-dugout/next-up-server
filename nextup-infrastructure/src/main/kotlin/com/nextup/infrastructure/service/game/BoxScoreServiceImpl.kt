package com.nextup.infrastructure.service.game

import com.nextup.common.exception.BattingRecordNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.PitchingRecordNotFoundException
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.BoxScoreService
import com.nextup.core.service.game.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 박스스코어 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class BoxScoreServiceImpl(
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
) : BoxScoreService {
    override fun getBoxScore(gameId: Long): BoxScoreDto {
        val gamePlayers = gamePlayerRepository.findAllByGameId(gameId)

        if (gamePlayers.isEmpty()) {
            throw InvalidStateException("NO_PLAYERS_IN_GAME", "경기 ID $gameId 에 출전 선수가 없습니다.")
        }

        val game = gamePlayers.first().gameTeam.game
        val homeTeam = gamePlayers.first { it.gameTeam.isHome }.gameTeam
        val awayTeam = gamePlayers.first { it.gameTeam.isAway }.gameTeam

        val homePlayers = gamePlayers.filter { it.gameTeam.isHome }
        val awayPlayers = gamePlayers.filter { it.gameTeam.isAway }

        return BoxScoreDto(
            gameId = gameId,
            homeTeam =
                buildTeamBoxScore(
                    homeTeam.team.id,
                    homeTeam.team.name,
                    homeTeam.team.logoUrl,
                    homeTeam,
                    homePlayers,
                ),
            awayTeam =
                buildTeamBoxScore(
                    awayTeam.team.id,
                    awayTeam.team.name,
                    awayTeam.team.logoUrl,
                    awayTeam,
                    awayPlayers,
                ),
            currentInning = game.currentInningDisplay,
            gameStatus = game.status.displayName,
        )
    }

    @Transactional
    override fun updateOnPlateAppearance(
        gameId: Long,
        batter: GamePlayer,
        pitcher: GamePlayer,
        result: PlateAppearanceResult,
        rbis: Int,
        runsScored: List<Long>,
        inning: Int,
    ) {
        // 타자 기록 갱신
        val battingRecord =
            battingRecordRepository.findByGamePlayer(batter)
                ?: throw BattingRecordNotFoundException(batter.id)

        battingRecord.applyPlateAppearanceResult(result, rbis)

        // 득점한 주자들의 득점 기록
        runsScored.forEach { runnerId ->
            val runnerGamePlayer =
                gamePlayerRepository.findByIdOrNull(runnerId)
                    ?: throw GamePlayerNotFoundException(runnerId)

            val runnerBattingRecord =
                battingRecordRepository.findByGamePlayer(runnerGamePlayer)
                    ?: throw BattingRecordNotFoundException(runnerId)

            runnerBattingRecord.recordRun()
        }

        // 투수 기록 갱신
        val pitchingRecord =
            pitchingRecordRepository.findByGamePlayer(pitcher)
                ?: throw PitchingRecordNotFoundException(pitcher.id)

        pitchingRecord.applyBatterFaced(result)

        // 아웃인 경우 아웃 카운트 증가
        if (!result.isOnBase) {
            pitchingRecord.recordInningOut()
        }

        // 득점이 발생한 경우 팀 점수 및 이닝별 점수 갱신
        if (runsScored.isNotEmpty() || result.isHomeRun) {
            val totalRuns = runsScored.size + (if (result.isHomeRun) 1 else 0)
            batter.gameTeam.addRunInInning(inning, totalRuns)

            // 투수의 실점 기록 (자책/비자책 구분은 별도 로직 필요)
            pitchingRecord.recordEarnedRun(totalRuns)
        }

        // 안타인 경우 팀 안타 수 증가
        if (result.isHit) {
            batter.gameTeam.addHit()
        }
    }

    private fun buildTeamBoxScore(
        teamId: Long,
        teamName: String,
        logoUrl: String?,
        gameTeam: com.nextup.core.domain.game.GameTeam,
        players: List<GamePlayer>,
    ): TeamBoxScoreDto {
        val inningScores = parseInningScores(gameTeam.inningScores)

        val batters =
            players
                .filter { it.battingOrder != null || it.isStarter }
                .map { buildBatterLine(it) }
                .sortedBy { it.battingOrder ?: 999 }

        val pitchers =
            players
                .filter { it.isPitcher }
                .map { buildPitcherLine(it) }

        return TeamBoxScoreDto(
            teamId = teamId,
            teamName = teamName,
            logoUrl = logoUrl,
            inningScores = inningScores,
            runs = gameTeam.totalScore,
            hits = gameTeam.totalHits,
            errors = gameTeam.totalErrors,
            batters = batters,
            pitchers = pitchers,
        )
    }

    private fun buildBatterLine(gamePlayer: GamePlayer): BatterLineDto {
        val battingRecord = battingRecordRepository.findByGamePlayer(gamePlayer)

        return BatterLineDto(
            playerId = gamePlayer.player.id,
            name = gamePlayer.player.name,
            position = gamePlayer.position.abbreviation,
            battingOrder = gamePlayer.battingOrder,
            plateAppearances = battingRecord?.plateAppearances ?: 0,
            atBats = battingRecord?.atBats ?: 0,
            runs = battingRecord?.runs ?: 0,
            hits = battingRecord?.hits ?: 0,
            rbis = battingRecord?.runsBattedIn ?: 0,
            walks = battingRecord?.walks ?: 0,
            strikeouts = battingRecord?.strikeouts ?: 0,
            avg = battingRecord?.let { BatterLineDto.formatAverage(it.battingAverage) } ?: ".000",
        )
    }

    private fun buildPitcherLine(gamePlayer: GamePlayer): PitcherLineDto {
        val pitchingRecord = pitchingRecordRepository.findByGamePlayer(gamePlayer)

        return PitcherLineDto(
            playerId = gamePlayer.player.id,
            name = gamePlayer.player.name,
            inningsPitched = pitchingRecord?.inningsPitchedDisplay ?: "0.0",
            hits = pitchingRecord?.hitsAllowed ?: 0,
            runs = pitchingRecord?.runsAllowed ?: 0,
            earnedRuns = pitchingRecord?.earnedRuns ?: 0,
            walks = pitchingRecord?.walksAllowed ?: 0,
            strikeouts = pitchingRecord?.strikeouts ?: 0,
            homeRuns = pitchingRecord?.homeRunsAllowed ?: 0,
            decision = pitchingRecord?.decision?.abbreviation,
            era = pitchingRecord?.let { PitcherLineDto.formatERA(it.earnedRunAverage) } ?: "0.00",
        )
    }

    private fun parseInningScores(inningScoresStr: String?): List<Int> {
        if (inningScoresStr.isNullOrBlank()) {
            return emptyList()
        }
        return inningScoresStr.split(",").mapNotNull { it.toIntOrNull() }
    }
}
