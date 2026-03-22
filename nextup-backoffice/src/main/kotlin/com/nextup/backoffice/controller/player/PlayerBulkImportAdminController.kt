package com.nextup.backoffice.controller.player

import com.nextup.backoffice.dto.player.ImportResultResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.common.exception.InvalidInputException
import com.nextup.core.service.player.PlayerBulkImportService
import com.nextup.core.service.player.PlayerImportRow
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

/**
 * 선수 벌크 임포트 API (관리자용)
 *
 * CSV 파일 기반 선수 일괄 등록을 제공합니다.
 *
 * CSV 형식 (헤더 행 필수):
 * name,primary_position,birth_date,height,weight,throwing_hand,batting_hand
 *
 * 예시:
 * 홍길동,SHORTSTOP,1990-01-15,180,75,RIGHT,RIGHT
 * 김철수,STARTING_PITCHER,,,,LEFT,
 */
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/backoffice/import")
class PlayerBulkImportAdminController(
    private val playerBulkImportService: PlayerBulkImportService,
) {
    /**
     * CSV 파일을 업로드하여 선수를 일괄 등록합니다.
     *
     * - 오류 행은 건너뛰고 성공한 행만 저장합니다.
     * - 응답에 성공/오류 건수 및 오류 상세가 포함됩니다.
     */
    @PostMapping("/players", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun importPlayers(
        @RequestParam("file") file: MultipartFile,
    ): ApiResponse<ImportResultResponse> {
        if (file.isEmpty) {
            throw InvalidInputException("EMPTY_FILE", "업로드된 파일이 비어 있습니다")
        }
        val contentType = file.contentType
        if (contentType != null &&
            contentType != "text/csv" &&
            contentType != "application/vnd.ms-excel" &&
            !contentType.startsWith("text/")
        ) {
            throw InvalidInputException("INVALID_FILE_TYPE", "CSV 파일만 업로드할 수 있습니다")
        }

        val rows = parseCsvFile(file)
        val result = playerBulkImportService.importPlayers(rows)
        return ApiResponse.success(ImportResultResponse.from(result))
    }

    private fun parseCsvFile(file: MultipartFile): List<PlayerImportRow> {
        val lines =
            file.inputStream
                .bufferedReader(Charsets.UTF_8)
                .readLines()
                .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            throw InvalidInputException("EMPTY_CSV", "CSV 파일에 데이터가 없습니다")
        }

        // Skip header row (first line)
        val dataLines = lines.drop(1)

        if (dataLines.isEmpty()) {
            throw InvalidInputException("NO_DATA_ROWS", "CSV 파일에 헤더 외 데이터 행이 없습니다")
        }

        return dataLines.mapIndexed { index, line ->
            val columns = line.split(",").map { it.trim() }
            // rowNumber is 1-based, +2 to account for header row
            val rowNumber = index + 2

            PlayerImportRow(
                rowNumber = rowNumber,
                name = columns.getOrElse(0) { "" },
                primaryPosition = columns.getOrElse(1) { "" },
                birthDate = columns.getOrElse(2) { "" }.ifBlank { null },
                height = columns.getOrElse(3) { "" }.ifBlank { null }?.toIntOrNull(),
                weight = columns.getOrElse(4) { "" }.ifBlank { null }?.toIntOrNull(),
                throwingHand = columns.getOrElse(5) { "" }.ifBlank { null },
                battingHand = columns.getOrElse(6) { "" }.ifBlank { null },
            )
        }
    }
}
