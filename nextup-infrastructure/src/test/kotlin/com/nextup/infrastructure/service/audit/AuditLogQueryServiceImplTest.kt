package com.nextup.infrastructure.service.audit

import com.nextup.common.exception.AuditLogNotFoundException
import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.port.repository.AuditLogRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@DisplayName("AuditLogQueryServiceImpl")
class AuditLogQueryServiceImplTest {
    private lateinit var auditLogRepository: AuditLogRepositoryPort
    private lateinit var service: AuditLogQueryServiceImpl

    @BeforeEach
    fun setUp() {
        auditLogRepository = mockk()
        service = AuditLogQueryServiceImpl(auditLogRepository)
    }

    @Nested
    @DisplayName("findAll")
    inner class FindAll {
        @Test
        fun `조건에 맞는 감사 로그를 페이지로 반환한다`() {
            val pageable = PageRequest.of(0, 10)
            val auditLog = mockk<AuditLog>()
            val page = PageImpl(listOf(auditLog), pageable, 1)

            every {
                auditLogRepository.findAllByCondition(
                    adminUserId = 1L,
                    action = "CREATE",
                    targetEntity = "Team",
                    fromDate = null,
                    toDate = null,
                    pageable = pageable,
                )
            } returns page

            val result =
                service.findAll(
                    adminUserId = 1L,
                    action = "CREATE",
                    targetEntity = "Team",
                    fromDate = null,
                    toDate = null,
                    pageable = pageable,
                )

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0]).isEqualTo(auditLog)
        }

        @Test
        fun `모든 조건이 null이어도 정상 동작한다`() {
            val pageable = PageRequest.of(0, 10)
            val page = PageImpl<AuditLog>(emptyList(), pageable, 0)

            every {
                auditLogRepository.findAllByCondition(
                    adminUserId = null,
                    action = null,
                    targetEntity = null,
                    fromDate = null,
                    toDate = null,
                    pageable = pageable,
                )
            } returns page

            val result =
                service.findAll(
                    adminUserId = null,
                    action = null,
                    targetEntity = null,
                    fromDate = null,
                    toDate = null,
                    pageable = pageable,
                )

            assertThat(result.content).isEmpty()
        }
    }

    @Nested
    @DisplayName("findById")
    inner class FindById {
        @Test
        fun `감사 로그를 찾으면 반환한다`() {
            val auditLog = mockk<AuditLog>()
            every { auditLogRepository.findAuditLogById(1L) } returns auditLog

            val result = service.findById(1L)

            assertThat(result).isEqualTo(auditLog)
        }

        @Test
        fun `감사 로그를 찾지 못하면 AuditLogNotFoundException을 던진다`() {
            every { auditLogRepository.findAuditLogById(999L) } returns null

            assertThatThrownBy { service.findById(999L) }
                .isInstanceOf(AuditLogNotFoundException::class.java)
        }
    }
}
