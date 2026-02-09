package com.nextup.core.domain.election

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class ElectionVoteTest {
    @Test
    fun `should create election vote with valid data`() {
        // given
        val electionId = 1L
        val voterId = 100L
        val candidateId = 10L
        val votedAt = Instant.now()

        // when
        val vote =
            ElectionVote.create(
                electionId = electionId,
                voterId = voterId,
                candidateId = candidateId,
                votedAt = votedAt,
            )

        // then
        assertThat(vote.electionId).isEqualTo(electionId)
        assertThat(vote.voterId).isEqualTo(voterId)
        assertThat(vote.candidateId).isEqualTo(candidateId)
        assertThat(vote.votedAt).isEqualTo(votedAt)
    }

    @Test
    fun `should create election vote with default votedAt`() {
        // given
        val electionId = 1L
        val voterId = 100L
        val candidateId = 10L

        // when
        val vote =
            ElectionVote.create(
                electionId = electionId,
                voterId = voterId,
                candidateId = candidateId,
            )

        // then
        assertThat(vote.electionId).isEqualTo(electionId)
        assertThat(vote.voterId).isEqualTo(voterId)
        assertThat(vote.candidateId).isEqualTo(candidateId)
        assertThat(vote.votedAt).isNotNull()
    }
}
