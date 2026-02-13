package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.LineupSubmission
import com.nextup.core.domain.game.LineupSubmissionStatus
import com.nextup.core.port.repository.LineupSubmissionRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LineupSubmissionRepository :
    JpaRepository<LineupSubmission, Long>,
    LineupSubmissionRepositoryPort {
    override fun findByIdOrNull(id: Long): LineupSubmission? = findById(id).orElse(null)

    @Query("SELECT ls FROM LineupSubmission ls WHERE ls.game.id = :gameId AND ls.team.id = :teamId")
    override fun findByGameIdAndTeamId(
        gameId: Long,
        teamId: Long,
    ): LineupSubmission?

    @Query("SELECT ls FROM LineupSubmission ls WHERE ls.game.id = :gameId")
    override fun findAllByGameId(gameId: Long): List<LineupSubmission>

    @Query("SELECT ls FROM LineupSubmission ls WHERE ls.team.id = :teamId")
    override fun findAllByTeamId(teamId: Long): List<LineupSubmission>

    @Query("SELECT ls FROM LineupSubmission ls WHERE ls.team.id = :teamId AND ls.status = :status")
    override fun findAllByTeamIdAndStatus(
        teamId: Long,
        status: LineupSubmissionStatus,
    ): List<LineupSubmission>

    @Query("SELECT ls FROM LineupSubmission ls WHERE ls.game.id = :gameId AND ls.status = :status")
    override fun findAllByGameIdAndStatus(
        gameId: Long,
        status: LineupSubmissionStatus,
    ): List<LineupSubmission>
}
