package com.nextup.core.port.repository

import com.nextup.core.domain.competition.CompetitionPlayer
import com.nextup.core.domain.competition.CompetitionPlayerStatus

/**
 * 대회 등록 선수 저장소 포트
 */
interface CompetitionPlayerRepositoryPort {
    fun save(competitionPlayer: CompetitionPlayer): CompetitionPlayer

    fun saveAll(players: List<CompetitionPlayer>): List<CompetitionPlayer>

    fun findByIdOrNull(id: Long): CompetitionPlayer?

    fun findByCompetitionId(competitionId: Long): List<CompetitionPlayer>

    fun findByCompetitionIdAndTeamId(
        competitionId: Long,
        teamId: Long,
    ): List<CompetitionPlayer>

    fun findByCompetitionIdAndStatus(
        competitionId: Long,
        status: CompetitionPlayerStatus,
    ): List<CompetitionPlayer>

    /**
     * 대회에 등록된 선수 ID 목록을 조회합니다. (부정선수 체크용)
     */
    fun findPlayerIdsByCompetitionId(competitionId: Long): Set<Long>

    /**
     * 대회에 등록된 활성 선수 ID 목록을 조회합니다. (라인업 검증용)
     */
    fun findEligiblePlayerIdsByCompetitionId(competitionId: Long): Set<Long>

    fun findByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): CompetitionPlayer?

    fun existsByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): Boolean

    fun deleteById(id: Long)

    /**
     * 팀 ID와 상태로 대회 선수를 조회합니다.
     * 팀 해산 시 해당 팀의 활성 대회 선수를 처리하는 데 사용합니다.
     */
    fun findByTeamIdAndStatus(
        teamId: Long,
        status: CompetitionPlayerStatus,
    ): List<CompetitionPlayer>

    /**
     * 팀이 참가 중인(WITHDRAWN이 아닌) 대회 ID 목록을 조회합니다.
     * 팀 해산 시 해당 팀이 참가 중인 대회들을 식별하는 데 사용합니다.
     */
    fun findActiveCompetitionIdsByTeamId(teamId: Long): Set<Long>

    /**
     * 선수 ID와 상태 목록으로 대회 선수를 조회합니다.
     * 팀원 개별 탈퇴/강퇴 시 해당 선수의 활성 대회 등록을 처리하는 데 사용합니다.
     */
    fun findByPlayerIdAndStatusIn(
        playerId: Long,
        statuses: List<CompetitionPlayerStatus>,
    ): List<CompetitionPlayer>

    /**
     * 대회에서 선수의 활성(WITHDRAWN이 아닌) 등록을 조회합니다.
     * 크로스 등록 방지: 동일 대회 타팀 이중 등록 여부 확인에 사용합니다.
     */
    fun findActiveByCompetitionIdAndPlayerId(
        competitionId: Long,
        playerId: Long,
    ): CompetitionPlayer?
}
