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

/**
 * 선수 교체 시나리오 통합 테스트
 *
 * 대타(Pinch Hitter), 대주자(Pinch Runner), 투수 교체(Pitcher Change), DH 해제 시나리오를
 * 실제 야구 규칙에 맞춰 검증합니다.
 */
@DisplayName("선수 교체 시나리오")
class SubstitutionScenarioTest {

    private lateinit var gameTeam: GameTeam

    @BeforeEach
    fun setUp() {
        gameTeam = mockk<GameTeam>(relaxed = true)
    }

    // =========================================================================
    // 대타 (Pinch Hitter) 시나리오
    // =========================================================================

    @Nested
    @DisplayName("대타 투입")
    inner class PinchHitterScenario {

        @Test
        fun `대타 선수는 교체 출전 후 isStarter가 false이고 타순이 이어받아진다`() {
            // given
            val originalBatter =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.LEFT_FIELD,
                    battingOrder = 5,
                )
            val pinchHitter =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.LEFT_FIELD,
                )

            // when: 원래 선수 퇴장
            originalBatter.exitGame(inning = 7)
            // when: 대타 투입 - 같은 타순 배정
            pinchHitter.enterAsSubstitute(
                inning = 7,
                newPosition = Position.LEFT_FIELD,
                newBattingOrder = 5,
            )

