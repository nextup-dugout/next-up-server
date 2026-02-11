package com.nextup.infrastructure.service.certificate

import com.nextup.core.port.service.QrCodeGeneratorPort
import org.springframework.stereotype.Component
import java.util.*

/**
 * QR 코드 생성기 스텁 구현
 *
 * 실제 QR 코드 생성 라이브러리는 추가하지 않고,
 * Base64로 인코딩된 더미 데이터를 반환합니다.
 */
@Component
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
