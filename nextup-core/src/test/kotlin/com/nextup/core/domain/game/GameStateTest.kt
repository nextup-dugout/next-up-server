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
}
