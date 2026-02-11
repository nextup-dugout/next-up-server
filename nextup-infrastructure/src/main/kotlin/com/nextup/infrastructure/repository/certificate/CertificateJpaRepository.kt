package com.nextup.infrastructure.repository.certificate

import com.nextup.core.domain.certificate.Certificate
import com.nextup.core.domain.certificate.CertificateStatus
import org.springframework.data.jpa.repository.JpaRepository

/**
 * Certificate JPA Repository
 */
interface CertificateJpaRepository : JpaRepository<Certificate, Long> {
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
}
