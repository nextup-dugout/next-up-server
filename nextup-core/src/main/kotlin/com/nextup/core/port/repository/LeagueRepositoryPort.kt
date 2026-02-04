package com.nextup.core.port.repository

import com.nextup.core.domain.league.League

/**
 * League Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface LeagueRepositoryPort {
    fun save(league: League): League

    fun findAll(): List<League>

    fun findByIdOrNull(id: Long): League?

    fun delete(league: League)

    fun deleteById(id: Long)

    fun existsById(id: Long): Boolean

    /**
     * 협회 ID로 리그 목록을 조회합니다.
     */
    fun findByAssociationId(associationId: Long): List<League>

    /**
     * 협회 ID로 활성화된 리그 목록을 조회합니다.
     */
    fun findActiveByAssociationId(associationId: Long): List<League>

    /**
     * 활성화된 모든 리그를 조회합니다.
     */
    fun findAllActive(): List<League>

    /**
     * 협회 내에서 리그 이름 존재 여부를 확인합니다.
     */
    fun existsByAssociationIdAndName(
        associationId: Long,
        name: String,
    ): Boolean

    /**
     * 협회 내에서 리그 이름으로 조회합니다.
     */
    fun findByAssociationIdAndName(
        associationId: Long,
        name: String,
    ): League?

    /**
     * 부 레벨(divisionLevel)별로 리그를 조회합니다.
     */
    fun findActiveByDivisionLevel(divisionLevel: Int): List<League>
}
