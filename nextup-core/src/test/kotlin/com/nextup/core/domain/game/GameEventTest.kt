package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("GameEvent 엔티티 테스트")
class GameEventTest {

    private lateinit var game: Game
    private lateinit var homeTeam: GameTeam
    private lateinit var awayTeam: GameTeam
    private lateinit var batter: GamePlayer
    private lateinit var pitcher: GamePlayer
    private lateinit var runner: GamePlayer

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        val competition = Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            status = CompetitionStatus.IN_PROGRESS
        )

        game = Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실야구장",
            status = GameStatus.IN_PROGRESS
        )

        val team1 = Team(league = league, name = "레드삭스", city = "서울", foundedYear = 2020)
        val team2 = Team(league = league, name = "양키스", city = "부산", foundedYear = 2020)

        homeTeam = GameTeam(game = game, team = team1, homeAway = HomeAway.HOME)
        awayTeam = GameTeam(game = game, team = team2, homeAway = HomeAway.AWAY)

        val batterPlayer = Player(
            name = "김타자",
            birthDate = LocalDate.of(1995, 5, 15),
            primaryPosition = Position.SHORTSTOP,
            battingHand = BattingHand.RIGHT,
            throwingHand = ThrowingHand.RIGHT
        )

        val pitcherPlayer = Player(
            name = "이투수",
            birthDate = LocalDate.of(1993, 3, 10),
            primaryPosition = Position.STARTING_PITCHER,
            battingHand = BattingHand.RIGHT,
            throwingHand = ThrowingHand.LEFT
        )

        val runnerPlayer = Player(
            name = "박주자",
            birthDate = LocalDate.of(1997, 7, 20),
            primaryPosition = Position.CENTER_FIELD,
            battingHand = BattingHand.LEFT,
            throwingHand = ThrowingHand.LEFT
        )

        batter = GamePlayer.createStarter(
            gameTeam = awayTeam,
            player = batterPlayer,
            position = Position.SHORTSTOP,
            battingOrder = 3,
            backNumber = 6
        )

        pitcher = GamePlayer.createStarter(
            gameTeam = homeTeam,
            player = pitcherPlayer,
            position = Position.STARTING_PITCHER,
            battingOrder = null,
            backNumber = 21
        )

        runner = GamePlayer.createStarter(
            gameTeam = awayTeam,
            player = runnerPlayer,
            position = Position.CENTER_FIELD,
            battingOrder = 1,
            backNumber = 51
        )
    }

    @Nested
    @DisplayName("타석 결과 이벤트 생성")
    inner class CreatePlateAppearance {

        @Test
        fun `안타 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createPlateAppearance(
                game = game,
                inning = 1,
                isTopInning = true,
                outCountBefore = 0,
                outCountAfter = 0,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.SINGLE,
                description = "좌전 안타",
                eventOrder = 1
            )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.PLATE_APPEARANCE)
            assertThat(event.plateAppearanceResult).isEqualTo(PlateAppearanceResult.SINGLE)
            assertThat(event.inningDisplay).isEqualTo("1회초")
            assertThat(event.outsAdded).isEqualTo(0)
        }

        @Test
        fun `홈런 득점 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createPlateAppearance(
                game = game,
                inning = 5,
                isTopInning = false,
                outCountBefore = 1,
                outCountAfter = 1,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.HOME_RUN,
                description = "2점 홈런",
                runsScored = 2,
                rbis = 2,
                eventOrder = 25
            )

            // then
            assertThat(event.runsScored).isEqualTo(2)
            assertThat(event.rbis).isEqualTo(2)
            assertThat(event.isScoringEvent).isTrue()
            assertThat(event.inningDisplay).isEqualTo("5회말")
        }

        @Test
        fun `삼진 아웃 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createPlateAppearance(
                game = game,
                inning = 3,
                isTopInning = true,
                outCountBefore = 2,
                outCountAfter = 3,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.STRIKEOUT,
                description = "헛스윙 삼진",
                eventOrder = 15
            )

            // then
            assertThat(event.outsAdded).isEqualTo(1)
            assertThat(event.isInningEndingEvent).isTrue()
        }

        @Test
        fun `타자 없이 타석 결과 이벤트를 생성하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.PLATE_APPEARANCE,
                    description = "안타",
                    batter = null,
                    pitcher = pitcher,
                    plateAppearanceResult = PlateAppearanceResult.SINGLE,
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("타자")
        }

        @Test
        fun `투수 없이 타석 결과 이벤트를 생성하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.PLATE_APPEARANCE,
                    description = "안타",
                    batter = batter,
                    pitcher = null,
                    plateAppearanceResult = PlateAppearanceResult.SINGLE,
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("투수")
        }

        @Test
        fun `결과 유형 없이 타석 결과 이벤트를 생성하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.PLATE_APPEARANCE,
                    description = "안타",
                    batter = batter,
                    pitcher = pitcher,
                    plateAppearanceResult = null,
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("결과 유형")
        }
    }

    @Nested
    @DisplayName("주루 이벤트 생성")
    inner class CreateBaseRunning {

        @Test
        fun `도루 성공 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createBaseRunning(
                game = game,
                inning = 2,
                isTopInning = true,
                outCountBefore = 1,
                outCountAfter = 1,
                runner = runner,
                pitcher = pitcher,
                event = BaseRunningEvent.STOLEN_BASE,
                description = "2루 도루 성공",
                eventOrder = 10
            )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.BASE_RUNNING)
            assertThat(event.baseRunningEvent).isEqualTo(BaseRunningEvent.STOLEN_BASE)
            assertThat(event.involvedRunner).isEqualTo(runner)
            assertThat(event.outsAdded).isEqualTo(0)
        }

        @Test
        fun `도루 실패 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createBaseRunning(
                game = game,
                inning = 4,
                isTopInning = false,
                outCountBefore = 0,
                outCountAfter = 1,
                runner = runner,
                pitcher = pitcher,
                event = BaseRunningEvent.CAUGHT_STEALING,
                description = "2루 도루 실패",
                eventOrder = 20
            )

            // then
            assertThat(event.baseRunningEvent).isEqualTo(BaseRunningEvent.CAUGHT_STEALING)
            assertThat(event.outsAdded).isEqualTo(1)
        }

        @Test
        fun `폭투로 득점하는 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createBaseRunning(
                game = game,
                inning = 7,
                isTopInning = true,
                outCountBefore = 2,
                outCountAfter = 2,
                runner = runner,
                pitcher = pitcher,
                event = BaseRunningEvent.WILD_PITCH,
                description = "폭투, 3루 주자 홈인",
                runsScored = 1,
                eventOrder = 35
            )

            // then
            assertThat(event.baseRunningEvent).isEqualTo(BaseRunningEvent.WILD_PITCH)
            assertThat(event.runsScored).isEqualTo(1)
            assertThat(event.isScoringEvent).isTrue()
        }

        @Test
        fun `이벤트 유형 없이 주루 이벤트를 생성하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.BASE_RUNNING,
                    description = "도루",
                    involvedRunner = runner,
                    pitcher = pitcher,
                    baseRunningEvent = null,
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이벤트 유형")
        }
    }

    @Nested
    @DisplayName("선수 교체 이벤트 생성")
    inner class CreateSubstitution {

        @Test
        fun `대타 교체 이벤트를 생성할 수 있다`() {
            // given
            val pinchHitterPlayer = Player(
                name = "최대타",
                birthDate = LocalDate.of(1994, 2, 28),
                primaryPosition = Position.DESIGNATED_HITTER,
                battingHand = BattingHand.LEFT,
                throwingHand = ThrowingHand.RIGHT
            )
            val pinchHitter = GamePlayer.createBench(
                gameTeam = awayTeam,
                player = pinchHitterPlayer,
                position = Position.DESIGNATED_HITTER,
                backNumber = 25
            )

            // when
            val event = GameEvent.createSubstitution(
                game = game,
                inning = 8,
                isTopInning = true,
                outCount = 2,
                substitutionType = SubstitutionType.PINCH_HITTER,
                playerIn = pinchHitter,
                playerOut = batter,
                description = "대타 최대타, 김타자 대신 출전",
                eventOrder = 40
            )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.SUBSTITUTION)
            assertThat(event.substitutionType).isEqualTo(SubstitutionType.PINCH_HITTER)
            assertThat(event.substitutedIn).isEqualTo(pinchHitter)
            assertThat(event.substitutedOut).isEqualTo(batter)
            assertThat(event.outsAdded).isEqualTo(0)
        }

        @Test
        fun `투수 교체 이벤트를 생성할 수 있다`() {
            // given
            val relieverPlayer = Player(
                name = "정마무리",
                birthDate = LocalDate.of(1996, 8, 12),
                primaryPosition = Position.RELIEF_PITCHER,
                battingHand = BattingHand.RIGHT,
                throwingHand = ThrowingHand.RIGHT
            )
            val reliever = GamePlayer.createBench(
                gameTeam = homeTeam,
                player = relieverPlayer,
                position = Position.RELIEF_PITCHER,
                backNumber = 47
            )

            // when
            val event = GameEvent.createSubstitution(
                game = game,
                inning = 9,
                isTopInning = true,
                outCount = 0,
                substitutionType = SubstitutionType.PITCHING_CHANGE,
                playerIn = reliever,
                playerOut = pitcher,
                description = "투수 교체: 정마무리 등판",
                eventOrder = 45
            )

            // then
            assertThat(event.substitutionType).isEqualTo(SubstitutionType.PITCHING_CHANGE)
        }

        @Test
        fun `교체 투입 선수 없이 교체 이벤트를 생성하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.SUBSTITUTION,
                    description = "대타",
                    substitutionType = SubstitutionType.PINCH_HITTER,
                    substitutedIn = null,
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("교체 투입 선수")
        }
    }

    @Nested
    @DisplayName("경기 진행 이벤트 생성")
    inner class CreateGameProgressEvents {

        @Test
        fun `경기 시작 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createGameStart(game = game)

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.GAME_START)
            assertThat(event.inning).isEqualTo(1)
            assertThat(event.isTopInning).isTrue()
            assertThat(event.eventOrder).isEqualTo(1)
        }

        @Test
        fun `이닝 전환 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createHalfInningChange(
                game = game,
                inning = 1,
                isTopInning = true,
                description = "1회초 종료, 공수 교대",
                eventOrder = 12
            )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.HALF_INNING_CHANGE)
            assertThat(event.outCountBefore).isEqualTo(3)
            assertThat(event.outCountAfter).isEqualTo(0)
        }

        @Test
        fun `경기 종료 이벤트를 생성할 수 있다`() {
            // when
            val event = GameEvent.createGameEnd(
                game = game,
                inning = 9,
                isTopInning = false,
                outCount = 3,
                description = "경기 종료, 최종 스코어 5:3",
                eventOrder = 100
            )

            // then
            assertThat(event.eventType).isEqualTo(GameEventType.GAME_END)
            assertThat(event.inning).isEqualTo(9)
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {

        @Test
        fun `이닝은 1 이상이어야 한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 0,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.GAME_START,
                    description = "경기 시작",
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝")
        }

        @Test
        fun `아웃 카운트는 0-3 사이여야 한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 4,
                    outCountAfter = 0,
                    eventType = GameEventType.HALF_INNING_CHANGE,
                    description = "이닝 전환",
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("아웃 카운트")
        }

        @Test
        fun `득점은 0 이상이어야 한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.PLATE_APPEARANCE,
                    description = "안타",
                    batter = batter,
                    pitcher = pitcher,
                    plateAppearanceResult = PlateAppearanceResult.SINGLE,
                    runsScored = -1,
                    eventOrder = 1
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("득점")
        }

        @Test
        fun `이벤트 순서는 1 이상이어야 한다`() {
            // when & then
            assertThatThrownBy {
                GameEvent(
                    game = game,
                    inning = 1,
                    isTopInning = true,
                    outCountBefore = 0,
                    outCountAfter = 0,
                    eventType = GameEventType.GAME_START,
                    description = "경기 시작",
                    eventOrder = 0
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이벤트 순서")
        }
    }
}
