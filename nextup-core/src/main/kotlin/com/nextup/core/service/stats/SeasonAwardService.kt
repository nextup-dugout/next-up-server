package com.nextup.core.service.stats

import com.nextup.core.domain.stats.SeasonAward

/**
 * 시즌 타이틀(개인상) 서비스 인터페이스
 *
 * 시즌 종료 시 각 부문별 타이틀을 자동으로 계산하고 부여합니다.
 * 규정타석, 규정이닝 등의 기준을 적용합니다.
 */
interface SeasonAwardService {
    /**
     * 시즌 종료 후 각 부문별 타이틀을 계산하고 부여합니다.
     *
     * 기존에 부여된 동일 연도 타이틀은 삭제 후 재계산합니다.
     *
     * @param year 시즌 연도
     * @param minPlateAppearances 규정타석 (타격 부문 타이틀 자격 기준)
     * @param minInningsPitchedOuts 규정이닝 아웃 수 (투수 부문 타이틀 자격 기준)
     * @return 부여된 타이틀 목록
     */
    fun calculateAndAwardTitles(
        year: Int,
        minPlateAppearances: Int,
        minInningsPitchedOuts: Int,
    ): List<SeasonAward>

    /**
     * 특정 연도의 시즌 타이틀 목록을 조회합니다.
     *
     * @param year 시즌 연도
     * @return 해당 연도의 타이틀 목록
     */
    fun getAwardsByYear(year: Int): List<SeasonAward>

    /**
     * 특정 선수의 시즌 타이틀 목록을 조회합니다.
     *
     * @param playerId 선수 ID
     * @return 해당 선수의 타이틀 목록
     */
    fun getAwardsByPlayerId(playerId: Long): List<SeasonAward>
}
