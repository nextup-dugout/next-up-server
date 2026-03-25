package com.nextup.backoffice.dto.player

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.service.player.PlayerImportError
import com.nextup.core.service.player.PlayerImportResult as CorePlayerImportResult

/**
 * Player Entity를 PlayerImportResult DTO로 변환하는 Extension Function
 */
fun Player.toImportResult(): PlayerImportResult =
    PlayerImportResult(
        id = this.id,
        name = this.name,
        primaryPosition = this.primaryPosition.displayName,
        birthDate = this.birthDate,
    )

/**
 * PlayerTeamHistory Entity를 PlayerTeamResponse DTO로 변환하는 Extension Function
 */
fun PlayerTeamHistory.toResponse(): PlayerTeamResponse =
    PlayerTeamResponse(
        id = this.id,
        playerId = this.player.id,
        playerName = this.player.name,
        teamId = this.team.id,
        teamName = this.team.fullName,
        teamLogoUrl = this.team.logoUrl,
        leagueId = this.team.league.id,
        leagueName = this.team.league.name,
        startDate = this.startDate,
        endDate = this.endDate,
        uniformNumber = this.uniformNumber,
        position = this.position,
        status = this.status,
        isCurrentAffiliation = this.isCurrentAffiliation,
        durationInDays = this.durationInDays,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )

/**
 * Core PlayerImportResult를 ImportResultResponse DTO로 변환하는 Extension Function
 */
fun CorePlayerImportResult.toResponse(): ImportResultResponse =
    ImportResultResponse(
        successCount = this.successCount,
        errorCount = this.errorCount,
        errors = this.errors.map { it.toResponse() },
    )

/**
 * PlayerImportError를 ImportErrorResponse DTO로 변환하는 Extension Function
 */
fun PlayerImportError.toResponse(): ImportErrorResponse =
    ImportErrorResponse(
        rowNumber = this.rowNumber,
        reason = this.reason,
    )
