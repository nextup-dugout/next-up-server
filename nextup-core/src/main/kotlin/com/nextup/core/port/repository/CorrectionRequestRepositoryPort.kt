package com.nextup.core.port.repository

import com.nextup.core.domain.game.CorrectionRequest
import com.nextup.core.domain.game.CorrectionRequestStatus

/**
 * CorrectionRequest Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface CorrectionRequestRepositoryPort {
    fun save(correctionRequest: CorrectionRequest): CorrectionRequest

    fun findByIdOrNull(id: Long): CorrectionRequest?

    fun findByGameId(gameId: Long): List<CorrectionRequest>

    fun findByStatus(status: CorrectionRequestStatus): List<CorrectionRequest>

    fun findByRequesterUserId(requesterUserId: Long): List<CorrectionRequest>
}
