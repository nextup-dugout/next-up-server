package com.nextup.infrastructure.repository.association

import com.nextup.core.domain.association.Association
import com.nextup.core.port.repository.AssociationRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AssociationRepository :
    JpaRepository<Association, Long>,
    AssociationRepositoryPort {
    /**
     * 협회 이름으로 조회합니다.
     */
    override fun findByName(name: String): Association?

    /**
     * 협회 이름 존재 여부를 확인합니다.
     */
    override fun existsByName(name: String): Boolean

    /**
     * 활성화된 협회 목록을 조회합니다.
     */
    @Query("SELECT a FROM Association a WHERE a.isActive = true ORDER BY a.name")
    override fun findAllActive(): List<Association>

    /**
     * 지역별 협회 목록을 조회합니다.
     */
    override fun findByRegion(region: String): List<Association>

    /**
     * 지역별 활성화된 협회 목록을 조회합니다.
     */
    @Query("SELECT a FROM Association a WHERE a.region = :region AND a.isActive = true ORDER BY a.name")
    override fun findActiveByRegion(region: String): List<Association>
}
