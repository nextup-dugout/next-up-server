package com.nextup.core.domain.game

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

class LineupSubmissionTest {
    @Test
    fun `should create lineup submission in DRAFT status`() {
        // given
        val game = createGame()
        val team = createTeam()
        val manager = createManager()

        // when
        val submission = LineupSubmission.create(game, team, manager)

        // then
        assertThat(submission.game).isEqualTo(game)
        assertThat(submission.team).isEqualTo(team)
        assertThat(submission.submittedBy).isEqualTo(manager)
        assertThat(submission.status).isEqualTo(LineupSubmissionStatus.DRAFT)
        assertThat(submission.submittedAt).isNull()
        assertThat(submission.confirmedAt).isNull()
        assertThat(submission.rejectionReason).isNull()
    }

    @Test
    fun `should submit lineup when status is DRAFT`() {
        // given
        val submission = createLineupSubmissionWithEntries()

        // when
        submission.submit()

        // then
        assertThat(submission.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        assertThat(submission.submittedAt).isNotNull()
    }

    @Test
    fun `should throw exception when submitting already submitted lineup`() {
        // given
        val submission = createLineupSubmissionWithEntries().apply { submit() }

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                submission.submit()
            }
        assertThat(exception.message).contains("제출 가능한 상태가 아닙니다")
    }

    @Test
    fun `should confirm lineup when status is SUBMITTED`() {
        // given
        val submission = createLineupSubmissionWithEntries().apply { submit() }
        val scorer = createScorer()

        // when
        submission.confirm(scorer)

        // then
        assertThat(submission.status).isEqualTo(LineupSubmissionStatus.CONFIRMED)
        assertThat(submission.confirmedAt).isNotNull()
        assertThat(submission.confirmedBy).isEqualTo(scorer)
    }

    @Test
    fun `should throw exception when confirming non-submitted lineup`() {
        // given
        val submission = createLineupSubmission()
        val scorer = createScorer()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                submission.confirm(scorer)
            }
        assertThat(exception.message).contains("제출된 상태의 라인업만 확인할 수 있습니다")
    }

    @Test
    fun `should reject lineup with reason when status is SUBMITTED`() {
        // given
        val submission = createLineupSubmissionWithEntries().apply { submit() }
        val scorer = createScorer()
        val reason = "선수 등록번호 확인 필요"

        // when
        submission.reject(scorer, reason)

        // then
        assertThat(submission.status).isEqualTo(LineupSubmissionStatus.REJECTED)
        assertThat(submission.rejectionReason).isEqualTo(reason)
        assertThat(submission.rejectedBy).isEqualTo(scorer)
    }

    @Test
    fun `should throw exception when rejecting non-submitted lineup`() {
        // given
        val submission = createLineupSubmission()
        val scorer = createScorer()

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                submission.reject(scorer, "반려 사유")
            }
        assertThat(exception.message).contains("제출된 상태의 라인업만 반려할 수 있습니다")
    }

    @Test
    fun `should resubmit lineup when status is REJECTED`() {
        // given
        val submission =
            createLineupSubmissionWithEntries().apply {
                submit()
                reject(createScorer(), "수정 필요")
            }

        // when
        submission.submit()

        // then
        assertThat(submission.status).isEqualTo(LineupSubmissionStatus.SUBMITTED)
        assertThat(submission.submittedAt).isNotNull()
        assertThat(submission.rejectionReason).isNull()
        assertThat(submission.rejectedBy).isNull()
    }

    @Test
    fun `should not allow editing confirmed lineup`() {
        // given
        val submission =
            createLineupSubmissionWithEntries().apply {
                submit()
                confirm(createScorer())
            }

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                submission.submit()
            }
        assertThat(exception.message).contains("제출 가능한 상태가 아닙니다")
    }

    private fun createLineupSubmissionWithEntries(): LineupSubmission {
        val submission = createLineupSubmission()
        addValidEntries(submission)
        return submission
    }

    private fun addValidEntries(submission: LineupSubmission) {
        val positions =
            listOf(
                Position.STARTING_PITCHER,
                Position.CATCHER,
                Position.FIRST_BASE,
                Position.SECOND_BASE,
                Position.THIRD_BASE,
                Position.SHORTSTOP,
                Position.LEFT_FIELD,
                Position.CENTER_FIELD,
                Position.RIGHT_FIELD,
            )
        positions.forEachIndexed { index, position ->
            val player =
                Player(
                    name = "선수${index + 1}",
                    primaryPosition = position,
                    id = (index + 1).toLong(),
                )
            submission.addEntry(
                LineupEntry(
                    submission = submission,
                    player = player,
                    position = position,
                    battingOrder = index + 1,
                    backNumber = index + 1,
                    isStarter = true,
                ),
            )
        }
    }

    private fun createLineupSubmission(): LineupSubmission =
        LineupSubmission.create(createGame(), createTeam(), createManager())

    private fun createGame(): Game {
        val association =
            com.nextup.core.domain.association.Association(
                name = "서울시야구협회",
                abbreviation = "서야협",
                region = "서울",
            )
        val league =
            League(
                association = association,
                name = "서울시 리그",
                foundedYear = 2020,
            )
        val competition =
            Competition(
                league = league,
                name = "2024 시즌",
                year = 2024,
                startDate =
                    java.time.LocalDate
                        .now()
                        .minusDays(30),
                endDate =
                    java.time.LocalDate
                        .now()
                        .plusDays(30),
            )
        return Game(
            competition = competition,
            scheduledAt = LocalDateTime.now().plusDays(1),
            location = "서울야구장",
        )
    }

    private fun createTeam(): Team {
        val association =
            com.nextup.core.domain.association.Association(
                name = "서울시야구협회",
                abbreviation = "서야협",
                region = "서울",
            )
        val league =
            League(
                association = association,
                name = "서울시 리그",
                foundedYear = 2020,
            )
        return Team(
            league = league,
            name = "Tigers",
            city = "서울",
            foundedYear = 2020,
        )
    }

    private fun createManager(): User =
        User.createLocalUser(
            email = "manager@test.com",
            encodedPassword = "encoded",
            nickname = "감독",
        )

    private fun createScorer(): User =
        User.createLocalUser(
            email = "scorer@test.com",
            encodedPassword = "encoded",
            nickname = "기록원",
        )
}
