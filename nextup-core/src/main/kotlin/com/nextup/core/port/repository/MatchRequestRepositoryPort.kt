package com.nextup.core.port.repository

import com.nextup.core.domain.match.MatchRequest
import com.nextup.core.domain.match.MatchRequestStatus
import com.nextup.core.domain.match.SkillLevel
import java.time.LocalDate

interface MatchRequestRepositoryPort {
    fun save(matchRequest: MatchRequest): MatchRequest

    fun findByIdOrNull(id: Long): MatchRequest?

    fun findByTeamId(teamId: Long): List<MatchRequest>

    fun findAllOpen(): List<MatchRequest>

    fun findByStatus(status: MatchRequestStatus): List<MatchRequest>

    /**
     * OPEN 상태의 매칭 요청을 필터 조건으로 조회합니다.
     *
     * @param area 지역 필터 (선호 장소에 포함된 문자열, null이면 무시)
     * @param date 날짜 필터 (null이면 무시)
     * @param skillLevel 실력 수준 필터 (null이면 무시)
     */
    fun findAllOpenWithFilter(
        area: String?,
        date: LocalDate?,
        skillLevel: SkillLevel?,
    ): List<MatchRequest>
}
