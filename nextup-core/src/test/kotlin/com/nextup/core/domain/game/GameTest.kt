package com.nextup.core.domain.game

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
    @DisplayName("경기 종료")
    inner class Finish {
        @Test
        fun `진행 중인 경기를 종료할 수 있다`() {
            // given
            val game = createGame(status = GameStatus.IN_PROGRESS)

            // when
            game.finish()

            // then
            assertThat(game.status).isEqualTo(GameStatus.FINISHED)
            assertThat(game.endedAt).isNotNull()
        }

        @Test
        fun `예정된 경기는 종료할 수 없다`() {
            // given
            val game = createGame(status = GameStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { game.finish() }
                .isInstanceOf(IllegalArgumentException::class.java)
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
}
