package com.nextup.api.controller.game

import com.nextup.api.dto.game.ScoresheetResponse
import com.nextup.api.mapper.game.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.port.PdfGeneratorPort
import com.nextup.core.service.game.ScoresheetService
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 공식 기록지 API 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/games/{gameId}/scoresheet")
class ScoresheetController(
    private val scoresheetService: ScoresheetService,
    private val pdfGenerator: PdfGeneratorPort,
) {
    /**
     * 경기의 공식 기록지 데이터를 조회합니다.
     *
     * @param gameId 경기 ID
     * @return 공식 기록지 데이터
     */
    @GetMapping
    fun getScoresheet(
        @PathVariable gameId: Long,
    ): ApiResponse<ScoresheetResponse> {
        val scoresheet = scoresheetService.getScoresheet(gameId)
        return ApiResponse.success(scoresheet.toResponse())
    }

    /**
     * 경기의 공식 기록지를 PDF로 다운로드합니다.
     *
     * @param gameId 경기 ID
     * @return PDF 파일
     */
    @GetMapping("/pdf")
    fun downloadScoresheetPdf(
        @PathVariable gameId: Long,
    ): ResponseEntity<ByteArray> {
        val scoresheet = scoresheetService.getScoresheet(gameId)
        val pdfBytes = pdfGenerator.generateScoresheetPdf(scoresheet)

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_PDF
        headers.setContentDispositionFormData(
            "attachment",
            "scoresheet_game_$gameId.pdf",
        )

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(pdfBytes)
    }
}
