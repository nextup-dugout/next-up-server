package com.nextup.core.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class NotificationPreferenceTest {
    @Test
    fun `should create notification preference with default enabled`() {
        // given & when
        val preference =
            NotificationPreference.create(
                userId = 1L,
                type = NotificationType.GAME_START,
            )

        // then
        assertThat(preference.userId).isEqualTo(1L)
        assertThat(preference.type).isEqualTo(NotificationType.GAME_START)
        assertThat(preference.enabled).isTrue()
    }

    @Test
    fun `should create notification preference with disabled`() {
        // given & when
        val preference =
            NotificationPreference.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                enabled = false,
            )

        // then
        assertThat(preference.enabled).isFalse()
    }

    @Test
    fun `should fail when userId is invalid`() {
        // given & when & then
        assertThatThrownBy {
            NotificationPreference.create(
                userId = 0L,
                type = NotificationType.GAME_START,
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("사용자 ID는 필수입니다")
    }

    @Test
    fun `should enable notification`() {
        // given
        val preference =
            NotificationPreference.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                enabled = false,
            )

        // when
        preference.enable()

        // then
        assertThat(preference.enabled).isTrue()
    }

    @Test
    fun `should disable notification`() {
        // given
        val preference =
            NotificationPreference.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                enabled = true,
            )

        // when
        preference.disable()

        // then
        assertThat(preference.enabled).isFalse()
    }

    @Test
    fun `should toggle notification preference`() {
        // given
        val preference =
            NotificationPreference.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                enabled = true,
            )

        // when
        preference.toggle()

        // then
        assertThat(preference.enabled).isFalse()

        // when
        preference.toggle()

        // then
        assertThat(preference.enabled).isTrue()
    }
}
