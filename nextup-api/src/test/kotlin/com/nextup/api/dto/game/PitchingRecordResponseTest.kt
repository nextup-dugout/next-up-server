package com.nextup.api.dto.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class PitchingRecordResponseTest {

    @Test
    fun `should create PitchingRecordResponse with all fields`() {
        // given
        val now = Instant.now()

        // when
        val response = PitchingRecordResponse(
            id = 1L,
            gamePlayerId = 200L,
            createdAt = now,
            updatedAt = now,
            inningsPitchedOuts = 18,
            earnedRuns = 2,
            runsAllowed = 2,
            hitsAllowed = 5,
            walksAllowed = 2,
            strikeouts = 6,
            homeRunsAllowed = 0,
            hitBatsmen = 0,
            wildPitches = 0,
            balks = 0,
            battersFaced = 25,
            decision = "WIN",
            isStartingPitcher = true,
            pitchesThrown = 90,
            strikesThrown = 60,
            completeInnings = 6,
            remainingOuts = 0,
            inningsPitched = "6.00",
            inningsPitchedDisplay = "6.0",
            earnedRunAverage = "3.00",
            whip = "1.17",
            strikeoutsPer9 = "9.00",
            walksPer9 = "3.00",
            strikeoutToWalkRatio = "3.00",
            strikePercentage = "0.667",
            unearnedRuns = 0,
            isQualifiedForWin = true
        )

        // then
        assertThat(response.id).isEqualTo(1L)
        assertThat(response.gamePlayerId).isEqualTo(200L)
        assertThat(response.decision).isEqualTo("WIN")
        assertThat(response.earnedRunAverage).isEqualTo("3.00")
    }

    @Test
    fun `should support nullable fields`() {
        // given
        val now = Instant.now()

        // when
        val response = PitchingRecordResponse(
            id = 1L,
            gamePlayerId = 200L,
            createdAt = now,
            updatedAt = now,
            inningsPitchedOuts = 0,
            earnedRuns = 0,
            runsAllowed = 0,
            hitsAllowed = 0,
            walksAllowed = 0,
            strikeouts = 0,
            homeRunsAllowed = 0,
            hitBatsmen = 0,
            wildPitches = 0,
            balks = 0,
            battersFaced = 0,
            decision = "NONE",
            isStartingPitcher = false,
            pitchesThrown = null,
            strikesThrown = null,
            completeInnings = 0,
            remainingOuts = 0,
            inningsPitched = "0.00",
            inningsPitchedDisplay = "0.0",
            earnedRunAverage = "0.00",
            whip = "0.00",
            strikeoutsPer9 = "0.00",
            walksPer9 = "0.00",
            strikeoutToWalkRatio = "0.00",
            strikePercentage = null,
            unearnedRuns = 0,
            isQualifiedForWin = false
        )

        // then
        assertThat(response.pitchesThrown).isNull()
        assertThat(response.strikesThrown).isNull()
        assertThat(response.strikePercentage).isNull()
    }
}
