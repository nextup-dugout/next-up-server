package com.nextup.core.domain.game

import com.nextup.common.exception.GameAlreadyLockedException
import com.nextup.common.exception.GameNotLockedByCurrentScorerException
import com.nextup.common.exception.GameNotLockedForRecordingException
import com.nextup.common.exception.ScorerMismatchException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

private fun createCompetitionWithTiebreaker(league: League): Competition =
    Competition(
        league = league,
        name = "타이브레이크 대회",
        year = 2025,
        season = 1,
        type = CompetitionType.LEAGUE,
        startDate = LocalDate.of(2025, 3, 1),
        status = CompetitionStatus.IN_PROGRESS,
        gameRules =
            GameRules(
                tiebreakerEnabled = true,
                maxExtraInnings = 3,
            ),
    )

@DisplayName("Game 엔티티 테스트")
class GameTest {
    private lateinit var competition: Competition
    private lateinit var league: League

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            )
    }

    private fun createGame(status: GameStatus = GameStatus.SCHEDULED): Game {
        val homeTeam = createTeam("홈팀", id = 1L)
        val awayTeam = createTeam("원정팀", city = "부산", id = 2L)
        return Game.createForTest(
            competition = competition,
            homeTeam = homeTeam,
            awayTeam = awayTeam,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실야구장",
            status = status,
        )
    }

    private fun createTeam(
        name: String,
        city: String = "서울",
        id: Long = 0L,
    ): Team =
        Team(
            league = league,
            name = name,
            city = city,
            foundedYear = 2020,
            id = id,
        )

    @Nested
    @DisplayName("경기 시작")
    inner class Start {
        @Test
        fun `예정된 경기를 시작할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when
            game.start()

            // then
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
            assertThat(game.currentInning).isEqualTo(1)
            assertThat(game.isTopInning).isTrue()
            assertThat(game.startedAt).isNotNull()
        }

        @Test
        fun `이미 진행 중인 경기는 시작할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when & then
            assertThatThrownBy { game.start() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("이닝 진행")
    inner class NextHalfInning {
        @Test
        fun `초에서 말로 진행한다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = true
                }

            // when
            game.nextHalfInning()

            // then
            assertThat(game.currentInning).isEqualTo(1)
            assertThat(game.isTopInning).isFalse()
        }

        @Test
        fun `말에서 다음 이닝 초로 진행한다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 1
                    isTopInning = false
                }

            // when
            game.nextHalfInning()

            // then
            assertThat(game.currentInning).isEqualTo(2)
            assertThat(game.isTopInning).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기는 이닝을 진행할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.nextHalfInning() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("타이브레이크 (C3)")
    inner class Tiebreaker {
        private fun createGameWithTiebreaker(
            currentInning: Int,
            isTopInning: Boolean,
        ): Game {
            val association = Association(name = "서울시야구협회", region = "서울")
            val leagueWithTiebreaker =
                League(association = association, name = "1부 리그", foundedYear = 2020)
            val competitionWithTiebreaker =
                createCompetitionWithTiebreaker(leagueWithTiebreaker)
            val homeTeam = createTeam("홈팀", id = 1L)
            val awayTeam = createTeam("원정팀", city = "부산", id = 2L)
            return Game.createForTest(
                competition = competitionWithTiebreaker,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                status = GameStatus.IN_PROGRESS,
                currentInning = currentInning,
                isTopInning = isTopInning,
                totalInnings = 9,
            )
        }

        @Test
        fun `연장전 초(Top) 시작 시 타이브레이크가 적용된다`() {
            // given: 9회말 종료 후 10회초 진입 상황
            val game = createGameWithTiebreaker(currentInning = 9, isTopInning = false)

            // when
            val result =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 10L,
                    tiebreakerSecondRunnerId = 11L,
                )

            // then
            assertThat(result).isEqualTo(TiebreakerResult.TIEBREAKER_APPLIED)
            assertThat(game.currentInning).isEqualTo(10)
            assertThat(game.isTopInning).isTrue()
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(10L)
            assertThat(game.gameState.runnerOnSecondId).isEqualTo(11L)
        }

        @Test
        fun `연장전 말(Bottom) 시작 시에도 타이브레이크가 적용된다 (C3 버그 수정)`() {
            // given: 10회초 종료 후 10회말 진입 상황
            val game = createGameWithTiebreaker(currentInning = 10, isTopInning = true)

            // when
            val result =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 20L,
                    tiebreakerSecondRunnerId = 21L,
                )

            // then: 말(Bottom)에도 타이브레이크가 적용되어야 함
            assertThat(result).isEqualTo(TiebreakerResult.TIEBREAKER_APPLIED)
            assertThat(game.currentInning).isEqualTo(10)
            assertThat(game.isTopInning).isFalse()
            assertThat(game.gameState.runnerOnFirstId).isEqualTo(20L)
            assertThat(game.gameState.runnerOnSecondId).isEqualTo(21L)
        }

        @Test
        fun `타이브레이크 비활성화 시 연장전 말에서 타이브레이크 미적용`() {
            // given: 타이브레이크 비활성화 대회
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 10
                    isTopInning = true
                    totalInnings = 9
                }

            // when
            val result =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 10L,
                    tiebreakerSecondRunnerId = 11L,
                )

            // then
            assertThat(result).isEqualTo(TiebreakerResult.NORMAL)
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnSecondId).isNull()
        }

        @Test
        fun `정규 이닝에서는 타이브레이크가 적용되지 않는다`() {
            // given: 5회초 종료 후 5회말 진입
            val game = createGameWithTiebreaker(currentInning = 5, isTopInning = true)

            // when
            val result =
                game.nextHalfInning(
                    tiebreakerFirstRunnerId = 10L,
                    tiebreakerSecondRunnerId = 11L,
                )

            // then
            assertThat(result).isEqualTo(TiebreakerResult.NORMAL)
            assertThat(game.gameState.runnerOnFirstId).isNull()
            assertThat(game.gameState.runnerOnSecondId).isNull()
        }
    }

    @Nested
    @DisplayName("경기 종료")
    inner class Finish {
        @Test
        fun `진행 중인 경기를 종료할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            val gameTeams = game.gameTeams

            // when
            game.finish(gameTeams)

            // then
            assertThat(game.status).isEqualTo(GameStatus.FINISHED)
            assertThat(game.endedAt).isNotNull()
        }

        @Test
        fun `예정된 경기는 종료할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            val gameTeams = game.gameTeams

            // when & then
            assertThatThrownBy { game.finish(gameTeams) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `정상 종료 시 홈팀 점수가 높으면 홈팀 WIN, 원정팀 LOSS`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            val gameTeams = game.gameTeams
            val homeTeam = gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayTeam = gameTeams.first { it.homeAway == HomeAway.AWAY }
            homeTeam.addScore(5)
            awayTeam.addScore(3)

            // when
            game.finish(gameTeams)

            // then
            assertThat(homeTeam.result).isEqualTo(GameResult.WIN)
            assertThat(awayTeam.result).isEqualTo(GameResult.LOSS)
        }

        @Test
        fun `정상 종료 시 원정팀 점수가 높으면 원정팀 WIN, 홈팀 LOSS`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            val gameTeams = game.gameTeams
            val homeTeam = gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayTeam = gameTeams.first { it.homeAway == HomeAway.AWAY }
            homeTeam.addScore(2)
            awayTeam.addScore(7)

            // when
            game.finish(gameTeams)

            // then
            assertThat(homeTeam.result).isEqualTo(GameResult.LOSS)
            assertThat(awayTeam.result).isEqualTo(GameResult.WIN)
        }

        @Test
        fun `정상 종료 시 동점이면 양 팀 모두 DRAW`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            val gameTeams = game.gameTeams
            val homeTeam = gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayTeam = gameTeams.first { it.homeAway == HomeAway.AWAY }
            homeTeam.addScore(4)
            awayTeam.addScore(4)

            // when
            game.finish(gameTeams)

            // then
            assertThat(homeTeam.result).isEqualTo(GameResult.DRAW)
            assertThat(awayTeam.result).isEqualTo(GameResult.DRAW)
        }
    }

    @Nested
    @DisplayName("콜드게임")
    inner class CallGame {
        @Test
        fun `최소 이닝 이상 진행된 경기를 콜드게임 처리할 수 있다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }

            // when
            game.callGame(reason = "우천")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
            assertThat(game.endedAt).isNotNull()
            assertThat(game.note).contains("우천")
        }

        @Test
        fun `최소 이닝 미만에서는 콜드게임 선언이 불가하다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 4
                    isTopInning = true
                }

            // when & then
            assertThatThrownBy { game.callGame() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 5이닝")
        }

        @Test
        fun `정확히 최소 이닝(5회초)에서 콜드게임 선언이 가능하다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }

            // when
            game.callGame()

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
        }

        @Test
        fun `홈팀 리드 시 4말(4점5이닝)에서 콜드게임 선언이 가능하다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 4
                    isTopInning = false
                }

            // when
            game.callGame(isHomeTeamLeading = true, reason = "점수차")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
            assertThat(game.note).contains("점수차")
        }

        @Test
        fun `홈팀 리드가 없으면 4말에서 콜드게임 선언이 불가하다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 4
                    isTopInning = false
                }

            // when & then
            assertThatThrownBy { game.callGame(isHomeTeamLeading = false) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 5이닝")
        }

        @Test
        fun `6회 이상 진행된 경기는 콜드게임 선언이 가능하다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 7
                    isTopInning = false
                }

            // when
            game.callGame()

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
        }

        @Test
        fun `사유 없이도 콜드게임 처리할 수 있다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }

            // when
            game.callGame()

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
            assertThat(game.note).isNull()
        }

        @Test
        fun `커스텀 최소 이닝을 지정할 수 있다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 3
                    isTopInning = true
                }

            // when
            game.callGame(minimumInning = 3)

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
        }

        @Test
        fun `진행 중이 아닌 경기는 콜드게임 처리할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.callGame() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }

        @Test
        fun `최소 이닝이 1 미만이면 예외가 발생한다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }

            // when & then
            assertThatThrownBy { game.callGame(minimumInning = 0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 이닝은 1 이상이어야 합니다")
        }

        @Test
        fun `3회에서 기본 최소이닝 5이닝 기준 콜드게임 선언이 불가하다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 3
                    isTopInning = true
                }

            // when & then
            assertThatThrownBy { game.callGame() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 5이닝")
        }

        @Test
        fun `기존 note가 있는 경기에 콜드게임 사유가 추가된다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                    note = "기존 메모"
                }

            // when
            game.callGame(reason = "우천")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CALLED)
            assertThat(game.note).contains("기존 메모")
            assertThat(game.note).contains("우천")
        }

        @Test
        fun `4회초에서 홈팀 리드여도 콜드게임 선언이 불가하다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 4
                    isTopInning = true
                }

            // when & then
            assertThatThrownBy { game.callGame(isHomeTeamLeading = true) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("최소 5이닝")
        }

        @Test
        fun `콜드게임 종료 시 홈팀 점수가 높으면 홈팀 WIN, 원정팀 LOSS`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }
            val gameTeams = game.gameTeams
            val homeTeam = gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayTeam = gameTeams.first { it.homeAway == HomeAway.AWAY }
            homeTeam.addScore(10)
            awayTeam.addScore(0)

            // when
            game.callGame(reason = "점수차", gameTeams = gameTeams)

            // then
            assertThat(homeTeam.result).isEqualTo(GameResult.WIN)
            assertThat(awayTeam.result).isEqualTo(GameResult.LOSS)
        }

        @Test
        fun `콜드게임 종료 시 원정팀 점수가 높으면 원정팀 WIN, 홈팀 LOSS`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 6
                    isTopInning = false
                }
            val gameTeams = game.gameTeams
            val homeTeam = gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayTeam = gameTeams.first { it.homeAway == HomeAway.AWAY }
            homeTeam.addScore(1)
            awayTeam.addScore(11)

            // when
            game.callGame(gameTeams = gameTeams)

            // then
            assertThat(homeTeam.result).isEqualTo(GameResult.LOSS)
            assertThat(awayTeam.result).isEqualTo(GameResult.WIN)
        }

        @Test
        fun `콜드게임 종료 시 동점이면 양 팀 모두 DRAW`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 7
                    isTopInning = false
                }
            val gameTeams = game.gameTeams
            val homeTeam = gameTeams.first { it.homeAway == HomeAway.HOME }
            val awayTeam = gameTeams.first { it.homeAway == HomeAway.AWAY }
            homeTeam.addScore(3)
            awayTeam.addScore(3)

            // when
            game.callGame(gameTeams = gameTeams)

            // then
            assertThat(homeTeam.result).isEqualTo(GameResult.DRAW)
            assertThat(awayTeam.result).isEqualTo(GameResult.DRAW)
        }

        @Test
        fun `gameTeams 없이 콜드게임 처리하면 result는 UNDECIDED 유지`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }
            val gameTeams = game.gameTeams

            // when
            game.callGame()

            // then
            gameTeams.forEach { assertThat(it.result).isEqualTo(GameResult.UNDECIDED) }
        }
    }

    @Nested
    @DisplayName("Mercy Rule 조건 판별")
    inner class CheckMercyRuleCondition {
        @Test
        fun `최소 이닝 이상이고 점수차가 기준 이상이면 Mercy Rule 조건이 충족된다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }

            // when
            val result = game.checkMercyRuleCondition(homeScore = 12, awayScore = 2)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `점수차가 기준 미만이면 Mercy Rule 조건이 충족되지 않는다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }

            // when
            val result = game.checkMercyRuleCondition(homeScore = 7, awayScore = 2)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `최소 이닝 미만이면 Mercy Rule 조건이 충족되지 않는다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 4
                    isTopInning = true
                }

            // when
            val result = game.checkMercyRuleCondition(homeScore = 15, awayScore = 0)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `경기가 진행 중이 아니면 Mercy Rule 조건이 충족되지 않는다`() {
            // given
            val game =
                createGame(status = GameStatus.FINISHED).apply {
                    currentInning = 7
                    isTopInning = true
                }

            // when
            val result = game.checkMercyRuleCondition(homeScore = 15, awayScore = 0)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `원정팀이 리드해도 점수차가 기준 이상이면 Mercy Rule 조건이 충족된다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 6
                    isTopInning = false
                }

            // when
            val result = game.checkMercyRuleCondition(homeScore = 0, awayScore = 10)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `점수차가 정확히 기준값이면 Mercy Rule 조건이 충족된다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = true
                }

            // when
            val result = game.checkMercyRuleCondition(homeScore = 10, awayScore = 0)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `커스텀 점수차와 최소 이닝을 지정할 수 있다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 3
                    isTopInning = false
                }

            // when
            val result =
                game.checkMercyRuleCondition(
                    homeScore = 15,
                    awayScore = 0,
                    mercyRunDifference = 15,
                    mercyMinimumInning = 3,
                )

            // then
            assertThat(result).isTrue()
        }
    }

    @Nested
    @DisplayName("경기 취소")
    inner class Cancel {
        @Test
        fun `예정된 경기를 취소할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when
            game.cancel("우천 예보")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
            assertThat(game.note).contains("우천 예보")
        }

        @Test
        fun `진행 중인 경기는 취소할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when & then
            assertThatThrownBy { game.cancel() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("경기 연기")
    inner class Postpone {
        @Test
        fun `예정된 경기를 연기할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            val newSchedule = LocalDateTime.of(2025, 4, 20, 14, 0)

            // when
            game.postpone(newSchedule, "우천")

            // then
            assertThat(game.status).isEqualTo(GameStatus.POSTPONED)
            assertThat(game.scheduledAt).isEqualTo(newSchedule)
            assertThat(game.note).contains("우천")
        }
    }

    @Nested
    @DisplayName("몰수패")
    inner class Forfeit {
        private val homeTeamId = 1L
        private val awayTeamId = 2L

        @Test
        fun `예정된 경기를 몰수 처리하면 7대0 점수가 반영된다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            val gameTeams = game.gameTeams

            // when
            game.forfeit(
                winnerTeamId = homeTeamId,
                reason = "상대팀 불참",
                gameTeams = gameTeams,
            )

            // then
            assertThat(game.status).isEqualTo(GameStatus.FORFEITED)
            assertThat(game.forfeitReason).isEqualTo("상대팀 불참")
            assertThat(game.note).contains("상대팀 불참")
            assertThat(game.endedAt).isNotNull()

            val winnerGameTeam = gameTeams.first { it.team.id == homeTeamId }
            val loserGameTeam = gameTeams.first { it.team.id == awayTeamId }

            assertThat(winnerGameTeam.totalScore).isEqualTo(7)
            assertThat(winnerGameTeam.result).isEqualTo(GameResult.WIN)
            assertThat(loserGameTeam.totalScore).isEqualTo(0)
            assertThat(loserGameTeam.result).isEqualTo(GameResult.LOSS)
        }

        @Test
        fun `진행 중인 경기도 몰수 처리할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            val gameTeams = game.gameTeams

            // when
            game.forfeit(
                winnerTeamId = awayTeamId,
                reason = "홈팀 규정 위반",
                gameTeams = gameTeams,
            )

            // then
            assertThat(game.status).isEqualTo(GameStatus.FORFEITED)
            val winnerGameTeam = gameTeams.first { it.team.id == awayTeamId }
            val loserGameTeam = gameTeams.first { it.team.id == homeTeamId }

            assertThat(winnerGameTeam.totalScore).isEqualTo(7)
            assertThat(winnerGameTeam.result).isEqualTo(GameResult.WIN)
            assertThat(loserGameTeam.totalScore).isEqualTo(0)
            assertThat(loserGameTeam.result).isEqualTo(GameResult.LOSS)
        }

        @Test
        fun `이미 종료된 경기는 몰수 처리할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.FINISHED)
            val gameTeams = game.gameTeams

            // when & then
            assertThatThrownBy {
                game.forfeit(
                    winnerTeamId = homeTeamId,
                    reason = "사유",
                    gameTeams = gameTeams,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `취소된 경기는 몰수 처리할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.CANCELLED)
            val gameTeams = game.gameTeams

            // when & then
            assertThatThrownBy {
                game.forfeit(
                    winnerTeamId = homeTeamId,
                    reason = "사유",
                    gameTeams = gameTeams,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }

        @Test
        fun `참여하지 않는 팀을 승리팀으로 지정하면 예외가 발생한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            val gameTeams = game.gameTeams

            // when & then
            assertThatThrownBy {
                game.forfeit(
                    winnerTeamId = 9999L,
                    reason = "사유",
                    gameTeams = gameTeams,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("참여하는 팀이 아닙니다")
        }

        @Test
        fun `몰수 사유가 forfeitReason 필드에 기록된다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            val gameTeams = game.gameTeams

            // when
            game.forfeit(
                winnerTeamId = homeTeamId,
                reason = "인원 미달로 경기 불가",
                gameTeams = gameTeams,
            )

            // then
            assertThat(game.forfeitReason).isEqualTo("인원 미달로 경기 불가")
        }
    }

    @Nested
    @DisplayName("일정 변경")
    inner class Reschedule {
        @Test
        fun `연기된 경기를 재스케줄하면 예정 상태로 변경된다`() {
            // given
            val game = createGame(status = GameStatus.POSTPONED)
            val newSchedule = LocalDateTime.of(2025, 5, 1, 14, 0)

            // when
            game.reschedule(newSchedule)

            // then
            assertThat(game.status).isEqualTo(GameStatus.SCHEDULED)
            assertThat(game.scheduledAt).isEqualTo(newSchedule)
        }
    }

    @Nested
    @DisplayName("이닝 표시")
    inner class InningDisplay {
        @Test
        fun `경기 전에는 '경기 전'을 반환한다`() {
            // given
            val game = createGame().apply { currentInning = 0 }

            // then
            assertThat(game.currentInningDisplay).isEqualTo("경기 전")
        }

        @Test
        fun `1회초는 '1회초'를 반환한다`() {
            // given
            val game =
                createGame().apply {
                    currentInning = 1
                    isTopInning = true
                }

            // then
            assertThat(game.currentInningDisplay).isEqualTo("1회초")
        }

        @Test
        fun `5회말은 '5회말'을 반환한다`() {
            // given
            val game =
                createGame().apply {
                    currentInning = 5
                    isTopInning = false
                }

            // then
            assertThat(game.currentInningDisplay).isEqualTo("5회말")
        }
    }

    @Nested
    @DisplayName("연장전 확인")
    inner class ExtraInning {
        @Test
        fun `9회까지는 연장전이 아니다`() {
            // given
            val game =
                createGame().apply {
                    currentInning = 9
                    totalInnings = 9
                }

            // then
            assertThat(game.isExtraInning).isFalse()
        }

        @Test
        fun `10회 이상은 연장전이다`() {
            // given
            val game =
                createGame().apply {
                    currentInning = 10
                    totalInnings = 9
                }

            // then
            assertThat(game.isExtraInning).isTrue()
        }
    }

    @Nested
    @DisplayName("아웃 기록")
    inner class RecordOut {
        @Test
        fun `진행 중인 경기에서 아웃을 기록할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            val isInningOver = game.recordOut()

            // then
            assertThat(isInningOver).isFalse()
        }

        @Test
        fun `3아웃이 되면 true를 반환한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.recordOut()
            game.recordOut()

            // when
            val isInningOver = game.recordOut()

            // then
            assertThat(isInningOver).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기에서는 아웃을 기록할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.recordOut() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("타자 진행")
    inner class AdvanceBatter {
        @Test
        fun `진행 중인 경기에서 타순을 진행할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.advanceBatter()

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 타순을 진행할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.FINISHED)

            // when & then
            assertThatThrownBy { game.advanceBatter() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("주자 설정")
    inner class SetRunner {
        @Test
        fun `진행 중인 경기에서 주자를 설정할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.setRunner(Base.FIRST, 123L)

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 주자를 설정할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.setRunner(Base.FIRST, 123L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("베이스 클리어")
    inner class ClearBases {
        @Test
        fun `진행 중인 경기에서 베이스를 클리어할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.setRunner(Base.FIRST, 123L)
            game.setRunner(Base.SECOND, 456L)

            // when
            game.clearBases()

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 베이스를 클리어할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.FINISHED)

            // when & then
            assertThatThrownBy { game.clearBases() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("볼카운트 리셋")
    inner class ResetCount {
        @Test
        fun `진행 중인 경기에서 볼카운트를 리셋할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.resetCount()

            // then (예외가 발생하지 않으면 성공)
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중이 아닌 경기에서는 볼카운트를 리셋할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.resetCount() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("볼 추가")
    inner class AddBall {
        @Test
        fun `진행 중인 경기에서 볼을 추가할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            val isWalk = game.addBall()

            // then
            assertThat(isWalk).isFalse()
        }

        @Test
        fun `4볼이 되면 true를 반환한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.addBall()
            game.addBall()
            game.addBall()

            // when
            val isWalk = game.addBall()

            // then
            assertThat(isWalk).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기에서는 볼을 추가할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.CANCELLED)

            // when & then
            assertThatThrownBy { game.addBall() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("스트라이크 추가")
    inner class AddStrike {
        @Test
        fun `진행 중인 경기에서 스트라이크를 추가할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            val isStrikeout = game.addStrike()

            // then
            assertThat(isStrikeout).isFalse()
        }

        @Test
        fun `3스트라이크가 되면 true를 반환한다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.addStrike()
            game.addStrike()

            // when
            val isStrikeout = game.addStrike()

            // then
            assertThat(isStrikeout).isTrue()
        }

        @Test
        fun `진행 중이 아닌 경기에서는 스트라이크를 추가할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.POSTPONED)

            // when & then
            assertThatThrownBy { game.addStrike() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }
    }

    @Nested
    @DisplayName("Game.create() 팩토리 메서드")
    inner class CreateFactory {
        private lateinit var homeTeam: Team
        private lateinit var awayTeam: Team

        @BeforeEach
        fun setUpTeams() {
            homeTeam = createTeam("홈팀", id = 1L)
            awayTeam = createTeam("원정팀", city = "부산", id = 2L)
        }

        @Test
        fun `create() 호출 시 GameTeam 2개가 자동 생성된다`() {
            // when
            val game =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                )

            // then
            assertThat(game.gameTeams).hasSize(2)
        }

        @Test
        fun `create() 시 HOME과 AWAY GameTeam이 각 1개씩 생성된다`() {
            // when
            val game =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                )

            // then
            val homeGameTeam = game.gameTeams.find { it.homeAway == HomeAway.HOME }
            val awayGameTeam = game.gameTeams.find { it.homeAway == HomeAway.AWAY }

            assertThat(homeGameTeam).isNotNull
            assertThat(awayGameTeam).isNotNull
            assertThat(homeGameTeam!!.team.id).isEqualTo(homeTeam.id)
            assertThat(awayGameTeam!!.team.id).isEqualTo(awayTeam.id)
        }

        @Test
        fun `create() 시 totalInnings가 competition의 defaultInnings로 설정된다`() {
            // given
            val customRules = GameRules(defaultInnings = 7)
            val comp =
                Competition(
                    league = league,
                    name = "7이닝 대회",
                    year = 2025,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2025, 3, 1),
                    gameRules = customRules,
                )

            // when
            val game =
                Game.create(
                    competition = comp,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                )

            // then
            assertThat(game.totalInnings).isEqualTo(7)
        }

        @Test
        fun `홈팀과 원정팀이 같으면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = homeTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("홈팀과 원정팀은 같을 수 없습니다")
        }

        @Test
        fun `create() 시 기본 상태는 SCHEDULED이다`() {
            // when
            val game =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                )

            // then
            assertThat(game.status).isEqualTo(GameStatus.SCHEDULED)
        }

        @Test
        fun `더블헤더 경기를 생성할 수 있다`() {
            // when
            val game1 =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 10, 0),
                    gameNumber = 1,
                    isDoubleheader = true,
                )
            val game2 =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                    gameNumber = 2,
                    isDoubleheader = true,
                )

            // then
            assertThat(game1.isDoubleheader).isTrue()
            assertThat(game1.gameNumber).isEqualTo(1)
            assertThat(game1.doubleheaderDisplay).isEqualTo("제1경기")
            assertThat(game2.isDoubleheader).isTrue()
            assertThat(game2.gameNumber).isEqualTo(2)
            assertThat(game2.doubleheaderDisplay).isEqualTo("제2경기")
        }

        @Test
        fun `더블헤더 경기에 gameNumber가 없으면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                    isDoubleheader = true,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("gameNumber가 1 또는 2여야 합니다")
        }

        @Test
        fun `더블헤더가 아닌 경기의 doubleheaderDisplay는 null이다`() {
            // when
            val game =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                )

            // then
            assertThat(game.isDoubleheader).isFalse()
            assertThat(game.doubleheaderDisplay).isNull()
        }
    }

    @Nested
    @DisplayName("경기 중단/재개")
    inner class SuspendAndResume {
        @Test
        fun `진행 중인 경기를 중단할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.suspend(reason = "우천")

            // then
            assertThat(game.status).isEqualTo(GameStatus.SUSPENDED)
            assertThat(game.note).contains("중단 사유: 우천")
        }

        @Test
        fun `중단 시 사유 없이도 가능하다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.suspend()

            // then
            assertThat(game.status).isEqualTo(GameStatus.SUSPENDED)
        }

        @Test
        fun `진행 중이 아닌 경기는 중단할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.suspend() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("진행 중인 경기만 중단할 수 있습니다")
        }

        @Test
        fun `중단된 경기를 재개할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SUSPENDED)

            // when
            game.resume()

            // then
            assertThat(game.status).isEqualTo(GameStatus.IN_PROGRESS)
        }

        @Test
        fun `중단되지 않은 경기는 재개할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when & then
            assertThatThrownBy { game.resume() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("중단된 경기만 재개할 수 있습니다")
        }

        @Test
        fun `중단된 경기를 취소할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SUSPENDED)

            // when
            game.cancel(reason = "재개 불가")

            // then
            assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
            assertThat(game.note).contains("취소 사유: 재개 불가")
        }

        @Test
        fun `중단 시 게임 상태가 보존된다`() {
            // given
            val game =
                createGame(status = GameStatus.IN_PROGRESS).apply {
                    currentInning = 5
                    isTopInning = false
                    gameState.restoreOuts(2)
                }

            // when
            game.suspend(reason = "우천")

            // then
            assertThat(game.currentInning).isEqualTo(5)
            assertThat(game.isTopInning).isFalse()
            assertThat(game.gameState.outs).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("시간 제한 상태 확인 (M-5)")
    inner class CheckTimeLimitStatusTest {

        private fun createGameWithTimeLimit(
            status: GameStatus,
            timeLimitMinutes: Int?,
            startedMinutesAgo: Long? = null,
        ): Game {
            val timeLimitCompetition =
                Competition(
                    league = league,
                    name = "시간 제한 대회",
                    year = 2025,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2025, 3, 1),
                    status = CompetitionStatus.IN_PROGRESS,
                ).also {
                    it.gameRules = GameRules(timeLimitMinutes = timeLimitMinutes)
                }
            val homeTeam = createTeam("홈팀", id = 10L)
            val awayTeam = createTeam("원정팀", city = "부산", id = 20L)
            return Game.createForTest(
                competition = timeLimitCompetition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                status = status,
                startedAt = startedMinutesAgo?.let { LocalDateTime.now().minusMinutes(it) },
            )
        }

        @Test
        fun `timeLimitMinutes가 설정되지 않으면 null을 반환한다`() {
            // given: timeLimitMinutes = null
            val game =
                createGameWithTimeLimit(
                    status = GameStatus.IN_PROGRESS,
                    timeLimitMinutes = null,
                    startedMinutesAgo = 30,
                )

            // when
            val result = game.checkTimeLimitStatus(now = LocalDateTime.now())

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `경기가 진행 중이 아니면 null을 반환한다`() {
            // given
            val game =
                createGameWithTimeLimit(
                    status = GameStatus.SCHEDULED,
                    timeLimitMinutes = 120,
                )

            // when
            val result = game.checkTimeLimitStatus(now = LocalDateTime.now())

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `startedAt이 null이면 null을 반환한다`() {
            // given: 진행 중이지만 startedAt 없음
            val game =
                createGameWithTimeLimit(
                    status = GameStatus.IN_PROGRESS,
                    timeLimitMinutes = 120,
                    startedMinutesAgo = null,
                )

            // when
            val result = game.checkTimeLimitStatus(now = LocalDateTime.now())

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `경과 시간이 제한 시간 미만이고 경고 범위 밖이면 null을 반환한다`() {
            // given: 120분 제한, 경고 10분 전, 현재 60분 경과
            val game =
                createGameWithTimeLimit(
                    status = GameStatus.IN_PROGRESS,
                    timeLimitMinutes = 120,
                    startedMinutesAgo = 60,
                )

            // when
            val result =
                game.checkTimeLimitStatus(
                    now = LocalDateTime.now(),
                    warningThresholdMinutes = 10,
                )

            // then
            assertThat(result).isNull()
        }

        @Test
        fun `경과 시간이 경고 범위에 들어오면 APPROACHING_LIMIT을 반환한다`() {
            // given: 120분 제한, 경고 10분 전, 현재 112분 경과 (8분 남음)
            val game =
                createGameWithTimeLimit(
                    status = GameStatus.IN_PROGRESS,
                    timeLimitMinutes = 120,
                    startedMinutesAgo = 112,
                )

            // when
            val result =
                game.checkTimeLimitStatus(
                    now = LocalDateTime.now(),
                    warningThresholdMinutes = 10,
                )

            // then
            assertThat(result).isEqualTo(TimeLimitStatus.APPROACHING_LIMIT)
        }

        @Test
        fun `경과 시간이 제한 시간에 도달하면 LIMIT_REACHED를 반환한다`() {
            // given: 120분 제한, 정확히 120분 경과
            val game =
                createGameWithTimeLimit(
                    status = GameStatus.IN_PROGRESS,
                    timeLimitMinutes = 120,
                    startedMinutesAgo = 120,
                )

            // when
            val result = game.checkTimeLimitStatus(now = LocalDateTime.now())

            // then
            assertThat(result).isEqualTo(TimeLimitStatus.LIMIT_REACHED)
        }

        @Test
        fun `경과 시간이 제한 시간을 초과하면 LIMIT_REACHED를 반환한다`() {
            // given: 120분 제한, 130분 경과
            val game =
                createGameWithTimeLimit(
                    status = GameStatus.IN_PROGRESS,
                    timeLimitMinutes = 120,
                    startedMinutesAgo = 130,
                )

            // when
            val result = game.checkTimeLimitStatus(now = LocalDateTime.now())

            // then
            assertThat(result).isEqualTo(TimeLimitStatus.LIMIT_REACHED)
        }
    }

    @Nested
    @DisplayName("기록원 독점 잠금")
    inner class ScorerLock {
        @Test
        fun `기록원이 경기를 잠금할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when
            game.lockForScorer(100L)

            // then
            assertThat(game.scorerId).isEqualTo(100L)
            assertThat(game.isLocked).isTrue()
            assertThat(game.isLockedByScorer(100L)).isTrue()
        }

        @Test
        fun `동일 기록원이 중복 잠금 시도하면 멱등하게 처리된다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when
            game.lockForScorer(100L)

            // then
            assertThat(game.scorerId).isEqualTo(100L)
        }

        @Test
        fun `다른 기록원이 잠금된 경기를 잠금 시도하면 예외가 발생한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when & then
            assertThatThrownBy { game.lockForScorer(200L) }
                .isInstanceOf(GameAlreadyLockedException::class.java)
                .hasMessageContaining("already locked by scorer 100")
        }

        @Test
        fun `잠금한 기록원이 잠금을 해제할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when
            game.unlockScorer(100L)

            // then
            assertThat(game.scorerId).isNull()
            assertThat(game.isLocked).isFalse()
        }

        @Test
        fun `잠금하지 않은 기록원이 해제 시도하면 예외가 발생한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when & then
            assertThatThrownBy { game.unlockScorer(200L) }
                .isInstanceOf(GameNotLockedByCurrentScorerException::class.java)
                .hasMessageContaining("not locked by scorer 200")
        }

        @Test
        fun `잠금이 없는 상태에서 해제 시도하면 예외가 발생한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.unlockScorer(100L) }
                .isInstanceOf(GameNotLockedByCurrentScorerException::class.java)
        }

        @Test
        fun `강제 잠금 해제는 어떤 상태에서든 가능하다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when
            game.forceUnlockScorer()

            // then
            assertThat(game.scorerId).isNull()
            assertThat(game.isLocked).isFalse()
        }

        @Test
        fun `잠금되지 않은 경기에 대해 isLockedByScorer는 false를 반환한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // then
            assertThat(game.isLockedByScorer(100L)).isFalse()
        }

        @Test
        fun `createForTest에서 scorerId를 지정할 수 있다`() {
            // given
            val homeTeam = createTeam("홈팀", id = 1L)
            val awayTeam = createTeam("원정팀", city = "부산", id = 2L)

            // when
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scorerId = 100L,
                )

            // then
            assertThat(game.scorerId).isEqualTo(100L)
            assertThat(game.isLocked).isTrue()
        }

        @Test
        fun `잠금한 기록원이 validateScorer를 호출하면 예외가 발생하지 않는다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when & then - 예외 없이 통과
            game.validateScorer(100L)
        }

        @Test
        fun `잠금되지 않은 경기에서 validateScorer를 호출하면 GameNotLockedForRecordingException이 발생한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.validateScorer(100L) }
                .isInstanceOf(GameNotLockedForRecordingException::class.java)
                .hasMessageContaining("not locked by any scorer")
        }

        @Test
        fun `다른 기록원이 validateScorer를 호출하면 ScorerMismatchException이 발생한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when & then
            assertThatThrownBy { game.validateScorer(200L) }
                .isInstanceOf(ScorerMismatchException::class.java)
                .hasMessageContaining("locked by scorer 100")
                .hasMessageContaining("scorer 200 attempted")
        }

        @Test
        fun `경기 종료 시 기록원 잠금이 자동 해제된다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.lockForScorer(100L)

            // when
            game.finish(game.gameTeams)

            // then
            assertThat(game.scorerId).isNull()
            assertThat(game.isLocked).isFalse()
        }

        @Test
        fun `lockForScorer 시 lockedAt이 설정된다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when
            game.lockForScorer(100L)

            // then
            assertThat(game.lockedAt).isNotNull()
        }

        @Test
        fun `unlockScorer 시 lockedAt이 null로 초기화된다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when
            game.cancel("테스트 취소")

            // then
            assertThat(game.scorerId).isNull()
            assertThat(game.isLocked).isFalse()
        }

        @Test
        fun `unlockScorer 시 lockedAt이 null로 초기화된다 - cancel`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when
            game.unlockScorer(100L)

            // then
            assertThat(game.lockedAt).isNull()
        }

        @Test
        fun `forceUnlockScorer 시 lockedAt이 null로 초기화된다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)
            game.lockForScorer(100L)

            // when
            game.forceUnlockScorer()

            // then
            assertThat(game.lockedAt).isNull()
        }

        @Test
        fun `콜드게임 시 기록원 잠금이 자동 해제된다`() {
            // given
            val homeTeam = createTeam("홈팀", id = 1L)
            val awayTeam = createTeam("원정팀", city = "부산", id = 2L)
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
                    location = "잠실야구장",
                    status = GameStatus.IN_PROGRESS,
                    currentInning = 5,
                    isTopInning = true,
                )
            game.lockForScorer(100L)

            // when
            game.callGame(
                minimumInning = 5,
                reason = "우천",
                gameTeams = game.gameTeams,
            )

            // then
            assertThat(game.scorerId).isNull()
            assertThat(game.isLocked).isFalse()
        }

        @Test
        fun `몰수 처리 시 기록원 잠금이 자동 해제된다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)
            game.lockForScorer(100L)

            // when
            game.forfeit(
                winnerTeamId = 1L,
                reason = "출석 인원 부족",
                gameTeams = game.gameTeams,
            )

            // then
            assertThat(game.scorerId).isNull()
            assertThat(game.isLocked).isFalse()
        }

        @Test
        fun `30분 이상 경과한 잠금은 만료된 것으로 판단한다`() {
            // given
            val homeTeam = createTeam("홈팀", id = 1L)
            val awayTeam = createTeam("원정팀", city = "부산", id = 2L)
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scorerId = 100L,
                    lockedAt = LocalDateTime.now().minusMinutes(31),
                )

            // then
            assertThat(game.isLockExpired()).isTrue()
        }

        @Test
        fun `30분 미만인 잠금은 만료되지 않은 것으로 판단한다`() {
            // given
            val homeTeam = createTeam("홈팀", id = 1L)
            val awayTeam = createTeam("원정팀", city = "부산", id = 2L)
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scorerId = 100L,
                    lockedAt = LocalDateTime.now().minusMinutes(10),
                )

            // then
            assertThat(game.isLockExpired()).isFalse()
        }

        @Test
        fun `lockedAt이 null이면 만료되지 않은 것으로 판단한다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // then
            assertThat(game.isLockExpired()).isFalse()
        }

        @Test
        fun `expireLock은 scorerId와 lockedAt을 모두 초기화한다`() {
            // given
            val homeTeam = createTeam("홈팀", id = 1L)
            val awayTeam = createTeam("원정팀", city = "부산", id = 2L)
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scorerId = 100L,
                    lockedAt = LocalDateTime.now().minusMinutes(31),
                )

            // when
            game.expireLock()

            // then
            assertThat(game.scorerId).isNull()
            assertThat(game.lockedAt).isNull()
            assertThat(game.isLocked).isFalse()
        }
    }
}
