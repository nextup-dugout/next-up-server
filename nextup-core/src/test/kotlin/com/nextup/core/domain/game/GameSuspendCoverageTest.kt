package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Game 중단/취소 커버리지 보완 테스트")
class GameSuspendCoverageTest {
    private lateinit var competition: Competition
    private lateinit var league: League

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춨계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            )
    }

    private fun createGame(status: GameStatus = GameStatus.SCHEDULED): Game {
        val homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        val awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)
        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            status = status,
        )
    }

    @Test
    fun `기존 note가 있는 경기를 중단하면 사유가 추가된다`() {
        // given
        val game =
            Game.createForTest(
                competition = competition,
                homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L),
                awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L),
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                status = GameStatus.IN_PROGRESS,
                note = "기존 메모",
            )

        // when
        game.suspend(reason = "우천")

        // then
        assertThat(game.status).isEqualTo(GameStatus.SUSPENDED)
        assertThat(game.note).contains("기존 메모")
        assertThat(game.note).contains("중단 사유: 우천")
    }

    @Test
    fun `연기 상태의 경기를 취소할 수 있다`() {
        // given
        val game = createGame(status = GameStatus.POSTPONED)

        // when
        game.cancel(reason = "일정 불가")

        // then
        assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
        assertThat(game.note).contains("취소 사유: 일정 불가")
    }
}
