package com.nextup.api.dto.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class BattingRecordResponseTest {

    @Test
    fun `should create BattingRecordResponse with all fields`() {
        // given
        val now = Instant.now()

        // when
        val response = BattingRecordResponse(
            id = 1L,
            gamePlayerId = 100L,
            createdAt = now,
            updatedAt = now,
            plateAppearances = 4,
            atBats = 3,
            hits = 2,
            doubles = 1,
            triples = 0,
            homeRuns = 0,
            runs = 1,
            runsBattedIn = 1,
            walks = 1,
            intentionalWalks = 0,
            hitByPitch = 0,
            strikeouts = 1,
            sacrificeBunts = 0,
            sacrificeFlies = 0,
            stolenBases = 0,
            caughtStealing = 0,
            groundedIntoDoublePlays = 0,
            singles = 1,
            totalBases = 3,
            extraBaseHits = 1,
            sacrifices = 0,
            totalWalks = 1,
            battingAverage = "0.667",
            onBasePercentage = "0.750",
            sluggingPercentage = "1.000",
            ops = "1.750",
            stolenBasePercentage = "0.000"
        )

        // then
        assertThat(response.id).isEqualTo(1L)
        assertThat(response.gamePlayerId).isEqualTo(100L)
        assertThat(response.hits).isEqualTo(2)
        assertThat(response.battingAverage).isEqualTo("0.667")
    }

    @Test
    fun `should support data class copy`() {
        // given
        val now = Instant.now()
        val original = BattingRecordResponse(
            id = 1L,
            gamePlayerId = 100L,
            createdAt = now,
            updatedAt = now,
            plateAppearances = 1,
            atBats = 1,
            hits = 1,
            doubles = 0,
            triples = 0,
            homeRuns = 0,
            runs = 0,
            runsBattedIn = 0,
            walks = 0,
            intentionalWalks = 0,
            hitByPitch = 0,
            strikeouts = 0,
            sacrificeBunts = 0,
            sacrificeFlies = 0,
            stolenBases = 0,
            caughtStealing = 0,
            groundedIntoDoublePlays = 0,
            singles = 1,
            totalBases = 1,
            extraBaseHits = 0,
            sacrifices = 0,
            totalWalks = 0,
            battingAverage = "1.000",
            onBasePercentage = "1.000",
            sluggingPercentage = "1.000",
            ops = "2.000",
            stolenBasePercentage = "0.000"
        )

        // when
        val copied = original.copy(hits = 2)

        // then
        assertThat(copied.hits).isEqualTo(2)
        assertThat(original.hits).isEqualTo(1)
    }
}
