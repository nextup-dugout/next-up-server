package com.nextup.infrastructure.repository.competition

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.port.repository.CompetitionRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CompetitionRepository :
    JpaRepository<Competition, Long>,
    CompetitionRepositoryPort {
    /**
     * 리그별 대회 목록 조회
     */
    override fun findByLeagueId(leagueId: Long): List<Competition>

    /**
     * 리그 + 연도 + 시즌으로 대회 조회
     */
    override fun findByLeagueIdAndYearAndSeason(
        leagueId: Long,
        year: Int,
        season: Int,
    ): Competition?

    /**
     * 특정 상태의 대회 목록 조회
     */
    override fun findByStatus(status: CompetitionStatus): List<Competition>

    /**
     * 진행 중인 대회 목록 조회
     */
    @Query("SELECT c FROM Competition c WHERE c.status = 'IN_PROGRESS'")
    override fun findInProgressCompetitions(): List<Competition>

    /**
     * 리그와 함께 대회 조회 (N+1 방지)
     */
    @Query("SELECT c FROM Competition c JOIN FETCH c.league WHERE c.id = :id")
    override fun findByIdWithLeague(id: Long): Competition?
}
