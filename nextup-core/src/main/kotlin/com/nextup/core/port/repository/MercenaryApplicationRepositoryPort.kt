package com.nextup.core.port.repository

import com.nextup.core.domain.mercenary.MercenaryApplication

/**
 * MercenaryApplication Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface MercenaryApplicationRepositoryPort {
    fun save(application: MercenaryApplication): MercenaryApplication

    fun findByIdOrNull(id: Long): MercenaryApplication?

    fun findByRequestId(requestId: Long): List<MercenaryApplication>

    fun findByPlayerId(playerId: Long): List<MercenaryApplication>

    fun existsByRequestIdAndPlayerId(
        requestId: Long,
        playerId: Long,
    ): Boolean

    fun countAcceptedByRequestId(requestId: Long): Long
}
