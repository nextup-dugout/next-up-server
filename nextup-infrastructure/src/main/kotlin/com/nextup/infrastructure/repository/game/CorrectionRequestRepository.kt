package com.nextup.infrastructure.repository.game

import com.nextup.core.domain.game.CorrectionRequest
import com.nextup.core.domain.game.CorrectionRequestStatus
import com.nextup.core.port.repository.CorrectionRequestRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository

interface CorrectionRequestRepository :
    JpaRepository<CorrectionRequest, Long>,
    CorrectionRequestRepositoryPort {
    override fun findByIdOrNull(id: Long): CorrectionRequest? = findById(id).orElse(null)

    override fun findByGameId(gameId: Long): List<CorrectionRequest>

    override fun findByStatus(status: CorrectionRequestStatus): List<CorrectionRequest>

    override fun findByRequesterUserId(requesterUserId: Long): List<CorrectionRequest>
}
