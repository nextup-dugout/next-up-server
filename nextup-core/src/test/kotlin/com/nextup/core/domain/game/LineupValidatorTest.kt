package com.nextup.core.domain.game

import com.nextup.common.exception.DuplicatePlayerInLineupException
import com.nextup.common.exception.InvalidDhRuleException
import com.nextup.common.exception.NoCatcherInLineupException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class LineupValidatorTest {
    @Test
    fun `should pass validation with valid lineup`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineup(submission)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should throw NoCatcherInLineupException when no catcher in starters`() {
        // given
        val submission = createLineupSubmission()
        val entries = createLineupWithoutCatcher(submission)

        // when & then
        assertThrows<NoCatcherInLineupException> {
            LineupValidator.validate(entries)
        }
    }

    @Test
    fun `should throw DuplicatePlayerInLineupException when same player appears twice`() {
        // given
        val submission = createLineupSubmission()
        val samePlayer = createPlayer("홍길동", Position.FIRST_BASE, 1L)
        val entries =
            listOf(
                createEntry(submission, samePlayer, Position.FIRST_BASE, 3, true),
                createEntry(submission, samePlayer, Position.THIRD_BASE, 5, true),
            )

        // when & then
        assertThrows<DuplicatePlayerInLineupException> {
            LineupValidator.validate(entries)
        }
    }

    @Test
    fun `should throw InvalidDhRuleException when DH and pitcher both have batting order`() {
        // given
        val submission = createLineupSubmission()
        val entries = createLineupWithDhAndPitcherBatting(submission)

        // when & then
        assertThrows<InvalidDhRuleException> {
            LineupValidator.validate(entries)
        }
    }

    @Test
    fun `should pass when DH exists and pitcher has no batting order`() {
        // given
        val submission = createLineupSubmission()
        val entries = createValidLineupWithDh(submission)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should pass when no DH and pitcher bats`() {
        // given
        val submission = createLineupSubmission()
        val entries = createLineupWithPitcherBatting(submission)

        // when & then
        assertThatCode {
            LineupValidator.validate(entries)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `should allow non-starter duplicate check to include substitutes`() {
        // given
        val submission = createLineupSubmission()
        val player = createPlayer("김대기", Position.LEFT_FIELD, 10L)
        val entries =
            listOf(
                createEntry(
                    submission,
                    createPlayer("투수", Position.STARTING_PITCHER, 1L),
                    Position.STARTING_PITCHER,
                    1,
                    true
                ),
                createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
                createEntry(submission, player, Position.LEFT_FIELD, 3, true),
                createEntry(submission, player, Position.RIGHT_FIELD, null, false),
            )

        // when & then - same player as starter AND substitute is a duplicate
        assertThrows<DuplicatePlayerInLineupException> {
            LineupValidator.validate(entries)
        }
    }

    // ========== Helper Methods ==========

    private fun createValidLineup(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                1,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 3, true),
            createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 4, true),
            createEntry(submission, createPlayer("3루수", Position.THIRD_BASE, 5L), Position.THIRD_BASE, 5, true),
            createEntry(submission, createPlayer("유격수", Position.SHORTSTOP, 6L), Position.SHORTSTOP, 6, true),
            createEntry(submission, createPlayer("좌익수", Position.LEFT_FIELD, 7L), Position.LEFT_FIELD, 7, true),
            createEntry(submission, createPlayer("중견수", Position.CENTER_FIELD, 8L), Position.CENTER_FIELD, 8, true),
            createEntry(submission, createPlayer("우익수", Position.RIGHT_FIELD, 9L), Position.RIGHT_FIELD, 9, true),
        )

    private fun createLineupWithoutCatcher(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                1,
                true
            ),
            createEntry(submission, createPlayer("1루수", Position.FIRST_BASE, 3L), Position.FIRST_BASE, 2, true),
            createEntry(submission, createPlayer("2루수", Position.SECOND_BASE, 4L), Position.SECOND_BASE, 3, true),
        )

    private fun createLineupWithDhAndPitcherBatting(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                1,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            createEntry(
                submission,
                createPlayer("DH", Position.DESIGNATED_HITTER, 10L),
                Position.DESIGNATED_HITTER,
                3,
                true
            ),
        )

    private fun createValidLineupWithDh(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                null,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
            createEntry(
                submission,
                createPlayer("DH", Position.DESIGNATED_HITTER, 10L),
                Position.DESIGNATED_HITTER,
                1,
                true
            ),
        )

    private fun createLineupWithPitcherBatting(submission: LineupSubmission): List<LineupEntry> =
        listOf(
            createEntry(
                submission,
                createPlayer("투수", Position.STARTING_PITCHER, 1L),
                Position.STARTING_PITCHER,
                9,
                true
            ),
            createEntry(submission, createPlayer("포수", Position.CATCHER, 2L), Position.CATCHER, 2, true),
        )

    private fun createEntry(
        submission: LineupSubmission,
        player: Player,
        position: Position,
        battingOrder: Int?,
        isStarter: Boolean,
    ): LineupEntry =
        LineupEntry(
            submission = submission,
            player = player,
            position = position,
            battingOrder = battingOrder,
            backNumber = null,
            isStarter = isStarter,
        )

    private fun createPlayer(
        name: String,
        position: Position,
        id: Long,
    ): Player =
        Player(
            name = name,
            primaryPosition = position,
            id = id,
        )

    private fun createLineupSubmission(): LineupSubmission {
        val association = Association(name = "서울시야구협회", abbreviation = "서야협", region = "서울")
        val league = League(association = association, name = "서울시 리그", foundedYear = 2020)
        val competition =
            Competition(
                league = league,
                name = "2024 시즌",
                year = 2024,
                startDate = LocalDate.now().minusDays(30),
            )
        val game =
            Game(
                competition = competition,
                scheduledAt = LocalDateTime.now().plusDays(1),
                location = "서울야구장",
            )
        val team = Team(league = league, name = "Tigers", city = "서울", foundedYear = 2020)
        val user = User.createLocalUser(email = "manager@test.com", encodedPassword = "encoded", nickname = "감독")
        return LineupSubmission.create(game, team, user)
    }
}
