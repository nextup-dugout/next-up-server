package com.nextup.infrastructure.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.orm.ObjectOptimisticLockingFailureException

@DisplayName("BaseExceptionHandler 테스트")
class BaseExceptionHandlerTest {
    private lateinit var handler: TestExceptionHandler

    /**
     * BaseExceptionHandler는 abstract이므로 테스트용 concrete 클래스 생성
     */
    private class TestExceptionHandler : BaseExceptionHandler()

    @BeforeEach
    fun setUp() {
        handler = TestExceptionHandler()
    }

    @Test
    fun `should handle OptimisticLockingFailureException and return 409`() {
        // given
        val exception =
            ObjectOptimisticLockingFailureException(
                "StadiumSlot",
                1L,
            )

        // when
        val response = handler.handleOptimisticLockingFailureException(exception)

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        assertThat(response.body?.success).isFalse()
        assertThat(response.body?.error?.code).isEqualTo("CONCURRENT_BOOKING")
        assertThat(response.body?.error?.message).isEqualTo("다른 사용자가 먼저 예약했습니다. 다시 시도해주세요.")
    }
}
