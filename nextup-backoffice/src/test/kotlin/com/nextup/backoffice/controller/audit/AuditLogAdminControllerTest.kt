package com.nextup.backoffice.controller.audit

import com.nextup.backoffice.exception.GlobalExceptionHandler
import com.nextup.common.exception.AuditLogNotFoundException
import com.nextup.core.common.PageResult
import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.service.audit.AuditLogQueryService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders

@DisplayName("AuditLogAdminController")
class AuditLogAdminControllerTest {
    private lateinit var mockMvc: MockMvc
    private lateinit var auditLogQueryService: AuditLogQueryService

    @BeforeEach
    fun setUp() {
        auditLogQueryService = mockk()
        val controller = AuditLogAdminController(auditLogQueryService)
        mockMvc =
            MockMvcBuilders
                .standaloneSetup(controller)
                .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(GlobalExceptionHandler())
                .build()
    }

    private fun createAuditLog(
        id: Long,
        adminUserId: Long = 1L,
        action: String = "CREATE_USER",
        targetEntity: String = "User",
        targetId: Long? = 10L,
        details: String? = null,
    ): AuditLog {
        val log =
            AuditLog.create(
                adminUserId = adminUserId,
                action = action,
                targetEntity = targetEntity,
                targetId = targetId,
                details = details,
            )
        val idField = AuditLog::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(log, id)
        return log
    }

    @Nested
    @DisplayName("GET /api/backoffice/audit-logs")
    inner class GetAuditLogs {
        @Test
        fun `감사 로그 목록을 페이징 조회할 수 있다`() {
            val log = createAuditLog(id = 1L)
            val pageResult =
                PageResult(
                    content = listOf(log),
                    page = 0,
                    size = 20,
                    totalElements = 1L,
                    totalPages = 1,
                )
            every {
                auditLogQueryService.findAll(null, null, null, null, null, any())
            } returns pageResult

            mockMvc
                .perform(get("/api/backoffice/audit-logs"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].action").value("CREATE_USER"))
                .andExpect(jsonPath("$.data.content[0].targetEntity").value("User"))
        }

        @Test
        fun `adminUserId 필터로 조회할 수 있다`() {
            val log = createAuditLog(id = 1L, adminUserId = 5L)
            val pageResult =
                PageResult(
                    content = listOf(log),
                    page = 0,
                    size = 20,
                    totalElements = 1L,
                    totalPages = 1,
                )
            every {
                auditLogQueryService.findAll(5L, null, null, null, null, any())
            } returns pageResult

            mockMvc
                .perform(get("/api/backoffice/audit-logs").param("adminUserId", "5"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].adminUserId").value(5))
        }

        @Test
        fun `action 필터로 조회할 수 있다`() {
            val log = createAuditLog(id = 2L, action = "DELETE_USER")
            val pageResult =
                PageResult(
                    content = listOf(log),
                    page = 0,
                    size = 20,
                    totalElements = 1L,
                    totalPages = 1,
                )
            every {
                auditLogQueryService.findAll(null, "DELETE_USER", null, null, null, any())
            } returns pageResult

            mockMvc
                .perform(get("/api/backoffice/audit-logs").param("action", "DELETE_USER"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].action").value("DELETE_USER"))
        }

        @Test
        fun `targetEntity 필터로 조회할 수 있다`() {
            val log = createAuditLog(id = 3L, targetEntity = "Team")
            val pageResult =
                PageResult(
                    content = listOf(log),
                    page = 0,
                    size = 20,
                    totalElements = 1L,
                    totalPages = 1,
                )
            every {
                auditLogQueryService.findAll(null, null, "Team", null, null, any())
            } returns pageResult

            mockMvc
                .perform(get("/api/backoffice/audit-logs").param("targetEntity", "Team"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].targetEntity").value("Team"))
        }

        @Test
        fun `결과가 없으면 빈 페이지를 반환한다`() {
            val pageResult =
                PageResult(
                    content = emptyList<AuditLog>(),
                    page = 0,
                    size = 20,
                    totalElements = 0L,
                    totalPages = 0,
                )
            every {
                auditLogQueryService.findAll(null, null, null, null, null, any())
            } returns pageResult

            mockMvc
                .perform(get("/api/backoffice/audit-logs"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
        }
    }

    @Nested
    @DisplayName("GET /api/backoffice/audit-logs/{id}")
    inner class GetAuditLog {
        @Test
        fun `특정 감사 로그를 상세 조회할 수 있다`() {
            val log = createAuditLog(id = 1L, details = "{\"email\":\"test@example.com\"}")
            every { auditLogQueryService.findById(1L) } returns log

            mockMvc
                .perform(get("/api/backoffice/audit-logs/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.action").value("CREATE_USER"))
                .andExpect(jsonPath("$.data.targetEntity").value("User"))
                .andExpect(jsonPath("$.data.targetId").value(10))
        }

        @Test
        fun `존재하지 않는 감사 로그 조회 시 404를 반환한다`() {
            every { auditLogQueryService.findById(999L) } throws AuditLogNotFoundException(999L)

            mockMvc
                .perform(get("/api/backoffice/audit-logs/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AUDIT_LOG_NOT_FOUND"))
        }
    }
}
