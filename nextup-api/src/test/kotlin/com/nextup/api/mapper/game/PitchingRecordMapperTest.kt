package com.nextup.api.mapper.game

import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingRecord
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PitchingRecordMapperTest {
    @Test
    fun `should map PitchingRecord to PitchingRecordResponse`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 123L

        val pitchingRecord =
            PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                repeat(15) { recordOut() } // 5 innings
                recordHit(isHomeRun = false, runsScored = 1, earnedRuns = 1)
                recordWalk()
                recordHitByPitch()
            }

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.gamePlayerId).isEqualTo(123L)
        assertThat(response.inningsPitchedOuts).isEqualTo(15)
        assertThat(response.completeInnings).isEqualTo(5)
        assertThat(response.earnedRuns).isEqualTo(1)
        assertThat(response.hitsAllowed).isEqualTo(1)
        assertThat(response.walksAllowed).isEqualTo(1)
        assertThat(response.isStartingPitcher).isTrue
    }

    @Test
    fun `should format innings pitched display correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        // 5.1 innings (16 outs)
        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                repeat(16) { recordOut() }
            }

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.inningsPitchedOuts).isEqualTo(16)
        assertThat(response.completeInnings).isEqualTo(5)
        assertThat(response.remainingOuts).isEqualTo(1)
        assertThat(response.inningsPitchedDisplay).isEqualTo("5.1")
        assertThat(response.inningsPitched).isEqualTo("5.33")
    }

    @Test
    fun `should format innings pitched display for complete innings`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        // 7.0 innings (21 outs)
        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                repeat(21) { recordOut() }
            }

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.inningsPitchedOuts).isEqualTo(21)
        assertThat(response.completeInnings).isEqualTo(7)
        assertThat(response.remainingOuts).isEqualTo(0)
        assertThat(response.inningsPitchedDisplay).isEqualTo("7.0")
        assertThat(response.inningsPitched).isEqualTo("7.00")
    }

    @Test
    fun `should calculate ERA correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        // 6 innings, 2 earned runs
        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                repeat(18) { recordOut() } // 6 innings
                recordHit(runsScored = 2, earnedRuns = 2)
            }

        // when
        val response = pitchingRecord.toResponse()

        // then: (2 / 6) * 9 = 3.00
        assertThat(response.earnedRunAverage).isEqualTo("3.00")
    }

    @Test
    fun `should return zero ERA when innings is zero`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val pitchingRecord = PitchingRecord.create(gamePlayer)

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.inningsPitchedOuts).isEqualTo(0)
        assertThat(response.earnedRunAverage).isEqualTo("0.00")
    }

    @Test
    fun `should calculate WHIP correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        // 6 innings, 5 hits, 2 walks
        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                repeat(18) { recordOut() } // 6 innings
                repeat(5) { recordHit() }
                repeat(2) { recordWalk() }
            }

        // when
        val response = pitchingRecord.toResponse()

        // then: (5+2) / 6 = 1.17
        assertThat(response.whip).isEqualTo("1.17")
        assertThat(response.hitsAllowed).isEqualTo(5)
        assertThat(response.walksAllowed).isEqualTo(2)
    }

    @Test
    fun `should calculate K per 9 and BB per 9 correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        // 6 innings, 9 strikeouts, 3 walks
        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                repeat(9) { recordOut(isStrikeout = true) } // 3 innings with K
                repeat(9) { recordOut() } // 3 more innings
                repeat(3) { recordWalk() }
            }

        // when
        val response = pitchingRecord.toResponse()

        // then
        // K/9 = (9 / 6) * 9 = 13.50
        // BB/9 = (3 / 6) * 9 = 4.50
        assertThat(response.strikeouts).isEqualTo(9)
        assertThat(response.strikeoutsPer9).isEqualTo("13.50")
        assertThat(response.walksPer9).isEqualTo("4.50")
    }

    @Test
    fun `should calculate K to BB ratio correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                repeat(9) { recordOut(isStrikeout = true) }
                repeat(3) { recordWalk() }
            }

        // when
        val response = pitchingRecord.toResponse()

        // then: 9 / 3 = 3.00
        assertThat(response.strikeoutToWalkRatio).isEqualTo("3.00")
    }

    @Test
    fun `should handle K to BB ratio when walks is zero`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                repeat(9) { recordOut(isStrikeout = true) }
            }

        // when
        val response = pitchingRecord.toResponse()

        // then: should return strikeouts as is when walks is 0
        assertThat(response.strikeouts).isEqualTo(9)
        assertThat(response.walksAllowed).isEqualTo(0)
        assertThat(response.strikeoutToWalkRatio).isEqualTo("9.00")
    }

    @Test
    fun `should calculate strike percentage when pitches are recorded`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                recordPitchCount(totalPitches = 100, strikes = 65)
            }

        // when
        val response = pitchingRecord.toResponse()

        // then: 65 / 100 = 0.650
        assertThat(response.pitchesThrown).isEqualTo(100)
        assertThat(response.strikesThrown).isEqualTo(65)
        assertThat(response.strikePercentage).isEqualTo("0.650")
    }

    @Test
    fun `should return null strike percentage when pitches not recorded`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val pitchingRecord = PitchingRecord.create(gamePlayer)

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.pitchesThrown).isNull()
        assertThat(response.strikesThrown).isNull()
        assertThat(response.strikePercentage).isNull()
    }

    @Test
    fun `should convert PitchingDecision enum to string`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val pitchingRecord =
            PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                repeat(15) { recordOut() } // 5 innings (qualified for win)
                assignWin()
            }

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.decision).isEqualTo("WIN")
        assertThat(response.isQualifiedForWin).isTrue
    }

    @Test
    fun `should calculate unearned runs correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val pitchingRecord =
            PitchingRecord.create(gamePlayer).apply {
                recordHit(runsScored = 3, earnedRuns = 1) // 3 runs, 1 earned
            }

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.runsAllowed).isEqualTo(3)
        assertThat(response.earnedRuns).isEqualTo(1)
        assertThat(response.unearnedRuns).isEqualTo(2)
    }

    @Test
    fun `should map list of PitchingRecords to list of responses`() {
        // given
        val gamePlayer1 = mockk<GamePlayer>()
        val gamePlayer2 = mockk<GamePlayer>()
        every { gamePlayer1.id } returns 1L
        every { gamePlayer2.id } returns 2L

        val records =
            listOf(
                PitchingRecord.create(gamePlayer1, isStartingPitcher = true),
                PitchingRecord.create(gamePlayer2, isStartingPitcher = false),
            )

        // when
        val responses = records.toResponse()

        // then
        assertThat(responses).hasSize(2)
        assertThat(responses[0].gamePlayerId).isEqualTo(1L)
        assertThat(responses[0].isStartingPitcher).isTrue
        assertThat(responses[1].gamePlayerId).isEqualTo(2L)
        assertThat(responses[1].isStartingPitcher).isFalse
    }

    @Test
    fun `should map meta fields correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 999L

        val pitchingRecord = PitchingRecord.create(gamePlayer)

        // when
        val response = pitchingRecord.toResponse()

        // then
        assertThat(response.id).isEqualTo(0L) // Entity not persisted yet
        assertThat(response.gamePlayerId).isEqualTo(999L)
        assertThat(response.createdAt).isNotNull
        assertThat(response.updatedAt).isNotNull
    }
}
