package com.nextup.core.domain.audit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("AuditLog")
class AuditLogTest {
    @Test
    fun `create - 모든 필드를 지정하여 AuditLog를 생성할 수 있다`() {
        // when
        val auditLog =
            AuditLog.create(
                adminUserId = 1L,
                action = "CREATE_USER",
                targetEntity = "User",
                targetId = 10L,
                details = "{\"email\":\"test@example.com\"}",
            )

        // then
        assertThat(auditLog.adminUserId).isEqualTo(1L)
        assertThat(auditLog.action).isEqualTo("CREATE_USER")
        assertThat(auditLog.targetEntity).isEqualTo("User")
        assertThat(auditLog.targetId).isEqualTo(10L)
        assertThat(auditLog.details).isEqualTo("{\"email\":\"test@example.com\"}")
        assertThat(auditLog.createdAt).isNotNull()
        assertThat(auditLog.id).isNull()
    }

    @Test
    fun `create - targetId와 details가 null이어도 생성할 수 있다`() {
        // when
        val auditLog =
            AuditLog.create(
                adminUserId = 2L,
                action = "DELETE_USER",
                targetEntity = "User",
            )

        // then
        assertThat(auditLog.adminUserId).isEqualTo(2L)
        assertThat(auditLog.action).isEqualTo("DELETE_USER")
        assertThat(auditLog.targetEntity).isEqualTo("User")
        assertThat(auditLog.targetId).isNull()
        assertThat(auditLog.details).isNull()
    }
}
