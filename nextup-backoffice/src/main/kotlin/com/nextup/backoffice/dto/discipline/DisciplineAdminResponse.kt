package com.nextup.backoffice.dto.discipline

import com.nextup.core.domain.discipline.DisciplineStatus
import com.nextup.core.domain.discipline.DisciplineType
import java.time.Instant
import java.time.LocalDateTime

/**
 * 징계 관리자 응답 DTO
 *
 * backoffice 모듈에 독립적으로 존재
 * 변환 로직은 DisciplineExtensions.kt의 Extension Function을 사용합니다.
 */
data class DisciplineAdminResponse(
    val id: Long,
    val playerId: Long,
    val playerName: String,
    val competitionId: Long,
    val competitionName: String,
    val type: DisciplineType,
    val reason: String,
    val suspensionGames: Int?,
    val servedGames: Int,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime?,
    val issuedBy: String,
    val status: DisciplineStatus,
    val isEffective: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)
