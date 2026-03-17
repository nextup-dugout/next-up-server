package com.nextup.core.service.match.dto

import com.nextup.core.domain.match.SkillLevel
import java.time.LocalDate

/**
 * 매칭 요청 필터 DTO
 *
 * 위치/날짜/실력 수준으로 매칭 요청을 필터링합니다.
 */
data class MatchRequestFilterDto(
    val area: String? = null,
    val date: LocalDate? = null,
    val skillLevel: SkillLevel? = null,
) {
    /**
     * 필터 조건이 하나라도 설정되어 있는지 확인합니다.
     */
    fun hasAnyFilter(): Boolean = area != null || date != null || skillLevel != null
}
