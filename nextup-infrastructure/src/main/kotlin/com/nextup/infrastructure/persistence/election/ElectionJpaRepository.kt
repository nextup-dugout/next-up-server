package com.nextup.infrastructure.persistence.election

import com.nextup.core.domain.election.Election
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Election JPA Repository
 */
interface ElectionJpaRepository : JpaRepository<Election, Long> {
    /**
     * 팀 ID로 모든 Election을 조회합니다.
     */
    fun findAllByTeamId(teamId: Long): List<Election>
}
