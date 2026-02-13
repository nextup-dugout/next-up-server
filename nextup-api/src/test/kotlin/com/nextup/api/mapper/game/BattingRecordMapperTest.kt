package com.nextup.api.mapper.game

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PlateAppearanceResult
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant

class BattingRecordMapperTest {
    @Test
    fun `should map BattingRecord to BattingRecordResponse`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 123L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                recordPlateAppearance(PlateAppearanceResult.SINGLE, 1, true)
                recordPlateAppearance(PlateAppearanceResult.HOME_RUN, 2, true)
            }

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.gamePlayerId).isEqualTo(123L)
        assertThat(response.plateAppearances).isEqualTo(2)
        assertThat(response.atBats).isEqualTo(2)
        assertThat(response.hits).isEqualTo(2)
        assertThat(response.singles).isEqualTo(1)
        assertThat(response.homeRuns).isEqualTo(1)
        assertThat(response.runs).isEqualTo(2)
        assertThat(response.runsBattedIn).isEqualTo(3)
        assertThat(response.totalBases).isEqualTo(5) // 1 + 4
    }

    @Test
    fun `should format batting average correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                repeat(3) {
                    recordPlateAppearance(PlateAppearanceResult.SINGLE)
                }
                repeat(2) {
                    recordPlateAppearance(PlateAppearanceResult.GROUND_OUT)
                }
            }

        // when
        val response = battingRecord.toResponse()

        // then: 3 hits / 5 at-bats = 0.600
        assertThat(response.battingAverage).isEqualTo("0.600")
        assertThat(response.battingAverage).matches("\\d\\.\\d{3}")
    }

    @Test
    fun `should return zero batting average when at-bats is zero`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                recordPlateAppearance(PlateAppearanceResult.WALK)
            }

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.atBats).isEqualTo(0)
        assertThat(response.battingAverage).isEqualTo("0.000")
    }

    @Test
    fun `should calculate OPS correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                // 2 hits (1 single, 1 home run) in 3 at-bats, 1 walk
                recordPlateAppearance(PlateAppearanceResult.SINGLE)
                recordPlateAppearance(PlateAppearanceResult.HOME_RUN)
                recordPlateAppearance(PlateAppearanceResult.GROUND_OUT)
                recordPlateAppearance(PlateAppearanceResult.WALK)
            }

        // when
        val response = battingRecord.toResponse()

        // then
        // AVG = 2/3 = 0.667
        // OBP = (2+1) / (3+1) = 0.750
        // SLG = (1+4) / 3 = 1.667
        // OPS = 0.750 + 1.667 = 2.417
        assertThat(response.battingAverage).isEqualTo("0.667")
        assertThat(response.onBasePercentage).isEqualTo("0.750")
        assertThat(response.sluggingPercentage).isEqualTo("1.667")
        assertThat(response.ops).isEqualTo("2.417")
    }

    @Test
    fun `should calculate stolen base percentage correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                repeat(3) { recordStolenBase() }
                repeat(1) { recordCaughtStealing() }
            }

        // when
        val response = battingRecord.toResponse()

        // then: 3 / (3+1) = 0.750
        assertThat(response.stolenBases).isEqualTo(3)
        assertThat(response.caughtStealing).isEqualTo(1)
        assertThat(response.stolenBasePercentage).isEqualTo("0.750")
    }

    @Test
    fun `should return zero stolen base percentage when no attempts`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord = BattingRecord.create(gamePlayer)

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.stolenBases).isEqualTo(0)
        assertThat(response.caughtStealing).isEqualTo(0)
        assertThat(response.stolenBasePercentage).isEqualTo("0.000")
    }

    @Test
    fun `should map all calculated properties correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                recordPlateAppearance(PlateAppearanceResult.SINGLE)
                recordPlateAppearance(PlateAppearanceResult.DOUBLE)
                recordPlateAppearance(PlateAppearanceResult.TRIPLE)
                recordPlateAppearance(PlateAppearanceResult.HOME_RUN)
            }

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.singles).isEqualTo(1)
        assertThat(response.doubles).isEqualTo(1)
        assertThat(response.triples).isEqualTo(1)
        assertThat(response.homeRuns).isEqualTo(1)
        assertThat(response.extraBaseHits).isEqualTo(3) // 2B + 3B + HR
        assertThat(response.totalBases).isEqualTo(10) // 1 + 2 + 3 + 4
    }

    @Test
    fun `should map walks and intentional walks correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                recordPlateAppearance(PlateAppearanceResult.WALK)
                recordPlateAppearance(PlateAppearanceResult.WALK)
                recordPlateAppearance(PlateAppearanceResult.INTENTIONAL_WALK)
            }

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.walks).isEqualTo(2)
        assertThat(response.intentionalWalks).isEqualTo(1)
        assertThat(response.totalWalks).isEqualTo(3)
    }

    @Test
    fun `should map sacrifices correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                recordPlateAppearance(PlateAppearanceResult.SACRIFICE_BUNT)
                recordPlateAppearance(PlateAppearanceResult.SACRIFICE_FLY)
            }

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.sacrificeBunts).isEqualTo(1)
        assertThat(response.sacrificeFlies).isEqualTo(1)
        assertThat(response.sacrifices).isEqualTo(2)
    }

    @Test
    fun `should map list of BattingRecords to list of responses`() {
        // given
        val gamePlayer1 = mockk<GamePlayer>()
        val gamePlayer2 = mockk<GamePlayer>()
        every { gamePlayer1.id } returns 1L
        every { gamePlayer2.id } returns 2L

        val records =
            listOf(
                BattingRecord.create(gamePlayer1).apply {
                    recordPlateAppearance(PlateAppearanceResult.SINGLE)
                },
                BattingRecord.create(gamePlayer2).apply {
                    recordPlateAppearance(PlateAppearanceResult.HOME_RUN)
                },
            )

        // when
        val responses = records.toResponse()

        // then
        assertThat(responses).hasSize(2)
        assertThat(responses[0].gamePlayerId).isEqualTo(1L)
        assertThat(responses[0].hits).isEqualTo(1)
        assertThat(responses[1].gamePlayerId).isEqualTo(2L)
        assertThat(responses[1].homeRuns).isEqualTo(1)
    }

    @Test
    fun `should map meta fields correctly`() {
        // given
        val now = Instant.now()
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 999L

        val battingRecord = BattingRecord.create(gamePlayer)

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.id).isEqualTo(0L) // Entity not persisted yet
        assertThat(response.gamePlayerId).isEqualTo(999L)
        assertThat(response.createdAt).isNotNull
        assertThat(response.updatedAt).isNotNull
    }

    @Test
    fun `should map strikeouts and other outs correctly`() {
        // given
        val gamePlayer = mockk<GamePlayer>()
        every { gamePlayer.id } returns 1L

        val battingRecord =
            BattingRecord.create(gamePlayer).apply {
                recordPlateAppearance(PlateAppearanceResult.STRIKEOUT)
                recordPlateAppearance(PlateAppearanceResult.GROUND_OUT)
                recordPlateAppearance(PlateAppearanceResult.DOUBLE_PLAY)
            }

        // when
        val response = battingRecord.toResponse()

        // then
        assertThat(response.strikeouts).isEqualTo(1)
        assertThat(response.groundedIntoDoublePlays).isEqualTo(1)
        assertThat(response.atBats).isEqualTo(3)
        assertThat(response.hits).isEqualTo(0)
    }
}
