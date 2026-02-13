package com.nextup.core.port.repository

import com.nextup.core.domain.election.Election

/**
 * Election Repository Port
 */
interface ElectionRepositoryPort {
    /**
     * Election을 저장합니다.
     */
    fun save(election: Election): Election

    /**
     * ID로 Election을 조회합니다.
     */
    fun findById(id: Long): Election?

    /**
     * 팀 ID로 모든 Election을 조회합니다.
     */
    fun findAllByTeamId(teamId: Long): List<Election>

    /**
     * Election을 삭제합니다.
     */
    fun delete(election: Election)
}
