package com.nextup.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 이미지 업로드 설정 프로퍼티
 *
 * application.yml에서 `image.upload.*` 프로퍼티를 바인딩합니다.
 */
@ConfigurationProperties(prefix = "image.upload")
data class ImageUploadProperties(
    /** 최대 파일 크기 (바이트, 기본 5MB) */
    val maxFileSize: Long = 5 * 1024 * 1024,
    /** 허용 MIME 타입 목록 */
    val allowedContentTypes: Set<String> =
        setOf(
            "image/jpeg",
            "image/png",
            "image/webp",
        ),
    /** 로컬 저장 경로 */
    val storagePath: String = "uploads/images",
    /** 이미지 서빙 기본 URL */
    val baseUrl: String = "/images",
)
