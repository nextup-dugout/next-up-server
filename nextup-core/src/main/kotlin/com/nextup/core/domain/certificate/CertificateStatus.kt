package com.nextup.core.domain.certificate

/**
 * 증명서 상태
 */
enum class CertificateStatus {
    /**
     * 유효한 증명서
     */
    VALID,

    /**
     * 만료된 증명서
     */
    EXPIRED,

    /**
     * 취소된 증명서
     */
    REVOKED,
}
