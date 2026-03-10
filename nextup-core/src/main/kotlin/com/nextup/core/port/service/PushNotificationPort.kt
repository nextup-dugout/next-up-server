package com.nextup.core.port.service

/**
 * 푸시 알림 발송 포트
 *
 * Infrastructure 계층에서 FCM 등 실제 푸시 서비스로 구현합니다.
 */
interface PushNotificationPort {
    /**
     * 단일 디바이스에 푸시 알림을 발송합니다.
     *
     * @param token FCM 디바이스 토큰
     * @param title 알림 제목
     * @param body 알림 본문
     * @param data 추가 데이터
     * @return 발송 성공 여부
     */
    fun send(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>? = null,
    ): Boolean

    /**
     * 여러 디바이스에 푸시 알림을 일괄 발송합니다.
     *
     * @param tokens FCM 디바이스 토큰 목록
     * @param title 알림 제목
     * @param body 알림 본문
     * @param data 추가 데이터
     * @return 성공한 발송 수
     */
    fun sendBatch(
        tokens: List<String>,
        title: String,
        body: String,
        data: Map<String, String>? = null,
    ): Int
}
