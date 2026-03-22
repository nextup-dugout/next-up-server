package com.nextup.core.service.game

import com.nextup.common.exception.FieldingRecordNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FieldingRecordServiceTest {
    private lateinit var fieldingRecordRepository: FieldingRecordRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var fieldingRecordService: FieldingRecordService

    private lateinit var mockGamePlayer: GamePlayer
    private lateinit var mockFieldingRecord: FieldingRecord

    @BeforeEach
    fun setUp() {
        fieldingRecordRepository = mockk()
        gamePlayerRepository = mockk()
        fieldingRecordService =
            FieldingRecordService(
                fieldingRecordRepository = fieldingRecordRepository,
                gamePlayerRepository = gamePlayerRepository,
            )

        mockGamePlayer = mockk(relaxed = true)
        mockFieldingRecord = mockk(relaxed = true)
    }

    @Test
    fun `should create fielding record successfully`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
        every { fieldingRecordRepository.findByGamePlayer(mockGamePlayer) } returns null
        every { fieldingRecordRepository.save(any()) } returns mockFieldingRecord
        every { mockFieldingRecord.id } returns 100L

        // when
        val result = fieldingRecordService.createRecord(gamePlayerId)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(100L)
        verify(exactly = 1) { fieldingRecordRepository.save(any()) }
    }

    @Test
    fun `should throw GamePlayerNotFoundException when GamePlayer not found on create`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns null

        // when & then
        assertThrows<GamePlayerNotFoundException> {
            fieldingRecordService.createRecord(gamePlayerId)
        }
    }

    @Test
    fun `should throw RecordAlreadyExistsException when fielding record already exists`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
        every { fieldingRecordRepository.findByGamePlayer(mockGamePlayer) } returns mockFieldingRecord

        // when & then
        assertThrows<RecordAlreadyExistsException> {
            fieldingRecordService.createRecord(gamePlayerId)
        }
    }

    @Test
    fun `should throw FieldingRecordNotFoundException when record not found on get`() {
        // given
        val gamePlayerId = 99L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns null

        // when & then
        assertThrows<FieldingRecordNotFoundException> {
            fieldingRecordService.getByGamePlayerId(gamePlayerId)
        }
    }

    @Test
    fun `should record put out`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        every { mockFieldingRecord.recordPutOut() } returns Unit

        // when
        fieldingRecordService.recordPutOut(gamePlayerId)

        // then
        verify(exactly = 1) { mockFieldingRecord.recordPutOut() }
    }

    @Test
    fun `should record assist`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        every { mockFieldingRecord.recordAssist() } returns Unit

        // when
        fieldingRecordService.recordAssist(gamePlayerId)

        // then
        verify(exactly = 1) { mockFieldingRecord.recordAssist() }
    }

    @Test
    fun `should record error`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        every { mockFieldingRecord.recordError() } returns Unit

        // when
        fieldingRecordService.recordError(gamePlayerId)

        // then
        verify(exactly = 1) { mockFieldingRecord.recordError() }
    }

    @Test
    fun `should record double play`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        every { mockFieldingRecord.recordDoublePlay() } returns Unit

        // when
        fieldingRecordService.recordDoublePlay(gamePlayerId)

        // then
        verify(exactly = 1) { mockFieldingRecord.recordDoublePlay() }
    }

    @Test
    fun `should record passed ball`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        every { mockFieldingRecord.recordPassedBall() } returns Unit

        // when
        fieldingRecordService.recordPassedBall(gamePlayerId)

        // then
        verify(exactly = 1) { mockFieldingRecord.recordPassedBall() }
    }

    @Test
    fun `should return all fielding records by game id`() {
        // given
        val gameId = 10L
        every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)

        // when
        val result = fieldingRecordService.getAllByGameId(gameId)

        // then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `should return all fielding records by player id`() {
        // given
        val playerId = 5L
        every { fieldingRecordRepository.findAllByPlayerId(playerId) } returns
            listOf(mockFieldingRecord, mockFieldingRecord)

        // when
        val result = fieldingRecordService.getAllByPlayerId(playerId)

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `should get fielding record by gamePlayerId successfully`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        every { mockFieldingRecord.id } returns 100L

        // when
        val result = fieldingRecordService.getByGamePlayerId(gamePlayerId)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(100L)
    }

    @Test
    fun `should return empty list when no fielding records exist for game`() {
        // given
        val gameId = 99L
        every { fieldingRecordRepository.findAllByGameId(gameId) } returns emptyList()

        // when
        val result = fieldingRecordService.getAllByGameId(gameId)

        // then
        assertThat(result).isEmpty()
    }

    @Nested
    @DisplayName("L-1: 포지션별 수비 기록 분리")
    inner class PositionBasedFieldingTest {
        @Test
        fun `기존 포지션 기록이 없으면 새로 생성한다`() {
            // given
            val gamePlayerId = 1L
            val position = Position.SHORTSTOP
            val savedRecord = mockk<FieldingRecord>(relaxed = true)
            every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
            every { fieldingRecordRepository.findByGamePlayerAndPosition(mockGamePlayer, position) } returns null
            val recordSlot = slot<FieldingRecord>()
            every { fieldingRecordRepository.save(capture(recordSlot)) } returns savedRecord

            // when
            val result = fieldingRecordService.getOrCreateByPosition(gamePlayerId, position)

            // then
            assertThat(result).isEqualTo(savedRecord)
            verify(exactly = 1) { fieldingRecordRepository.save(any()) }
        }

        @Test
        fun `기존 포지션 기록이 있으면 해당 기록을 반환한다`() {
            // given
            val gamePlayerId = 1L
            val position = Position.CATCHER
            val existingRecord = mockk<FieldingRecord>(relaxed = true)
            every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
            every {
                fieldingRecordRepository.findByGamePlayerAndPosition(mockGamePlayer, position)
            } returns existingRecord

            // when
            val result = fieldingRecordService.getOrCreateByPosition(gamePlayerId, position)

            // then
            assertThat(result).isEqualTo(existingRecord)
            verify(exactly = 0) { fieldingRecordRepository.save(any()) }
        }

        @Test
        fun `GamePlayer가 없으면 GamePlayerNotFoundException이 발생한다`() {
            // given
            val gamePlayerId = 99L
            every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns null

            // when & then
            assertThrows<GamePlayerNotFoundException> {
                fieldingRecordService.getOrCreateByPosition(gamePlayerId, Position.FIRST_BASE)
            }
        }

        @Test
        fun `getAllByGamePlayer로 모든 포지션별 기록을 조회할 수 있다`() {
            // given
            val gamePlayerId = 1L
            val records = listOf(mockFieldingRecord, mockFieldingRecord)
            every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
            every { fieldingRecordRepository.findAllByGamePlayer(mockGamePlayer) } returns records

            // when
            val result = fieldingRecordService.getAllByGamePlayer(gamePlayerId)

            // then
            assertThat(result).hasSize(2)
        }

        @Test
        fun `getAllByGamePlayer에서 GamePlayer가 없으면 예외가 발생한다`() {
            // given
            val gamePlayerId = 99L
            every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns null

            // when & then
            assertThrows<GamePlayerNotFoundException> {
                fieldingRecordService.getAllByGamePlayer(gamePlayerId)
            }
        }
    }
}
