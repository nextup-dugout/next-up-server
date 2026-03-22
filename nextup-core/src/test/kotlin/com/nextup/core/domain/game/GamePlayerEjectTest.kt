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

@DisplayName("GamePlayer 퇴장 (eject)")
class GamePlayerEjectTest {
    private lateinit var gameTeam: GameTeam
    private lateinit var player: Player

    @BeforeEach
    fun setup() {
        gameTeam = mockk<GameTeam>(relaxed = true)
        player = mockk<Player>(relaxed = true)
    }

    @Nested
    @DisplayName("eject()")
    inner class EjectTest {
        @Test
        fun `부상 사유로 퇴장할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.SHORTSTOP,
                    battingOrder = 3,
                )

            gamePlayer.eject(5, EjectionReason.INJURY)

            assertThat(gamePlayer.isCurrentlyPlaying).isFalse()
            assertThat(gamePlayer.exitInning).isEqualTo(5)
            assertThat(gamePlayer.ejectionReason).isEqualTo(EjectionReason.INJURY)
            assertThat(gamePlayer.isEjected).isTrue()
        }

        @Test
        fun `심판 퇴장 사유로 퇴장할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.CATCHER,
                    battingOrder = 5,
                )

            gamePlayer.eject(3, EjectionReason.EJECTION_BY_UMPIRE)

            assertThat(gamePlayer.isCurrentlyPlaying).isFalse()
            assertThat(gamePlayer.ejectionReason).isEqualTo(EjectionReason.EJECTION_BY_UMPIRE)
            assertThat(gamePlayer.isEjected).isTrue()
        }

        @Test
        fun `기타 사유로 퇴장할 수 있다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.LEFT_FIELD,
                    battingOrder = 7,
                )

            gamePlayer.eject(4, EjectionReason.OTHER)

            assertThat(gamePlayer.ejectionReason).isEqualTo(EjectionReason.OTHER)
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
                gamePlayer.eject(0, EjectionReason.INJURY)
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
                gamePlayer.eject(5, EjectionReason.INJURY)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }

        @Test
        fun `퇴장하지 않은 선수의 isEjected는 false이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.CENTER_FIELD,
                    battingOrder = 8,
                )

            assertThat(gamePlayer.isEjected).isFalse()
            assertThat(gamePlayer.ejectionReason).isNull()
        }

        @Test
        fun `일반 교체로 퇴장한 선수의 isEjected는 false이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.RIGHT_FIELD,
                    battingOrder = 9,
                )

            gamePlayer.exitGame(6)

            assertThat(gamePlayer.hasExited).isTrue()
            assertThat(gamePlayer.isEjected).isFalse()
            assertThat(gamePlayer.ejectionReason).isNull()
        }

        @Test
        fun `퇴장 후 hasExited가 true이다`() {
            val gamePlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = player,
                    position = Position.SECOND_BASE,
                    battingOrder = 4,
                )

            gamePlayer.eject(7, EjectionReason.INJURY)

            assertThat(gamePlayer.hasExited).isTrue()
        }
    }

    @Nested
    @DisplayName("EjectionReason enum")
    inner class EjectionReasonEnumTest {
        @Test
        fun `EjectionReason 값이 올바르다`() {
            assertThat(EjectionReason.INJURY.displayName).isEqualTo("부상")
            assertThat(EjectionReason.EJECTION_BY_UMPIRE.displayName).isEqualTo("심판 퇴장")
            assertThat(EjectionReason.OTHER.displayName).isEqualTo("기타")
        }

        @Test
        fun `EjectionReason enum 값이 3개이다`() {
            assertThat(EjectionReason.entries).hasSize(3)
        }
    }
}
