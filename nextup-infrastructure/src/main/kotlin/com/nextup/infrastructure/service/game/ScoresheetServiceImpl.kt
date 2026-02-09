package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidStateException
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.ScoresheetService
import com.nextup.core.service.game.dto.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.format.DateTimeFormatter

/**
 * 공식 기록지 서비스 구현
 */
@Service
@Transactional(readOnly = true)
class ScoresheetServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val gamePlayerRepository: GamePlayerRepositoryPort,
    private val battingRecordRepository: BattingRecordRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val gameEventRepository: GameEventRepositoryPort,
) : ScoresheetService {
    override fun getScoresheet(gameId: Long): ScoresheetDto {
        val game =
            gameRepository.findByIdOrNull(gameId)
                ?: throw GameNotFoundException(gameId)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        if (gameTeams.size != 2) {
            throw InvalidStateException(
                "INVALID_GAME_TEAMS",
                "경기에 정확히 2개의 팀이 필요합니다.",
            )
        }

        val homeTeam =
            gameTeams.firstOrNull { it.isHome }
                ?: throw InvalidStateException("MISSING_HOME_TEAM", "홈팀을 찾을 수 없습니다.")
        val awayTeam =
            gameTeams.firstOrNull { it.isAway }
                ?: throw InvalidStateException("MISSING_AWAY_TEAM", "원정팀을 찾을 수 없습니다.")

        val gamePlayers = gamePlayerRepository.findAllByGameId(gameId)
        val homePlayers = gamePlayers.filter { it.gameTeam.isHome }
        val awayPlayers = gamePlayers.filter { it.gameTeam.isAway }

        val gameEvents = gameEventRepository.findAllByGameIdOrderByEventTimestamp(gameId)
        val keyEvents = gameEvents.filter { !it.undone }.map { buildKeyEvent(it) }

        return ScoresheetDto(
            gameInfo = buildGameInfo(game),
            teams =
                TeamsScoresheetDto(
                    home = buildTeamInfo(homeTeam),
                    away = buildTeamInfo(awayTeam),
                ),
            inningScores = buildInningScores(game, homeTeam, awayTeam),
            battingRecords =
                BattingRecordsDto(
                    home = buildBattingRecords(homePlayers),
                    away = buildBattingRecords(awayPlayers),
                ),
            pitchingRecords =
                PitchingRecordsDto(
                    home = buildPitchingRecords(homePlayers),
                    away = buildPitchingRecords(awayPlayers),
                ),
            keyEvents = keyEvents,
        )
    }

    private fun buildGameInfo(game: com.nextup.core.domain.game.Game): GameInfoDto =
        GameInfoDto(
            gameId = game.id,
            competitionName = game.competition.name,
            gameNumber = game.gameNumber,
            scheduledAt = game.scheduledAt,
            startedAt = game.startedAt,
            endedAt = game.endedAt,
            location = game.location,
            fieldName = game.fieldName,
            status = game.status.displayName,
            currentInning = game.currentInningDisplay,
            totalInnings = game.totalInnings,
        )

    private fun buildTeamInfo(gameTeam: com.nextup.core.domain.game.GameTeam): TeamScoresheetInfoDto =
        TeamScoresheetInfoDto(
            teamId = gameTeam.team.id,
            teamName = gameTeam.team.name,
            totalScore = gameTeam.totalScore,
            totalHits = gameTeam.totalHits,
            totalErrors = gameTeam.totalErrors,
            result = gameTeam.result.displayName,
        )

    private fun buildInningScores(
        game: com.nextup.core.domain.game.Game,
        homeTeam: com.nextup.core.domain.game.GameTeam,
        awayTeam: com.nextup.core.domain.game.GameTeam,
    ): InningScoresDto {
        val maxInning = maxOf(game.currentInning, game.totalInnings)
        val homeScores = (1..maxInning).map { homeTeam.getInningScore(it) }
        val awayScores = (1..maxInning).map { awayTeam.getInningScore(it) }

        return InningScoresDto(
            innings = maxInning,
            homeScores = homeScores,
            awayScores = awayScores,
        )
    }

    private fun buildBattingRecords(players: List<com.nextup.core.domain.game.GamePlayer>,): List<BatterScoresheetDto> =
        players
            .filter { it.battingOrder != null || it.isStarter }
            .mapNotNull { player ->
                val battingRecord = battingRecordRepository.findByGamePlayer(player)
                if (battingRecord != null) {
                    BatterScoresheetDto(
                        playerId = player.player.id,
                        name = player.player.name,
                        backNumber = player.backNumber,
                        position = player.position.abbreviation,
                        battingOrder = player.battingOrder,
                        plateAppearances = battingRecord.plateAppearances,
                        atBats = battingRecord.atBats,
                        runs = battingRecord.runs,
                        hits = battingRecord.hits,
                        doubles = battingRecord.doubles,
                        triples = battingRecord.triples,
                        homeRuns = battingRecord.homeRuns,
                        rbis = battingRecord.runsBattedIn,
                        walks = battingRecord.walks,
                        strikeouts = battingRecord.strikeouts,
                        stolenBases = battingRecord.stolenBases,
                        avg = formatAverage(battingRecord.battingAverage),
                    )
                } else {
                    null
                }
            }.sortedBy { it.battingOrder ?: 999 }

    private fun buildPitchingRecords(
        players: List<com.nextup.core.domain.game.GamePlayer>,
    ): List<PitcherScoresheetDto> =
        players
            .filter { it.isPitcher }
            .mapNotNull { player ->
                val pitchingRecord = pitchingRecordRepository.findByGamePlayer(player)
                if (pitchingRecord != null) {
                    PitcherScoresheetDto(
                        playerId = player.player.id,
                        name = player.player.name,
                        backNumber = player.backNumber,
                        isStartingPitcher = pitchingRecord.isStartingPitcher,
                        inningsPitched = pitchingRecord.inningsPitchedDisplay,
                        hitsAllowed = pitchingRecord.hitsAllowed,
                        runsAllowed = pitchingRecord.runsAllowed,
                        earnedRuns = pitchingRecord.earnedRuns,
                        walks = pitchingRecord.walksAllowed,
                        strikeouts = pitchingRecord.strikeouts,
                        homeRunsAllowed = pitchingRecord.homeRunsAllowed,
                        decision = pitchingRecord.decision.abbreviation,
                        era = formatERA(pitchingRecord.earnedRunAverage),
                    )
                } else {
                    null
                }
            }.sortedByDescending { it.isStartingPitcher }

    private fun buildKeyEvent(event: com.nextup.core.domain.game.GameEvent): KeyEventDto {
        val inningDisplay = "${event.inning}회${if (event.isTopInning) "초" else "말"}"
        val timestamp =
            event.eventTimestamp.atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        return KeyEventDto(
            inning = inningDisplay,
            description = event.description,
            timestamp = timestamp,
        )
    }

    private fun formatAverage(avg: java.math.BigDecimal): String = avg.setScale(3, RoundingMode.HALF_UP).toString()

    private fun formatERA(era: java.math.BigDecimal): String = era.setScale(2, RoundingMode.HALF_UP).toString()
}
