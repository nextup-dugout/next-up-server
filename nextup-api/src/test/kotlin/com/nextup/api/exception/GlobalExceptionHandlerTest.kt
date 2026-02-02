package com.nextup.api.exception

import com.nextup.common.exception.AuthenticationException
import com.nextup.common.exception.BusinessException
import com.nextup.common.exception.InvalidStateException
import com.nextup.common.exception.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private lateinit var handler: GlobalExceptionHandler

    @BeforeEach
    fun setUp() {
        handler = GlobalExceptionHandler()
    }

    @Test
    fun `should handle AuthenticationException and return 401`() {
        // given
        val exception = object : AuthenticationException("INVALID_CREDENTIALS", "Invalid email or password") {}

        // when
        val response = handler.handleAuthenticationException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        assertThat(response.body?.success).isFalse()
        assertThat(response.body?.error?.code).isEqualTo("INVALID_CREDENTIALS")
        assertThat(response.body?.error?.message).isEqualTo("Invalid email or password")
    }

    @Test
    fun `should handle NotFoundException and return 404`() {
        // given
        val exception = object : NotFoundException("ENTITY_NOT_FOUND", "Entity not found") {}

        // when
        val response = handler.handleNotFoundException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        assertThat(response.body?.success).isFalse()
        assertThat(response.body?.error?.code).isEqualTo("ENTITY_NOT_FOUND")
        assertThat(response.body?.error?.message).isEqualTo("Entity not found")
    }

    @Test
    fun `should handle InvalidStateException and return 400`() {
        // given
        val exception = object : InvalidStateException("INVALID_STATE", "Invalid operation") {}

        // when
        val response = handler.handleInvalidStateException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.success).isFalse()
        assertThat(response.body?.error?.code).isEqualTo("INVALID_STATE")
        assertThat(response.body?.error?.message).isEqualTo("Invalid operation")
    }

    @Test
    fun `should handle BusinessException and return 400`() {
        // given
        val exception = object : BusinessException("BUSINESS_ERROR", "Business rule violated") {}

        // when
        val response = handler.handleBusinessException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.success).isFalse()
        assertThat(response.body?.error?.code).isEqualTo("BUSINESS_ERROR")
        assertThat(response.body?.error?.message).isEqualTo("Business rule violated")
    }

    @Test
    fun `should handle MethodArgumentNotValidException and return 400`() {
        // given
        data class TestRequest(val name: String?)
        val target = TestRequest(null)
        val bindingResult = BeanPropertyBindingResult(target, "testRequest")
        bindingResult.addError(FieldError("testRequest", "name", "must not be null"))

        val exception = MethodArgumentNotValidException(
            org.springframework.core.MethodParameter(
                TestRequest::class.java.constructors.first(), -1
            ),
            bindingResult
        )

        // when
        val response = handler.handleValidationException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(response.body?.success).isFalse()
        assertThat(response.body?.error?.code).isEqualTo("VALIDATION_ERROR")
        assertThat(response.body?.error?.message).contains("name")
    }

    @Test
    fun `should handle generic Exception and return 500`() {
        // given
        val exception = RuntimeException("Unexpected error")

        // when
        val response = handler.handleException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(response.body?.success).isFalse()
        assertThat(response.body?.error?.code).isEqualTo("INTERNAL_ERROR")
        assertThat(response.body?.error?.message).isEqualTo("An unexpected error occurred")
    }
}
