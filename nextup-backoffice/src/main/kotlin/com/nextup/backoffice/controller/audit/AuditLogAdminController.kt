package com.nextup.backoffice.controller.audit

import com.nextup.backoffice.dto.audit.AuditLogResponse
import com.nextup.backoffice.dto.audit.toResponse
import com.nextup.common.dto.ApiResponse
import com.nextup.core.common.PageResult
import com.nextup.core.service.audit.AuditLogQueryService
import com.nextup.infrastructure.common.toPageCommand
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/backoffice/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
class AuditLogAdminController(
    private val auditLogQueryService: AuditLogQueryService,
) {
    @GetMapping
    fun getAuditLogs(
        @RequestParam(required = false) adminUserId: Long?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) targetEntity: String?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        fromDate: Instant?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        toDate: Instant?,
        @PageableDefault(size = 20, sort = ["createdAt"], direction = Sort.Direction.DESC)
        pageable: Pageable,
    ): ApiResponse<PageResult<AuditLogResponse>> {
        val page =
            auditLogQueryService.findAll(
                adminUserId = adminUserId,
                action = action,
                targetEntity = targetEntity,
                fromDate = fromDate,
                toDate = toDate,
                pageCommand = pageable.toPageCommand(),
            )
        return ApiResponse.success(page.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    fun getAuditLog(
        @PathVariable id: Long,
    ): ApiResponse<AuditLogResponse> = ApiResponse.success(auditLogQueryService.findById(id).toResponse())
}
