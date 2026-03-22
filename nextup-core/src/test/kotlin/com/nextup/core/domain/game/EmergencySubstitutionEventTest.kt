package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameEvent - 긴급 교체")
class EmergencySubstitutionEventTest {
    private lateinit var game: Game
    private lateinit var ejectedPlayer: GamePlayer
    private lateinit var replacementPlayer: GamePlayer

    @BeforeEach
    fun setUp() {
        game = createGame()
        ejectedPlayer = mockk(relaxed = true)
        replacementPlayer = mockk(relaxed = true)
    }

    @Nested
    @DisplayName("createEmergencySubstitution")
    inner class CreateEmergencySubstitution {
        @Test
        fun `부상으로 인한 긴급 교체 이벤트를 생성한다`() {
            // when
            val event =
                GameEvent.createEmergencySubstitution(
                    game = game,
                    incomingPlayer = replacementPlayer,
                    outgoingPlayer = ejectedPlayer,
                    reason = EjectionReason.INJURY,
                    description = "1회초: 홍길동 퇴장 (부상) → 김철수 긴급 교체 (유격수)",
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.EMERGENCY_SUBSTITUTION)
            assertThat(event.description).isEqualTo("1회초: 홍길동 퇴장 (부상) → 김철수 긴급 교체 (유격수)")
            assertThat(event.inning).isEqualTo(game.currentInning)
            assertThat(event.isTopInning).isEqualTo(game.isTopInning)
            assertThat(event.outCountBefore).isEqualTo(game.gameState.outs)
            assertThat(event.outCountAfter).isEqualTo(game.gameState.outs)
            assertThat(event.batter).isEqualTo(replacementPlayer)
            assertThat(event.pitcher).isEqualTo(ejectedPlayer)
            assertThat(event.plateAppearanceResult).isNull()
            assertThat(event.runsScored).isEqualTo(0)
        }

        @Test
        fun `심판 퇴장으로 인한 긴급 교체 이벤트를 생성한다`() {
            // when
            val event =
                GameEvent.createEmergencySubstitution(
                    game = game,
                    incomingPlayer = replacementPlayer,
                    outgoingPlayer = ejectedPlayer,
                    reason = EjectionReason.EJECTION_BY_UMPIRE,
                    description = "3회말: 박선수 퇴장 (심판 퇴장) → 이대타 긴급 교체 (좌익수)",
                )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.EMERGENCY_SUBSTITUTION)
            assertThat(event.batter).isEqualTo(replacementPlayer)
            assertThat(event.pitcher).isEqualTo(ejectedPlayer)
        }
    }

    private fun createGame(): Game {
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = null,
                region = "서울",
                description = null,
                logoUrl = null,
                websiteUrl = null,
            ).apply {
                val idField = Association::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }

        val league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = null,
                foundedYear = 2020,
                divisionLevel = 1,
                description = null,
                logoUrl = null,
            ).apply {
                val idField = League::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }

        val competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                endDate = LocalDate.of(2025, 6, 30),
                status = CompetitionStatus.IN_PROGRESS,
                description = null,
                maxTeams = null,
            ).apply {
                val idField = Competition::class.java.getDeclaredField("id")
                idField.isAccessible = true
                idField.set(this, 1L)
            }

        val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 10L)
        val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 20L)

        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실구장",
            fieldName = "1구장",
            gameNumber = 1,
            status = GameStatus.IN_PROGRESS,
            currentInning = 1,
            isTopInning = true,
            totalInnings = 9,
            gameState = GameState(),
            id = 1L,
        )
    }
}
