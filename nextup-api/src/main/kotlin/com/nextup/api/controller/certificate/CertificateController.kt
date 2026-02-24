package com.nextup.api.controller.certificate

import com.nextup.api.dto.certificate.CertificateResponse
import com.nextup.api.dto.certificate.CertificateVerificationResponse
import com.nextup.api.dto.certificate.IssueCertificateRequest
import com.nextup.api.mapper.certificate.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.service.certificate.CertificateService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

/**
 * 기록 증명서 API
 *
 * 선수의 기록 증명서 발급, 조회, 검증 기능을 제공합니다.
 * 모든 응답은 ApiResponse로 래핑됩니다.
 */
@RestController
@RequestMapping("/api/v1")
class CertificateController(
    private val certificateService: CertificateService,
) {
    /**
     * 선수의 기록 증명서를 발급합니다.
     *
     * POST /api/v1/players/{playerId}/certificate
     *
     * @param playerId 선수 ID
     * @param request 증명서 발급 요청 (유효 기간)
     * @return 발급된 증명서
     */
    @PostMapping("/players/{playerId}/certificate")
    @ResponseStatus(HttpStatus.CREATED)
    fun issueCertificate(
        @PathVariable playerId: Long,
        @RequestBody @Valid request: IssueCertificateRequest = IssueCertificateRequest(),
    ): ApiResponse<CertificateResponse> {
        val certificate = certificateService.issueCertificate(playerId, request.validityDays)
        return ApiResponse.success(certificate.toResponse())
    }

    /**
     * 증명서를 조회합니다.
     *
     * GET /api/v1/certificates/{certificateId}
     *
     * @param certificateId 증명서 ID
     * @return 증명서 정보
     */
    @GetMapping("/certificates/{certificateId}")
    fun getCertificate(
        @PathVariable certificateId: Long,
    ): ApiResponse<CertificateResponse> {
        val certificate = certificateService.getCertificate(certificateId)
        return ApiResponse.success(certificate.toResponse())
    }

    /**
     * 증명서의 유효성을 검증합니다.
     *
     * GET /api/v1/certificates/{issueNumber}/verify
     *
     * @param issueNumber 발급 번호 (UUID)
     * @return 검증 결과
     */
    @GetMapping("/certificates/{issueNumber}/verify")
    fun verifyCertificate(
        @PathVariable issueNumber: String,
    ): ApiResponse<CertificateVerificationResponse> {
        val verification = certificateService.verifyCertificate(issueNumber)
        return ApiResponse.success(verification.toResponse())
    }

    /**
     * 선수의 모든 증명서를 조회합니다.
     *
     * GET /api/v1/players/{playerId}/certificates
     *
     * @param playerId 선수 ID
     * @return 증명서 리스트
     */
    @GetMapping("/players/{playerId}/certificates")
    fun getPlayerCertificates(
        @PathVariable playerId: Long,
    ): ApiResponse<List<CertificateResponse>> {
        val certificates = certificateService.getPlayerCertificates(playerId)
        return ApiResponse.success(certificates.toResponse())
    }

    /**
     * 증명서를 취소합니다.
     *
     * DELETE /api/v1/certificates/{certificateId}
     *
     * @param certificateId 증명서 ID
     */
    @DeleteMapping("/certificates/{certificateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revokeCertificate(
        @PathVariable certificateId: Long,
    ): ApiResponse<Unit> {
        certificateService.revokeCertificate(certificateId)
        return ApiResponse.success(Unit)
    }
}
