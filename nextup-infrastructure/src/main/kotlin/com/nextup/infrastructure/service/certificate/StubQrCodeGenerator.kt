package com.nextup.infrastructure.service.certificate

import com.nextup.core.port.service.QrCodeGeneratorPort
import org.springframework.stereotype.Component
import java.util.*

/**
 * QR 코드 생성기 스텁 구현 (테스트 전용)
 *
 * 테스트 환경에서 외부 라이브러리 의존 없이 더미 데이터를 반환합니다.
 * 프로덕션에서는 ZxingQrCodeGenerator가 @Primary로 우선 사용됩니다.
 */
@Component
@org.springframework.context.annotation.Profile("test")
class StubQrCodeGenerator : QrCodeGeneratorPort {
    override fun generate(
        content: String,
        size: Int,
    ): String {
        // TODO: 실제 QR 코드 생성 라이브러리 (예: ZXing) 추가 시 구현
        val dummyData = "QR:$content:SIZE:$size"
        return Base64.getEncoder().encodeToString(dummyData.toByteArray())
    }
}
