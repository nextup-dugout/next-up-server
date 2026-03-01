package com.nextup.backoffice.dto.player

import com.nextup.core.service.player.PlayerImportError
import com.nextup.core.service.player.PlayerImportResult

/**
 * 벌크 임포트 결과 응답 DTO
 */
data class ImportResultResponse(
    val successCount: Int,
    val errorCount: Int,
    val errors: List<ImportErrorResponse>,
) {
    companion object {
        fun from(result: PlayerImportResult): ImportResultResponse =
            ImportResultResponse(
                successCount = result.successCount,
                errorCount = result.errorCount,
                errors = result.errors.map { ImportErrorResponse.from(it) },
            )
    }
}

/**
 * 임포트 오류 상세 응답 DTO
 */
data class ImportErrorResponse(
    val rowNumber: Int,
    val reason: String,
) {
    companion object {
        fun from(error: PlayerImportError): ImportErrorResponse =
            ImportErrorResponse(
                rowNumber = error.rowNumber,
                reason = error.reason,
            )
    }
}
