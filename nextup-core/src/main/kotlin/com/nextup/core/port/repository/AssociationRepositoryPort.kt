package com.nextup.core.port.repository

import com.nextup.core.domain.association.Association
import java.util.Optional

/**
 * Association Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface AssociationRepositoryPort {

    fun save(association: Association): Association

    fun findAll(): List<Association>

    fun findByIdOrNull(id: Long): Association?

    fun delete(association: Association)

    fun deleteById(id: Long)

    fun existsById(id: Long): Boolean

    /**
     * 협회 이름으로 조회합니다.
     */
    fun findByName(name: String): Association?

    /**
     * 협회 이름 존재 여부를 확인합니다.
     */
    fun existsByName(name: String): Boolean

    /**
     * 활성화된 협회 목록을 조회합니다.
     */
    fun findAllActive(): List<Association>

    /**
     * 지역별 협회 목록을 조회합니다.
     */
    fun findByRegion(region: String): List<Association>

    /**
     * 지역별 활성화된 협회 목록을 조회합니다.
     */
    fun findActiveByRegion(region: String): List<Association>
}
