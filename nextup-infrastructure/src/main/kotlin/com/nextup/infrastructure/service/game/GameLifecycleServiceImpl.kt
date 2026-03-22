package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.event.GameCancelledEvent
import com.nextup.core.domain.event.GameEndedEvent
import com.nextup.core.domain.event.GamePostponedEvent
import com.nextup.core.domain.event.GameRescheduledEvent
import com.nextup.core.domain.event.GameResultConfirmedEvent
import com.nextup.core.domain.event.GameStartedEvent
import com.nextup.core.domain.event.HalfInningAdvancedEvent
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.GameLifecycleService
import com.nextup.core.service.game.PitchingDecisionService
import com.nextup.core.service.game.dto.GameEndReason
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 경기 생명주기 관리 서비스 구현
 *
 * 경기 시작, 이닝 진행, 종료, 몰수, 취소를 담당합니다.
 */
@Service
@Transactional(readOnly = true)
class GameLifecycleServiceImpl(
    private val gameRepository: GameRepositoryPort,
    private val gameTeamRepository: GameTeamRepositoryPort,
    private val pitchingRecordRepository: PitchingRecordRepositoryPort,
    private val pitchingDecisionService: PitchingDecisionService,
    private val eventPublisher: ApplicationEventPublisher,
) : GameLifecycleService {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    override fun startGame(
        gameId: Long,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.validateScorer(scorerId)
        game.start()
        val savedGame = gameRepository.save(game)
        eventPublisher.publishEvent(GameStartedEvent(gameId = gameId))
        return savedGame
    }

    @Transactional
    override fun advanceHalfInning(
        gameId: Long,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.validateScorer(scorerId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException("진행 중인 경기만 이닝을 진행할 수 있습니다. 현재 상태: ${game.status.displayName}")
        }

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val result = game.nextHalfInning(gameTeams = gameTeams)

        // 홈팀 리드로 말 이닝 생략 → 경기 종료 시 투수 판정 및 결과 이벤트 발행
        if (result == com.nextup.core.domain.game.TiebreakerResult.HOME_TEAM_LEADS_SKIP_BOTTOM) {
            val savedGame = gameRepository.save(game)
            assignPitchingDecisions(gameId)
            publishGameResultEvent(gameId)
            eventPublisher.publishEvent(
                GameEndedEvent(
                    gameId = gameId,
                    finalStatus = game.status.name,
                ),
            )
            return savedGame
        }

        val savedGame = gameRepository.save(game)
        eventPublisher.publishEvent(
            HalfInningAdvancedEvent(
                gameId = gameId,
                newInning = savedGame.currentInning,
                newIsTopInning = savedGame.isTopInning,
            ),
        )
        return savedGame
    }

    @Transactional
    override fun endGame(
        gameId: Long,
        reason: GameEndReason,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.validateScorer(scorerId)

        if (game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException("진행 중인 경기만 종료할 수 있습니다. 현재 상태: ${game.status.displayName}")
        }

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)

        when (reason) {
            GameEndReason.REGULATION -> game.finish(gameTeams)
            GameEndReason.MERCY_RULE ->
                game.callGame(reason = "콜드게임 (점수차)", gameTeams = gameTeams)
            GameEndReason.WEATHER ->
                game.callGame(reason = "콜드게임 (기상 조건)", gameTeams = gameTeams)
            GameEndReason.FORFEIT -> throw InvalidGameStateException(
                "몰수 처리는 전용 API를 사용해주세요.",
            )
            GameEndReason.OTHER ->
                game.callGame(reason = "기타 사유", gameTeams = gameTeams)
        }

        val savedGame = gameRepository.save(game)

        assignPitchingDecisions(gameId)
        publishGameResultEvent(gameId)
        eventPublisher.publishEvent(
            GameEndedEvent(
                gameId = gameId,
                finalStatus = savedGame.status.name,
            ),
        )
        return savedGame
    }

    @Transactional
    override fun forfeitGame(
        gameId: Long,
        winnerTeamId: Long,
        reason: String,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.validateScorer(scorerId)

        if (game.status != GameStatus.SCHEDULED && game.status != GameStatus.IN_PROGRESS) {
            throw InvalidGameStateException(
                "예정 또는 진행 중인 경기만 몰수 처리할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)

        if (gameTeams.size != 2) {
            throw InvalidGameStateException(
                "몰수 처리를 위해서는 정확히 2개의 팀이 필요합니다. 현재 팀 수: ${gameTeams.size}",
            )
        }

        game.forfeit(
            winnerTeamId = winnerTeamId,
            reason = reason,
            gameTeams = gameTeams,
        )

        val savedGame = gameRepository.save(game)
        publishGameResultEvent(gameId)
        eventPublisher.publishEvent(
            GameEndedEvent(
                gameId = gameId,
                finalStatus = savedGame.status.name,
            ),
        )
        return savedGame
    }

    @Transactional
    override fun cancelGame(
        gameId: Long,
        reason: String?,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.validateScorer(scorerId)

        if (game.status != GameStatus.SCHEDULED &&
            game.status != GameStatus.POSTPONED &&
            game.status != GameStatus.SUSPENDED
        ) {
            throw InvalidGameStateException(
                "예정, 연기, 또는 중단 상태의 경기만 취소할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        game.cancel(reason)
        val savedGame = gameRepository.save(game)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME }
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY }
        eventPublisher.publishEvent(
            GameCancelledEvent(
                gameId = gameId,
                homeTeamId = homeTeam?.team?.id ?: 0L,
                awayTeamId = awayTeam?.team?.id ?: 0L,
            ),
        )
        eventPublisher.publishEvent(
            GameEndedEvent(
                gameId = gameId,
                finalStatus = savedGame.status.name,
            ),
        )

        return savedGame
    }

    @Transactional
    override fun postponeGame(
        gameId: Long,
        newScheduledAt: LocalDateTime,
        reason: String?,
    ): Game {
        val game = findGame(gameId)

        if (game.status != GameStatus.SCHEDULED) {
            throw InvalidGameStateException(
                "예정 상태의 경기만 연기할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        game.postpone(newScheduledAt, reason)
        val savedGame = gameRepository.save(game)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME }
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY }
        if (homeTeam != null && awayTeam != null) {
            eventPublisher.publishEvent(
                GamePostponedEvent(
                    gameId = gameId,
                    homeTeamId = homeTeam.team.id,
                    awayTeamId = awayTeam.team.id,
                    newScheduledAt = newScheduledAt,
                ),
            )
        }

        return savedGame
    }

    @Transactional
    override fun rescheduleGame(
        gameId: Long,
        newScheduledAt: LocalDateTime,
    ): Game {
        val game = findGame(gameId)

        if (game.status != GameStatus.SCHEDULED && game.status != GameStatus.POSTPONED) {
            throw InvalidGameStateException(
                "예정 또는 연기 상태의 경기만 일정을 변경할 수 있습니다. 현재 상태: ${game.status.displayName}",
            )
        }

        game.reschedule(newScheduledAt)
        val savedGame = gameRepository.save(game)

        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME }
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY }
        if (homeTeam != null && awayTeam != null) {
            eventPublisher.publishEvent(
                GameRescheduledEvent(
                    gameId = gameId,
                    homeTeamId = homeTeam.team.id,
                    awayTeamId = awayTeam.team.id,
                    newScheduledAt = newScheduledAt,
                ),
            )
        }

        return savedGame
    }

    @Transactional
    override fun suspendGame(
        gameId: Long,
        reason: String?,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.validateScorer(scorerId)
        game.suspend(reason)
        return gameRepository.save(game)
    }

    @Transactional
    override fun resumeGame(
        gameId: Long,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.validateScorer(scorerId)
        game.resume()
        return gameRepository.save(game)
    }

    /**
     * 경기 종료 시 투수 결정(승/패/세이브/홀드)을 자동으로 할당합니다.
     *
     * PitchingDecisionService를 사용하여 GameRules 기반으로 판정합니다.
     */
    private fun assignPitchingDecisions(gameId: Long) {
        val game = findGame(gameId)
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        if (gameTeams.size != 2) return

        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME } ?: return
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY } ?: return

        val winnerTeam =
            when {
                homeTeam.totalScore > awayTeam.totalScore -> homeTeam
                awayTeam.totalScore > homeTeam.totalScore -> awayTeam
                else -> null
            }
        val loserTeam =
            when {
                homeTeam.result == com.nextup.core.domain.game.GameResult.LOSS -> homeTeam
                awayTeam.result == com.nextup.core.domain.game.GameResult.LOSS -> awayTeam
                winnerTeam == homeTeam -> awayTeam
                winnerTeam == awayTeam -> homeTeam
                else -> null
            }

        // 무승부인 경우 투수 결정 불필요
        if (winnerTeam == null || loserTeam == null) return

        val allPitchingRecords = pitchingRecordRepository.findAllByGameId(gameId)
        if (allPitchingRecords.isEmpty()) return

        val winnerTeamId = winnerTeam.team.id
        val winnerTeamRecords =
            allPitchingRecords
                .filter { it.gamePlayer.gameTeam.team.id == winnerTeamId }
                .sortedWith(
                    compareBy<com.nextup.core.domain.game.PitchingRecord> {
                        it.gamePlayer.entryInning ?: 1
                    }.thenByDescending { if (it.isStartingPitcher) 1 else 0 },
                )
        val loserTeamRecords =
            allPitchingRecords
                .filter { it.gamePlayer.gameTeam.team.id != winnerTeamId }
                .sortedWith(
                    compareBy<com.nextup.core.domain.game.PitchingRecord> {
                        it.gamePlayer.entryInning ?: 1
                    }.thenByDescending { if (it.isStartingPitcher) 1 else 0 },
                )

        pitchingDecisionService.assignDecisions(
            winnerTeamRecords = winnerTeamRecords,
            loserTeamRecords = loserTeamRecords,
            winnerGameTeam = winnerTeam,
            loserGameTeam = loserTeam,
            gameRules = game.competition.gameRules,
        )

        allPitchingRecords.forEach { pitchingRecordRepository.save(it) }
    }

    private fun publishGameResultEvent(gameId: Long) {
        val gameTeams = gameTeamRepository.findAllByGameId(gameId)
        val homeTeam = gameTeams.find { it.homeAway == HomeAway.HOME } ?: return
        val awayTeam = gameTeams.find { it.homeAway == HomeAway.AWAY } ?: return
        eventPublisher.publishEvent(
            GameResultConfirmedEvent(
                gameId = gameId,
                homeTeamId = homeTeam.team.id,
                awayTeamId = awayTeam.team.id,
                homeScore = homeTeam.totalScore,
                awayScore = awayTeam.totalScore,
            ),
        )
    }

    @Transactional
    override fun lockGame(
        gameId: Long,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.lockForScorer(scorerId)
        return gameRepository.save(game)
    }

    @Transactional
    override fun unlockGame(
        gameId: Long,
        scorerId: Long,
    ): Game {
        val game = findGame(gameId)
        game.unlockScorer(scorerId)
        return gameRepository.save(game)
    }

    private fun findGame(id: Long): Game =
        gameRepository.findByIdOrNull(id)
            ?: throw GameNotFoundException(id)
}
