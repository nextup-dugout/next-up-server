package com.nextup.infrastructure.service.game.correction

import com.nextup.common.exception.BattingRecordNotFoundByIdException
import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.PitchingRecordNotFoundByIdException
import com.nextup.core.domain.audit.AuditLog
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.CorrectionType
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.RecordCorrection
import com.nextup.core.port.repository.AuditLogRepositoryPort
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.RecordCorrectionRepositoryPort
import com.nextup.core.service.game.correction.BattingCorrectionRequest
import com.nextup.core.service.game.correction.PitchingCorrectionRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RecordCorrectionServiceImpl")
class RecordCorrectionServiceImplTest {
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var recordCorrectionRepository: RecordCorrectionRepositoryPort
    private lateinit var auditLogRepository: AuditLogRepositoryPort
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var service: RecordCorrectionServiceImpl

    private val gameId = 1L
    private val recordId = 10L
    private val adminUserId = 100L

    @BeforeEach
    fun setUp() {
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        recordCorrectionRepository = mockk()
        auditLogRepository = mockk()
        gameRepository = mockk()

        service =
            RecordCorrectionServiceImpl(
                battingRecordRepository = battingRecordRepository,
                pitchingRecordRepository = pitchingRecordRepository,
                recordCorrectionRepository = recordCorrectionRepository,
                auditLogRepository = auditLogRepository,
                gameRepository = gameRepository,
            )
    }

