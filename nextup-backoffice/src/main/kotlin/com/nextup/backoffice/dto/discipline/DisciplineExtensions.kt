package com.nextup.backoffice.dto.discipline

import com.nextup.core.domain.discipline.Discipline

/**
 * Discipline Entity를 DisciplineAdminResponse DTO로 변환하는 Extension Function
 */
fun Discipline.toAdminResponse(): DisciplineAdminResponse =
    DisciplineAdminResponse(
        id = this.id,
        playerId = this.player.id,
        playerName = this.player.name,
        competitionId = this.competition.id,
        competitionName = this.competition.name,
        type = this.type,
        reason = this.reason,
        suspensionGames = this.suspensionGames,
        servedGames = this.servedGames,
        issuedAt = this.issuedAt,
        expiresAt = this.expiresAt,
        issuedBy = this.issuedBy,
        status = this.status,
        isEffective = this.isEffective(),
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
