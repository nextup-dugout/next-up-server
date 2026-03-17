package com.nextup.core.port.repository

import com.nextup.core.domain.mercenary.MercenaryRequest
import com.nextup.core.domain.mercenary.MercenaryRequestStatus

/**
 * MercenaryRequest Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface MercenaryRequestRepositoryPort {
    fun save(mercenaryRequest: MercenaryRequest): MercenaryRequest

    fun findByIdOrNull(id: Long): MercenaryRequest?

    fun findByStatus(status: MercenaryRequestStatus): List<MercenaryRequest>

    fun findByRequestingTeamId(teamId: Long): List<MercenaryRequest>

    fun findByGameId(gameId: Long): List<MercenaryRequest>
}
