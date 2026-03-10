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
}
