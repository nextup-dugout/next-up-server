package com.nextup.core.domain.certificate

import com.nextup.core.common.BaseTimeEntity
import com.nextup.core.domain.player.Player
import jakarta.persistence.*
import java.time.Instant
import java.util.*

/**
 * 기록 증명서 엔티티
 *
 * 선수의 경기 기록을 증명하는 공식 문서입니다.
 * QR 코드를 통해 진위 여부를 확인할 수 있습니다.
 */
@Entity
@Table(
    name = "certificates",
    indexes = [
        Index(name = "idx_certificates_issue_number", columnList = "issue_number", unique = true),
        Index(name = "idx_certificates_player_id", columnList = "player_id"),
        Index(name = "idx_certificates_status", columnList = "status"),
        Index(name = "idx_certificates_expires_at", columnList = "expires_at"),
    ],
)
class Certificate private constructor(
    @Column(name = "issue_number", nullable = false, unique = true, length = 36)
    val issueNumber: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    val player: Player,
    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant,
    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: CertificateStatus = CertificateStatus.VALID,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    companion object {
        /**
         * 새로운 증명서를 발급합니다.
         *
         * @param player 선수
         * @param validityDays 유효 기간 (일)
         * @return 발급된 증명서
         */
        fun issue(
            player: Player,
            validityDays: Long = 365,
        ): Certificate {
            val now = Instant.now()
            return Certificate(
                issueNumber = UUID.randomUUID().toString(),
                player = player,
                issuedAt = now,
                expiresAt = now.plusSeconds(validityDays * 24 * 60 * 60),
                status = CertificateStatus.VALID,
            )
        }
    }

    /**
     * 증명서가 유효한지 확인합니다.
     */
    fun isValid(): Boolean = status == CertificateStatus.VALID && !isExpired()

    /**
     * 증명서가 만료되었는지 확인합니다.
     */
    fun isExpired(): Boolean = Instant.now().isAfter(expiresAt)

    /**
     * 증명서가 취소되었는지 확인합니다.
     */
    fun isRevoked(): Boolean = status == CertificateStatus.REVOKED

    /**
     * 증명서를 취소합니다.
     */
    fun revoke() {
        require(status == CertificateStatus.VALID) { "이미 취소되었거나 만료된 증명서입니다." }
        this.status = CertificateStatus.REVOKED
    }

    /**
     * 만료된 증명서를 만료 상태로 변경합니다.
     */
    fun markAsExpired() {
        require(isExpired()) { "아직 만료되지 않은 증명서입니다." }
        this.status = CertificateStatus.EXPIRED
    }
}
