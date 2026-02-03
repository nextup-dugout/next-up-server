package com.nextup.scorer.websocket.mapper

import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.scorer.dto.websocket.InningScoresDto
import com.nextup.scorer.dto.websocket.ScoreboardMessage
import com.nextup.scorer.dto.websocket.TeamScoreDto
import org.springframework.stereotype.Component

/**
 * Game + GameTeams → ScoreboardMessage 매퍼
 *
 * 경기와 팀 정보를 스코어보드 메시지로 변환합니다.
 */
@Component
class ScoreboardMapper {

    /**
     * 스코어보드 메시지를 생성합니다.
     *
     * @param game 경기
     * @param homeTeam 홈팀 경기 정보
     * @param awayTeam 원정팀 경기 정보
     * @return ScoreboardMessage
     */
    fun toScoreboardMessage(
        game: Game,
        homeTeam: GameTeam,
        awayTeam: GameTeam
    ): ScoreboardMessage {
        require(homeTeam.homeAway == HomeAway.HOME) { "홈팀 정보가 아닙니다" }
        require(awayTeam.homeAway == HomeAway.AWAY) { "원정팀 정보가 아닙니다" }

        return ScoreboardMessage(
            gameId = game.id,
            homeTeam = toTeamScoreDto(homeTeam),
            awayTeam = toTeamScoreDto(awayTeam),
            inningScores = toInningScoresDto(homeTeam, awayTeam, game.totalInnings),
            currentInning = game.currentInning,
            isTopInning = game.isTopInning
        )
    }

    /**
     * 팀 점수 DTO를 생성합니다.
     */
    private fun toTeamScoreDto(gameTeam: GameTeam): TeamScoreDto {
        return TeamScoreDto(
            teamId = gameTeam.team.id,
            teamName = gameTeam.team.name,
            runs = gameTeam.totalScore,
            hits = gameTeam.totalHits,
            errors = gameTeam.totalErrors
        )
    }

    /**
     * 이닝별 점수 DTO를 생성합니다.
     */
    private fun toInningScoresDto(
        homeTeam: GameTeam,
        awayTeam: GameTeam,
        totalInnings: Int
    ): InningScoresDto {
        val maxInning = maxOf(
            totalInnings,
            homeTeam.inningScores?.split(",")?.size ?: 0,
            awayTeam.inningScores?.split(",")?.size ?: 0
        )

        val homeScores = (1..maxInning).map { homeTeam.getInningScore(it) }
        val awayScores = (1..maxInning).map { awayTeam.getInningScore(it) }

        return InningScoresDto(
            homeScores = homeScores,
            awayScores = awayScores
        )
    }
}
