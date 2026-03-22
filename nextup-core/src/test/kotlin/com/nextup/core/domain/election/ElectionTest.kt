package com.nextup.core.domain.election

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit

class ElectionTest {
    @Test
    fun `should create election with valid data`() {
        // given
        val teamId = 1L
        val title = "구단주 선출"
        val description = "2024년 구단주 선출"
        val electionType = ElectionType.OWNER_ELECTION
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)

        // when
        val election =
            Election.create(
                teamId = teamId,
                title = title,
                description = description,
                electionType = electionType,
                startAt = startAt,
                endAt = endAt,
            )

        // then
        assertThat(election.teamId).isEqualTo(teamId)
        assertThat(election.title).isEqualTo(title)
        assertThat(election.description).isEqualTo(description)
        assertThat(election.electionType).isEqualTo(electionType)
        assertThat(election.startAt).isEqualTo(startAt)
        assertThat(election.endAt).isEqualTo(endAt)
        assertThat(election.status).isEqualTo(ElectionStatus.SCHEDULED)
    }

    @Test
    fun `should throw exception when title is blank`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)

        // when & then
        assertThrows<IllegalArgumentException> {
            Election.create(
                teamId = 1L,
                title = "",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        }
    }

    @Test
    fun `should throw exception when end time is before start time`() {
        // given
        val startAt = Instant.now().plus(7, ChronoUnit.DAYS)
        val endAt = startAt.minus(1, ChronoUnit.DAYS)

        // when & then
        assertThrows<IllegalArgumentException> {
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        }
    }

    @Test
    fun `should start election when status is SCHEDULED`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )

        // when
        election.start()

        // then
        assertThat(election.status).isEqualTo(ElectionStatus.IN_PROGRESS)
    }

    @Test
    fun `should throw exception when starting already started election`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        election.start()

        // when & then
        assertThrows<IllegalArgumentException> {
            election.start()
        }
    }

    @Test
    fun `should complete election when status is IN_PROGRESS`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        election.start()

        // when
        election.complete()

        // then
        assertThat(election.status).isEqualTo(ElectionStatus.COMPLETED)
    }

    @Test
    fun `should throw exception when completing not started election`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )

        // when & then
        assertThrows<IllegalArgumentException> {
            election.complete()
        }
    }

    @Test
    fun `should cancel election when status is SCHEDULED`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )

        // when
        election.cancel()

        // then
        assertThat(election.status).isEqualTo(ElectionStatus.CANCELLED)
    }

    @Test
    fun `should cancel election when status is IN_PROGRESS`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        election.start()

        // when
        election.cancel()

        // then
        assertThat(election.status).isEqualTo(ElectionStatus.CANCELLED)
    }

    @Test
    fun `should throw exception when canceling completed election`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        election.start()
        election.complete()

        // when & then
        assertThrows<IllegalArgumentException> {
            election.cancel()
        }
    }

    @Test
    fun `should return true when voting is open`() {
        // given
        val startAt = Instant.now().minus(1, ChronoUnit.HOURS)
        val endAt = Instant.now().plus(1, ChronoUnit.HOURS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        election.start()

        // when
        val isVotingOpen = election.isVotingOpen()

        // then
        assertThat(isVotingOpen).isTrue()
    }

    @Test
    fun `should return false when voting is not started yet`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.HOURS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        election.start()

        // when
        val isVotingOpen = election.isVotingOpen()

        // then
        assertThat(isVotingOpen).isFalse()
    }

    @Test
    fun `should return false when voting is ended`() {
        // given
        val startAt = Instant.now().minus(2, ChronoUnit.HOURS)
        val endAt = Instant.now().minus(1, ChronoUnit.HOURS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        election.start()

        // when
        val isVotingOpen = election.isVotingOpen()

        // then
        assertThat(isVotingOpen).isFalse()
    }

    @Test
    fun `should return false when election status is not IN_PROGRESS`() {
        // given
        val startAt = Instant.now().minus(1, ChronoUnit.HOURS)
        val endAt = Instant.now().plus(1, ChronoUnit.HOURS)
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )

        // when
        val isVotingOpen = election.isVotingOpen()

        // then
        assertThat(isVotingOpen).isFalse()
    }

    @Test
    fun `should create runoff election from parent election`() {
        // given
        val startAt = Instant.now().minus(7, ChronoUnit.DAYS)
        val endAt = startAt.plus(3, ChronoUnit.DAYS)
        val parentElection =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = "2024년 구단주 선출",
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        setElectionId(parentElection, 100L)

        // when
        val runoff =
            Election.createRunoff(
                parentElection = parentElection,
                currentRunoffCount = 0,
            )

        // then
        assertThat(runoff.teamId).isEqualTo(1L)
        assertThat(runoff.electionType).isEqualTo(ElectionType.OWNER_ELECTION)
        assertThat(runoff.isRunoff).isTrue()
        assertThat(runoff.parentElectionId).isEqualTo(100L)
        assertThat(runoff.status).isEqualTo(ElectionStatus.SCHEDULED)
        assertThat(runoff.title).contains("재선거 #1")
        assertThat(runoff.description).contains("동률 발생으로 인한 재선거")
    }

    @Test
    fun `should create second runoff election`() {
        // given
        val startAt = Instant.now().minus(7, ChronoUnit.DAYS)
        val endAt = startAt.plus(3, ChronoUnit.DAYS)
        val parentElection =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        setElectionId(parentElection, 100L)

        // when
        val runoff =
            Election.createRunoff(
                parentElection = parentElection,
                currentRunoffCount = 1,
            )

        // then
        assertThat(runoff.title).contains("재선거 #2")
        assertThat(runoff.isRunoff).isTrue()
    }

    @Test
    fun `should throw exception when max runoff count exceeded`() {
        // given
        val startAt = Instant.now().minus(7, ChronoUnit.DAYS)
        val endAt = startAt.plus(3, ChronoUnit.DAYS)
        val parentElection =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        setElectionId(parentElection, 100L)

        // when & then
        assertThrows<IllegalArgumentException> {
            Election.createRunoff(
                parentElection = parentElection,
                currentRunoffCount = Election.MAX_RUNOFF_COUNT.toLong(),
            )
        }
    }

    @Test
    fun `runoff election should preserve original duration`() {
        // given
        val startAt = Instant.now().minus(7, ChronoUnit.DAYS)
        val endAt = startAt.plus(5, ChronoUnit.DAYS)
        val parentElection =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )
        setElectionId(parentElection, 100L)
        val originalDuration =
            java.time.Duration.between(parentElection.startAt, parentElection.endAt)

        // when
        val runoff =
            Election.createRunoff(
                parentElection = parentElection,
                currentRunoffCount = 0,
            )

        // then
        val runoffDuration =
            java.time.Duration.between(runoff.startAt, runoff.endAt)
        assertThat(runoffDuration).isEqualTo(originalDuration)
    }

    @Test
    fun `default election should not be runoff`() {
        // given
        val startAt = Instant.now().plus(1, ChronoUnit.DAYS)
        val endAt = startAt.plus(7, ChronoUnit.DAYS)

        // when
        val election =
            Election.create(
                teamId = 1L,
                title = "구단주 선출",
                description = null,
                electionType = ElectionType.OWNER_ELECTION,
                startAt = startAt,
                endAt = endAt,
            )

        // then
        assertThat(election.isRunoff).isFalse()
        assertThat(election.parentElectionId).isNull()
    }

    private fun setElectionId(
        election: Election,
        id: Long,
    ) {
        val idField = Election::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(election, id)
    }
}
