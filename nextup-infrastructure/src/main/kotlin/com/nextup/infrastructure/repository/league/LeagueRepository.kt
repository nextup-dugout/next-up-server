package com.nextup.infrastructure.repository.league

import com.nextup.core.domain.league.League
import com.nextup.core.port.repository.LeagueRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface LeagueRepository :
    JpaRepository<League, Long>,
    LeagueRepositoryPort {
    /**
     * 협회 ID로 리그 목록을 조회합니다.
     */
    override fun findByAssociationId(associationId: Long): List<League>

    /**
     * 협회 ID로 활성화된 리그 목록을 조회합니다.
     */
    @Query(
        "SELECT l FROM League l WHERE l.association.id = :associationId AND l.isActive = true ORDER BY l.divisionLevel, l.name"
    )
    override fun findActiveByAssociationId(associationId: Long): List<League>

    /**
     * 활성화된 모든 리그를 조회합니다.
     */
    @Query("SELECT l FROM League l WHERE l.isActive = true ORDER BY l.association.name, l.divisionLevel, l.name")
    override fun findAllActive(): List<League>

    /**
     * 협회 내에서 리그 이름 존재 여부를 확인합니다.
     */
    override fun existsByAssociationIdAndName(
        associationId: Long,
        name: String,
    ): Boolean

    /**
     * 협회 내에서 리그 이름으로 조회합니다.
     */
    override fun findByAssociationIdAndName(
        associationId: Long,
        name: String,
    ): League?

    /**
     * 부 레벨(divisionLevel)별로 리그를 조회합니다.
     */
    @Query(
        "SELECT l FROM League l WHERE l.divisionLevel = :divisionLevel AND l.isActive = true ORDER BY l.association.name, l.name"
    )
    override fun findActiveByDivisionLevel(divisionLevel: Int): List<League>
}
