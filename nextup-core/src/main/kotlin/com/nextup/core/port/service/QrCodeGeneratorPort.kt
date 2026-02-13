package com.nextup.core.port.service

/**
 * QR 코드 생성 Port
 * Core 모듈의 서비스 인터페이스 - Infrastructure에서 구현
 */
interface QrCodeGeneratorPort {
    /**
     * QR 코드를 생성합니다.
     *
     * @param content QR 코드에 인코딩할 내용
     * @param size QR 코드 크기 (픽셀)
     * @return Base64 인코딩된 PNG 이미지
     */
    fun generate(
        content: String,
        size: Int = 300,
    ): String
}
