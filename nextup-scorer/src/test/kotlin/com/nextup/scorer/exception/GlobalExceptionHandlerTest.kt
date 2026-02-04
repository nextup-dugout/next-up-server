package com.nextup.scorer.exception

import com.nextup.common.exception.BusinessException
import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {
    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
    }

    @Nested
    @DisplayName("handleBusinessException")
    inner class HandleBusinessException {
        @Test
        fun `should return 404 for NotFoundException`() {
            // given
            val exception = object : NotFoundException("ENTITY_NOT_FOUND", "Entity not found") {}

            // when
            val response = handler.handleBusinessException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
            assertThat(response.body?.success).isFalse()
            assertThat(response.body?.error?.code).isEqualTo("ENTITY_NOT_FOUND")
            assertThat(response.body?.error?.message).isEqualTo("Entity not found")
        }

        @Test
        fun `should return 400 for InvalidStateException`() {
            // given
            val exception = object : InvalidStateException("INVALID_STATE", "Invalid state") {}

            // when
            val response = handler.handleBusinessException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.success).isFalse()
            assertThat(response.body?.error?.code).isEqualTo("INVALID_STATE")
            assertThat(response.body?.error?.message).isEqualTo("Invalid state")
        }

        @Test
        fun `should return 400 for InvalidInputException`() {
            // given
            val exception = object : InvalidInputException("INVALID_INPUT", "Invalid input") {}

            // when
            val response = handler.handleBusinessException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.success).isFalse()
            assertThat(response.body?.error?.code).isEqualTo("INVALID_INPUT")
            assertThat(response.body?.error?.message).isEqualTo("Invalid input")
        }

        @Test
        fun `should return 500 for generic BusinessException`() {
            // given
            val exception = object : BusinessException("UNKNOWN_ERROR", "Unknown error") {}

            // when
            val response = handler.handleBusinessException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body?.success).isFalse()
            assertThat(response.body?.error?.code).isEqualTo("UNKNOWN_ERROR")
            assertThat(response.body?.error?.message).isEqualTo("Unknown error")
        }
    }

    @Nested
    @DisplayName("handleValidationException")
    inner class HandleValidationException {
        @Test
        fun `should return 400 with field error messages`() {
            // given
            data class TestRequest(
                val name: String?,
                val age: Int?,
            )

            val target = TestRequest(null, null)
            val bindingResult = BeanPropertyBindingResult(target, "testRequest")
            bindingResult.addError(FieldError("testRequest", "name", "must not be null"))
            bindingResult.addError(FieldError("testRequest", "age", "must be positive"))

            val exception =
                MethodArgumentNotValidException(
                    org.springframework.core.MethodParameter(
                        TestRequest::class.java.constructors.first(),
                        -1,
                    ),
                    bindingResult,
                )

            // when
            val response = handler.handleValidationException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.success).isFalse()
            assertThat(response.body?.error?.code).isEqualTo("VALIDATION_ERROR")
            assertThat(response.body?.error?.message).contains("name")
            assertThat(response.body?.error?.message).contains("age")
        }

        @Test
        fun `should return 400 with single field error`() {
            // given
            data class TestRequest(
                val email: String?,
            )

            val target = TestRequest(null)
            val bindingResult = BeanPropertyBindingResult(target, "testRequest")
            bindingResult.addError(FieldError("testRequest", "email", "must be a valid email"))

            val exception =
                MethodArgumentNotValidException(
                    org.springframework.core.MethodParameter(
                        TestRequest::class.java.constructors.first(),
                        -1,
                    ),
                    bindingResult,
                )

            // when
            val response = handler.handleValidationException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.error?.message).isEqualTo("email: must be a valid email")
        }
    }

    @Nested
    @DisplayName("handleIllegalArgumentException")
    inner class HandleIllegalArgumentException {
        @Test
        fun `should return 400 with exception message`() {
            // given
            val exception = IllegalArgumentException("Value must be positive")

            // when
            val response = handler.handleIllegalArgumentException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.success).isFalse()
            assertThat(response.body?.error?.code).isEqualTo("INVALID_ARGUMENT")
            assertThat(response.body?.error?.message).isEqualTo("Value must be positive")
        }

        @Test
        fun `should return default message when exception message is null`() {
            // given
            val exception = IllegalArgumentException()

            // when
            val response = handler.handleIllegalArgumentException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
            assertThat(response.body?.error?.message).isEqualTo("Invalid argument")
        }
    }

    @Nested
    @DisplayName("handleException")
    inner class HandleException {
        @Test
        fun `should return 500 for generic exception`() {
            // given
            val exception = RuntimeException("Something went wrong")

            // when
            val response = handler.handleException(exception)

            // then
            assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            assertThat(response.body?.success).isFalse()
            assertThat(response.body?.error?.code).isEqualTo("INTERNAL_SERVER_ERROR")
            assertThat(response.body?.error?.message).isEqualTo("An unexpected error occurred")
        }

        @Test
        fun `should not expose internal exception details`() {
            // given
            val exception = NullPointerException("Internal NPE at line 42")

            // when
            val response = handler.handleException(exception)

            // then
            assertThat(response.body?.error?.message).doesNotContain("NPE")
            assertThat(response.body?.error?.message).doesNotContain("line 42")
            assertThat(response.body?.error?.message).isEqualTo("An unexpected error occurred")
        }
    }
}
