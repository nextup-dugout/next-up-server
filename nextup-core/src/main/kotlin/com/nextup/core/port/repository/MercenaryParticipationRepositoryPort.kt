package com.nextup.core.port.repository

import com.nextup.core.domain.mercenary.MercenaryParticipation

/**
 * MercenaryParticipation Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface MercenaryParticipationRepositoryPort {
    fun save(participation: MercenaryParticipation): MercenaryParticipation

    fun findByGameId(gameId: Long): List<MercenaryParticipation>

    fun findByPlayerId(playerId: Long): List<MercenaryParticipation>

    fun findByTeamId(teamId: Long): List<MercenaryParticipation>

    fun existsByGameIdAndPlayerId(
        gameId: Long,
        playerId: Long,
    ): Boolean
}
