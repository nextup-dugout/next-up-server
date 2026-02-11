package com.nextup.core.port.repository

import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus

/**
 * LineupSubmission Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface LineupSubmissionRepositoryPort {
    fun save(submission: LineupSubmission): LineupSubmission

    fun findByIdOrNull(id: Long): LineupSubmission?

    fun findByGameIdAndTeamId(
        gameId: Long,
        teamId: Long,
    ): LineupSubmission?

    fun findAllByGameId(gameId: Long): List<LineupSubmission>

    fun findAllByTeamId(teamId: Long): List<LineupSubmission>

    fun findAllByTeamIdAndStatus(
        teamId: Long,
        status: LineupSubmissionStatus,
    ): List<LineupSubmission>

    fun findAllByGameIdAndStatus(
        gameId: Long,
        status: LineupSubmissionStatus,
    ): List<LineupSubmission>

    fun delete(submission: LineupSubmission)

    fun deleteById(id: Long)
}
