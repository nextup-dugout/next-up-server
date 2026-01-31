package com.nextup.api.dto.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ApiResponseTest {

    @Test
    fun `should create success response with data`() {
        // given
        val data = "test data"

        // when
        val response = ApiResponse.success(data)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo("test data")
        assertThat(response.error as ErrorDetails?).isNull()
    }

    @Test
    fun `should create success response with null data`() {
        // when
        val response: ApiResponse<String?> = ApiResponse.success(null)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data as String?).isNull()
        assertThat(response.error as ErrorDetails?).isNull()
    }

    @Test
    fun `should create error response without data`() {
        // when
        val response = ApiResponse.error<String>("ERROR_CODE", "Error message")

        // then
        assertThat(response.success).isFalse()
        assertThat(response.data as String?).isNull()
        assertThat(response.error).isNotNull()
        assertThat(response.error?.code).isEqualTo("ERROR_CODE")
        assertThat(response.error?.message).isEqualTo("Error message")
    }

    @Test
    fun `should create success response with complex object`() {
        // given
        data class TestData(val id: Long, val name: String)
        val testData = TestData(1L, "test")

        // when
        val response = ApiResponse.success(testData)

        // then
        assertThat(response.success).isTrue()
        assertThat(response.data).isEqualTo(testData)
        assertThat(response.data?.id).isEqualTo(1L)
        assertThat(response.data?.name).isEqualTo("test")
        assertThat(response.error as ErrorDetails?).isNull()
    }

    @Test
    fun `should create error response with various error codes`() {
        // when
        val notFoundResponse = ApiResponse.error<String>("NOT_FOUND", "Resource not found")
        val badRequestResponse = ApiResponse.error<String>("BAD_REQUEST", "Invalid request")
        val unauthorizedResponse = ApiResponse.error<String>("UNAUTHORIZED", "Not authorized")

        // then
        assertThat(notFoundResponse.error?.code).isEqualTo("NOT_FOUND")
        assertThat(badRequestResponse.error?.code).isEqualTo("BAD_REQUEST")
        assertThat(unauthorizedResponse.error?.code).isEqualTo("UNAUTHORIZED")
    }
}
