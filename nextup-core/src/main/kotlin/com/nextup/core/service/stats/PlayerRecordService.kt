package com.nextup.core.service.stats

import com.nextup.core.service.stats.dto.PlayerRecordDto
import com.nextup.core.service.stats.dto.RecordScope
import com.nextup.core.service.stats.dto.RecordType

/**
 * 선수 기록 서비스 인터페이스
 *
 * 선수의 타격/투수 기록을 조회하는 서비스입니다.
 */
interface PlayerRecordService {
    /**
     * 선수 기록을 조회합니다.
     *
     * @param playerId 선수 ID
     * @param scope 조회 범위 (SEASON, CAREER, COMPETITION)
     * @param type 조회 타입 (BATTING, PITCHING, ALL)
     * @param year 시즌 연도 (scope=SEASON일 때 필수, null이면 현재 연도)
     * @param competitionId 대회 ID (scope=COMPETITION일 때 필수)
     * @return 선수 기록 DTO
     */
    fun getPlayerRecord(
        playerId: Long,
        scope: RecordScope,
        type: RecordType,
        year: Int?,
        competitionId: Long?,
    ): PlayerRecordDto
}
