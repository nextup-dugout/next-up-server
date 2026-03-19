package com.nextup.infrastructure.repository.stats

import com.nextup.core.domain.stats.SeasonAward
import com.nextup.core.domain.stats.SeasonAwardTitle
import com.nextup.core.port.repository.SeasonAwardRepositoryPort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SeasonAwardRepository :
    JpaRepository<SeasonAward, Long>,
    SeasonAwardRepositoryPort {
    override fun findByIdOrNull(id: Long): SeasonAward? = findById(id).orElse(null)

    override fun saveAll(seasonAwards: List<SeasonAward>): List<SeasonAward> =
        saveAll(seasonAwards as Iterable<SeasonAward>)

    /**
     * 특정 연도의 모든 시즌 타이틀을 조회합니다.
     */
    @Query("SELECT a FROM SeasonAward a JOIN FETCH a.player WHERE a.year = :year ORDER BY a.title")
    override fun findAllByYear(
        @Param("year") year: Int,
    ): List<SeasonAward>

    /**
     * 특정 선수의 모든 시즌 타이틀을 조회합니다.
     */
    @Query("SELECT a FROM SeasonAward a WHERE a.player.id = :playerId ORDER BY a.year DESC, a.title")
    override fun findAllByPlayerId(
        @Param("playerId") playerId: Long,
    ): List<SeasonAward>

    /**
     * 특정 연도, 특정 타이틀의 수상자를 조회합니다.
     */
    @Query(
        "SELECT a FROM SeasonAward a JOIN FETCH a.player WHERE a.year = :year AND a.title = :title",
    )
    override fun findByYearAndTitle(
        @Param("year") year: Int,
        @Param("title") title: SeasonAwardTitle,
    ): List<SeasonAward>

    /**
     * 특정 연도의 모든 시즌 타이틀을 삭제합니다 (재계산 시 사용).
     */
    @Modifying
    @Query("DELETE FROM SeasonAward a WHERE a.year = :year")
    override fun deleteAllByYear(
        @Param("year") year: Int,
    )
}
