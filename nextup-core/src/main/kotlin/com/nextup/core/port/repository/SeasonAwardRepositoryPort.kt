package com.nextup.core.port.repository

import com.nextup.core.domain.stats.SeasonAward
import com.nextup.core.domain.stats.SeasonAwardTitle

/**
 * SeasonAward Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface SeasonAwardRepositoryPort {
    fun save(seasonAward: SeasonAward): SeasonAward

    fun saveAll(seasonAwards: List<SeasonAward>): List<SeasonAward>

    fun findByIdOrNull(id: Long): SeasonAward?

    fun delete(seasonAward: SeasonAward)

    /**
     * 특정 연도의 모든 시즌 타이틀을 조회합니다.
     */
    fun findAllByYear(year: Int): List<SeasonAward>

    /**
     * 특정 선수의 모든 시즌 타이틀을 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<SeasonAward>

    /**
     * 특정 연도, 특정 타이틀의 수상자를 조회합니다.
     */
    fun findByYearAndTitle(
        year: Int,
        title: SeasonAwardTitle,
    ): List<SeasonAward>

    /**
     * 특정 대회의 모든 시즌 타이틀을 조회합니다.
     */
    fun findAllByCompetitionId(competitionId: Long): List<SeasonAward>

    /**
     * 특정 연도의 모든 시즌 타이틀을 삭제합니다 (재계산 시 사용).
     */
    fun deleteAllByYear(year: Int)

    /**
     * 특정 대회의 모든 시즌 타이틀을 삭제합니다 (재계산 시 사용).
     */
    fun deleteAllByCompetitionId(competitionId: Long)
}
