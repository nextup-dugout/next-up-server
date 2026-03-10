package com.nextup.infrastructure.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.io.FileInputStream

/**
 * Firebase Admin SDK 초기화 설정
 *
 * GOOGLE_APPLICATION_CREDENTIALS 환경변수 또는 firebase.credentials-path 속성으로
 * 서비스 계정 키 파일 경로를 지정합니다.
 */
@Configuration
@Profile("!test")
class FirebaseConfig(
    @Value("\${firebase.credentials-path:}")
    private val credentialsPath: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun initialize() {
        if (FirebaseApp.getApps().isNotEmpty()) {
            log.info("Firebase 이미 초기화됨")
            return
        }

        try {
            val options =
                if (credentialsPath.isNotBlank()) {
                    FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(FileInputStream(credentialsPath)))
                        .build()
                } else {
                    FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .build()
                }
            FirebaseApp.initializeApp(options)
            log.info("Firebase Admin SDK 초기화 완료")
        } catch (e: Exception) {
            log.warn("Firebase Admin SDK 초기화 실패: {}. 푸시 알림이 비활성화됩니다.", e.message)
        }
    }
}
