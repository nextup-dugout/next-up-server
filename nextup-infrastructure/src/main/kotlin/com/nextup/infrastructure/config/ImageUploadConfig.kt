package com.nextup.infrastructure.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * 이미지 업로드 설정
 *
 * ImageUploadProperties를 빈으로 등록합니다.
 */
@Configuration
@EnableConfigurationProperties(ImageUploadProperties::class)
class ImageUploadConfig
