package com.nextup.backoffice.dto.competition

import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import java.time.Instant
import java.time.LocalDate

/**
 * 대회 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 * 변환 로직은 CompetitionExtensions.kt의 Extension Function을 사용합니다.
 */
data class CompetitionAdminResponse(
    val id: Long,
    val leagueId: Long,
    val leagueName: String,
    val leagueAbbreviation: String?,
    val name: String,
    val year: Int,
    val season: Int,
    val type: CompetitionType,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val status: CompetitionStatus,
    val description: String?,
    val maxTeams: Int?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
