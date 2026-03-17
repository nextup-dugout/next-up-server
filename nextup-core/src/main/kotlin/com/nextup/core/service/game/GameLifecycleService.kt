package com.nextup.core.service.game

import com.nextup.core.domain.game.Game
import com.nextup.core.service.game.dto.GameEndReason
import java.time.LocalDateTime

/**
 * 경기 생명주기 관리 서비스 인터페이스
 *
 * 경기 시작, 이닝 진행, 종료, 몰수, 취소, 연기, 일정 변경을 담당합니다.
 */
interface GameLifecycleService {
    fun startGame(gameId: Long): Game

    fun advanceHalfInning(gameId: Long): Game

    fun endGame(
        gameId: Long,
        reason: GameEndReason,
    ): Game

    fun forfeitGame(
        gameId: Long,
        winnerTeamId: Long,
        reason: String,
    ): Game

    fun cancelGame(
        gameId: Long,
        reason: String? = null,
    ): Game

    fun postponeGame(
        gameId: Long,
        newScheduledAt: LocalDateTime,
        reason: String? = null,
    ): Game

    fun rescheduleGame(
        gameId: Long,
        newScheduledAt: LocalDateTime,
    ): Game

    /**
     * 기록원이 경기를 독점 잠금합니다.
     *
     * @param gameId 경기 ID
     * @param scorerId 기록원 ID
     * @return 잠금된 Game
     */
    fun lockGame(
        gameId: Long,
        scorerId: Long,
    ): Game

    /**
     * 기록원의 경기 잠금을 해제합니다.
     *
     * @param gameId 경기 ID
     * @param scorerId 기록원 ID
     * @return 잠금 해제된 Game
     */
    fun unlockGame(
        gameId: Long,
        scorerId: Long,
    ): Game
}
