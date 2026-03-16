package com.nextup.core.domain.event

import com.nextup.core.domain.election.ElectionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ElectionTiedEvent 테스트")
class ElectionTiedEventTest {
    @Test
    fun `동률 이벤트를 올바르게 생성한다`() {
        // when
        val event =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 10L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 3,
                tiedVoteCount = 5L,
            )

        // then
        assertThat(event.electionId).isEqualTo(1L)
        assertThat(event.teamId).isEqualTo(10L)
        assertThat(event.electionType).isEqualTo(ElectionType.OWNER_ELECTION)
        assertThat(event.tiedCandidateCount).isEqualTo(3)
        assertThat(event.tiedVoteCount).isEqualTo(5L)
    }

    @Test
    fun `data class의 equals가 올바르게 동작한다`() {
        // given
        val event1 =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 10L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 2,
                tiedVoteCount = 5L,
            )
        val event2 =
            ElectionTiedEvent(
                electionId = 1L,
                teamId = 10L,
                electionType = ElectionType.OWNER_ELECTION,
                tiedCandidateCount = 2,
                tiedVoteCount = 5L,
            )

        // then
        assertThat(event1).isEqualTo(event2)
    }
}
