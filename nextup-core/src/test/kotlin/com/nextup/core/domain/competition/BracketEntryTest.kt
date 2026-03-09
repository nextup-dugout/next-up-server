package com.nextup.core.domain.competition

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime

class BracketEntryTest {
    @Test
    fun `should record winner when no winner exists`() {
        // given
        val (competition, team1, team2) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = team2,
            )

        // when
        bracketEntry.recordWinner(team1)

        // then
        assertThat(bracketEntry.winner).isEqualTo(team1)
    }

    @Test
    fun `should throw exception when recording winner twice`() {
        // given
        val (competition, team1, team2) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = team2,
            )
        bracketEntry.recordWinner(team1)

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                bracketEntry.recordWinner(team2)
            }
        assertThat(exception.message).isEqualTo("이미 승자가 결정된 경기입니다")
    }

    @Test
    fun `should throw exception when recording non-participant as winner`() {
        // given
        val (competition, team1, team2) = createTestData()
        val association = Association(name = "다른 협회")
        val league = League(association = association, name = "다른 리그", foundedYear = 2020)
        val otherTeam = Team(league = league, name = "다른 팀", city = "부산", foundedYear = 2020)

        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = team2,
            )

        // when & then
        val exception =
            assertThrows<IllegalArgumentException> {
                bracketEntry.recordWinner(otherTeam)
            }
        assertThat(exception.message).isEqualTo("참가팀만 승자로 지정할 수 있습니다")
    }

    @Test
    fun `should return true for isBye when team1 is null`() {
        // given
        val (competition, _, team2) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = null,
                team2 = team2,
            )

        // when & then
        assertThat(bracketEntry.isBye()).isTrue()
    }

    @Test
    fun `should return true for isBye when team2 is null`() {
        // given
        val (competition, team1, _) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = null,
            )

        // when & then
        assertThat(bracketEntry.isBye()).isTrue()
    }

    @Test
    fun `should return false for isBye when both teams exist`() {
        // given
        val (competition, team1, team2) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = team2,
            )

        // when & then
        assertThat(bracketEntry.isBye()).isFalse()
    }

    @Test
    fun `should return true for isCompleted when winner exists`() {
        // given
        val (competition, team1, team2) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = team2,
            )
        bracketEntry.recordWinner(team1)

        // when & then
        assertThat(bracketEntry.isCompleted()).isTrue()
    }

    @Test
    fun `should return true for isCompleted when it is a bye`() {
        // given
        val (competition, team1, _) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = null,
            )

        // when & then
        assertThat(bracketEntry.isCompleted()).isTrue()
    }

    @Test
    fun `should return false for isCompleted when no winner and not a bye`() {
        // given
        val (competition, team1, team2) = createTestData()
        val bracketEntry =
            BracketEntry(
                competition = competition,
                roundNumber = 1,
                matchNumber = 1,
                team1 = team1,
                team2 = team2,
            )

        // when & then
        assertThat(bracketEntry.isCompleted()).isFalse()
    }

    @Nested
    inner class LinkGame {
        @Test
        fun `두 팀이 모두 있는 경우 경기를 연결할 수 있다`() {
            // given
            val (competition, team1, team2) = createTestData()
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team1,
                    team2 = team2,
                )
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team1,
                    awayTeam = team2,
                    scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0),
                )

            // when
            bracketEntry.linkGame(game)

            // then
            assertThat(bracketEntry.game).isEqualTo(game)
        }

        @Test
        fun `이미 경기가 연결된 경우 예외가 발생한다`() {
            // given
            val (competition, team1, team2) = createTestData()
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team1,
                    team2 = team2,
                )
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team1,
                    awayTeam = team2,
                    scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0),
                )
            bracketEntry.linkGame(game)

            // when & then
            val anotherGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team1,
                    awayTeam = team2,
                    scheduledAt = LocalDateTime.of(2025, 3, 16, 14, 0),
                )
            val exception =
                assertThrows<IllegalArgumentException> {
                    bracketEntry.linkGame(anotherGame)
                }
            assertThat(exception.message).isEqualTo("이미 경기가 연결된 대진표 엔트리입니다")
        }

        @Test
        fun `부전승 경기에는 경기를 연결할 수 없다`() {
            // given
            val (competition, team1, team2) = createTestData()
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 1,
                    matchNumber = 1,
                    team1 = team1,
                    team2 = null,
                )
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team1,
                    awayTeam = team2,
                    scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0),
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    bracketEntry.linkGame(game)
                }
            assertThat(exception.message).isEqualTo("부전승 경기에는 경기를 연결할 수 없습니다")
        }

        @Test
        fun `팀이 결정되지 않은 슬롯에는 경기를 연결할 수 없다`() {
            // given
            val (competition, team1, team2) = createTestData()
            val bracketEntry =
                BracketEntry(
                    competition = competition,
                    roundNumber = 2,
                    matchNumber = 1,
                    team1 = null,
                    team2 = null,
                )
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = team1,
                    awayTeam = team2,
                    scheduledAt = LocalDateTime.of(2025, 3, 15, 14, 0),
                )

            // when & then
            val exception =
                assertThrows<IllegalArgumentException> {
                    bracketEntry.linkGame(game)
                }
            assertThat(exception.message).isEqualTo("부전승 경기에는 경기를 연결할 수 없습니다")
        }
    }

    private fun createTestData(): Triple<Competition, Team, Team> {
        val association = Association(name = "테스트 협회")
        val league = League(association = association, name = "테스트 리그", foundedYear = 2020)
        val competition =
            Competition(
                league = league,
                name = "2025 춘계 토너먼트",
                year = 2025,
                season = 1,
                type = CompetitionType.TOURNAMENT,
                startDate = LocalDate.of(2025, 3, 1),
            )
        val team1 = Team(league = league, name = "팀A", city = "서울", foundedYear = 2020)
        val team2 = Team(league = league, name = "팀B", city = "서울", foundedYear = 2020)

        return Triple(competition, team1, team2)
    }
}
