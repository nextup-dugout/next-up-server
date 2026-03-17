package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MercenaryRequestJpaRepository : JpaRepository<MercenaryRequest, Long> {
    fun findByStatus(status: MercenaryRequestStatus): List<MercenaryRequest>

    fun findByRequestingTeamId(teamId: Long): List<MercenaryRequest>

    fun findByGameId(gameId: Long): List<MercenaryRequest>
}
