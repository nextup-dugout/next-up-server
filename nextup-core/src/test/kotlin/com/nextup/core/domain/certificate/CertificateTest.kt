package com.nextup.core.domain.certificate

import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.LocalDate

class CertificateTest {
    @Test
    fun `should issue certificate with valid player`() {
        // given
        val player = createPlayer()

        // when
        val certificate = Certificate.issue(player, validityDays = 365)

        // then
        assertThat(certificate.player).isEqualTo(player)
        assertThat(certificate.issueNumber).isNotBlank()
        assertThat(certificate.status).isEqualTo(CertificateStatus.VALID)
        assertThat(certificate.issuedAt).isBeforeOrEqualTo(Instant.now())
        assertThat(certificate.expiresAt).isAfter(certificate.issuedAt)
    }

    @Test
    fun `should return true when certificate is valid and not expired`() {
        // given
        val player = createPlayer()
        val certificate = Certificate.issue(player, validityDays = 365)

        // when & then
        assertThat(certificate.isValid()).isTrue()
        assertThat(certificate.isExpired()).isFalse()
        assertThat(certificate.isRevoked()).isFalse()
    }

    @Test
    fun `should revoke valid certificate`() {
        // given
        val player = createPlayer()
        val certificate = Certificate.issue(player, validityDays = 365)

        // when
        certificate.revoke()

        // then
        assertThat(certificate.status).isEqualTo(CertificateStatus.REVOKED)
        assertThat(certificate.isRevoked()).isTrue()
        assertThat(certificate.isValid()).isFalse()
    }

    @Test
    fun `should throw exception when revoking already revoked certificate`() {
        // given
        val player = createPlayer()
        val certificate = Certificate.issue(player, validityDays = 365)
        certificate.revoke()

        // when & then
        assertThrows<IllegalArgumentException> {
            certificate.revoke()
        }
    }

    @Test
    fun `should mark expired certificate as expired`() {
        // given
        val player = createPlayer()
        val certificate = Certificate.issue(player, validityDays = -1) // 이미 만료됨

        // when
        certificate.markAsExpired()

        // then
        assertThat(certificate.status).isEqualTo(CertificateStatus.EXPIRED)
        assertThat(certificate.isExpired()).isTrue()
    }

    @Test
    fun `should throw exception when marking non-expired certificate as expired`() {
        // given
        val player = createPlayer()
        val certificate = Certificate.issue(player, validityDays = 365)

        // when & then
        assertThrows<IllegalArgumentException> {
            certificate.markAsExpired()
        }
    }

    private fun createPlayer(): Player =
        Player(
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 1),
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            primaryPosition = Position.STARTING_PITCHER,
        )
}
