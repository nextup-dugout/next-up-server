package com.nextup.backoffice.dto.player

import com.nextup.core.domain.player.Player
import java.time.LocalDate

/**
 * 선수 벌크 임포트 결과 응답 DTO
 */
data class PlayerBulkImportResponse(
    val totalRequested: Int,
    val successCount: Int,
    val failureCount: Int,
    val importedPlayers: List<PlayerImportResult>,
    val failures: List<PlayerImportFailure>,
)

data class PlayerImportResult(
    val id: Long,
    val name: String,
    val primaryPosition: String,
    val birthDate: LocalDate?,
) {
    companion object {
        fun from(player: Player): PlayerImportResult =
            PlayerImportResult(
                id = player.id,
                name = player.name,
                primaryPosition = player.primaryPosition.displayName,
                birthDate = player.birthDate,
            )
    }
}

data class PlayerImportFailure(
    val rowIndex: Int,
    val name: String?,
    val reason: String,
)
