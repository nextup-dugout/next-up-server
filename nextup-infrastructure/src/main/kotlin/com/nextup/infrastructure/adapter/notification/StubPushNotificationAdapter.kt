package com.nextup.infrastructure.adapter.notification

import com.nextup.core.port.service.PushNotificationPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 푸시 알림 Stub 어댑터
 *
 * 테스트 환경에서 실제 FCM 발송 없이 로그만 출력합니다.
 */
@Component
@Profile("test")
class StubPushNotificationAdapter : PushNotificationPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>?,
    ): Boolean {
        log.info("[STUB] 푸시 발송: token={}, title={}", token.take(20), title)
        return true
    }

    override fun sendBatch(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>?,
    ): Int {
        log.info("[STUB] 푸시 배치 발송: count={}, title={}", tokens.size, title)
        return tokens.size
    }
}
