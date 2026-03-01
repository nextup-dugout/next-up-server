package com.nextup.core.service.player

import com.nextup.core.domain.player.Player

/**
 * 선수 벌크 임포트 서비스 인터페이스
 *
 * CSV 파일 기반 선수 일괄 등록 유스케이스를 정의합니다.
 */
interface PlayerBulkImportService {
    /**
     * 선수 데이터를 일괄 임포트합니다.
     *
     * @param rows CSV에서 파싱된 선수 데이터 행 목록
     * @return 임포트 결과 (성공/실패 건수, 오류 상세)
     */
    fun importPlayers(rows: List<PlayerImportRow>): PlayerImportResult
}

/**
 * CSV 한 행에서 파싱된 선수 임포트 데이터
 */
data class PlayerImportRow(
    val rowNumber: Int,
    val name: String,
    val primaryPosition: String,
    val birthDate: String?,
    val height: Int?,
    val weight: Int?,
    val throwingHand: String?,
    val battingHand: String?,
)

/**
 * 벌크 임포트 결과
 */
data class PlayerImportResult(
    val successCount: Int,
    val errorCount: Int,
    val importedPlayers: List<Player>,
    val errors: List<PlayerImportError>,
)

/**
 * 임포트 오류 상세
 */
data class PlayerImportError(
    val rowNumber: Int,
    val reason: String,
)
