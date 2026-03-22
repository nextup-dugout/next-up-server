package com.nextup.core.port.repository

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus

/**
 * Competition Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface CompetitionRepositoryPort {
    fun save(competition: Competition): Competition

    fun findAll(): List<Competition>

    fun findByIdOrNull(id: Long): Competition?

    fun delete(competition: Competition)

    fun deleteById(id: Long)

    fun existsById(id: Long): Boolean

    /**
     * 리그별 대회 목록 조회
     */
    fun findByLeagueId(leagueId: Long): List<Competition>

    /**
     * 리그 + 연도 + 시즌으로 대회 조회
     */
    fun findByLeagueIdAndYearAndSeason(
        leagueId: Long,
        year: Int,
        season: Int,
    ): Competition?

    /**
     * 특정 상태의 대회 목록 조회
     */
    fun findByStatus(status: CompetitionStatus): List<Competition>

    /**
     * 진행 중인 대회 목록 조회
     */
    fun findInProgressCompetitions(): List<Competition>

    /**
     * 리그와 함께 대회 조회 (N+1 방지)
     */
    fun findByIdWithLeague(id: Long): Competition?

    /**
     * 이름에 키워드가 포함된 대회 목록을 조회합니다. (대소문자 무시)
     * 빈 리스트 전달 시 빈 결과를 반환합니다.
     */
    fun findByNameContaining(
        name: String,
        limit: Int,
    ): List<Competition>
}
