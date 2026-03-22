package com.nextup.core.service.game

import com.nextup.common.exception.FieldingRecordNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.event.FieldingEventType
import com.nextup.core.domain.event.FieldingRecordUpdatedEvent
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.context.ApplicationEventPublisher

class FieldingRecordServiceTest {
    private lateinit var fieldingRecordRepository: FieldingRecordRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var eventPublisher: ApplicationEventPublisher
    private lateinit var fieldingRecordService: FieldingRecordService

    private lateinit var mockGamePlayer: GamePlayer
    private lateinit var mockFieldingRecord: FieldingRecord

    @BeforeEach
    fun setUp() {
        fieldingRecordRepository = mockk()
        gamePlayerRepository = mockk()
        eventPublisher = mockk(relaxed = true)
        fieldingRecordService =
            FieldingRecordService(
                fieldingRecordRepository = fieldingRecordRepository,
                gamePlayerRepository = gamePlayerRepository,
                eventPublisher = eventPublisher,
            )

        mockGamePlayer = mockk(relaxed = true)
        mockFieldingRecord = mockk(relaxed = true)

        // L-6: 이벤트 발행을 위한 GamePlayer → GameTeam → Game 체인 설정
        every { mockFieldingRecord.gamePlayer.gameTeam.game.id } returns 10L
        every { mockFieldingRecord.gamePlayer.player.id } returns 1L
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

    // L-6: 수비 기록 변경 시 FieldingRecordUpdatedEvent 발행 검증

    @Test
    fun `recordPutOut should publish FieldingRecordUpdatedEvent with PUT_OUT type`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        val eventSlot = slot<FieldingRecordUpdatedEvent>()

        // when
        fieldingRecordService.recordPutOut(gamePlayerId)

        // then
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        assertThat(eventSlot.captured.type).isEqualTo(FieldingEventType.PUT_OUT)
        assertThat(eventSlot.captured.gameId).isEqualTo(10L)
        assertThat(eventSlot.captured.playerId).isEqualTo(1L)
        assertThat(eventSlot.captured.isRevert).isFalse()
    }

    @Test
    fun `recordAssist should publish FieldingRecordUpdatedEvent with ASSIST type`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        val eventSlot = slot<FieldingRecordUpdatedEvent>()

        // when
        fieldingRecordService.recordAssist(gamePlayerId)

        // then
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        assertThat(eventSlot.captured.type).isEqualTo(FieldingEventType.ASSIST)
    }

    @Test
    fun `recordError should publish FieldingRecordUpdatedEvent with ERROR type`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        val eventSlot = slot<FieldingRecordUpdatedEvent>()

        // when
        fieldingRecordService.recordError(gamePlayerId)

        // then
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        assertThat(eventSlot.captured.type).isEqualTo(FieldingEventType.ERROR)
    }

    @Test
    fun `recordDoublePlay should publish FieldingRecordUpdatedEvent with DOUBLE_PLAY type`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        val eventSlot = slot<FieldingRecordUpdatedEvent>()

        // when
        fieldingRecordService.recordDoublePlay(gamePlayerId)

        // then
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        assertThat(eventSlot.captured.type).isEqualTo(FieldingEventType.DOUBLE_PLAY)
    }

    @Test
    fun `recordPassedBall should publish FieldingRecordUpdatedEvent with PASSED_BALL type`() {
        // given
        val gamePlayerId = 1L
        every { fieldingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockFieldingRecord
        val eventSlot = slot<FieldingRecordUpdatedEvent>()

        // when
        fieldingRecordService.recordPassedBall(gamePlayerId)

        // then
        verify { eventPublisher.publishEvent(capture(eventSlot)) }
        assertThat(eventSlot.captured.type).isEqualTo(FieldingEventType.PASSED_BALL)
    }
}
