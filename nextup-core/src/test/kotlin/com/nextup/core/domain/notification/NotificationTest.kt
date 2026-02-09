package com.nextup.core.domain.notification

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class NotificationTest {
    @Test
    fun `should create notification with valid data`() {
        // given & when
        val notification =
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "30분 후 경기가 시작됩니다",
                data = """{"gameId": 123}""",
            )

        // then
        assertThat(notification.userId).isEqualTo(1L)
        assertThat(notification.type).isEqualTo(NotificationType.GAME_START)
        assertThat(notification.title).isEqualTo("경기 시작")
        assertThat(notification.body).isEqualTo("30분 후 경기가 시작됩니다")
        assertThat(notification.data).isEqualTo("""{"gameId": 123}""")
        assertThat(notification.readAt).isNull()
        assertThat(notification.sentAt).isNull()
    }

    @Test
    fun `should fail when userId is invalid`() {
        // given & when & then
        assertThatThrownBy {
            Notification.create(
                userId = 0L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "30분 후 경기가 시작됩니다",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("사용자 ID는 필수입니다")
    }

    @Test
    fun `should fail when title is blank`() {
        // given & when & then
        assertThatThrownBy {
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "",
                body = "30분 후 경기가 시작됩니다",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("제목은 필수입니다")
    }

    @Test
    fun `should fail when body is blank`() {
        // given & when & then
        assertThatThrownBy {
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "",
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("내용은 필수입니다")
    }

    @Test
    fun `should mark notification as read`() {
        // given
        val notification =
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "30분 후 경기가 시작됩니다",
            )

        // when
        notification.markAsRead()

        // then
        assertThat(notification.readAt).isNotNull()
        assertThat(notification.isRead()).isTrue()
    }

    @Test
    fun `should mark notification as sent`() {
        // given
        val notification =
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "30분 후 경기가 시작됩니다",
            )

        // when
        notification.markAsSent()

        // then
        assertThat(notification.sentAt).isNotNull()
        assertThat(notification.isSent()).isTrue()
    }

    @Test
    fun `should not change readAt when already read`() {
        // given
        val notification =
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "30분 후 경기가 시작됩니다",
            )
        notification.markAsRead()
        val firstReadAt = notification.readAt

        // when
        notification.markAsRead()

        // then
        assertThat(notification.readAt).isEqualTo(firstReadAt)
    }

    @Test
    fun `should not change sentAt when already sent`() {
        // given
        val notification =
            Notification.create(
                userId = 1L,
                type = NotificationType.GAME_START,
                title = "경기 시작",
                body = "30분 후 경기가 시작됩니다",
            )
        notification.markAsSent()
        val firstSentAt = notification.sentAt

        // when
        notification.markAsSent()

        // then
        assertThat(notification.sentAt).isEqualTo(firstSentAt)
    }
}
