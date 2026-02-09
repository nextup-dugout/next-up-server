package com.nextup.infrastructure.repository.certificate

import com.nextup.core.domain.certificate.Certificate
import com.nextup.core.domain.certificate.CertificateStatus
import com.nextup.core.port.repository.CertificateRepositoryPort
import org.springframework.stereotype.Repository

/**
 * Certificate Repository Adapter
 * Core의 CertificateRepositoryPort를 Infrastructure의 JPA Repository로 구현
 */
@Repository
class CertificateRepositoryAdapter(
    private val jpaRepository: CertificateJpaRepository,
) : CertificateRepositoryPort {
    override fun save(certificate: Certificate): Certificate = jpaRepository.save(certificate)

    override fun findById(id: Long): Certificate? = jpaRepository.findById(id).orElse(null)

    override fun findByIssueNumber(issueNumber: String): Certificate? = jpaRepository.findByIssueNumber(issueNumber)

    override fun findAllByPlayerId(playerId: Long): List<Certificate> = jpaRepository.findAllByPlayerId(playerId)

    override fun findAllByPlayerIdAndStatus(
        playerId: Long,
        status: CertificateStatus,
    ): List<Certificate> = jpaRepository.findAllByPlayerIdAndStatus(playerId, status)

    override fun delete(certificate: Certificate) {
        jpaRepository.delete(certificate)
    }
}
