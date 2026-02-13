package com.nextup.infrastructure.service.certificate

import com.nextup.common.exception.CertificateNotFoundByIssueNumberException
import com.nextup.common.exception.CertificateNotFoundException
import com.nextup.core.domain.certificate.Certificate
import com.nextup.core.domain.certificate.CertificateStatus
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CertificateRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.service.QrCodeGeneratorPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class CertificateServiceImplTest {
    private lateinit var certificateRepository: CertificateRepositoryPort
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var qrCodeGenerator: QrCodeGeneratorPort
    private lateinit var certificateService: CertificateServiceImpl

    @BeforeEach
    fun setUp() {
        certificateRepository = mockk()
        playerRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        gamePlayerRepository = mockk()
        qrCodeGenerator = mockk()

        certificateService =
            CertificateServiceImpl(
                certificateRepository,
                playerRepository,
                battingRecordRepository,
                pitchingRecordRepository,
                gamePlayerRepository,
                qrCodeGenerator,
            )
    }

    @Test
    fun `should issue certificate for player`() {
        // given
        val playerId = 1L
        val player = createPlayer(playerId)
        val certificateSlot = slot<Certificate>()

        every { playerRepository.findByIdOrNull(playerId) } returns player
        every { certificateRepository.save(capture(certificateSlot)) } answers { certificateSlot.captured }
        every { gamePlayerRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { battingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { qrCodeGenerator.generate(any(), any()) } returns "QR_CODE_DATA"

        // when
        val result = certificateService.issueCertificate(playerId, validityDays = 365)

        // then
        assertThat(result.playerId).isEqualTo(playerId)
        assertThat(result.playerName).isEqualTo(player.name)
        assertThat(result.status).isEqualTo(CertificateStatus.VALID)
        assertThat(result.qrCodeData).isEqualTo("QR_CODE_DATA")
        assertThat(result.playerCareer.totalGames).isEqualTo(0)

        verify { certificateRepository.save(any()) }
        verify { qrCodeGenerator.generate(any(), any()) }
    }

    @Test
    fun `should throw exception when issuing certificate for non-existent player`() {
        // given
        val playerId = 999L
        every { playerRepository.findByIdOrNull(playerId) } returns null

        // when & then
        assertThrows<IllegalArgumentException> {
            certificateService.issueCertificate(playerId)
        }
    }

    @Test
    fun `should get certificate by id`() {
        // given
        val certificateId = 1L
        val player = createPlayer(1L)
        val certificate = Certificate.issue(player, validityDays = 365)

        every { certificateRepository.findById(certificateId) } returns certificate
        every { gamePlayerRepository.findAllByPlayerId(player.id) } returns emptyList()
        every { battingRecordRepository.findAllByPlayerId(player.id) } returns emptyList()
        every { pitchingRecordRepository.findAllByPlayerId(player.id) } returns emptyList()
        every { qrCodeGenerator.generate(any(), any()) } returns "QR_CODE_DATA"

        // when
        val result = certificateService.getCertificate(certificateId)

        // then
        assertThat(result.playerId).isEqualTo(player.id)
        assertThat(result.playerName).isEqualTo(player.name)
        assertThat(result.status).isEqualTo(CertificateStatus.VALID)
    }

    @Test
    fun `should throw exception when getting non-existent certificate`() {
        // given
        val certificateId = 999L
        every { certificateRepository.findById(certificateId) } returns null

        // when & then
        assertThrows<CertificateNotFoundException> {
            certificateService.getCertificate(certificateId)
        }
    }

    @Test
    fun `should verify certificate by issue number`() {
        // given
        val issueNumber = "test-uuid-123"
        val player = createPlayer(1L)
        val certificate = Certificate.issue(player, validityDays = 365)

        every { certificateRepository.findByIssueNumber(issueNumber) } returns certificate

        // when
        val result = certificateService.verifyCertificate(issueNumber)

        // then
        assertThat(result.valid).isTrue()
        assertThat(result.issueNumber).isEqualTo(certificate.issueNumber)
        assertThat(result.playerName).isEqualTo(player.name)
        assertThat(result.status).isEqualTo(CertificateStatus.VALID)
    }

    @Test
    fun `should throw exception when verifying non-existent certificate`() {
        // given
        val issueNumber = "non-existent-uuid"
        every { certificateRepository.findByIssueNumber(issueNumber) } returns null

        // when & then
        assertThrows<CertificateNotFoundByIssueNumberException> {
            certificateService.verifyCertificate(issueNumber)
        }
    }

    @Test
    fun `should revoke certificate`() {
        // given
        val certificateId = 1L
        val player = createPlayer(1L)
        val certificate = Certificate.issue(player, validityDays = 365)

        every { certificateRepository.findById(certificateId) } returns certificate
        every { certificateRepository.save(any()) } returns certificate

        // when
        certificateService.revokeCertificate(certificateId)

        // then
        assertThat(certificate.status).isEqualTo(CertificateStatus.REVOKED)
        verify { certificateRepository.save(certificate) }
    }

    @Test
    fun `should get all player certificates`() {
        // given
        val playerId = 1L
        val player = createPlayer(playerId)
        val certificate1 = Certificate.issue(player, validityDays = 365)
        val certificate2 = Certificate.issue(player, validityDays = 365)

        every { certificateRepository.findAllByPlayerId(playerId) } returns listOf(certificate1, certificate2)
        every { gamePlayerRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { battingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { qrCodeGenerator.generate(any(), any()) } returns "QR_CODE_DATA"

        // when
        val result = certificateService.getPlayerCertificates(playerId)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].playerId).isEqualTo(playerId)
        assertThat(result[1].playerId).isEqualTo(playerId)
    }

    private fun createPlayer(id: Long): Player =
        Player(
            name = "홍길동",
            birthDate = LocalDate.of(1990, 1, 1),
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            primaryPosition = Position.STARTING_PITCHER,
            id = id,
        )
}
