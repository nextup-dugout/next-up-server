package com.nextup.infrastructure.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import com.nextup.common.exception.InvalidTokenException
import com.nextup.common.exception.TokenExpiredException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import java.io.PrintWriter
import java.io.StringWriter

@DisplayName("CustomAuthenticationEntryPoint 테스트")
class CustomAuthenticationEntryPointTest {
    private lateinit var objectMapper: ObjectMapper
    private lateinit var entryPoint: CustomAuthenticationEntryPoint
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var stringWriter: StringWriter
    private lateinit var printWriter: PrintWriter

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        entryPoint = CustomAuthenticationEntryPoint(objectMapper)
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        stringWriter = StringWriter()
        printWriter = PrintWriter(stringWriter)

        every { response.writer } returns printWriter
    }

    @Nested
    @DisplayName("토큰 만료 시")
    inner class TokenExpired {
        @Test
        fun `should return TOKEN_EXPIRED error`() {
            // given
            val exception = TokenExpiredException("Token has expired")
            every { request.getAttribute("exception") } returns exception

            // when
            entryPoint.commence(request, response, BadCredentialsException(""))

            // then
            verify(response, HttpStatus.UNAUTHORIZED.value())

            printWriter.flush()
            val responseBody = stringWriter.toString()
            assertThat(responseBody).contains("TOKEN_EXPIRED")
            assertThat(responseBody).contains("Token has expired")
        }
    }

    @Nested
    @DisplayName("유효하지 않은 토큰")
    inner class InvalidToken {
        @Test
        fun `should return INVALID_TOKEN error`() {
            // given
            val exception = InvalidTokenException("Invalid token format")
            every { request.getAttribute("exception") } returns exception

            // when
            entryPoint.commence(request, response, BadCredentialsException(""))

            // then
            verify(response, HttpStatus.UNAUTHORIZED.value())

            printWriter.flush()
            val responseBody = stringWriter.toString()
            assertThat(responseBody).contains("INVALID_TOKEN")
            assertThat(responseBody).contains("Invalid token format")
        }
    }

    @Nested
    @DisplayName("기타 인증 오류")
    inner class OtherAuthError {
        @Test
        fun `should return UNAUTHORIZED error when no exception attribute`() {
            // given
            every { request.getAttribute("exception") } returns null

            // when
            entryPoint.commence(request, response, BadCredentialsException("Bad credentials"))

            // then
            verify(response, HttpStatus.UNAUTHORIZED.value())

            printWriter.flush()
            val responseBody = stringWriter.toString()
            assertThat(responseBody).contains("UNAUTHORIZED")
            assertThat(responseBody).contains("Authentication required")
        }

        @Test
        fun `should return UNAUTHORIZED error for unknown exception type`() {
            // given
            every { request.getAttribute("exception") } returns RuntimeException("Unknown error")

            // when
            entryPoint.commence(request, response, BadCredentialsException(""))

            // then
            verify(response, HttpStatus.UNAUTHORIZED.value())

            printWriter.flush()
            val responseBody = stringWriter.toString()
            assertThat(responseBody).contains("UNAUTHORIZED")
        }
    }

    @Nested
    @DisplayName("응답 형식")
    inner class ResponseFormat {
        @Test
        fun `should set correct content type and encoding`() {
            // given
            val statusSlot = slot<Int>()
            val contentTypeSlot = slot<String>()
            val encodingSlot = slot<String>()

            every { response.status = capture(statusSlot) } returns Unit
            every { response.contentType = capture(contentTypeSlot) } returns Unit
            every { response.characterEncoding = capture(encodingSlot) } returns Unit
            every { request.getAttribute("exception") } returns null

            // when
            entryPoint.commence(request, response, BadCredentialsException(""))

            // then
            assertThat(statusSlot.captured).isEqualTo(HttpStatus.UNAUTHORIZED.value())
            assertThat(contentTypeSlot.captured).isEqualTo(MediaType.APPLICATION_JSON_VALUE)
            assertThat(encodingSlot.captured).isEqualTo("UTF-8")
        }

        @Test
        fun `should return proper JSON structure`() {
            // given
            every { request.getAttribute("exception") } returns null

            // when
            entryPoint.commence(request, response, BadCredentialsException(""))

            // then
            printWriter.flush()
            val responseBody = stringWriter.toString()
            val json = objectMapper.readTree(responseBody)

            assertThat(json.has("success")).isTrue()
            assertThat(json.get("success").asBoolean()).isFalse()
            assertThat(json.has("error")).isTrue()
            assertThat(json.get("error").has("code")).isTrue()
            assertThat(json.get("error").has("message")).isTrue()
        }
    }

    private fun verify(
        response: HttpServletResponse,
        expectedStatus: Int,
    ) {
        io.mockk.verify { response.status = expectedStatus }
        io.mockk.verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
        io.mockk.verify { response.characterEncoding = "UTF-8" }
    }
}
