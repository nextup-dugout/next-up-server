package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.LineupEntry
import com.nextup.core.port.repository.LineupEntryRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query

interface LineupEntryRepository :
    JpaRepository<LineupEntry, Long>,
    LineupEntryRepositoryPort {
    override fun findByIdOrNull(id: Long): LineupEntry? = findById(id).orElse(null)

    @Query(
        "SELECT le FROM LineupEntry le WHERE le.submission.id = :submissionId " +
            "ORDER BY le.battingOrder ASC NULLS LAST",
    )
    override fun findAllBySubmissionId(submissionId: Long): List<LineupEntry>

    @Query("SELECT le FROM LineupEntry le WHERE le.submission.id = :submissionId AND le.player.id = :playerId")
    override fun findBySubmissionIdAndPlayerId(
        submissionId: Long,
        playerId: Long,
    ): LineupEntry?

    @Query("SELECT le FROM LineupEntry le WHERE le.submission.id = :submissionId AND le.battingOrder = :battingOrder")
    override fun findBySubmissionIdAndBattingOrder(
        submissionId: Long,
        battingOrder: Int,
    ): LineupEntry?

    @Modifying
    @Query("DELETE FROM LineupEntry le WHERE le.submission.id = :submissionId")
    override fun deleteAllBySubmissionId(submissionId: Long)
}
