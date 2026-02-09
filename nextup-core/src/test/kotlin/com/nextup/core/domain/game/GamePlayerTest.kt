package com.nextup.core.domain.game

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GamePlayer")
class GamePlayerTest {
    private lateinit var gameTeam: GameTeam
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        gameTeam = mockk<GameTeam>(relaxed = true)
        player = mockk<Player>(relaxed = true)
    }

    @Nested
    @DisplayName("선발 선수 생성")
    inner class CreateStarterTest {
        @Test
        fun `선발 선수를 생성하면 isStarter가 true이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.SHORTSTOP,
                    battingOrder = 3,
                    backNumber = 6,
                )

            assertThat(gamePlayer.isStarter).isTrue()
            assertThat(gamePlayer.isCurrentlyPlaying).isTrue()
            assertThat(gamePlayer.position).isEqualTo(Position.SHORTSTOP)
            assertThat(gamePlayer.battingOrder).isEqualTo(3)
            assertThat(gamePlayer.backNumber).isEqualTo(6)
            assertThat(gamePlayer.entryInning).isEqualTo(1)
        }

        @Test
        fun `선발 선수는 isStartingLineup이 true이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.CATCHER,
                    battingOrder = 5,
                )

            assertThat(gamePlayer.isStartingLineup).isTrue()
        }

        @Test
        fun `타순이 null인 선발 선수는 isStartingLineup이 false이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.STARTING_PITCHER,
                    battingOrder = null,
                )

            assertThat(gamePlayer.isStartingLineup).isFalse()
        }

        @Test
        fun `타순이 있으면 isInBattingOrder가 true이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.FIRST_BASE,
                    battingOrder = 4,
                )

            assertThat(gamePlayer.isInBattingOrder).isTrue()
        }
    }

    @Nested
    @DisplayName("벤치 선수 생성")
    inner class CreateBenchTest {
        @Test
        fun `벤치 선수를 생성하면 isStarter가 false이다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.LEFT_FIELD,
                    backNumber = 25,
                )

            assertThat(gamePlayer.isStarter).isFalse()
            assertThat(gamePlayer.isCurrentlyPlaying).isFalse()
            assertThat(gamePlayer.battingOrder).isNull()
            assertThat(gamePlayer.backNumber).isEqualTo(25)
        }

        @Test
        fun `벤치 선수는 isInBattingOrder가 false이다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.RIGHT_FIELD,
                )

            assertThat(gamePlayer.isInBattingOrder).isFalse()
        }
    }

    @Nested
    @DisplayName("isPitcher 확인")
    inner class IsPitcherTest {
        @Test
        fun `투수 포지션이면 isPitcher가 true이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.STARTING_PITCHER,
                    battingOrder = null,
                )

            assertThat(gamePlayer.isPitcher).isTrue()
        }

        @Test
        fun `야수 포지션이면 isPitcher가 false이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.CENTER_FIELD,
                    battingOrder = 8,
                )

            assertThat(gamePlayer.isPitcher).isFalse()
        }

        @Test
        fun `중간계투도 isPitcher가 true이다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.RELIEF_PITCHER,
                )

            assertThat(gamePlayer.isPitcher).isTrue()
        }

        @Test
        fun `마무리투수도 isPitcher가 true이다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.CLOSER,
                )

            assertThat(gamePlayer.isPitcher).isTrue()
        }
    }

    @Nested
    @DisplayName("교체 출전")
    inner class EnterAsSubstituteTest {
        @Test
        fun `교체 선수로 출전할 수 있다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.LEFT_FIELD,
                )

            gamePlayer.enterAsSubstitute(
                inning = 5,
                newPosition = Position.RIGHT_FIELD,
                newBattingOrder = 7,
            )

            assertThat(gamePlayer.isStarter).isFalse()
            assertThat(gamePlayer.isCurrentlyPlaying).isTrue()
            assertThat(gamePlayer.entryInning).isEqualTo(5)
            assertThat(gamePlayer.position).isEqualTo(Position.RIGHT_FIELD)
            assertThat(gamePlayer.battingOrder).isEqualTo(7)
        }

        @Test
        fun `이닝이 0이면 예외가 발생한다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.LEFT_FIELD,
                )

            assertThatThrownBy {
                gamePlayer.enterAsSubstitute(0, Position.LEFT_FIELD, 1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝은 1 이상")
        }
    }

    @Nested
    @DisplayName("경기 퇴장")
    inner class ExitGameTest {
        @Test
        fun `현재 출전 중인 선수가 퇴장할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.SECOND_BASE,
                    battingOrder = 6,
                )

            gamePlayer.exitGame(7)

            assertThat(gamePlayer.isCurrentlyPlaying).isFalse()
            assertThat(gamePlayer.exitInning).isEqualTo(7)
        }

        @Test
        fun `이닝이 0이면 예외가 발생한다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.THIRD_BASE,
                    battingOrder = 5,
                )

            assertThatThrownBy {
                gamePlayer.exitGame(0)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝은 1 이상")
        }

        @Test
        fun `출전 중이 아닌 선수는 퇴장할 수 없다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.LEFT_FIELD,
                )

            assertThatThrownBy {
                gamePlayer.exitGame(5)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }
    }

    @Nested
    @DisplayName("포지션 변경")
    inner class ChangePositionTest {
        @Test
        fun `출전 중인 선수의 포지션을 변경할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.SHORTSTOP,
                    battingOrder = 2,
                )

            gamePlayer.changePosition(Position.SECOND_BASE)

            assertThat(gamePlayer.position).isEqualTo(Position.SECOND_BASE)
        }

        @Test
        fun `출전 중이 아닌 선수는 포지션을 변경할 수 없다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.SHORTSTOP,
                )

            assertThatThrownBy {
                gamePlayer.changePosition(Position.THIRD_BASE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }
    }

    @Nested
    @DisplayName("타순 변경")
    inner class ChangeBattingOrderTest {
        @Test
        fun `출전 중인 선수의 타순을 변경할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.FIRST_BASE,
                    battingOrder = 3,
                )

            gamePlayer.changeBattingOrder(5)

            assertThat(gamePlayer.battingOrder).isEqualTo(5)
        }

        @Test
        fun `출전 중이 아닌 선수는 타순을 변경할 수 없다`() {
            val gamePlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.CENTER_FIELD,
                )

            assertThatThrownBy {
                gamePlayer.changeBattingOrder(1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }
    }

    @Nested
    @DisplayName("DH 설정")
    inner class DesignatedHitterTest {
        @Test
        fun `지명타자 포지션인 선수를 DH로 설정할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.DESIGNATED_HITTER,
                    battingOrder = 4,
                )

            gamePlayer.setAsDesignatedHitter(pitcherOrder = 9)

            assertThat(gamePlayer.isDesignatedHitter).isTrue()
            assertThat(gamePlayer.pitcherBattingOrder).isEqualTo(9)
        }

        @Test
        fun `지명타자 포지션이 아닌 선수는 DH로 설정할 수 없다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.FIRST_BASE,
                    battingOrder = 3,
                )

            assertThatThrownBy {
                gamePlayer.setAsDesignatedHitter(pitcherOrder = 9)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("지명타자 포지션만")
        }

        @Test
        fun `DH를 해제할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.DESIGNATED_HITTER,
                    battingOrder = 4,
                )
            gamePlayer.setAsDesignatedHitter(pitcherOrder = 9)

            gamePlayer.releaseDH()

            assertThat(gamePlayer.isDesignatedHitter).isFalse()
            assertThat(gamePlayer.pitcherBattingOrder).isNull()
        }
    }
}
