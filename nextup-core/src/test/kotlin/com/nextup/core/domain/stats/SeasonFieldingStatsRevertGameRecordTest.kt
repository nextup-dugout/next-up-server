package com.nextup.core.domain.stats

import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SeasonFieldingStats.revertGameRecord 테스트")
class SeasonFieldingStatsRevertGameRecordTest {
    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private fun createMockFieldingRecord(
        putOuts: Int = 0,
        assists: Int = 0,
        errors: Int = 0,
        doublePlays: Int = 0,
        passedBalls: Int = 0,
    ): FieldingRecord {
        val record = mockk<FieldingRecord>()
        every { record.putOuts } returns putOuts
        every { record.assists } returns assists
        every { record.errors } returns errors
        every { record.doublePlays } returns doublePlays
        every { record.passedBalls } returns passedBalls
        return record
    }

    @Nested
    @DisplayName("경기 수비 기여분 롤백")
    inner class RevertGameRecord {
        private lateinit var stats: SeasonFieldingStats

        @BeforeEach
        fun setUp() {
            stats = SeasonFieldingStats.create(testPlayer, 2024)
        }

        @Test
        fun `경기 기록이 반영된 시즌 통계에서 정확히 차감됨`() {
            // given: 2경기 기록이 반영된 시즌 통계
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record1 = FieldingRecord(gamePlayer)
            record1.recordPutOut()
            record1.recordPutOut()
            record1.recordAssist()
            record1.recordError()

            val record2 = FieldingRecord(gamePlayer)
            record2.recordPutOut()
            record2.recordDoublePlay()
            record2.recordPassedBall()

            stats.addGameRecord(record1)
            stats.addGameRecord(record2)

            assertThat(stats.gamesPlayed).isEqualTo(2)
            assertThat(stats.putOuts).isEqualTo(3)
            assertThat(stats.assists).isEqualTo(1)
            assertThat(stats.errors).isEqualTo(1)

            // when: 첫 번째 경기 기록 롤백
            stats.revertGameRecord(record1)

            // then: 두 번째 경기 기록만 남음
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.putOuts).isEqualTo(1)
            assertThat(stats.assists).isZero
            assertThat(stats.errors).isZero
            assertThat(stats.doublePlays).isEqualTo(1)
            assertThat(stats.passedBalls).isEqualTo(1)
        }

        @Test
        fun `gamesPlayed가 1 감소함`() {
            // given
            val gamePlayer = mockk<GamePlayer>(relaxed = true)
            val record = FieldingRecord(gamePlayer)
            stats.addGameRecord(record)
            assertThat(stats.gamesPlayed).isEqualTo(1)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isZero
        }

        @Test
        fun `모든 수비 필드가 정확히 차감됨`() {
            // given
            val record =
                createMockFieldingRecord(
                    putOuts = 3,
                    assists = 2,
                    errors = 1,
                    doublePlays = 1,
                    passedBalls = 1,
                )

            stats.addGameRecord(record)
            assertThat(stats.putOuts).isEqualTo(3)
            assertThat(stats.assists).isEqualTo(2)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.putOuts).isZero
            assertThat(stats.assists).isZero
            assertThat(stats.errors).isZero
            assertThat(stats.doublePlays).isZero
            assertThat(stats.passedBalls).isZero
        }
    }
}
