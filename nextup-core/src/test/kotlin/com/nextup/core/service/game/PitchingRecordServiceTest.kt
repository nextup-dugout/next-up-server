package com.nextup.core.service.game

import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.PitchingRecordNotFoundException
import com.nextup.common.exception.RecordAlreadyExistsException
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PitchingRecordServiceTest {
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var pitchingRecordService: PitchingRecordService

    private lateinit var mockGamePlayer: GamePlayer
    private lateinit var mockPitchingRecord: PitchingRecord

    @BeforeEach
    fun setUp() {
        pitchingRecordRepository = mockk()
        gamePlayerRepository = mockk()
        pitchingRecordService =
            PitchingRecordService(
                pitchingRecordRepository = pitchingRecordRepository,
                gamePlayerRepository = gamePlayerRepository,
            )

        mockGamePlayer = mockk(relaxed = true)
        mockPitchingRecord = mockk(relaxed = true)
    }

    @Test
    fun `should create pitching record for relief pitcher successfully`() {
        // given
        val gamePlayerId = 1L
        val isStartingPitcher = false
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
        every { pitchingRecordRepository.findByGamePlayer(mockGamePlayer) } returns null
        every { pitchingRecordRepository.save(any()) } returns mockPitchingRecord
        every { mockPitchingRecord.id } returns 100L

        // when
        val result = pitchingRecordService.createRecord(gamePlayerId, isStartingPitcher)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(100L)
        verify(exactly = 1) { pitchingRecordRepository.save(any()) }
    }

    @Test
    fun `should create pitching record for starting pitcher successfully`() {
        // given
        val gamePlayerId = 1L
        val isStartingPitcher = true
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
        every { pitchingRecordRepository.findByGamePlayer(mockGamePlayer) } returns null
        every { pitchingRecordRepository.save(any()) } returns mockPitchingRecord
        every { mockPitchingRecord.id } returns 100L

        // when
        val result = pitchingRecordService.createRecord(gamePlayerId, isStartingPitcher)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(100L)
        verify(exactly = 1) { pitchingRecordRepository.save(any()) }
    }

    @Test
    fun `should throw GamePlayerNotFoundException when GamePlayer not found on create`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns null

        // when & then
        assertThrows<GamePlayerNotFoundException> {
            pitchingRecordService.createRecord(gamePlayerId, false)
        }
    }

    @Test
    fun `should throw RecordAlreadyExistsException when pitching record already exists`() {
        // given
        val gamePlayerId = 1L
        every { gamePlayerRepository.findByIdOrNull(gamePlayerId) } returns mockGamePlayer
        every { pitchingRecordRepository.findByGamePlayer(mockGamePlayer) } returns mockPitchingRecord

        // when & then
        assertThrows<RecordAlreadyExistsException> {
            pitchingRecordService.createRecord(gamePlayerId, false)
        }
    }

    @Test
    fun `should get pitching record by GamePlayer ID successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.id } returns 100L

        // when
        val result = pitchingRecordService.getByGamePlayerId(gamePlayerId)

        // then
        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(100L)
    }

    @Test
    fun `should throw PitchingRecordNotFoundException when pitching record not found`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns null

        // when & then
        assertThrows<PitchingRecordNotFoundException> {
            pitchingRecordService.getByGamePlayerId(gamePlayerId)
        }
    }

    @Test
    fun `should record out successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordOut(false) } returns Unit

        // when
        pitchingRecordService.recordOut(gamePlayerId, isStrikeout = false)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordOut(false) }
    }

    @Test
    fun `should record strikeout successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordOut(true) } returns Unit

        // when
        pitchingRecordService.recordOut(gamePlayerId, isStrikeout = true)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordOut(true) }
    }

    @Test
    fun `should record hit successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordHit(false, 0, 0) } returns Unit

        // when
        pitchingRecordService.recordHit(gamePlayerId, isHomeRun = false, runsScored = 0, earnedRuns = 0)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordHit(false, 0, 0) }
    }

    @Test
    fun `should record walk successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordWalk() } returns Unit

        // when
        pitchingRecordService.recordWalk(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordWalk() }
    }

    @Test
    fun `should record hit by pitch successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordHitByPitch() } returns Unit

        // when
        pitchingRecordService.recordHitByPitch(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordHitByPitch() }
    }

    @Test
    fun `should assign win successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.assignWin() } returns Unit

        // when
        pitchingRecordService.assignWin(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.assignWin() }
    }

    @Test
    fun `should assign loss successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.assignLoss() } returns Unit

        // when
        pitchingRecordService.assignLoss(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.assignLoss() }
    }

    @Test
    fun `should assign save successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.assignSave() } returns Unit

        // when
        pitchingRecordService.assignSave(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.assignSave() }
    }

    @Test
    fun `should assign hold successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.assignHold() } returns Unit

        // when
        pitchingRecordService.assignHold(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.assignHold() }
    }

    @Test
    fun `should get starting pitchers by game ID successfully`() {
        // given
        val gameId = 1L
        val mockRecords = listOf(mockPitchingRecord, mockk())
        every { pitchingRecordRepository.findStartingPitchersByGameId(gameId) } returns mockRecords

        // when
        val result = pitchingRecordService.getStartingPitchersByGameId(gameId)

        // then
        assertThat(result).hasSize(2)
    }

    @Test
    fun `should get all pitching records by game ID successfully`() {
        // given
        val gameId = 1L
        val mockRecords = listOf(mockPitchingRecord, mockk(), mockk())
        every { pitchingRecordRepository.findAllByGameId(gameId) } returns mockRecords

        // when
        val result = pitchingRecordService.getAllByGameId(gameId)

        // then
        assertThat(result).hasSize(3)
    }

    @Test
    fun `should get all pitching records by player ID successfully`() {
        // given
        val playerId = 1L
        val mockRecords = listOf(mockPitchingRecord)
        every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns mockRecords

        // when
        val result = pitchingRecordService.getAllByPlayerId(playerId)

        // then
        assertThat(result).hasSize(1)
    }

    @Test
    fun `should record earned run successfully`() {
        // given
        val gamePlayerId = 1L
        val isEarned = true
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordRun(isEarned) } returns Unit

        // when
        pitchingRecordService.recordRun(gamePlayerId, isEarned)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordRun(true) }
    }

    @Test
    fun `should record unearned run successfully`() {
        // given
        val gamePlayerId = 1L
        val isEarned = false
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordRun(isEarned) } returns Unit

        // when
        pitchingRecordService.recordRun(gamePlayerId, isEarned)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordRun(false) }
    }

    @Test
    fun `should record wild pitch successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordWildPitch() } returns Unit

        // when
        pitchingRecordService.recordWildPitch(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordWildPitch() }
    }

    @Test
    fun `should record balk successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordBalk() } returns Unit

        // when
        pitchingRecordService.recordBalk(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordBalk() }
    }

    @Test
    fun `should record pitch count successfully`() {
        // given
        val gamePlayerId = 1L
        val totalPitches = 100
        val strikes = 65
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.recordPitchCount(totalPitches, strikes) } returns Unit

        // when
        pitchingRecordService.recordPitchCount(gamePlayerId, totalPitches, strikes)

        // then
        verify(exactly = 1) { mockPitchingRecord.recordPitchCount(100, 65) }
    }

    @Test
    fun `should assign blown save successfully`() {
        // given
        val gamePlayerId = 1L
        every { pitchingRecordRepository.findByGamePlayerId(gamePlayerId) } returns mockPitchingRecord
        every { mockPitchingRecord.assignBlownSave() } returns Unit

        // when
        pitchingRecordService.assignBlownSave(gamePlayerId)

        // then
        verify(exactly = 1) { mockPitchingRecord.assignBlownSave() }
    }

    @Test
    fun `should get relief pitchers by game ID successfully`() {
        // given
        val gameId = 1L
        val mockRecords = listOf(mockPitchingRecord, mockk(), mockk())
        every { pitchingRecordRepository.findReliefPitchersByGameId(gameId) } returns mockRecords

        // when
        val result = pitchingRecordService.getReliefPitchersByGameId(gameId)

        // then
        assertThat(result).hasSize(3)
    }
}
