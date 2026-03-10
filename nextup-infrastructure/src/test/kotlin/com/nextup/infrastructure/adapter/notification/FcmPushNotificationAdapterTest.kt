package com.nextup.infrastructure.adapter.notification

import com.google.firebase.messaging.BatchResponse
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.SendResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("FcmPushNotificationAdapter")
class FcmPushNotificationAdapterTest {
    private lateinit var adapter: FcmPushNotificationAdapter
    private lateinit var firebaseMessaging: FirebaseMessaging

    @BeforeEach
    fun setUp() {
        adapter = FcmPushNotificationAdapter()
        firebaseMessaging = mockk()
        mockkStatic(FirebaseMessaging::class)
        every { FirebaseMessaging.getInstance() } returns firebaseMessaging
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FirebaseMessaging::class)
    }

    @Nested
    @DisplayName("send")
    inner class Send {
        @Test
        fun `성공적으로 푸시를 발송한다`() {
            // given
            every { firebaseMessaging.send(any()) } returns "message-id"

            // when
            val result = adapter.send("token-1", "제목", "내용")

            // then
            assertThat(result).isTrue()
            verify(exactly = 1) { firebaseMessaging.send(any()) }
        }

        @Test
        fun `data와 함께 발송한다`() {
            // given
            every { firebaseMessaging.send(any()) } returns "message-id"

            // when
            val result =
                adapter.send("token-1", "제목", "내용", mapOf("key" to "value"))

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `재시도 가능한 오류 시 재시도 후 성공한다`() {
            // given
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.UNAVAILABLE

            var callCount = 0
            every { firebaseMessaging.send(any()) } answers {
                callCount++
                if (callCount == 1) throw exception
                "message-id"
            }

            // when
            val result = adapter.send("token-1", "제목", "내용")

            // then
            assertThat(result).isTrue()
            verify(exactly = 2) { firebaseMessaging.send(any()) }
        }

        @Test
        fun `재시도 불가능한 오류 시 즉시 실패한다`() {
            // given
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.INVALID_ARGUMENT

            every { firebaseMessaging.send(any()) } throws exception

            // when
            val result = adapter.send("token-1", "제목", "내용")

            // then
            assertThat(result).isFalse()
            verify(exactly = 1) { firebaseMessaging.send(any()) }
        }

        @Test
        fun `INTERNAL 오류로 최대 재시도 횟수 초과 시 실패한다`() {
            // given
            val exception = mockk<FirebaseMessagingException>()
            every { exception.messagingErrorCode } returns MessagingErrorCode.INTERNAL

            every { firebaseMessaging.send(any()) } throws exception

            // when
            val result = adapter.send("token-1", "제목", "내용")

            // then
            assertThat(result).isFalse()
            verify(exactly = 3) { firebaseMessaging.send(any()) }
        }

        @Test
        fun `예상치 못한 예외 시 즉시 실패한다`() {
            // given
            every { firebaseMessaging.send(any()) } throws
                RuntimeException("unexpected")

            // when
            val result = adapter.send("token-1", "제목", "내용")

            // then
            assertThat(result).isFalse()
            verify(exactly = 1) { firebaseMessaging.send(any()) }
        }
    }

    @Nested
    @DisplayName("sendBatch")
    inner class SendBatch {
        @Test
        fun `빈 토큰 목록이면 0을 반환한다`() {
            // when
            val result = adapter.sendBatch(emptyList(), "제목", "내용")

            // then
            assertThat(result).isEqualTo(0)
        }

        @Test
        fun `배치 발송 성공`() {
            // given
            val batchResponse = mockk<BatchResponse>()
            every { batchResponse.successCount } returns 3
            every { batchResponse.failureCount } returns 0
            every { firebaseMessaging.sendEachForMulticast(any()) } returns batchResponse

            // when
            val result =
                adapter.sendBatch(listOf("t1", "t2", "t3"), "제목", "내용")

            // then
            assertThat(result).isEqualTo(3)
        }

        @Test
        fun `data와 함께 배치 발송한다`() {
            // given
            val batchResponse = mockk<BatchResponse>()
            every { batchResponse.successCount } returns 2
            every { batchResponse.failureCount } returns 0
            every { firebaseMessaging.sendEachForMulticast(any()) } returns batchResponse

            // when
            val result =
                adapter.sendBatch(
                    listOf("t1", "t2"),
                    "제목",
                    "내용",
                    mapOf("key" to "value"),
                )

            // then
            assertThat(result).isEqualTo(2)
        }

        @Test
        fun `부분 실패 시 성공 수만 반환한다`() {
            // given
            val failedException = mockk<FirebaseMessagingException>()
            every { failedException.messagingErrorCode } returns MessagingErrorCode.INVALID_ARGUMENT

            val failedResponse = mockk<SendResponse>()
            every { failedResponse.isSuccessful } returns false
            every { failedResponse.exception } returns failedException

            val successResponse = mockk<SendResponse>()
            every { successResponse.isSuccessful } returns true

            val batchResponse = mockk<BatchResponse>()
            every { batchResponse.successCount } returns 2
            every { batchResponse.failureCount } returns 1
            every { batchResponse.responses } returns
                listOf(successResponse, successResponse, failedResponse)
            every { firebaseMessaging.sendEachForMulticast(any()) } returns batchResponse

            // when
            val result =
                adapter.sendBatch(listOf("t1", "t2", "t3"), "제목", "내용")

            // then
            assertThat(result).isEqualTo(2)
        }

        @Test
        fun `배치 발송 중 예외 발생 시 0을 반환한다`() {
            // given
            every { firebaseMessaging.sendEachForMulticast(any()) } throws
                RuntimeException("error")

            // when
            val result = adapter.sendBatch(listOf("t1"), "제목", "내용")

            // then
            assertThat(result).isEqualTo(0)
        }
    }
}
