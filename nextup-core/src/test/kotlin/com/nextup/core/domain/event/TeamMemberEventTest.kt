package com.nextup.core.domain.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("팀원 변동 이벤트 데이터 클래스 테스트")
class TeamMemberEventTest {
    @Nested
    @DisplayName("TeamDisbandedEvent")
    inner class TeamDisbandedEventTest {
        @Test
        @DisplayName("올바른 필드로 이벤트를 생성한다")
        fun `should create event with correct fields`() {
            // given & when
            val event = TeamDisbandedEvent(teamId = 1L)

            // then
            assertThat(event.teamId).isEqualTo(1L)
        }

        @Test
        @DisplayName("equals와 hashCode를 지원한다")
        fun `should support equals and hashCode`() {
            // given
            val event1 = TeamDisbandedEvent(teamId = 1L)
            val event2 = TeamDisbandedEvent(teamId = 1L)
            val event3 = TeamDisbandedEvent(teamId = 2L)

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        @DisplayName("copy 메서드를 지원한다")
        fun `should support copy`() {
            // given
            val original = TeamDisbandedEvent(teamId = 1L)

            // when
            val copied = original.copy(teamId = 99L)

            // then
            assertThat(copied.teamId).isEqualTo(99L)
            assertThat(original.teamId).isEqualTo(1L)
        }

        @Test
        @DisplayName("toString이 필드 정보를 포함한다")
        fun `should include field info in toString`() {
            // given
            val event = TeamDisbandedEvent(teamId = 42L)

            // then
            assertThat(event.toString()).contains("42")
        }
    }

    @Nested
    @DisplayName("TeamMemberKickedEvent")
    inner class TeamMemberKickedEventTest {
        @Test
        @DisplayName("올바른 필드로 이벤트를 생성한다")
        fun `should create event with correct fields`() {
            // given & when
            val event =
                TeamMemberKickedEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // then
            assertThat(event.teamId).isEqualTo(1L)
            assertThat(event.userId).isEqualTo(10L)
            assertThat(event.playerId).isEqualTo(20L)
            assertThat(event.memberId).isEqualTo(100L)
            assertThat(event.teamName).isEqualTo("타이거즈")
        }

        @Test
        @DisplayName("equals와 hashCode를 지원한다")
        fun `should support equals and hashCode`() {
            // given
            val event1 =
                TeamMemberKickedEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )
            val event2 =
                TeamMemberKickedEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )
            val event3 =
                TeamMemberKickedEvent(
                    teamId = 2L,
                    userId = 11L,
                    playerId = 30L,
                    memberId = 200L,
                    teamName = "라이온즈",
                )

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        @DisplayName("copy 메서드를 지원한다")
        fun `should support copy`() {
            // given
            val original =
                TeamMemberKickedEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // when
            val copied = original.copy(playerId = 99L)

            // then
            assertThat(copied.playerId).isEqualTo(99L)
            assertThat(copied.teamId).isEqualTo(1L)
            assertThat(copied.memberId).isEqualTo(100L)
        }

        @Test
        @DisplayName("toString이 필드 정보를 포함한다")
        fun `should include field info in toString`() {
            // given
            val event =
                TeamMemberKickedEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // then
            val str = event.toString()
            assertThat(str).contains("1")
            assertThat(str).contains("20")
            assertThat(str).contains("100")
        }
    }

    @Nested
    @DisplayName("TeamMemberLeftEvent")
    inner class TeamMemberLeftEventTest {
        @Test
        @DisplayName("올바른 필드로 이벤트를 생성한다")
        fun `should create event with correct fields`() {
            // given & when
            val event =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // then
            assertThat(event.teamId).isEqualTo(1L)
            assertThat(event.userId).isEqualTo(10L)
            assertThat(event.playerId).isEqualTo(20L)
            assertThat(event.memberId).isEqualTo(100L)
            assertThat(event.teamName).isEqualTo("타이거즈")
        }

        @Test
        @DisplayName("equals와 hashCode를 지원한다")
        fun `should support equals and hashCode`() {
            // given
            val event1 =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )
            val event2 =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )
            val event3 =
                TeamMemberLeftEvent(
                    teamId = 2L,
                    userId = 11L,
                    playerId = 30L,
                    memberId = 200L,
                    teamName = "라이온즈",
                )

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        @DisplayName("copy 메서드를 지원한다")
        fun `should support copy`() {
            // given
            val original =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // when
            val copied = original.copy(memberId = 999L)

            // then
            assertThat(copied.memberId).isEqualTo(999L)
            assertThat(copied.teamId).isEqualTo(1L)
            assertThat(copied.playerId).isEqualTo(20L)
        }

        @Test
        @DisplayName("toString이 필드 정보를 포함한다")
        fun `should include field info in toString`() {
            // given
            val event =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // then
            val str = event.toString()
            assertThat(str).contains("1")
            assertThat(str).contains("20")
            assertThat(str).contains("100")
        }

        @Test
        @DisplayName("TeamMemberLeftEvent와 TeamMemberKickedEvent는 서로 다른 타입이다")
        fun `should be different types from TeamMemberKickedEvent`() {
            // given
            val leftEvent =
                TeamMemberLeftEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )
            val kickedEvent =
                TeamMemberKickedEvent(
                    teamId = 1L,
                    userId = 10L,
                    playerId = 20L,
                    memberId = 100L,
                    teamName = "타이거즈",
                )

            // then
            assertThat(leftEvent).isNotEqualTo(kickedEvent)
        }
    }
}
