package com.nextup.core.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DeviceTokenTest {
    @Test
    fun `should create device token with valid data`() {
        // given & when
        val deviceToken =
            DeviceToken.create(
                userId = 1L,
                token = "fcm-token-12345",
                platform = DevicePlatform.IOS,
            )

        // then
        assertThat(deviceToken.userId).isEqualTo(1L)
        assertThat(deviceToken.token).isEqualTo("fcm-token-12345")
        assertThat(deviceToken.platform).isEqualTo(DevicePlatform.IOS)
    }

    @Test
    fun `should fail when userId is invalid`() {
        // given & when & then
        assertThatThrownBy {
            DeviceToken.create(
                userId = 0L,
                token = "fcm-token-12345",
                platform = DevicePlatform.IOS,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("사용자 ID는 필수입니다")
    }

    @Test
    fun `should fail when token is blank`() {
        // given & when & then
        assertThatThrownBy {
            DeviceToken.create(
                userId = 1L,
                token = "",
                platform = DevicePlatform.IOS,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("토큰은 필수입니다")
    }

    @Test
    fun `should create device token for different platforms`() {
        // given & when
        val iosToken =
            DeviceToken.create(
                userId = 1L,
                token = "ios-token",
                platform = DevicePlatform.IOS,
            )
        val androidToken =
            DeviceToken.create(
                userId = 1L,
                token = "android-token",
                platform = DevicePlatform.ANDROID,
            )
        val webToken =
            DeviceToken.create(
                userId = 1L,
                token = "web-token",
                platform = DevicePlatform.WEB,
            )

        // then
        assertThat(iosToken.platform).isEqualTo(DevicePlatform.IOS)
        assertThat(androidToken.platform).isEqualTo(DevicePlatform.ANDROID)
        assertThat(webToken.platform).isEqualTo(DevicePlatform.WEB)
    }
}
