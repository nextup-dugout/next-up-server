package com.nextup.core.port.repository

import com.nextup.core.domain.competition.BracketEntry

/**
 * 대진표 엔트리 저장소 포트
 */
interface BracketEntryRepositoryPort {
    fun save(bracketEntry: BracketEntry): BracketEntry

    fun saveAll(entries: List<BracketEntry>): List<BracketEntry>

    fun findByCompetitionId(competitionId: Long): List<BracketEntry>

    fun findByIdOrNull(id: Long): BracketEntry?

    fun deleteByCompetitionId(competitionId: Long)
}
