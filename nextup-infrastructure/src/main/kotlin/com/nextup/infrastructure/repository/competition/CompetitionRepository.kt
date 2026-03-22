package com.nextup.infrastructure.repository.competition

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.port.repository.CompetitionRepositoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CompetitionRepository :
    JpaRepository<Competition, Long>,
    CompetitionRepositoryPort {
    override fun findByIdOrNull(id: Long): Competition? = findById(id).orElse(null)

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

    /**
     * 이름에 키워드가 포함된 대회 목록을 조회합니다. (대소문자 무시, Pageable 기반)
     */
    @Query(
        """
        SELECT c FROM Competition c
        WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))
    """,
    )
    fun findByNameContainingWithPageable(
        name: String,
        pageable: Pageable,
    ): List<Competition>

    override fun findByNameContaining(
        name: String,
        limit: Int,
    ): List<Competition> = findByNameContainingWithPageable(name, PageRequest.of(0, limit))
}
