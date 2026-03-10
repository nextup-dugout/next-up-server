package com.nextup.infrastructure.service.certificate

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64
import javax.imageio.ImageIO

@DisplayName("ZxingQrCodeGenerator")
class ZxingQrCodeGeneratorTest {
    private val generator = ZxingQrCodeGenerator()

    @Test
    fun `QR 코드를 Base64 인코딩된 PNG 이미지로 생성한다`() {
        // when
        val result = generator.generate("https://nextup.kr/verify/ABC-123")

        // then
        assertThat(result).isNotBlank()
        val decoded = Base64.getDecoder().decode(result)
        assertThat(decoded).isNotEmpty()

        // PNG 헤더 확인 (0x89504E47)
        assertThat(decoded[0]).isEqualTo(0x89.toByte())
        assertThat(decoded[1]).isEqualTo(0x50.toByte()) // 'P'
        assertThat(decoded[2]).isEqualTo(0x4E.toByte()) // 'N'
        assertThat(decoded[3]).isEqualTo(0x47.toByte()) // 'G'
    }

    @Test
    fun `지정된 크기로 QR 코드를 생성한다`() {
        // when
        val result = generator.generate("test-content", size = 200)

        // then
        val decoded = Base64.getDecoder().decode(result)
        val image = ImageIO.read(decoded.inputStream())
        assertThat(image.width).isEqualTo(200)
        assertThat(image.height).isEqualTo(200)
    }

    @Test
    fun `기본 크기 300px로 QR 코드를 생성한다`() {
        // when
        val result = generator.generate("default-size-test")

        // then
        val decoded = Base64.getDecoder().decode(result)
        val image = ImageIO.read(decoded.inputStream())
        assertThat(image.width).isEqualTo(300)
        assertThat(image.height).isEqualTo(300)
    }

    @Test
    fun `한글 콘텐츠로 QR 코드를 생성할 수 있다`() {
        // when
        val result = generator.generate("증명서 검증: 홍길동")

        // then
        assertThat(result).isNotBlank()
        val decoded = Base64.getDecoder().decode(result)
        assertThat(decoded).isNotEmpty()
    }

    @Test
    fun `빈 문자열이 아닌 콘텐츠는 서로 다른 QR 코드를 생성한다`() {
        // when
        val result1 = generator.generate("content-A")
        val result2 = generator.generate("content-B")

        // then
        assertThat(result1).isNotEqualTo(result2)
    }
}
