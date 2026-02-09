package com.nextup.core.domain.election

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CandidateTest {
    @Test
    fun `should create candidate with valid data`() {
        // given
        val electionId = 1L
        val memberId = 100L
        val memberName = "홍길동"
        val statement = "열심히 하겠습니다"

        // when
        val candidate =
            Candidate.create(
                electionId = electionId,
                memberId = memberId,
                memberName = memberName,
                statement = statement,
            )

        // then
        assertThat(candidate.electionId).isEqualTo(electionId)
        assertThat(candidate.memberId).isEqualTo(memberId)
        assertThat(candidate.memberName).isEqualTo(memberName)
        assertThat(candidate.statement).isEqualTo(statement)
    }

    @Test
    fun `should create candidate without statement`() {
        // given
        val electionId = 1L
        val memberId = 100L
        val memberName = "홍길동"

        // when
        val candidate =
            Candidate.create(
                electionId = electionId,
                memberId = memberId,
                memberName = memberName,
                statement = null,
            )

        // then
        assertThat(candidate.electionId).isEqualTo(electionId)
        assertThat(candidate.memberId).isEqualTo(memberId)
        assertThat(candidate.memberName).isEqualTo(memberName)
        assertThat(candidate.statement).isNull()
    }

    @Test
    fun `should throw exception when member name is blank`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            Candidate.create(
                electionId = 1L,
                memberId = 100L,
                memberName = "",
                statement = null,
            )
        }
    }
}
