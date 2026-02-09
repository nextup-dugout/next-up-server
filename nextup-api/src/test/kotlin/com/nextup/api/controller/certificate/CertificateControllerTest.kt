package com.nextup.api.controller.certificate

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.api.dto.certificate.IssueCertificateRequest
import com.nextup.api.exception.GlobalExceptionHandler
import com.nextup.common.exception.CertificateNotFoundByIssueNumberException
import com.nextup.common.exception.CertificateNotFoundException
import com.nextup.core.domain.certificate.CertificateStatus
import com.nextup.core.dto.certificate.*
import com.nextup.core.service.certificate.CertificateService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant

class CertificateControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var certificateService: CertificateService
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setUp() {
        certificateService = mockk()
        objectMapper = ObjectMapper()

        mockMvc =
            MockMvcBuilders
                .standaloneSetup(CertificateController(certificateService))
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    @Test
    fun `should issue certificate for player`() {
        // given
        val playerId = 1L
        val request = IssueCertificateRequest(validityDays = 365)
        val certificateDto = createCertificateDto(playerId)

        every { certificateService.issueCertificate(playerId, 365) } returns certificateDto

        // when & then
        mockMvc
            .perform(
                post("/api/v1/players/{playerId}/certificate", playerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.playerId").value(playerId))
            .andExpect(jsonPath("$.data.playerName").value("홍길동"))
            .andExpect(jsonPath("$.data.status").value("VALID"))

        verify { certificateService.issueCertificate(playerId, 365) }
    }

    @Test
    fun `should issue certificate with default validity days`() {
        // given
        val playerId = 1L
        val certificateDto = createCertificateDto(playerId)

        every { certificateService.issueCertificate(playerId, 365) } returns certificateDto

        // when & then
        mockMvc
            .perform(
                post("/api/v1/players/{playerId}/certificate", playerId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"),
            )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.success").value(true))

        verify { certificateService.issueCertificate(playerId, 365) }
    }

    @Test
    fun `should get certificate by id`() {
        // given
        val certificateId = 1L
        val certificateDto = createCertificateDto(1L)

        every { certificateService.getCertificate(certificateId) } returns certificateDto

        // when & then
        mockMvc
            .perform(get("/api/v1/certificates/{certificateId}", certificateId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(certificateId))
            .andExpect(jsonPath("$.data.playerName").value("홍길동"))

        verify { certificateService.getCertificate(certificateId) }
    }

    @Test
    fun `should return 404 when certificate not found`() {
        // given
        val certificateId = 999L

        every { certificateService.getCertificate(certificateId) } throws
            CertificateNotFoundException(certificateId)

        // when & then
        mockMvc
            .perform(get("/api/v1/certificates/{certificateId}", certificateId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CERTIFICATE_NOT_FOUND"))
    }

    @Test
    fun `should verify certificate by issue number`() {
        // given
        val issueNumber = "test-uuid-123"
        val verificationDto =
            CertificateVerificationDto(
                valid = true,
                issueNumber = issueNumber,
                playerName = "홍길동",
                issuedAt = Instant.now(),
                expiresAt = Instant.now().plusSeconds(365 * 24 * 60 * 60),
                status = CertificateStatus.VALID,
            )

        every { certificateService.verifyCertificate(issueNumber) } returns verificationDto

        // when & then
        mockMvc
            .perform(get("/api/v1/certificates/{issueNumber}/verify", issueNumber))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.valid").value(true))
            .andExpect(jsonPath("$.data.issueNumber").value(issueNumber))
            .andExpect(jsonPath("$.data.playerName").value("홍길동"))
            .andExpect(jsonPath("$.data.status").value("VALID"))

        verify { certificateService.verifyCertificate(issueNumber) }
    }

    @Test
    fun `should return 404 when verifying non-existent certificate`() {
        // given
        val issueNumber = "non-existent-uuid"

        every { certificateService.verifyCertificate(issueNumber) } throws
            CertificateNotFoundByIssueNumberException(issueNumber)

        // when & then
        mockMvc
            .perform(get("/api/v1/certificates/{issueNumber}/verify", issueNumber))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CERTIFICATE_NOT_FOUND"))
    }

    @Test
    fun `should get all player certificates`() {
        // given
        val playerId = 1L
        val certificates = listOf(createCertificateDto(playerId), createCertificateDto(playerId))

        every { certificateService.getPlayerCertificates(playerId) } returns certificates

        // when & then
        mockMvc
            .perform(get("/api/v1/players/{playerId}/certificates", playerId))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isArray)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].playerId").value(playerId))
            .andExpect(jsonPath("$.data[1].playerId").value(playerId))

        verify { certificateService.getPlayerCertificates(playerId) }
    }

    @Test
    fun `should revoke certificate`() {
        // given
        val certificateId = 1L

        every { certificateService.revokeCertificate(certificateId) } returns Unit

        // when & then
        mockMvc
            .perform(delete("/api/v1/certificates/{certificateId}", certificateId))
            .andExpect(status().isNoContent)

        verify { certificateService.revokeCertificate(certificateId) }
    }

    private fun createCertificateDto(playerId: Long): CertificateDto =
        CertificateDto(
            id = 1L,
            issueNumber = "test-uuid-123",
            playerId = playerId,
            playerName = "홍길동",
            issuedAt = Instant.now(),
            expiresAt = Instant.now().plusSeconds(365 * 24 * 60 * 60),
            status = CertificateStatus.VALID,
            playerCareer =
                PlayerCareerDto(
                    totalGames = 10,
                    battingStats = null,
                    pitchingStats = null,
                    competitionHistory = emptyList(),
                ),
            qrCodeData = "QR_CODE_DATA",
        )
}
