package com.nextup.core.service.game

import com.nextup.common.exception.FieldingRecordNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
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
    fun `should return all fielding records by game id`() {
        // given
        val gameId = 10L
        every { fieldingRecordRepository.findAllByGameId(gameId) } returns listOf(mockFieldingRecord)

        // when
        val result = fieldingRecordService.getAllByGameId(gameId)

        // then
        assertThat(result).hasSize(1)
    }
}
