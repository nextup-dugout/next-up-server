package com.nextup.core.port

import com.nextup.core.service.game.dto.ScoresheetDto

/**
 * PDF 생성 포트 인터페이스
 *
 * Infrastructure 계층에서 실제 PDF 라이브러리를 사용하여 구현합니다.
 */
interface PdfGeneratorPort {
    /**
     * 공식 기록지 데이터를 PDF로 변환합니다.
     *
     * @param scoresheet 공식 기록지 데이터
     * @return PDF 바이트 배열
     */
    fun generateScoresheetPdf(scoresheet: ScoresheetDto): ByteArray
}
