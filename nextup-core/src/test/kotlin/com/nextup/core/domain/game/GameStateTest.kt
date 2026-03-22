package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class GameStateTest {
    @Test
    fun `should create GameState with default values`() {
        // when
        val gameState = GameState()

        // then
        assertThat(gameState.outs).isEqualTo(0)
        assertThat(gameState.balls).isEqualTo(0)
        assertThat(gameState.strikes).isEqualTo(0)
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isNull()
        assertThat(gameState.homeBattingOrder).isEqualTo(1)
        assertThat(gameState.awayBattingOrder).isEqualTo(1)
        assertThat(gameState.currentPitcherId).isNull()
        assertThat(gameState.currentBatterId).isNull()
        assertThat(gameState.wasDhReleased).isFalse()
    }

    @Test
    fun `should validate outs range on creation`() {
        // when & then
        assertThatThrownBy { GameState(outs = -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("아웃 카운트는 0-3 사이여야 합니다")

        assertThatThrownBy { GameState(outs = 4) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("아웃 카운트는 0-3 사이여야 합니다")

        assertDoesNotThrow { GameState(outs = 0) }
        assertDoesNotThrow { GameState(outs = 3) }
    }

    @Test
    fun `should validate balls range on creation`() {
        // when & then
        assertThatThrownBy { GameState(balls = -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("볼 카운트는 0-4 사이여야 합니다")

        assertThatThrownBy { GameState(balls = 5) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("볼 카운트는 0-4 사이여야 합니다")

        assertDoesNotThrow { GameState(balls = 0) }
        assertDoesNotThrow { GameState(balls = 4) }
    }

    @Test
    fun `should validate strikes range on creation`() {
        // when & then
        assertThatThrownBy { GameState(strikes = -1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("스트라이크 카운트는 0-3 사이여야 합니다")

        assertThatThrownBy { GameState(strikes = 4) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("스트라이크 카운트는 0-3 사이여야 합니다")

        assertDoesNotThrow { GameState(strikes = 0) }
        assertDoesNotThrow { GameState(strikes = 3) }
    }

    @Test
    fun `should validate home batting order range on creation`() {
        // when & then
        assertThatThrownBy { GameState(homeBattingOrder = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("홈팀 타순은 1-9 사이여야 합니다")

        assertThatThrownBy { GameState(homeBattingOrder = 10) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("홈팀 타순은 1-9 사이여야 합니다")

        assertDoesNotThrow { GameState(homeBattingOrder = 1) }
        assertDoesNotThrow { GameState(homeBattingOrder = 9) }
    }

    @Test
    fun `should validate away batting order range on creation`() {
        // when & then
        assertThatThrownBy { GameState(awayBattingOrder = 0) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("원정팀 타순은 1-9 사이여야 합니다")

        assertThatThrownBy { GameState(awayBattingOrder = 10) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("원정팀 타순은 1-9 사이여야 합니다")

        assertDoesNotThrow { GameState(awayBattingOrder = 1) }
        assertDoesNotThrow { GameState(awayBattingOrder = 9) }
    }

    @Test
    fun `should record out and return false when not third out`() {
        // given
        val gameState = GameState(outs = 0)

        // when
        val isInningOver = gameState.recordOut()

        // then
        assertThat(gameState.outs).isEqualTo(1)
        assertThat(isInningOver).isFalse()
    }

    @Test
    fun `should record out and return true when third out`() {
        // given
        val gameState = GameState(outs = 2)

        // when
        val isInningOver = gameState.recordOut()

        // then
        assertThat(gameState.outs).isEqualTo(3)
        assertThat(isInningOver).isTrue()
    }

    @Test
    fun `should throw exception when recording out at 3 outs`() {
        // given
        val gameState = GameState(outs = 3)

        // when & then
        assertThatThrownBy { gameState.recordOut() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("이미 3아웃입니다")
    }

    @Test
    fun `should add ball and return false when not fourth ball`() {
        // given
        val gameState = GameState(balls = 2)

        // when
        val isWalk = gameState.addBall()

        // then
        assertThat(gameState.balls).isEqualTo(3)
        assertThat(isWalk).isFalse()
    }

    @Test
    fun `should add ball and return true when fourth ball`() {
        // given
        val gameState = GameState(balls = 3)

        // when
        val isWalk = gameState.addBall()

        // then
        assertThat(gameState.balls).isEqualTo(4)
        assertThat(isWalk).isTrue()
    }

    @Test
    fun `should throw exception when adding ball at 4 balls`() {
        // given
        val gameState = GameState(balls = 4)

        // when & then
        assertThatThrownBy { gameState.addBall() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("이미 4볼입니다")
    }

    @Test
    fun `should add strike and return false when not third strike`() {
        // given
        val gameState = GameState(strikes = 1)

        // when
        val isStrikeout = gameState.addStrike()

        // then
        assertThat(gameState.strikes).isEqualTo(2)
        assertThat(isStrikeout).isFalse()
    }

    @Test
    fun `should add strike and return true when third strike`() {
        // given
        val gameState = GameState(strikes = 2)

        // when
        val isStrikeout = gameState.addStrike()

        // then
        assertThat(gameState.strikes).isEqualTo(3)
        assertThat(isStrikeout).isTrue()
    }

    @Test
    fun `should throw exception when adding strike at 3 strikes`() {
        // given
        val gameState = GameState(strikes = 3)

        // when & then
        assertThatThrownBy { gameState.addStrike() }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("이미 3스트라이크입니다")
    }

    @Test
    fun `should reset count`() {
        // given
        val gameState = GameState(balls = 3, strikes = 2)

        // when
        gameState.resetCount()

        // then
        assertThat(gameState.balls).isEqualTo(0)
        assertThat(gameState.strikes).isEqualTo(0)
    }

    @Test
    fun `should set runner on first base`() {
        // given
        val gameState = GameState()

        // when
        gameState.setRunner(Base.FIRST, 100L)

        // then
        assertThat(gameState.runnerOnFirstId).isEqualTo(100L)
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isNull()
    }

    @Test
    fun `should set runner on second base`() {
        // given
        val gameState = GameState()

        // when
        gameState.setRunner(Base.SECOND, 200L)

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isEqualTo(200L)
        assertThat(gameState.runnerOnThirdId).isNull()
    }

    @Test
    fun `should set runner on third base`() {
        // given
        val gameState = GameState()

        // when
        gameState.setRunner(Base.THIRD, 300L)

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isEqualTo(300L)
    }

    @Test
    fun `should clear runner by setting null`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
                runnerOnThirdId = 300L,
            )

        // when
        gameState.setRunner(Base.FIRST, null)
        gameState.setRunner(Base.SECOND, null)

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isEqualTo(300L)
    }

    @Test
    fun `should ignore HOME base when setting runner`() {
        // given
        val gameState = GameState()

        // when
        gameState.setRunner(Base.HOME, 400L)

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isNull()
    }

    @Test
    fun `should get runner from base`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
                runnerOnThirdId = 300L,
            )

        // when & then
        assertThat(gameState.getRunner(Base.FIRST)).isEqualTo(100L)
        assertThat(gameState.getRunner(Base.SECOND)).isEqualTo(200L)
        assertThat(gameState.getRunner(Base.THIRD)).isEqualTo(300L)
        assertThat(gameState.getRunner(Base.HOME)).isNull()
    }

    @Test
    fun `should clear all bases`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
                runnerOnThirdId = 300L,
            )

        // when
        gameState.clearBases()

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isNull()
    }

    @Test
    fun `should advance home team batter`() {
        // given
        val gameState = GameState(homeBattingOrder = 5)

        // when
        gameState.advanceBatter(isHomeTeam = true)

        // then
        assertThat(gameState.homeBattingOrder).isEqualTo(6)
        assertThat(gameState.awayBattingOrder).isEqualTo(1) // 원정팀은 그대로
    }

    @Test
    fun `should advance away team batter`() {
        // given
        val gameState = GameState(awayBattingOrder = 7)

        // when
        gameState.advanceBatter(isHomeTeam = false)

        // then
        assertThat(gameState.awayBattingOrder).isEqualTo(8)
        assertThat(gameState.homeBattingOrder).isEqualTo(1) // 홈팀은 그대로
    }

    @Test
    fun `should wrap home team batter from 9 to 1`() {
        // given
        val gameState = GameState(homeBattingOrder = 9)

        // when
        gameState.advanceBatter(isHomeTeam = true)

        // then
        assertThat(gameState.homeBattingOrder).isEqualTo(1)
    }

    @Test
    fun `should wrap away team batter from 9 to 1`() {
        // given
        val gameState = GameState(awayBattingOrder = 9)

        // when
        gameState.advanceBatter(isHomeTeam = false)

        // then
        assertThat(gameState.awayBattingOrder).isEqualTo(1)
    }

    @Test
    fun `should reset for new inning`() {
        // given
        val gameState =
            GameState(
                outs = 2,
                balls = 3,
                strikes = 2,
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
                runnerOnThirdId = 300L,
            )

        // when
        gameState.resetForNewInning()

        // then
        assertThat(gameState.outs).isEqualTo(0)
        assertThat(gameState.balls).isEqualTo(0)
        assertThat(gameState.strikes).isEqualTo(0)
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isNull()
    }

    @Test
    fun `should check if runner exists on base`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = null,
                runnerOnThirdId = 300L,
            )

        // when & then
        assertThat(gameState.hasRunner(Base.FIRST)).isTrue()
        assertThat(gameState.hasRunner(Base.SECOND)).isFalse()
        assertThat(gameState.hasRunner(Base.THIRD)).isTrue()
        assertThat(gameState.hasRunner(Base.HOME)).isFalse()
    }

    @Test
    fun `should check if bases loaded`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
                runnerOnThirdId = 300L,
            )

        // when & then
        assertThat(gameState.isBasesLoaded()).isTrue()
    }

    @Test
    fun `should return false when bases not loaded`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = null,
                runnerOnThirdId = 300L,
            )

        // when & then
        assertThat(gameState.isBasesLoaded()).isFalse()
    }

    @Test
    fun `should display count correctly`() {
        // given
        val gameState = GameState(balls = 2, strikes = 1)

        // when
        val display = gameState.countDisplay

        // then
        assertThat(display).isEqualTo("2B-1S")
    }

    @Test
    fun `should display count at full count`() {
        // given
        val gameState = GameState(balls = 3, strikes = 2)

        // when
        val display = gameState.countDisplay

        // then
        assertThat(display).isEqualTo("3B-2S")
    }

    // ── Inherited Runner Tracking Tests ──────────────────────────────────────

    @Test
    fun `should record responsible pitcher id when runner reaches base`() {
        // given
        val gameState = GameState(currentPitcherId = 10L)

        // when
        gameState.setRunner(Base.FIRST, 100L)

        // then
        assertThat(gameState.runnerOnFirstId).isEqualTo(100L)
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(10L)
    }

    @Test
    fun `should use currentPitcherId automatically when runner reaches base`() {
        // given
        val gameState = GameState(currentPitcherId = 20L)

        // when
        gameState.setRunner(Base.SECOND, 200L)
        gameState.setRunner(Base.THIRD, 300L)

        // then
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(20L)
        assertThat(gameState.runnerOnThirdPitcherId).isEqualTo(20L)
    }

    @Test
    fun `should preserve original pitcher id when runner advances bases`() {
        // given - pitcher A(id=10) puts runner on first
        val gameState = GameState(currentPitcherId = 10L)
        gameState.setRunner(Base.FIRST, 100L)

        // when - pitcher changes to B(id=99), runner advances to second
        gameState.currentPitcherId = 99L
        val originalPitcherId = gameState.getRunnerPitcherId(Base.FIRST)
        gameState.setRunner(Base.FIRST, null)
        gameState.setRunner(Base.SECOND, 100L, pitcherId = originalPitcherId)

        // then - pitcher id on second should still be pitcher A(id=10)
        assertThat(gameState.runnerOnSecondId).isEqualTo(100L)
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(10L)
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
    }

    @Test
    fun `should record new pitcher id for new runner after pitcher change`() {
        // given - pitcher A(id=10) already has runner on first
        val gameState = GameState(currentPitcherId = 10L)
        gameState.setRunner(Base.FIRST, 100L)

        // when - pitcher changes to B(id=99), new batter reaches base
        gameState.currentPitcherId = 99L
        gameState.setRunner(Base.SECOND, 200L)

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(10L) // original pitcher
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(99L) // new pitcher
    }

    @Test
    fun `should clear pitcher ids when runner is removed`() {
        // given
        val gameState = GameState(currentPitcherId = 10L)
        gameState.setRunner(Base.FIRST, 100L)
        gameState.setRunner(Base.SECOND, 200L)

        // when - runner on first is out
        gameState.setRunner(Base.FIRST, null)

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        // second base unaffected
        assertThat(gameState.runnerOnSecondId).isEqualTo(200L)
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(10L)
    }

    @Test
    fun `should clear all pitcher ids when clearBases is called`() {
        // given
        val gameState = GameState(currentPitcherId = 10L)
        gameState.setRunner(Base.FIRST, 100L)
        gameState.setRunner(Base.SECOND, 200L)
        gameState.setRunner(Base.THIRD, 300L)

        // when
        gameState.clearBases()

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        assertThat(gameState.runnerOnSecondPitcherId).isNull()
        assertThat(gameState.runnerOnThirdPitcherId).isNull()
    }

    @Test
    fun `should clear all pitcher ids when resetForNewInning is called`() {
        // given
        val gameState =
            GameState(
                outs = 2,
                currentPitcherId = 10L,
                runnerOnFirstId = 100L,
                runnerOnFirstPitcherId = 10L,
                runnerOnSecondId = 200L,
                runnerOnSecondPitcherId = 10L,
            )

        // when
        gameState.resetForNewInning()

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        assertThat(gameState.runnerOnSecondPitcherId).isNull()
        assertThat(gameState.runnerOnThirdPitcherId).isNull()
        assertThat(gameState.outs).isEqualTo(0)
    }

    @Test
    fun `should get runner pitcher id by base`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnFirstPitcherId = 10L,
                runnerOnSecondId = 200L,
                runnerOnSecondPitcherId = 20L,
                runnerOnThirdId = 300L,
                runnerOnThirdPitcherId = 30L,
            )

        // when & then
        assertThat(gameState.getRunnerPitcherId(Base.FIRST)).isEqualTo(10L)
        assertThat(gameState.getRunnerPitcherId(Base.SECOND)).isEqualTo(20L)
        assertThat(gameState.getRunnerPitcherId(Base.THIRD)).isEqualTo(30L)
        assertThat(gameState.getRunnerPitcherId(Base.HOME)).isNull()
    }

    @Test
    fun `should restore runner pitcher ids from json`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
            )

        // when
        gameState.restoreRunnerPitchers("1루:10,2루:20")

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(10L)
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(20L)
        assertThat(gameState.runnerOnThirdPitcherId).isNull()
    }

    @Test
    fun `should clear all pitcher ids when restoreRunnerPitchers called with null`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstPitcherId = 10L,
                runnerOnSecondPitcherId = 20L,
            )

        // when
        gameState.restoreRunnerPitchers(null)

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        assertThat(gameState.runnerOnSecondPitcherId).isNull()
    }

    @Test
    fun `should serialize runner pitcher ids to json`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnFirstPitcherId = 10L,
                runnerOnSecondId = 200L,
                runnerOnSecondPitcherId = 20L,
            )

        // when
        val json = gameState.serializeRunnerPitchers()

        // then
        assertThat(json).isEqualTo("1루:10,2루:20")
    }

    @Test
    fun `should return null when serializing with no pitcher assignments`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnFirstPitcherId = null,
            )

        // when
        val json = gameState.serializeRunnerPitchers()

        // then
        assertThat(json).isNull()
    }

    @Test
    fun `should not set pitcher id for HOME base`() {
        // given
        val gameState = GameState(currentPitcherId = 10L)

        // when
        gameState.setRunner(Base.HOME, 400L)

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        assertThat(gameState.runnerOnSecondPitcherId).isNull()
        assertThat(gameState.runnerOnThirdPitcherId).isNull()
    }

    @Test
    fun `should allow explicit pitcher id override when setting runner`() {
        // given
        val gameState = GameState(currentPitcherId = 99L)

        // when - explicitly pass a different pitcher id
        gameState.setRunner(Base.FIRST, 100L, pitcherId = 42L)

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(42L)
    }

    @Test
    fun `should create GameState with inherited runner pitcher id fields`() {
        // when
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnFirstPitcherId = 10L,
                runnerOnSecondId = 200L,
                runnerOnSecondPitcherId = 20L,
                runnerOnThirdId = 300L,
                runnerOnThirdPitcherId = 30L,
            )

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(10L)
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(20L)
        assertThat(gameState.runnerOnThirdPitcherId).isEqualTo(30L)
    }

    @Test
    fun `should serialize runner pitcher ids including third base`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnFirstPitcherId = 10L,
                runnerOnSecondId = 200L,
                runnerOnSecondPitcherId = 20L,
                runnerOnThirdId = 300L,
                runnerOnThirdPitcherId = 30L,
            )

        // when
        val json = gameState.serializeRunnerPitchers()

        // then
        assertThat(json).isEqualTo("1루:10,2루:20,3루:30")
    }

    @Test
    fun `should restore runner pitcher ids including third base`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
                runnerOnThirdId = 300L,
            )

        // when
        gameState.restoreRunnerPitchers("1루:10,2루:20,3루:30")

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(10L)
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(20L)
        assertThat(gameState.runnerOnThirdPitcherId).isEqualTo(30L)
    }

    // ── RestoreRunners Tests ─────────────────────────────────────────────────

    @Test
    fun `should restore runners from json string`() {
        // given
        val gameState = GameState()

        // when
        gameState.restoreRunners("1루:100,2루:200,3루:300")

        // then
        assertThat(gameState.runnerOnFirstId).isEqualTo(100L)
        assertThat(gameState.runnerOnSecondId).isEqualTo(200L)
        assertThat(gameState.runnerOnThirdId).isEqualTo(300L)
    }

    @Test
    fun `should clear all runners when restoreRunners called with null`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
                runnerOnSecondId = 200L,
                runnerOnThirdId = 300L,
            )

        // when
        gameState.restoreRunners(null)

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isNull()
    }

    @Test
    fun `should clear all runners when restoreRunners called with blank string`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstId = 100L,
            )

        // when
        gameState.restoreRunners("")

        // then
        assertThat(gameState.runnerOnFirstId).isNull()
    }

    @Test
    fun `should restore partial runners from json string`() {
        // given
        val gameState = GameState()

        // when
        gameState.restoreRunners("1루:100,3루:300")

        // then
        assertThat(gameState.runnerOnFirstId).isEqualTo(100L)
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isEqualTo(300L)
    }

    @Test
    fun `should handle malformed entry in restoreRunners`() {
        // given
        val gameState = GameState()

        // when
        gameState.restoreRunners("1루:100,invalid,3루:300")

        // then
        assertThat(gameState.runnerOnFirstId).isEqualTo(100L)
        assertThat(gameState.runnerOnSecondId).isNull()
        assertThat(gameState.runnerOnThirdId).isEqualTo(300L)
    }

    // ── RevertBatter Tests ───────────────────────────────────────────────────

    @Test
    fun `should revert home team batter`() {
        // given
        val gameState = GameState(homeBattingOrder = 5)

        // when
        gameState.revertBatter(isHomeTeam = true)

        // then
        assertThat(gameState.homeBattingOrder).isEqualTo(4)
    }

    @Test
    fun `should revert away team batter`() {
        // given
        val gameState = GameState(awayBattingOrder = 7)

        // when
        gameState.revertBatter(isHomeTeam = false)

        // then
        assertThat(gameState.awayBattingOrder).isEqualTo(6)
    }

    @Test
    fun `should wrap home team batter from 1 to 9 when reverting`() {
        // given
        val gameState = GameState(homeBattingOrder = 1)

        // when
        gameState.revertBatter(isHomeTeam = true)

        // then
        assertThat(gameState.homeBattingOrder).isEqualTo(9)
    }

    @Test
    fun `should wrap away team batter from 1 to 9 when reverting`() {
        // given
        val gameState = GameState(awayBattingOrder = 1)

        // when
        gameState.revertBatter(isHomeTeam = false)

        // then
        assertThat(gameState.awayBattingOrder).isEqualTo(9)
    }

    // ── RestoreOuts Tests ────────────────────────────────────────────────────

    @Test
    fun `should restore outs count`() {
        // given
        val gameState = GameState(outs = 0)

        // when
        gameState.restoreOuts(2)

        // then
        assertThat(gameState.outs).isEqualTo(2)
    }

    @Test
    fun `should throw exception when restoring invalid outs`() {
        // given
        val gameState = GameState()

        // when & then
        assertThatThrownBy { gameState.restoreOuts(-1) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("아웃 카운트는 0-3 사이여야 합니다")

        assertThatThrownBy { gameState.restoreOuts(4) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("아웃 카운트는 0-3 사이여야 합니다")
    }

    // ── RestoreRunnerPitchers edge case ──────────────────────────────────────

    @Test
    fun `should clear pitcher ids when restoreRunnerPitchers called with blank string`() {
        // given
        val gameState =
            GameState(
                runnerOnFirstPitcherId = 10L,
                runnerOnSecondPitcherId = 20L,
                runnerOnThirdPitcherId = 30L,
            )

        // when
        gameState.restoreRunnerPitchers("")

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        assertThat(gameState.runnerOnSecondPitcherId).isNull()
        assertThat(gameState.runnerOnThirdPitcherId).isNull()
    }

    @Test
    fun `should return null when batting order is correct for home team`() {
        // given
        val gameState = GameState(homeBattingOrder = 3)

        // when
        val violation = gameState.validateBattingOrder(batterBattingOrder = 3, isHomeTeam = true)

        // then
        assertThat(violation).isNull()
    }

    @Test
    fun `should return null when batting order is correct for away team`() {
        // given
        val gameState = GameState(awayBattingOrder = 5)

        // when
        val violation = gameState.validateBattingOrder(batterBattingOrder = 5, isHomeTeam = false)

        // then
        assertThat(violation).isNull()
    }

    @Test
    fun `should return violation when home team batting order is wrong`() {
        // given
        val gameState = GameState(homeBattingOrder = 3)

        // when
        val violation = gameState.validateBattingOrder(batterBattingOrder = 5, isHomeTeam = true)

        // then
        assertThat(violation).isNotNull()
        assertThat(violation!!.expectedBattingOrder).isEqualTo(3)
        assertThat(violation.actualBattingOrder).isEqualTo(5)
    }

    @Test
    fun `should return violation when away team batting order is wrong`() {
        // given
        val gameState = GameState(awayBattingOrder = 2)

        // when
        val violation = gameState.validateBattingOrder(batterBattingOrder = 4, isHomeTeam = false)

        // then
        assertThat(violation).isNotNull()
        assertThat(violation!!.expectedBattingOrder).isEqualTo(2)
        assertThat(violation.actualBattingOrder).isEqualTo(4)
    }

    @Test
    fun `should return null when batterBattingOrder is null`() {
        // given
        val gameState = GameState(homeBattingOrder = 3)

        // when
        val violation = gameState.validateBattingOrder(batterBattingOrder = null, isHomeTeam = true)

        // then
        assertThat(violation).isNull()
    }

    @Test
    fun `should not confuse home and away batting orders`() {
        // given
        val gameState = GameState(homeBattingOrder = 3, awayBattingOrder = 7)

        // when: correct order for home team but wrong for away team
        val homeViolation = gameState.validateBattingOrder(batterBattingOrder = 3, isHomeTeam = true)
        val awayViolation = gameState.validateBattingOrder(batterBattingOrder = 3, isHomeTeam = false)

        // then
        assertThat(homeViolation).isNull()
        assertThat(awayViolation).isNotNull()
        assertThat(awayViolation!!.expectedBattingOrder).isEqualTo(7)
        assertThat(awayViolation.actualBattingOrder).isEqualTo(3)
    }

    // ── wasDhReleased Tests ──────────────────────────────────────────────────

    @Test
    fun `should default wasDhReleased to false`() {
        // when
        val gameState = GameState()

        // then
        assertThat(gameState.wasDhReleased).isFalse()
    }

    @Test
    fun `should set wasDhReleased to true`() {
        // given
        val gameState = GameState()

        // when
        gameState.wasDhReleased = true

        // then
        assertThat(gameState.wasDhReleased).isTrue()
    }

    @Test
    fun `should create GameState with wasDhReleased parameter`() {
        // when
        val gameState = GameState(wasDhReleased = true)

        // then
        assertThat(gameState.wasDhReleased).isTrue()
    }

    @Test
    fun `should preserve wasDhReleased after resetForNewInning`() {
        // given
        val gameState =
            GameState(
                outs = 2,
                balls = 3,
                strikes = 2,
                wasDhReleased = true,
            )

        // when
        gameState.resetForNewInning()

        // then - wasDhReleased should persist across innings
        assertThat(gameState.wasDhReleased).isTrue()
        assertThat(gameState.outs).isEqualTo(0)
    }

    // ── Tiebreaker Responsible Pitcher Tests ──────────────────────────────────

    @Test
    fun `setupTiebreaker should use currentPitcherId when responsiblePitcherId is null`() {
        // given
        val gameState = GameState(currentPitcherId = 50L)

        // when
        gameState.setupTiebreaker(firstRunnerId = 10L, secondRunnerId = 20L)

        // then
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(50L)
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(50L)
    }

    @Test
    fun `setupTiebreaker should use explicit responsiblePitcherId when provided`() {
        // given
        val gameState = GameState(currentPitcherId = 50L)

        // when
        gameState.setupTiebreaker(
            firstRunnerId = 10L,
            secondRunnerId = 20L,
            responsiblePitcherId = 77L,
        )

        // then - responsiblePitcherId(77) overrides currentPitcherId(50)
        assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(77L)
        assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(77L)
    }

    @Test
    fun `setupTiebreaker should set null pitcher ids when runners are null`() {
        // given
        val gameState = GameState(currentPitcherId = 50L)

        // when
        gameState.setupTiebreaker(
            firstRunnerId = null,
            secondRunnerId = null,
            responsiblePitcherId = 77L,
        )

        // then - no runners, so no pitcher ids
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        assertThat(gameState.runnerOnSecondPitcherId).isNull()
    }

    @Test
    fun `setupTiebreaker should clear third base runner and pitcher id`() {
        // given
        val gameState = GameState(currentPitcherId = 50L)
        gameState.setRunner(Base.THIRD, 99L)
        assertThat(gameState.runnerOnThirdId).isEqualTo(99L)
        assertThat(gameState.runnerOnThirdPitcherId).isEqualTo(50L)

        // when
        gameState.setupTiebreaker(
            firstRunnerId = 10L,
            secondRunnerId = 20L,
            responsiblePitcherId = 77L,
        )

        // then
        assertThat(gameState.runnerOnThirdId).isNull()
        assertThat(gameState.runnerOnThirdPitcherId).isNull()
    }

    @Test
    fun `setupTiebreaker with null currentPitcherId and no responsiblePitcherId results in null pitcher ids`() {
        // given - currentPitcherId is null (edge case)
        val gameState = GameState(currentPitcherId = null)

        // when
        gameState.setupTiebreaker(firstRunnerId = 10L, secondRunnerId = 20L)

        // then - pitcherId falls back to currentPitcherId which is null
        assertThat(gameState.runnerOnFirstId).isEqualTo(10L)
        assertThat(gameState.runnerOnFirstPitcherId).isNull()
        assertThat(gameState.runnerOnSecondId).isEqualTo(20L)
        assertThat(gameState.runnerOnSecondPitcherId).isNull()
    }
}
