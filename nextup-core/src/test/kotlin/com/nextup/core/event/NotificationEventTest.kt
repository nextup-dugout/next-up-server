package com.nextup.core.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("알림 이벤트 데이터 클래스 테스트")
class NotificationEventTest {
    @Nested
    @DisplayName("TeamJoinApprovedEvent")
    inner class TeamJoinApprovedEventTest {
        @Test
        fun `should create event with correct fields`() {
            // given & when
            val event =
                TeamJoinApprovedEvent(
                    teamId = 1L,
                    userId = 10L,
                    teamName = "타이거즈",
                )

            // then
            assertThat(event.teamId).isEqualTo(1L)
            assertThat(event.userId).isEqualTo(10L)
            assertThat(event.teamName).isEqualTo("타이거즈")
        }

        @Test
        fun `should support equals and hashCode`() {
            // given
            val event1 = TeamJoinApprovedEvent(teamId = 1L, userId = 10L, teamName = "타이거즈")
            val event2 = TeamJoinApprovedEvent(teamId = 1L, userId = 10L, teamName = "타이거즈")
            val event3 = TeamJoinApprovedEvent(teamId = 2L, userId = 10L, teamName = "라이온즈")

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        fun `should support copy`() {
            // given
            val original = TeamJoinApprovedEvent(teamId = 1L, userId = 10L, teamName = "타이거즈")

            // when
            val copied = original.copy(teamName = "라이온즈")

            // then
            assertThat(copied.teamId).isEqualTo(1L)
            assertThat(copied.userId).isEqualTo(10L)
            assertThat(copied.teamName).isEqualTo("라이온즈")
        }

        @Test
        fun `should support toString`() {
            // given
            val event = TeamJoinApprovedEvent(teamId = 1L, userId = 10L, teamName = "타이거즈")

            // when
            val str = event.toString()

            // then
            assertThat(str).contains("타이거즈")
            assertThat(str).contains("TeamJoinApprovedEvent")
        }
    }

    @Nested
    @DisplayName("TeamJoinRejectedEvent")
    inner class TeamJoinRejectedEventTest {
        @Test
        fun `should create event with correct fields`() {
            // given & when
            val event =
                TeamJoinRejectedEvent(
                    teamId = 2L,
                    userId = 20L,
                    teamName = "라이온즈",
                )

            // then
            assertThat(event.teamId).isEqualTo(2L)
            assertThat(event.userId).isEqualTo(20L)
            assertThat(event.teamName).isEqualTo("라이온즈")
        }

        @Test
        fun `should support equals and hashCode`() {
            // given
            val event1 = TeamJoinRejectedEvent(teamId = 2L, userId = 20L, teamName = "라이온즈")
            val event2 = TeamJoinRejectedEvent(teamId = 2L, userId = 20L, teamName = "라이온즈")
            val event3 = TeamJoinRejectedEvent(teamId = 3L, userId = 30L, teamName = "베어즈")

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        fun `should support copy`() {
            // given
            val original = TeamJoinRejectedEvent(teamId = 2L, userId = 20L, teamName = "라이온즈")

            // when
            val copied = original.copy(userId = 99L)

            // then
            assertThat(copied.teamId).isEqualTo(2L)
            assertThat(copied.userId).isEqualTo(99L)
            assertThat(copied.teamName).isEqualTo("라이온즈")
        }

        @Test
        fun `should support toString`() {
            // given
            val event = TeamJoinRejectedEvent(teamId = 2L, userId = 20L, teamName = "라이온즈")

            // when
            val str = event.toString()

            // then
            assertThat(str).contains("라이온즈")
            assertThat(str).contains("TeamJoinRejectedEvent")
        }
    }

    @Nested
    @DisplayName("AttendanceVoteCreatedEvent")
    inner class AttendanceVoteCreatedEventTest {
        @Test
        fun `should create event with correct fields`() {
            // given
            val eventDate = LocalDateTime.of(2026, 3, 15, 10, 0)

            // when
            val event =
                AttendanceVoteCreatedEvent(
                    teamId = 1L,
                    pollId = 5L,
                    eventDate = eventDate,
                )

            // then
            assertThat(event.teamId).isEqualTo(1L)
            assertThat(event.pollId).isEqualTo(5L)
            assertThat(event.eventDate).isEqualTo(eventDate)
        }

        @Test
        fun `should support equals and hashCode`() {
            // given
            val date = LocalDateTime.of(2026, 3, 15, 10, 0)
            val event1 = AttendanceVoteCreatedEvent(teamId = 1L, pollId = 5L, eventDate = date)
            val event2 = AttendanceVoteCreatedEvent(teamId = 1L, pollId = 5L, eventDate = date)
            val event3 = AttendanceVoteCreatedEvent(teamId = 2L, pollId = 6L, eventDate = date)

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        fun `should support copy`() {
            // given
            val date = LocalDateTime.of(2026, 3, 15, 10, 0)
            val original = AttendanceVoteCreatedEvent(teamId = 1L, pollId = 5L, eventDate = date)

            // when
            val newDate = LocalDateTime.of(2026, 4, 1, 14, 0)
            val copied = original.copy(eventDate = newDate, pollId = 10L)

            // then
            assertThat(copied.teamId).isEqualTo(1L)
            assertThat(copied.pollId).isEqualTo(10L)
            assertThat(copied.eventDate).isEqualTo(newDate)
        }

        @Test
        fun `should support toString`() {
            // given
            val event =
                AttendanceVoteCreatedEvent(
                    teamId = 1L,
                    pollId = 5L,
                    eventDate = LocalDateTime.of(2026, 3, 15, 10, 0),
                )

            // when
            val str = event.toString()

            // then
            assertThat(str).contains("AttendanceVoteCreatedEvent")
        }
    }

    @Nested
    @DisplayName("GameResultConfirmedEvent")
    inner class GameResultConfirmedEventTest {
        @Test
        fun `should create event with correct fields`() {
            // given & when
            val event =
                GameResultConfirmedEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // then
            assertThat(event.gameId).isEqualTo(100L)
            assertThat(event.homeTeamId).isEqualTo(1L)
            assertThat(event.awayTeamId).isEqualTo(2L)
            assertThat(event.homeScore).isEqualTo(5)
            assertThat(event.awayScore).isEqualTo(3)
        }

        @Test
        fun `should support equals and hashCode`() {
            // given
            val event1 =
                GameResultConfirmedEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )
            val event2 =
                GameResultConfirmedEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )
            val event3 =
                GameResultConfirmedEvent(
                    gameId = 200L,
                    homeTeamId = 3L,
                    awayTeamId = 4L,
                    homeScore = 0,
                    awayScore = 7,
                )

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        fun `should support copy`() {
            // given
            val original =
                GameResultConfirmedEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when
            val copied = original.copy(homeScore = 10, awayScore = 0)

            // then
            assertThat(copied.gameId).isEqualTo(100L)
            assertThat(copied.homeScore).isEqualTo(10)
            assertThat(copied.awayScore).isEqualTo(0)
        }

        @Test
        fun `should support toString`() {
            // given
            val event =
                GameResultConfirmedEvent(
                    gameId = 100L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 5,
                    awayScore = 3,
                )

            // when
            val str = event.toString()

            // then
            assertThat(str).contains("GameResultConfirmedEvent")
        }

        @Test
        fun `should correctly represent tie game`() {
            // given & when
            val event =
                GameResultConfirmedEvent(
                    gameId = 50L,
                    homeTeamId = 1L,
                    awayTeamId = 2L,
                    homeScore = 3,
                    awayScore = 3,
                )

            // then
            assertThat(event.homeScore).isEqualTo(event.awayScore)
        }
    }

    @Nested
    @DisplayName("LineupConfirmedEvent")
    inner class LineupConfirmedEventTest {
        @Test
        fun `should create event with correct fields`() {
            // given & when
            val event =
                LineupConfirmedEvent(
                    gameId = 100L,
                    teamId = 1L,
                )

            // then
            assertThat(event.gameId).isEqualTo(100L)
            assertThat(event.teamId).isEqualTo(1L)
        }

        @Test
        fun `should support equals and hashCode`() {
            // given
            val event1 = LineupConfirmedEvent(gameId = 100L, teamId = 1L)
            val event2 = LineupConfirmedEvent(gameId = 100L, teamId = 1L)
            val event3 = LineupConfirmedEvent(gameId = 200L, teamId = 2L)

            // then
            assertThat(event1).isEqualTo(event2)
            assertThat(event1).isNotEqualTo(event3)
            assertThat(event1.hashCode()).isEqualTo(event2.hashCode())
        }

        @Test
        fun `should support copy`() {
            // given
            val original = LineupConfirmedEvent(gameId = 100L, teamId = 1L)

            // when
            val copied = original.copy(teamId = 99L)

            // then
            assertThat(copied.gameId).isEqualTo(100L)
            assertThat(copied.teamId).isEqualTo(99L)
        }

        @Test
        fun `should support toString`() {
            // given
            val event = LineupConfirmedEvent(gameId = 100L, teamId = 1L)

            // when
            val str = event.toString()

            // then
            assertThat(str).contains("LineupConfirmedEvent")
        }
    }
}
