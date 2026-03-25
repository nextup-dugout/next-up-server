package com.nextup.core.domain.eventgame

import com.nextup.common.exception.InvalidStateException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("EventGameParticipant 엔티티 테스트")
class EventGameParticipantTest {
    private fun createEventGame(): EventGame =
        EventGame.create(
            organizerId = 1L,
            title = "주말 픽업 게임",
            scheduledAt = LocalDateTime.now().plusDays(7),
            maxParticipants = 20,
        )

    @Nested
    @DisplayName("confirm()")
    inner class Confirm {
        @Test
        fun `참가 확정 성공`() {
            val participant = EventGameParticipant.create(createEventGame(), 10L)
            participant.confirm()
            assertThat(participant.status).isEqualTo(EventGameParticipantStatus.CONFIRMED)
        }

        @Test
        fun `이미 확정된 참가자를 다시 확정하면 예외`() {
            val participant = EventGameParticipant.create(createEventGame(), 10L)
            participant.confirm()
            assertThatThrownBy { participant.confirm() }
                .isInstanceOf(InvalidStateException::class.java)
        }
    }

    @Nested
    @DisplayName("cancel()")
    inner class Cancel {
        @Test
        fun `참가 취소 성공 (APPLIED 상태)`() {
            val participant = EventGameParticipant.create(createEventGame(), 10L)
            participant.cancel()
            assertThat(participant.status).isEqualTo(EventGameParticipantStatus.CANCELLED)
        }

        @Test
        fun `참가 취소 성공 (CONFIRMED 상태)`() {
            val participant = EventGameParticipant.create(createEventGame(), 10L)
            participant.confirm()
            participant.cancel()
            assertThat(participant.status).isEqualTo(EventGameParticipantStatus.CANCELLED)
            assertThat(participant.teamAssignment).isNull()
        }

        @Test
        fun `이미 취소된 참가자를 다시 취소하면 예외`() {
            val participant = EventGameParticipant.create(createEventGame(), 10L)
            participant.cancel()
            assertThatThrownBy { participant.cancel() }
                .isInstanceOf(InvalidStateException::class.java)
        }
    }

    @Nested
    @DisplayName("assignTeam()")
    inner class AssignTeam {
        @Test
        fun `팀 배정 성공`() {
            val participant = EventGameParticipant.create(createEventGame(), 10L)
            participant.confirm()
            participant.assignTeam(TeamAssignment.TEAM_A)
            assertThat(participant.teamAssignment).isEqualTo(TeamAssignment.TEAM_A)
        }

        @Test
        fun `확정되지 않은 참가자에게 팀 배정하면 예외`() {
            val participant = EventGameParticipant.create(createEventGame(), 10L)
            assertThatThrownBy { participant.assignTeam(TeamAssignment.TEAM_A) }
                .isInstanceOf(InvalidStateException::class.java)
                .hasMessageContaining("확정된 참가자")
        }
    }
}
