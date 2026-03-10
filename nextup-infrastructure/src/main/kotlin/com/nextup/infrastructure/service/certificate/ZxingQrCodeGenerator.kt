package com.nextup.infrastructure.service.certificate

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.client.j2se.MatrixToImageWriter
import com.google.zxing.qrcode.QRCodeWriter
import com.nextup.core.port.service.QrCodeGeneratorPort
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.io.ByteArrayOutputStream
import java.util.Base64

/**
 * ZXing 기반 QR 코드 생성기
 *
 * QR 코드를 실제 PNG 이미지로 생성하여 Base64로 인코딩합니다.
 */
@Component
@Primary
class ZxingQrCodeGenerator : QrCodeGeneratorPort {
    override fun generate(
        content: String,
        size: Int,
    ): String {
        val hints =
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
            )

        val bitMatrix =
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                hints,
            )

        val outputStream = ByteArrayOutputStream()
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream)

        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }
}