            // then
            assertThat(originalBatter.isCurrentlyPlaying).isFalse()
            assertThat(originalBatter.exitInning).isEqualTo(7)
            assertThat(pinchHitter.isStarter).isFalse()
            assertThat(pinchHitter.isCurrentlyPlaying).isTrue()
            assertThat(pinchHitter.battingOrder).isEqualTo(5)
            assertThat(pinchHitter.entryInning).isEqualTo(7)
        }

        @Test
        fun `대타 투입 후 원래 선수는 재출전 불가 상태가 된다`() {
            // given
            val originalBatter =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.RIGHT_FIELD,
                    battingOrder = 3,
                )

            // when: 퇴장 처리 (대타 투입으로 인한 교체)
            originalBatter.exitGame(inning = 5)

            // then: 퇴장된 선수는 isCurrentlyPlaying이 false이므로 포지션/타순 변경 불가
            assertThat(originalBatter.isCurrentlyPlaying).isFalse()
            assertThatThrownBy {
                originalBatter.changePosition(Position.CENTER_FIELD)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }

        @Test
        fun `대타는 타석 후 수비 포지션에 배정될 수 있다`() {
            // given
            val pinchHitter =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.CENTER_FIELD,
                )

            // when: 대타로 출전 (타석만 서는 경우 포지션은 나중에 배정)
            pinchHitter.enterAsSubstitute(
                inning = 6,
                newPosition = Position.CENTER_FIELD,
                newBattingOrder = 8,
            )

            // when: 다음 이닝 수비에서 포지션 변경 (예: 좌익수로 배정)
            pinchHitter.changePosition(Position.LEFT_FIELD)

            // then
            assertThat(pinchHitter.isCurrentlyPlaying).isTrue()
            assertThat(pinchHitter.battingOrder).isEqualTo(8)
            assertThat(pinchHitter.position).isEqualTo(Position.LEFT_FIELD)
        }

        @Test
        fun `대타 투입 시 이닝이 0 이하이면 예외가 발생한다`() {
            // given
            val pinchHitter =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.FIRST_BASE,
                )

            // when & then
            assertThatThrownBy {
                pinchHitter.enterAsSubstitute(
                    inning = 0,
                    newPosition = Position.FIRST_BASE,
                    newBattingOrder = 4,
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝은 1 이상")
        }
    }

    // =========================================================================
    // 대주자 (Pinch Runner) 시나리오
    // =========================================================================

    @Nested
    @DisplayName("대주자 투입")
    inner class PinchRunnerScenario {

        @Test
        fun `대주자는 교체 출전 후 같은 타순을 이어받는다`() {
            // given: 1루에 있던 선수를 대주자로 교체
            val originalRunner =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.FIRST_BASE,
                    battingOrder = 2,
                )
            val pinchRunner =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.CENTER_FIELD,
                )

            // when
            originalRunner.exitGame(inning = 4)
            pinchRunner.enterAsSubstitute(
                inning = 4,
                newPosition = Position.CENTER_FIELD,
                newBattingOrder = 2,
            )

            // then
            assertThat(originalRunner.isCurrentlyPlaying).isFalse()
            assertThat(pinchRunner.isStarter).isFalse()
            assertThat(pinchRunner.isCurrentlyPlaying).isTrue()
            assertThat(pinchRunner.battingOrder).isEqualTo(2)
            assertThat(pinchRunner.entryInning).isEqualTo(4)
        }

        @Test
        fun `대주자 투입 후 원래 주자 선수는 재출전할 수 없다`() {
            // given
            val originalRunner =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.SHORTSTOP,
                    battingOrder = 6,
                )

            // when: 대주자 교체로 퇴장
            originalRunner.exitGame(inning = 8)

            // then: 퇴장 이후 타순 변경 시도 불가
            assertThat(originalRunner.isCurrentlyPlaying).isFalse()
            assertThatThrownBy {
                originalRunner.changeBattingOrder(7)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }

        @Test
        fun `대주자는 수비 이닝에서 포지션이 배정된다`() {
            // given
            val pinchRunner =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.SECOND_BASE,
                )

            // when: 대주자로 출전 후 수비 이닝 포지션 배정
            pinchRunner.enterAsSubstitute(
                inning = 3,
                newPosition = Position.SECOND_BASE,
                newBattingOrder = 7,
            )

            // then
            assertThat(pinchRunner.isCurrentlyPlaying).isTrue()
            assertThat(pinchRunner.position).isEqualTo(Position.SECOND_BASE)
        }
    }

    // =========================================================================
    // 투수 교체 (Pitcher Change) 시나리오
    // =========================================================================

    @Nested
    @DisplayName("투수 교체")
    inner class PitcherChangeScenario {

        @Test
        fun `선발투수 교체 시 기존 투수는 퇴장하고 새 투수가 출전한다`() {
            // given
            val startingPitcher =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.STARTING_PITCHER,
                    battingOrder = null,
                )
            val reliefPitcher =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.RELIEF_PITCHER,
                )

            // when: 선발투수 교체
            startingPitcher.exitGame(inning = 5)
            reliefPitcher.enterAsSubstitute(
                inning = 5,
                newPosition = Position.RELIEF_PITCHER,
                newBattingOrder = null,
            )

            // then
            assertThat(startingPitcher.isCurrentlyPlaying).isFalse()
            assertThat(startingPitcher.exitInning).isEqualTo(5)
            assertThat(reliefPitcher.isStarter).isFalse()
            assertThat(reliefPitcher.isCurrentlyPlaying).isTrue()
            assertThat(reliefPitcher.isPitcher).isTrue()
            assertThat(reliefPitcher.battingOrder).isNull()
            assertThat(reliefPitcher.entryInning).isEqualTo(5)
        }

        @Test
        fun `마무리투수로 교체 시 이전 투수는 퇴장 처리된다`() {
            // given
            val reliefPitcher =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.RELIEF_PITCHER,
                    battingOrder = null,
                )
            val closer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.CLOSER,
                )

            // when: 마무리 교체
            reliefPitcher.exitGame(inning = 9)
            closer.enterAsSubstitute(
                inning = 9,
                newPosition = Position.CLOSER,
                newBattingOrder = null,
            )

            // then
            assertThat(reliefPitcher.isCurrentlyPlaying).isFalse()
            assertThat(reliefPitcher.exitInning).isEqualTo(9)
            assertThat(closer.isCurrentlyPlaying).isTrue()
            assertThat(closer.isPitcher).isTrue()
            assertThat(closer.entryInning).isEqualTo(9)
        }

        @Test
        fun `투수 교체 후 이전 투수는 포지션 변경이 불가하다`() {
            // given
            val startingPitcher =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.STARTING_PITCHER,
                    battingOrder = null,
                )

            // when: 교체 퇴장
            startingPitcher.exitGame(inning = 6)

            // then: 퇴장 후 포지션 변경 불가
            assertThat(startingPitcher.isCurrentlyPlaying).isFalse()
            assertThatThrownBy {
                startingPitcher.changePosition(Position.RELIEF_PITCHER)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }

        @Test
        fun `투수 교체 후 새 투수의 출전 이닝이 올바르게 기록된다`() {
            // given
            val newPitcher =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.RELIEF_PITCHER,
                )

            // when: 7회에 등판
            newPitcher.enterAsSubstitute(
                inning = 7,
                newPosition = Position.RELIEF_PITCHER,
                newBattingOrder = null,
            )

            // then
            assertThat(newPitcher.entryInning).isEqualTo(7)
            assertThat(newPitcher.isStarter).isFalse()
            assertThat(newPitcher.isCurrentlyPlaying).isTrue()
            assertThat(newPitcher.battingOrder).isNull()
            assertThat(newPitcher.isInBattingOrder).isFalse()
        }
    }

    // =========================================================================
    // DH 해제 시나리오 (Designated Hitter Rule Release)
    // =========================================================================

    @Nested
    @DisplayName("DH 해제")
    inner class DesignatedHitterReleaseScenario {

        @Test
        fun `DH 해제 시 지명타자의 isDesignatedHitter가 false가 된다`() {
            // given: DH로 등록된 선수
            val dhPlayer =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.DESIGNATED_HITTER,
                    battingOrder = 4,
                )
            dhPlayer.setAsDesignatedHitter(pitcherOrder = 9)

            // when: DH 해제 (투수가 타순에 들어올 때)
            dhPlayer.releaseDH()

            // then
            assertThat(dhPlayer.isDesignatedHitter).isFalse()
            assertThat(dhPlayer.pitcherBattingOrder).isNull()
        }

        @Test
        fun `DH 해제 후 투수가 타순에 들어가기 위해 타순을 배정받을 수 있다`() {
            // given: 투수
            val pitcher =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.STARTING_PITCHER,
                    battingOrder = null,
                )

            // when: DH 해제로 투수가 타순에 편입
            pitcher.changeBattingOrder(4)

            // then
            assertThat(pitcher.battingOrder).isEqualTo(4)
            assertThat(pitcher.isPitcher).isTrue()
            assertThat(pitcher.isInBattingOrder).isTrue()
        }

        @Test
        fun `DH 규칙 활성화 시 투수는 타순에 포함되지 않는다`() {
            // given: DH 규칙이 적용된 경기에서 투수 (타순 없음)
            val pitcher =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.STARTING_PITCHER,
                    battingOrder = null,
                )

            // then: 타순이 없으면 isInBattingOrder가 false
            assertThat(pitcher.battingOrder).isNull()
            assertThat(pitcher.isInBattingOrder).isFalse()
            assertThat(pitcher.isPitcher).isTrue()
        }
    }

    // =========================================================================
    // 복합 교체 시나리오
    // =========================================================================

    @Nested
    @DisplayName("복합 교체 시나리오")
    inner class ComplexSubstitutionScenario {

        @Test
        fun `여러 번 교체가 발생해도 각 선수의 출전 기록이 독립적으로 유지된다`() {
            // given
            val starter =
                GamePlayer.createStarter(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.CENTER_FIELD,
                    battingOrder = 1,
                )
            val firstSub =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.LEFT_FIELD,
                )

            // when: 5회 교체
            starter.exitGame(inning = 5)
            firstSub.enterAsSubstitute(
                inning = 5,
                newPosition = Position.CENTER_FIELD,
                newBattingOrder = 1,
            )

            // then: 각 선수 기록이 독립적
            assertThat(starter.entryInning).isEqualTo(1) // 선발이므로 entryInning = 1
            assertThat(starter.exitInning).isEqualTo(5)
            assertThat(starter.isCurrentlyPlaying).isFalse()
            assertThat(firstSub.entryInning).isEqualTo(5)
            assertThat(firstSub.exitInning).isNull() // 아직 출전 중
            assertThat(firstSub.isCurrentlyPlaying).isTrue()
        }

        @Test
        fun `벤치 선수는 출전 전 isCurrentlyPlaying이 false이다`() {
            // given
            val benchPlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.THIRD_BASE,
                )

            // then: 출전 전 상태 확인
            assertThat(benchPlayer.isCurrentlyPlaying).isFalse()
            assertThat(benchPlayer.isStarter).isFalse()
            assertThat(benchPlayer.battingOrder).isNull()
            assertThat(benchPlayer.entryInning).isNull()
        }

        @Test
        fun `벤치 선수는 출전 전에 퇴장할 수 없다`() {
            // given
            val benchPlayer =
                GamePlayer.createBench(
                    gameTeam = gameTeam,
                    player = mockk<Player>(relaxed = true),
                    position = Position.SHORTSTOP,
                )

            // when & then: 출전하지 않은 선수는 퇴장 불가
            assertThatThrownBy {
                benchPlayer.exitGame(inning = 3)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만 퇴장할 수 있습니다")
        }
    }
}
