package com.nextup.common.exception

/**
 * 증명서를 찾을 수 없을 때 발생하는 예외
 */
class CertificateNotFoundException(
    certificateId: Long,
) : NotFoundException(
        code = "CERTIFICATE_NOT_FOUND",
        message = "증명서를 찾을 수 없습니다: $certificateId",
    )

/**
 * 증명서 발급 번호로 찾을 수 없을 때 발생하는 예외
 */
class CertificateNotFoundByIssueNumberException(
    issueNumber: String,
) : NotFoundException(
        code = "CERTIFICATE_NOT_FOUND",
        message = "증명서를 찾을 수 없습니다: $issueNumber",
    )

/**
 * 만료된 증명서 접근 시 발생하는 예외
 */
class CertificateExpiredException(
    issueNumber: String,
) : InvalidStateException(
        code = "CERTIFICATE_EXPIRED",
        message = "만료된 증명서입니다: $issueNumber",
    )

/**
 * 취소된 증명서 접근 시 발생하는 예외
 */
class CertificateRevokedException(
    issueNumber: String,
) : InvalidStateException(
        code = "CERTIFICATE_REVOKED",
        message = "취소된 증명서입니다: $issueNumber",
    )
