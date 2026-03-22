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

    /**
     * 특정 선거를 부모로 하는 재선거 횟수를 조회합니다.
     * 재선거 체인 깊이를 확인하여 최대 횟수 제한에 사용합니다.
     */
    fun countByParentElectionId(parentElectionId: Long): Long
}
