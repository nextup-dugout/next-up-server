package com.nextup.infrastructure.service.audit

import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.port.repository.AuditLogRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AuditServiceImpl")
class AuditServiceImplTest {
    private lateinit var auditLogRepository: AuditLogRepositoryPort
    private lateinit var auditService: AuditServiceImpl

    @BeforeEach
    fun setUp() {
        auditLogRepository = mockk()
        auditService = AuditServiceImpl(auditLogRepository)
    }

    @Test
    fun `log - AuditLog를 생성하고 저장한다`() {
        // given
        val slot = slot<AuditLog>()
        every { auditLogRepository.save(capture(slot)) } answers { firstArg() }

        // when
        auditService.log(
            adminUserId = 1L,
            action = "CREATE_USER",
            targetEntity = "User",
            targetId = 10L,
            details = "{\"email\":\"test@example.com\"}",
        )

        // then
        verify(exactly = 1) { auditLogRepository.save(any()) }
        val captured = slot.captured
        assertThat(captured.adminUserId).isEqualTo(1L)
        assertThat(captured.action).isEqualTo("CREATE_USER")
        assertThat(captured.targetEntity).isEqualTo("User")
        assertThat(captured.targetId).isEqualTo(10L)
        assertThat(captured.details).isEqualTo("{\"email\":\"test@example.com\"}")
    }

    @Test
    fun `log - targetId와 details가 null이어도 저장할 수 있다`() {
        // given
        val slot = slot<AuditLog>()
        every { auditLogRepository.save(capture(slot)) } answers { firstArg() }

        // when
        auditService.log(
            adminUserId = 2L,
            action = "DEACTIVATE_USER",
            targetEntity = "User",
        )

        // then
        verify(exactly = 1) { auditLogRepository.save(any()) }
        val captured = slot.captured
        assertThat(captured.targetId).isNull()
        assertThat(captured.details).isNull()
    }
}
