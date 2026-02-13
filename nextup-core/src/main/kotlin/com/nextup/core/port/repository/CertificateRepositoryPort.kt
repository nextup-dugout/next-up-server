package com.nextup.core.port.repository

import com.nextup.core.domain.certificate.Certificate
import com.nextup.core.domain.certificate.CertificateStatus

/**
 * Certificate Repository Port
 * Core 모듈의 Repository 인터페이스 - Infrastructure에서 구현
 */
interface CertificateRepositoryPort {
    /**
     * 증명서를 저장합니다.
     */
    fun save(certificate: Certificate): Certificate

    /**
     * ID로 증명서를 조회합니다.
     */
    fun findById(id: Long): Certificate?

    /**
     * 발급 번호로 증명서를 조회합니다.
     */
    fun findByIssueNumber(issueNumber: String): Certificate?

    /**
     * 선수 ID로 모든 증명서를 조회합니다.
     */
    fun findAllByPlayerId(playerId: Long): List<Certificate>

    /**
     * 선수 ID와 상태로 증명서를 조회합니다.
     */
    fun findAllByPlayerIdAndStatus(
        playerId: Long,
        status: CertificateStatus,
    ): List<Certificate>

    /**
     * 증명서를 삭제합니다.
     */
    fun delete(certificate: Certificate)
}