    @Nested
    @DisplayName("correctBattingRecord")
    inner class CorrectBattingRecordTest {
        private lateinit var mockGame: Game
        private lateinit var mockGamePlayer: GamePlayer
        private lateinit var mockBattingRecord: BattingRecord
        private lateinit var mockCorrection: RecordCorrection

        @BeforeEach
        fun setUp() {
            mockGame = mockk(relaxed = true)
            mockGamePlayer = mockk(relaxed = true)
            mockBattingRecord = mockk(relaxed = true)
            mockCorrection = mockk(relaxed = true)
        }

        @Test
        fun `타격 기록을 정정하면 정정된 기록이 반환된다`() {
            // given
            val request =
                BattingCorrectionRequest(
                    adminUserId = adminUserId,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "기록원 오류 정정",
                )
            every { gameRepository.findByIdOrNull(gameId) } returns mockGame
            every { battingRecordRepository.findByIdOrNull(recordId) } returns mockBattingRecord
            every { mockBattingRecord.correctField("hits", "3") } returns "2"
            every { recordCorrectionRepository.save(any()) } returns mockCorrection
            every { auditLogRepository.save(any()) } returns mockk()

            // when
            val result = service.correctBattingRecord(gameId, recordId, request)

            // then
            assertThat(result).isNotNull
            verify(exactly = 1) { mockBattingRecord.correctField("hits", "3") }
            verify(exactly = 1) { recordCorrectionRepository.save(any()) }
            verify(exactly = 1) { auditLogRepository.save(any()) }
        }

        @Test
        fun `경기가 존재하지 않으면 GameNotFoundException이 발생한다`() {
            // given
            val request =
                BattingCorrectionRequest(
                    adminUserId = adminUserId,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "정정 사유",
                )
            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy {
                service.correctBattingRecord(gameId, recordId, request)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `타격 기록이 존재하지 않으면 BattingRecordNotFoundByIdException이 발생한다`() {
            // given
            val request =
                BattingCorrectionRequest(
                    adminUserId = adminUserId,
                    fieldName = "hits",
                    newValue = "3",
                    reason = "정정 사유",
                )
            every { gameRepository.findByIdOrNull(gameId) } returns mockGame
            every { battingRecordRepository.findByIdOrNull(recordId) } returns null

            // when & then
            assertThatThrownBy {
                service.correctBattingRecord(gameId, recordId, request)
            }.isInstanceOf(BattingRecordNotFoundByIdException::class.java)
        }
    }

    @Nested
    @DisplayName("correctPitchingRecord")
    inner class CorrectPitchingRecordTest {
        private lateinit var mockGame: Game
        private lateinit var mockGamePlayer: GamePlayer
        private lateinit var mockPitchingRecord: PitchingRecord
        private lateinit var mockCorrection: RecordCorrection

        @BeforeEach
        fun setUp() {
            mockGame = mockk(relaxed = true)
            mockGamePlayer = mockk(relaxed = true)
            mockPitchingRecord = mockk(relaxed = true)
            mockCorrection = mockk(relaxed = true)
        }

        @Test
        fun `투수 기록을 정정하면 정정된 기록이 반환된다`() {
            // given
            val request =
                PitchingCorrectionRequest(
                    adminUserId = adminUserId,
                    fieldName = "strikeouts",
                    newValue = "5",
                    reason = "삼진 기록 정정",
                )
            every { gameRepository.findByIdOrNull(gameId) } returns mockGame
            every { pitchingRecordRepository.findByIdOrNull(recordId) } returns mockPitchingRecord
            every { mockPitchingRecord.correctField("strikeouts", "5") } returns "3"
            every { recordCorrectionRepository.save(any()) } returns mockCorrection
            every { auditLogRepository.save(any()) } returns mockk()

            // when
            val result = service.correctPitchingRecord(gameId, recordId, request)

            // then
            assertThat(result).isNotNull
            verify(exactly = 1) { mockPitchingRecord.correctField("strikeouts", "5") }
            verify(exactly = 1) { recordCorrectionRepository.save(any()) }
            verify(exactly = 1) { auditLogRepository.save(any()) }
        }

        @Test
        fun `경기가 존재하지 않으면 GameNotFoundException이 발생한다`() {
            // given
            val request =
                PitchingCorrectionRequest(
                    adminUserId = adminUserId,
                    fieldName = "strikeouts",
                    newValue = "5",
                    reason = "정정 사유",
                )
            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy {
                service.correctPitchingRecord(gameId, recordId, request)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `투수 기록이 존재하지 않으면 PitchingRecordNotFoundByIdException이 발생한다`() {
            // given
            val request =
                PitchingCorrectionRequest(
                    adminUserId = adminUserId,
                    fieldName = "strikeouts",
                    newValue = "5",
                    reason = "정정 사유",
                )
            every { gameRepository.findByIdOrNull(gameId) } returns mockGame
            every { pitchingRecordRepository.findByIdOrNull(recordId) } returns null

            // when & then
            assertThatThrownBy {
                service.correctPitchingRecord(gameId, recordId, request)
            }.isInstanceOf(PitchingRecordNotFoundByIdException::class.java)
        }
    }

    @Nested
    @DisplayName("getCorrectionHistory")
    inner class GetCorrectionHistoryTest {
        @Test
        fun `정정 이력을 조회하면 DTO 목록이 반환된다`() {
            // given
            val mockGame = mockk<Game>(relaxed = true)
            val mockCorrection1 =
                mockk<RecordCorrection>(relaxed = true).also {
                    every { it.id } returns 1L
                    every { it.gameId } returns gameId
                    every { it.adminUserId } returns adminUserId
                    every { it.correctionType } returns CorrectionType.BATTING
                    every { it.targetRecordId } returns recordId
                    every { it.fieldName } returns "hits"
                    every { it.oldValue } returns "2"
                    every { it.newValue } returns "3"
                    every { it.reason } returns "기록원 오류"
                }

            every { gameRepository.findByIdOrNull(gameId) } returns mockGame
            every { recordCorrectionRepository.findAllByGameId(gameId) } returns listOf(mockCorrection1)

            // when
            val result = service.getCorrectionHistory(gameId)

            // then
            assertThat(result).hasSize(1)
            assertThat(result[0].gameId).isEqualTo(gameId)
            assertThat(result[0].fieldName).isEqualTo("hits")
            assertThat(result[0].oldValue).isEqualTo("2")
            assertThat(result[0].newValue).isEqualTo("3")
        }

        @Test
        fun `경기가 존재하지 않으면 GameNotFoundException이 발생한다`() {
            // given
            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy {
                service.getCorrectionHistory(gameId)
            }.isInstanceOf(GameNotFoundException::class.java)
        }

        @Test
        fun `정정 이력이 없으면 빈 목록이 반환된다`() {
            // given
            val mockGame = mockk<Game>(relaxed = true)
            every { gameRepository.findByIdOrNull(gameId) } returns mockGame
            every { recordCorrectionRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            val result = service.getCorrectionHistory(gameId)

            // then
            assertThat(result).isEmpty()
        }
    }
}
