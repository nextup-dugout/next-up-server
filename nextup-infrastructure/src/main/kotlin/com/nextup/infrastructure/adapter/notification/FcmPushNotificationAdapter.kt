package com.nextup.infrastructure.adapter.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import com.nextup.core.port.service.PushNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

/**
 * Firebase Cloud Messaging 기반 푸시 알림 어댑터
 *
 * FirebaseMessaging을 사용하여 실제 푸시 알림을 발송합니다.
 * 발송 실패 시 최대 3회 재시도합니다.
 */
@Component
@Primary
class FcmPushNotificationAdapter : PushNotificationPort {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MAX_RETRY = 3
        private const val MAX_MULTICAST_SIZE = 500
    }

    override fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>?,
    ): Boolean {
        val message =
            Message.builder()
                .setToken(token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                )
                .apply { data?.let { putAllData(it) } }
                .build()

        return sendWithRetry(token, message)
    }

    override fun sendBatch(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>?,
    ): Int {
        if (tokens.isEmpty()) return 0

        var successCount = 0

        tokens.chunked(MAX_MULTICAST_SIZE).forEach { chunk ->
            val message =
                MulticastMessage.builder()
                    .addAllTokens(chunk)
                    .setNotification(
                        Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build(),
                    )
                    .apply { data?.let { putAllData(it) } }
                    .build()

            try {
                val response = FirebaseMessaging.getInstance().sendEachForMulticast(message)
                successCount += response.successCount
                if (response.failureCount > 0) {
                    response.responses
                        .filter { !it.isSuccessful }
                        .forEach { sendResponse ->
                            log.warn(
                                "FCM 배치 발송 실패: {}",
                                sendResponse.exception?.messagingErrorCode,
                            )
                        }
                }
            } catch (e: Exception) {
                log.error("FCM 배치 발송 중 오류: {}", e.message, e)
            }
        }

        return successCount
    }

    private fun sendWithRetry(
        token: String,
        message: Message,
    ): Boolean {
        repeat(MAX_RETRY) { attempt ->
            try {
                FirebaseMessaging.getInstance().send(message)
                return true
            } catch (e: com.google.firebase.messaging.FirebaseMessagingException) {
                if (isRetryable(e.messagingErrorCode)) {
                    log.warn(
                        "FCM 발송 재시도 ({}/{}): token={}, error={}",
                        attempt + 1,
                        MAX_RETRY,
                        token.take(20),
                        e.messagingErrorCode,
                    )
                } else {
                    log.error(
                        "FCM 발송 실패 (재시도 불가): token={}, error={}",
                        token.take(20),
                        e.messagingErrorCode,
                    )
                    return false
                }
            } catch (e: Exception) {
                log.error("FCM 발송 중 예상치 못한 오류: {}", e.message, e)
                return false
            }
        }
        log.error("FCM 발송 최종 실패: token={}", token.take(20))
        return false
    }

    private fun isRetryable(errorCode: MessagingErrorCode?): Boolean =
        errorCode == MessagingErrorCode.UNAVAILABLE ||
            errorCode == MessagingErrorCode.INTERNAL
}
