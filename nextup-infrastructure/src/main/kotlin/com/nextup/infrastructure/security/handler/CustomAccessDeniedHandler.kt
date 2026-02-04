package com.nextup.infrastructure.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component

/**
 * 접근 거부 시 처리를 담당하는 Handler
 *
 * 인증된 사용자가 권한이 없는 리소스에 접근할 때 호출됩니다.
 */
@Component
class CustomAccessDeniedHandler(
    private val objectMapper: ObjectMapper,
) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        response.status = HttpStatus.FORBIDDEN.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse =
            mapOf(
                "success" to false,
                "error" to
                    mapOf(
                        "code" to "ACCESS_DENIED",
                        "message" to "Access denied: insufficient permissions",
                    ),
            )

        objectMapper.writeValue(response.writer, errorResponse)
    }
}
