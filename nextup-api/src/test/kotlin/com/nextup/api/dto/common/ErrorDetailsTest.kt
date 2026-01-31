package com.nextup.api.dto.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ErrorDetailsTest {

    @Test
    fun `should create ErrorDetails with code and message`() {
        // given
        val code = "VALIDATION_ERROR"
        val message = "Invalid input data"

        // when
        val errorDetails = ErrorDetails(code, message)

        // then
        assertThat(errorDetails.code).isEqualTo("VALIDATION_ERROR")
        assertThat(errorDetails.message).isEqualTo("Invalid input data")
    }

    @Test
    fun `should support data class equality`() {
        // given
        val error1 = ErrorDetails("CODE", "Message")
        val error2 = ErrorDetails("CODE", "Message")
        val error3 = ErrorDetails("OTHER", "Message")

        // then
        assertThat(error1).isEqualTo(error2)
        assertThat(error1).isNotEqualTo(error3)
    }

    @Test
    fun `should support data class copy`() {
        // given
        val original = ErrorDetails("CODE", "Original message")

        // when
        val copied = original.copy(message = "Updated message")

        // then
        assertThat(copied.code).isEqualTo("CODE")
        assertThat(copied.message).isEqualTo("Updated message")
        assertThat(original.message).isEqualTo("Original message")
    }

    @Test
    fun `should support toString`() {
        // given
        val errorDetails = ErrorDetails("ERROR", "Something went wrong")

        // when
        val toString = errorDetails.toString()

        // then
        assertThat(toString).contains("ERROR")
        assertThat(toString).contains("Something went wrong")
    }
}
