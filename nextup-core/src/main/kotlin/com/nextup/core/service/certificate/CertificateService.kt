package com.nextup.core.service.certificate

import com.nextup.core.dto.certificate.CertificateDto
import com.nextup.core.dto.certificate.CertificateVerificationDto

/**
 * 증명서 서비스 인터페이스
 *
 * 증명서 발급, 조회, 검증 기능을 제공합니다.
 * Infrastructure 계층에서 구현됩니다.
 */
interface CertificateService {
    /**
     * 선수의 기록 증명서를 발급합니다.
     *
     * @param playerId 선수 ID
     * @param validityDays 유효 기간 (일)
     * @return 발급된 증명서 DTO
     */
    fun issueCertificate(
        playerId: Long,
        validityDays: Long = 365,
    ): CertificateDto

    /**
     * 증명서를 조회합니다.
     *
     * @param certificateId 증명서 ID
     * @return 증명서 DTO
     */
    fun getCertificate(certificateId: Long): CertificateDto

    /**
     * 발급 번호로 증명서를 조회합니다.
     *
     * @param issueNumber 발급 번호
     * @return 증명서 DTO
     */
    fun getCertificateByIssueNumber(issueNumber: String): CertificateDto

    /**
     * 증명서의 유효성을 검증합니다.
     *
     * @param issueNumber 발급 번호
     * @return 검증 결과 DTO
     */
    fun verifyCertificate(issueNumber: String): CertificateVerificationDto

    /**
     * 선수의 모든 증명서를 조회합니다.
     *
     * @param playerId 선수 ID
     * @return 증명서 DTO 리스트
     */
    fun getPlayerCertificates(playerId: Long): List<CertificateDto>

    /**
     * 증명서를 취소합니다.
     *
     * @param certificateId 증명서 ID
     */
    fun revokeCertificate(certificateId: Long)
}
