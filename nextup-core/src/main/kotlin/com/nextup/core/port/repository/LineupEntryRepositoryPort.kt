package com.nextup.core.port.repository

import com.nextup.core.domain.game.LineupEntry

/**
 * LineupEntry Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface LineupEntryRepositoryPort {
    fun save(entry: LineupEntry): LineupEntry

    fun saveAll(entries: List<LineupEntry>): List<LineupEntry>

    fun findByIdOrNull(id: Long): LineupEntry?

    fun findAllBySubmissionId(submissionId: Long): List<LineupEntry>

    fun findBySubmissionIdAndPlayerId(
        submissionId: Long,
        playerId: Long,
    ): LineupEntry?

    fun findBySubmissionIdAndBattingOrder(
        submissionId: Long,
        battingOrder: Int,
    ): LineupEntry?

    fun delete(entry: LineupEntry)

    fun deleteAllBySubmissionId(submissionId: Long)
}
