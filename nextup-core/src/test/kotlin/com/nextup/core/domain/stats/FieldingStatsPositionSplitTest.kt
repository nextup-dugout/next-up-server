package com.nextup.core.domain.stats

import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("L-1: 포지션별 수비 기록 분리 — 통계 집계")
class FieldingStatsPositionSplitTest {
    private lateinit var player: Player
    private lateinit var gamePlayer: GamePlayer

    @BeforeEach
    fun setup() {
        player = mockk<Player>(relaxed = true)
        every { player.id } returns 1L
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.player } returns player
    }

    private fun createFieldingRecordWithStats(
        position: Position,
        putOuts: Int = 0,
        assists: Int = 0,
        errors: Int = 0,
    ): FieldingRecord {
        val record = FieldingRecord.create(gamePlayer, position)
        repeat(putOuts) { record.recordPutOut() }
        repeat(assists) { record.recordAssist() }
        repeat(errors) { record.recordError() }
        return record
    }

    @Nested
    @DisplayName("SeasonFieldingStats.addGameRecords")
    inner class SeasonFieldingStatsAddGameRecordsTest {
        @Test
        fun `여러 포지션 기록을 한꺼번에 추가하면 gamesPlayed는 1만 증가한다`() {
            // given
            val seasonStats = SeasonFieldingStats.create(player, 2026)
            val ssRecord = createFieldingRecordWithStats(Position.SHORTSTOP, putOuts = 3, assists = 2)
            val thirdBaseRecord =
                createFieldingRecordWithStats(Position.THIRD_BASE, putOuts = 1, assists = 1, errors = 1)

            // when
            seasonStats.addGameRecords(listOf(ssRecord, thirdBaseRecord))

            // then
            assertThat(seasonStats.gamesPlayed).isEqualTo(1)
            assertThat(seasonStats.putOuts).isEqualTo(4) // 3 + 1
            assertThat(seasonStats.assists).isEqualTo(3) // 2 + 1
            assertThat(seasonStats.errors).isEqualTo(1) // 0 + 1
        }

        @Test
        fun `단일 포지션 기록도 addGameRecords로 추가할 수 있다`() {
            // given
            val seasonStats = SeasonFieldingStats.create(player, 2026)
            val record = createFieldingRecordWithStats(Position.CATCHER, putOuts = 5)

            // when
            seasonStats.addGameRecords(listOf(record))

            // then
            assertThat(seasonStats.gamesPlayed).isEqualTo(1)
            assertThat(seasonStats.putOuts).isEqualTo(5)
        }

        @Test
        fun `빈 목록으로 addGameRecords를 호출하면 예외가 발생한다`() {
            // given
            val seasonStats = SeasonFieldingStats.create(player, 2026)

            // when & then
            assertThatThrownBy { seasonStats.addGameRecords(emptyList()) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("수비 기록 목록이 비어있습니다")
        }

        @Test
        fun `여러 경기의 포지션별 기록을 누적하면 gamesPlayed가 경기 수만큼 증가한다`() {
            // given
            val seasonStats = SeasonFieldingStats.create(player, 2026)

            // game 1: SS + 3B
            val game1Records =
                listOf(
                    createFieldingRecordWithStats(Position.SHORTSTOP, putOuts = 2),
                    createFieldingRecordWithStats(Position.THIRD_BASE, putOuts = 1),
                )

            // game 2: SS only
            val game2Records =
                listOf(
                    createFieldingRecordWithStats(Position.SHORTSTOP, putOuts = 3),
                )

            // when
            seasonStats.addGameRecords(game1Records)
            seasonStats.addGameRecords(game2Records)

            // then
            assertThat(seasonStats.gamesPlayed).isEqualTo(2)
            assertThat(seasonStats.putOuts).isEqualTo(6) // 2 + 1 + 3
        }

        @Test
        fun `기존 addGameRecord와 새 addGameRecords를 혼합 사용할 수 있다`() {
            // given
            val seasonStats = SeasonFieldingStats.create(player, 2026)
            val singleRecord = createFieldingRecordWithStats(Position.FIRST_BASE, putOuts = 10)
            val multiRecords =
                listOf(
                    createFieldingRecordWithStats(Position.SHORTSTOP, assists = 3),
                    createFieldingRecordWithStats(Position.SECOND_BASE, assists = 2),
                )

            // when
            seasonStats.addGameRecord(singleRecord) // 기존 메서드
            seasonStats.addGameRecords(multiRecords) // 새 메서드

            // then
            assertThat(seasonStats.gamesPlayed).isEqualTo(2)
            assertThat(seasonStats.putOuts).isEqualTo(10)
            assertThat(seasonStats.assists).isEqualTo(5)
        }
    }

    @Nested
    @DisplayName("CareerFieldingStats.addGameRecords")
    inner class CareerFieldingStatsAddGameRecordsTest {
        @Test
        fun `여러 포지션 기록을 한꺼번에 추가하면 gamesPlayed는 1만 증가한다`() {
            // given
            val careerStats = CareerFieldingStats.create(player)
            val ssRecord = createFieldingRecordWithStats(Position.SHORTSTOP, putOuts = 4, assists = 3)
            val catcherRecord =
                createFieldingRecordWithStats(Position.CATCHER, putOuts = 8, errors = 1)

            // when
            careerStats.addGameRecords(listOf(ssRecord, catcherRecord))

            // then
            assertThat(careerStats.gamesPlayed).isEqualTo(1)
            assertThat(careerStats.putOuts).isEqualTo(12) // 4 + 8
            assertThat(careerStats.assists).isEqualTo(3)
            assertThat(careerStats.errors).isEqualTo(1)
        }

        @Test
        fun `빈 목록으로 addGameRecords를 호출하면 예외가 발생한다`() {
            // given
            val careerStats = CareerFieldingStats.create(player)

            // when & then
            assertThatThrownBy { careerStats.addGameRecords(emptyList()) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("수비 기록 목록이 비어있습니다")
        }
    }

    @Nested
    @DisplayName("FieldingRecord 포지션별 생성")
    inner class FieldingRecordPositionCreateTest {
        @Test
        fun `같은 GamePlayer에 대해 서로 다른 포지션의 FieldingRecord를 생성할 수 있다`() {
            // when
            val ssRecord = FieldingRecord.create(gamePlayer, Position.SHORTSTOP)
            val thirdBaseRecord = FieldingRecord.create(gamePlayer, Position.THIRD_BASE)

            // then
            assertThat(ssRecord.position).isEqualTo(Position.SHORTSTOP)
            assertThat(thirdBaseRecord.position).isEqualTo(Position.THIRD_BASE)
            assertThat(ssRecord.gamePlayer).isEqualTo(thirdBaseRecord.gamePlayer)
        }

        @Test
        fun `포지션별 FieldingRecord는 독립적으로 기록을 관리한다`() {
            // given
            val ssRecord = FieldingRecord.create(gamePlayer, Position.SHORTSTOP)
            val catcherRecord = FieldingRecord.create(gamePlayer, Position.CATCHER)

            // when
            ssRecord.recordAssist()
            ssRecord.recordAssist()
            catcherRecord.recordPutOut()
            catcherRecord.recordPassedBall()

            // then
            assertThat(ssRecord.assists).isEqualTo(2)
            assertThat(ssRecord.putOuts).isEqualTo(0)
            assertThat(catcherRecord.putOuts).isEqualTo(1)
            assertThat(catcherRecord.passedBalls).isEqualTo(1)
            assertThat(catcherRecord.assists).isEqualTo(0)
        }
    }
}
