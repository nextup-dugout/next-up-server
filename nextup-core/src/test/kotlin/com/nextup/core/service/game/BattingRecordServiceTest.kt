package com.nextup.core.service.game

import com.nextup.common.exception.BattingRecordNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BattingRecordServiceTest {
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var battingRecordService: BattingRecordService

    private lateinit var mockGamePlayer: GamePlayer
    private lateinit var mockBattingRecord: BattingRecord

    @BeforeEach
    fun setUp() {
        battingRecordRepository = mockk()
        gamePlayerRepository = mockk()
        battingRecordService =
            BattingRecordService(
                battingRecordRepository = battingRecordRepository,
                gamePlayerRepository = gamePlayerRepository,
            )

        mockGamePlayer = mockk(relaxed = true)
        mockBattingRecord = mockk(relaxed = true)
    }

    @Test
    fun `should create batting record successfully`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
        every { battingRecordRepository.findByGamePlayer(mockGamePlayer) } returns null
        every { battingRecordRepository.save(any()) } returns mockBattingRecord
        every { mockBattingRecord.id } returns 100L

        // when
        val result = battingRecordService.createRecord(gamePlayerId)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(100L)
        verify(exactly = 1) { battingRecordRepository.save(any()) }
    }

    @Test
    fun `should throw GamePlayerNotFoundException when GamePlayer not found on create`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns null

        // when & then
        assertThrows<GamePlayerNotFoundException> {
            battingRecordService.createRecord(gamePlayerId)
        }
    }

    @Test
    fun `should throw RecordAlreadyExistsException when batting record already exists`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
        every { battingRecordRepository.findByGamePlayer(mockGamePlayer) } returns mockBattingRecord

        // when & then
        assertThrows<RecordAlreadyExistsException> {
            battingRecordService.createRecord(gamePlayerId)
        }
    }

    @Test
    fun `should get batting record by GamePlayer ID successfully`() {
        // given
        val gamePlayerId = 1L
        every { battingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockBattingRecord
        every { mockBattingRecord.id } returns 100L

        // when
        val result = battingRecordService.getByGamePlayerId(gamePlayerId)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(100L)
    }

    @Test
    fun `should throw BattingRecordNotFoundException when batting record not found`() {
        // given
        val gamePlayerId = 1L
        every { battingRecordRepository.findByGamePlayerId(gamePlayerId) } returns null

        // when & then
        assertThrows<BattingRecordNotFoundException> {
            battingRecordService.getByGamePlayerId(gamePlayerId)
        }
    }

    @Test
    fun `should record plate appearance successfully`() {
        // given
        val gamePlayerId = 1L
        val result = PlateAppearanceResult.SINGLE
        val runsBattedIn = 1
        val runsScored = true

        every { battingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockBattingRecord
        every { mockBattingRecord.applyPlateAppearanceResult(result, runsBattedIn) } returns Unit
        every { mockBattingRecord.recordRun() } returns Unit

        // when
        battingRecordService.recordPlateAppearance(gamePlayerId, result, runsBattedIn, runsScored)

        // then
        verify(exactly = 1) { mockBattingRecord.applyPlateAppearanceResult(result, runsBattedIn) }
        verify(exactly = 1) { mockBattingRecord.recordRun() }
    }

    @Test
    fun `should record plate appearance without run when runsScored is false`() {
        // given
        val gamePlayerId = 1L
        val result = PlateAppearanceResult.SINGLE
        val runsBattedIn = 0
        val runsScored = false

        every { battingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockBattingRecord
        every { mockBattingRecord.applyPlateAppearanceResult(result, runsBattedIn) } returns Unit

        // when
        battingRecordService.recordPlateAppearance(gamePlayerId, result, runsBattedIn, runsScored)

        // then
        verify(exactly = 1) { mockBattingRecord.applyPlateAppearanceResult(result, runsBattedIn) }
        verify(exactly = 0) { mockBattingRecord.recordRun() }
    }

    @Test
    fun `should record stolen base successfully`() {
        // given
        val gamePlayerId = 1L
        every { battingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockBattingRecord
        every { mockBattingRecord.recordStolenBase() } returns Unit

        // when
        battingRecordService.recordStolenBase(gamePlayerId)

        // then
        verify(exactly = 1) { mockBattingRecord.recordStolenBase() }
    }

    @Test
    fun `should record caught stealing successfully`() {
        // given
        val gamePlayerId = 1L
        every { battingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockBattingRecord
        every { mockBattingRecord.recordCaughtStealing() } returns Unit

        // when
        battingRecordService.recordCaughtStealing(gamePlayerId)

        // then
        verify(exactly = 1) { mockBattingRecord.recordCaughtStealing() }
    }

    @Test
    fun `should record run successfully`() {
        // given
        val gamePlayerId = 1L
        every { battingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockBattingRecord
        every { mockBattingRecord.recordRun() } returns Unit

        // when
        battingRecordService.recordRun(gamePlayerId)

        // then
        verify(exactly = 1) { mockBattingRecord.recordRun() }
    }

    @Test
    fun `should get all batting records by game ID successfully`() {
        // given
        val gameId = 1L
        val mockRecords = listOf(mockBattingRecord, mockk())
        every { battingRecordRepository.findAllByGameId(gameId) } returns mockRecords

        // when
        val result = battingRecordService.getAllByGameId(gameId)

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `should get all batting records by player ID successfully`() {
        // given
        val playerId = 1L
        val mockRecords = listOf(mockBattingRecord, mockk())
        every { battingRecordRepository.findAllByPlayerId(playerId) } returns mockRecords

        // when
        val result = battingRecordService.getAllByPlayerId(playerId)

        // then
        assertThat(result).hasSize(2)
    }
}
