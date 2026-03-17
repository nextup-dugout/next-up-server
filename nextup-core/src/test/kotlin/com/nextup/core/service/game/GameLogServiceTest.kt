package com.nextup.core.service.game

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.FieldingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("GameLogService 테스트")
class GameLogServiceTest {
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var fieldingRecordRepository: FieldingRecordRepositoryPort
    private lateinit var gameLogService: GameLogService

    private val playerId = 1L

    @BeforeEach
    fun setUp() {
        gamePlayerRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        fieldingRecordRepository = mockk()
        gameLogService =
            GameLogService(
                gamePlayerRepository = gamePlayerRepository,
                battingRecordRepository = battingRecordRepository,
                pitchingRecordRepository = pitchingRecordRepository,
                fieldingRecordRepository = fieldingRecordRepository,
            )
    }

    @Test
    fun `should return empty list when player has no games`() {
        // given
        every { gamePlayerRepository.findAllByPlayerId(playerId) } returns emptyList()

        // when
        val result = gameLogService.getGameLog(playerId)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `should return game log entries with batting records`() {
        // given
        val game =
            mockk<Game> {
                every { scheduledAt } returns LocalDateTime.of(2025, 6, 15, 14, 0)
            }
        val gameTeam =
            mockk<GameTeam> {
                every { this@mockk.game } returns game
            }
        val gamePlayer =
            mockk<GamePlayer> {
                every { id } returns 10L
                every { this@mockk.gameTeam } returns gameTeam
            }
        val battingRecord =
            mockk<BattingRecord> {
                every { this@mockk.gamePlayer } returns gamePlayer
            }

        every { gamePlayerRepository.findAllByPlayerId(playerId) } returns listOf(gamePlayer)
        every { battingRecordRepository.findAllByPlayerId(playerId) } returns listOf(battingRecord)
        every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { fieldingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()

        // when
        val result = gameLogService.getGameLog(playerId)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0].gamePlayer).isEqualTo(gamePlayer)
        assertThat(result[0].battingRecord).isEqualTo(battingRecord)
        assertThat(result[0].pitchingRecord).isNull()
        assertThat(result[0].fieldingRecord).isNull()
    }

    @Test
    fun `should limit number of entries returned`() {
        // given
        val gamePlayers =
            (1..5).map { i ->
                val game =
                    mockk<Game> {
                        every { scheduledAt } returns LocalDateTime.of(2025, 6, i, 14, 0)
                    }
                val gameTeam =
                    mockk<GameTeam> {
                        every { this@mockk.game } returns game
                    }
                mockk<GamePlayer> {
                    every { id } returns i.toLong()
                    every { this@mockk.gameTeam } returns gameTeam
                }
            }

        every { gamePlayerRepository.findAllByPlayerId(playerId) } returns gamePlayers
        every { battingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { fieldingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()

        // when
        val result = gameLogService.getGameLog(playerId, limit = 3)

        // then
        assertThat(result).hasSize(3)
    }

    @Test
    fun `should sort entries by game date descending`() {
        // given
        val game1 =
            mockk<Game> {
                every { scheduledAt } returns LocalDateTime.of(2025, 6, 1, 14, 0)
            }
        val game2 =
            mockk<Game> {
                every { scheduledAt } returns LocalDateTime.of(2025, 6, 15, 14, 0)
            }
        val gameTeam1 =
            mockk<GameTeam> {
                every { game } returns game1
            }
        val gameTeam2 =
            mockk<GameTeam> {
                every { game } returns game2
            }
        val gp1 =
            mockk<GamePlayer> {
                every { id } returns 1L
                every { gameTeam } returns gameTeam1
            }
        val gp2 =
            mockk<GamePlayer> {
                every { id } returns 2L
                every { gameTeam } returns gameTeam2
            }

        every { gamePlayerRepository.findAllByPlayerId(playerId) } returns listOf(gp1, gp2)
        every { battingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
        every { fieldingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()

        // when
        val result = gameLogService.getGameLog(playerId)

        // then
        assertThat(result).hasSize(2)
        // Most recent game (June 15) should come first
        assertThat(result[0].gamePlayer).isEqualTo(gp2)
        assertThat(result[1].gamePlayer).isEqualTo(gp1)
    }
}
