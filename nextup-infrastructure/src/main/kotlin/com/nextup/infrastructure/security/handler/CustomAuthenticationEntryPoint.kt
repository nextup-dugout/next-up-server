package com.nextup.infrastructure.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.common.exception.InvalidTokenException
import com.nextup.common.exception.TokenExpiredException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * 인증 실패 시 처리를 담당하는 EntryPoint
 *
 * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출됩니다.
 */
@Component
class CustomAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper,
) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) {
        val exception = request.getAttribute("exception")

        val (code, message) =
            when (exception) {
                is TokenExpiredException -> "TOKEN_EXPIRED" to exception.message
                is InvalidTokenException -> "INVALID_TOKEN" to exception.message
                else -> "UNAUTHORIZED" to "Authentication required"
            }

        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"

        val errorResponse =
            mapOf(
                "success" to false,
                "error" to
                    mapOf(
                        "code" to code,
                        "message" to message,
                    ),
            )

        objectMapper.writeValue(response.writer, errorResponse)
    }
}
