package com.nextup.infrastructure.persistence.bracket

import com.nextup.core.domain.competition.BracketEntry
import org.springframework.data.jpa.repository.JpaRepository

interface BracketEntryJpaRepository : JpaRepository<BracketEntry, Long> {
    fun findByCompetitionId(competitionId: Long): List<BracketEntry>

    fun deleteByCompetitionId(competitionId: Long)
}
