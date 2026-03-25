package com.nextup.backoffice.dto.player

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

/**
 * 선수 임포트 결과 DTO
 *
 * 변환 로직은 PlayerExtensions.kt의 Extension Function을 사용합니다.
 */
data class PlayerImportResult(
    val id: Long,
    val name: String,
    val primaryPosition: String,
    val birthDate: LocalDate?,
)

data class PlayerImportFailure(
    val rowIndex: Int,
    val name: String?,
    val reason: String,
)
