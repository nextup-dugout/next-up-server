package com.nextup.infrastructure.security.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import java.io.PrintWriter
import java.io.StringWriter

@DisplayName("CustomAccessDeniedHandler 테스트")
class CustomAccessDeniedHandlerTest {
    private lateinit var objectMapper: ObjectMapper
    private lateinit var handler: CustomAccessDeniedHandler
    private lateinit var request: HttpServletRequest
    private lateinit var response: HttpServletResponse
    private lateinit var stringWriter: StringWriter
    private lateinit var printWriter: PrintWriter

    @BeforeEach
    fun setUp() {
        objectMapper = ObjectMapper()
        handler = CustomAccessDeniedHandler(objectMapper)
        request = mockk(relaxed = true)
        response = mockk(relaxed = true)
        stringWriter = StringWriter()
        printWriter = PrintWriter(stringWriter)

        every { response.writer } returns printWriter
    }

    @Nested
    @DisplayName("접근 거부 처리")
    inner class HandleAccessDenied {
        @Test
        fun `should return ACCESS_DENIED error`() {
            // given
            val exception = AccessDeniedException("Access is denied")

            // when
            handler.handle(request, response, exception)

            // then
            printWriter.flush()
            val responseBody = stringWriter.toString()
            assertThat(responseBody).contains("ACCESS_DENIED")
            assertThat(responseBody).contains("Access denied: insufficient permissions")
        }

        @Test
        fun `should set 403 status code`() {
            // given
            val statusSlot = slot<Int>()
            every { response.status = capture(statusSlot) } returns Unit
            val exception = AccessDeniedException("Access is denied")

            // when
            handler.handle(request, response, exception)

            // then
            assertThat(statusSlot.captured).isEqualTo(HttpStatus.FORBIDDEN.value())
        }
    }

    @Nested
    @DisplayName("응답 형식")
    inner class ResponseFormat {
        @Test
        fun `should set correct content type and encoding`() {
            // given
            val contentTypeSlot = slot<String>()
            val encodingSlot = slot<String>()

            every { response.contentType = capture(contentTypeSlot) } returns Unit
            every { response.characterEncoding = capture(encodingSlot) } returns Unit
            val exception = AccessDeniedException("Access is denied")

            // when
            handler.handle(request, response, exception)

            // then
            assertThat(contentTypeSlot.captured).isEqualTo(MediaType.APPLICATION_JSON_VALUE)
            assertThat(encodingSlot.captured).isEqualTo("UTF-8")
        }

        @Test
        fun `should return proper JSON structure`() {
            // given
            val exception = AccessDeniedException("Access is denied")

            // when
            handler.handle(request, response, exception)

            // then
            printWriter.flush()
            val responseBody = stringWriter.toString()
            val json = objectMapper.readTree(responseBody)

            assertThat(json.has("success")).isTrue()
            assertThat(json.get("success").asBoolean()).isFalse()
            assertThat(json.has("error")).isTrue()
            assertThat(json.get("error").has("code")).isTrue()
            assertThat(json.get("error").get("code").asText()).isEqualTo("ACCESS_DENIED")
            assertThat(json.get("error").has("message")).isTrue()
        }
    }

    @Nested
    @DisplayName("다양한 시나리오")
    inner class VariousScenarios {
        @Test
        fun `should handle exception with custom message`() {
            // given
            val exception = AccessDeniedException("User does not have admin role")

            // when
            handler.handle(request, response, exception)

            // then
            verify { response.status = HttpStatus.FORBIDDEN.value() }
            printWriter.flush()
            val responseBody = stringWriter.toString()
            // Handler always returns same message regardless of exception message
            assertThat(responseBody).contains("Access denied: insufficient permissions")
        }

        @Test
        fun `should handle null message in exception`() {
            // given
            val exception = AccessDeniedException(null as String?)

            // when
            handler.handle(request, response, exception)

            // then
            verify { response.status = HttpStatus.FORBIDDEN.value() }
            printWriter.flush()
            val responseBody = stringWriter.toString()
            assertThat(responseBody).contains("ACCESS_DENIED")
        }
    }
}
