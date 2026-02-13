package com.nextup.infrastructure.persistence.bracket

import com.nextup.core.domain.competition.BracketEntry
import com.nextup.core.port.repository.BracketEntryRepositoryPort
import org.springframework.stereotype.Repository

@Repository
class BracketEntryRepositoryAdapter(
    private val jpaRepository: BracketEntryJpaRepository,
) : BracketEntryRepositoryPort {
    override fun save(bracketEntry: BracketEntry): BracketEntry = jpaRepository.save(bracketEntry)

    override fun saveAll(entries: List<BracketEntry>): List<BracketEntry> = jpaRepository.saveAll(entries)

    override fun findByCompetitionId(competitionId: Long): List<BracketEntry> =
        jpaRepository.findByCompetitionId(competitionId)

    override fun findByIdOrNull(id: Long): BracketEntry? = jpaRepository.findById(id).orElse(null)

    override fun deleteByCompetitionId(competitionId: Long) = jpaRepository.deleteByCompetitionId(competitionId)
}
