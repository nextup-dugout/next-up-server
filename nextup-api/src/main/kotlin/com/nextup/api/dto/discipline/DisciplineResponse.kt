package com.nextup.api.dto.discipline

import com.nextup.core.domain.discipline.Discipline
import com.nextup.core.domain.discipline.DisciplineStatus
import com.nextup.core.domain.discipline.DisciplineType
import java.time.LocalDateTime

/**
 * 징계 응답 DTO (공개 API용)
 *
 * api 모듈에 독립적으로 존재
 */
data class DisciplineResponse(
    val id: Long,
    val type: DisciplineType,
    val reason: String,
    val suspensionGames: Int?,
    val servedGames: Int,
    val issuedAt: LocalDateTime,
    val expiresAt: LocalDateTime?,
    val status: DisciplineStatus,
    val isEffective: Boolean,
) {
    companion object {
        fun from(discipline: Discipline): DisciplineResponse =
            DisciplineResponse(
                id = discipline.id,
                type = discipline.type,
                reason = discipline.reason,
                suspensionGames = discipline.suspensionGames,
                servedGames = discipline.servedGames,
                issuedAt = discipline.issuedAt,
                expiresAt = discipline.expiresAt,
                status = discipline.status,
                isEffective = discipline.isEffective(),
            )
    }
}

/**
 * 선수 징계 이력 응답 DTO
 */
data class PlayerDisciplineHistoryResponse(
    val playerId: Long,
    val playerName: String,
    val totalDisciplines: Int,
    val activeDisciplines: Int,
    val disciplines: List<DisciplineResponse>,
)
