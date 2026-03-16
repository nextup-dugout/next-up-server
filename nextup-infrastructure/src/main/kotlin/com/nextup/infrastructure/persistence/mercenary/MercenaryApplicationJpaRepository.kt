package com.nextup.infrastructure.persistence.mercenary

import com.nextup.core.domain.mercenary.MercenaryApplication
import com.nextup.core.domain.mercenary.MercenaryApplicationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface MercenaryApplicationJpaRepository : JpaRepository<MercenaryApplication, Long> {
    fun findByRequestId(requestId: Long): List<MercenaryApplication>

    fun findByPlayerId(playerId: Long): List<MercenaryApplication>

    fun existsByRequestIdAndPlayerId(
        requestId: Long,
        playerId: Long,
    ): Boolean

    @Query(
        "SELECT COUNT(ma) FROM MercenaryApplication ma " +
            "WHERE ma.requestId = :requestId AND ma.status = :status",
    )
    fun countByRequestIdAndStatus(
        requestId: Long,
        status: MercenaryApplicationStatus,
    ): Long
}
