package com.nextup.backoffice.dto.player

/**
 * 벌크 임포트 결과 응답 DTO
 *
 * 변환 로직은 PlayerExtensions.kt의 Extension Function을 사용합니다.
 */
data class ImportResultResponse(
    val successCount: Int,
    val errorCount: Int,
    val errors: List<ImportErrorResponse>,
)

/**
 * 임포트 오류 상세 응답 DTO
 *
 * 변환 로직은 PlayerExtensions.kt의 Extension Function을 사용합니다.
 */
data class ImportErrorResponse(
    val rowNumber: Int,
    val reason: String,
)
