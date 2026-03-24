package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.common.exception.GamePlayerNotFoundException
import com.nextup.common.exception.InvalidGameStateException
import com.nextup.core.domain.game.Base
import com.nextup.core.domain.game.EjectionReason
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameEvent
import com.nextup.core.domain.game.GameEventType
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameState
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.PositionCategory
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameEventRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.service.game.dto.EjectAndSubstituteRequest
import com.nextup.core.service.game.dto.EjectionRequest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("EmergencySubstitutionServiceImpl")
class EmergencySubstitutionServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameEventRepository: GameEventRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var service: EmergencySubstitutionServiceImpl

    private val scorerId = 99L
    private val gameId = 1L

    @BeforeEach
    fun setUp() {
        gameRepository = mockk(relaxed = true)
        gamePlayerRepository = mockk(relaxed = true)
        gameEventRepository = mockk(relaxed = true)
        battingRecordRepository = mockk(relaxed = true)
        pitchingRecordRepository = mockk(relaxed = true)

        service =
            EmergencySubstitutionServiceImpl(
                gameRepository = gameRepository,
                gamePlayerRepository = gamePlayerRepository,
                gameEventRepository = gameEventRepository,
                battingRecordRepository = battingRecordRepository,
                pitchingRecordRepository = pitchingRecordRepository,
            )
    }

    @Nested
    @DisplayName("ejectPlayer()")
    inner class EjectPlayerTest {
        @Test
        fun `부상 퇴장을 처리한다`() {
            // given
            val game = createMockGame(GameStatus.IN_PROGRESS)
            val ejectedPlayer = createMockPlayer(10L, isPlaying = true, position = Position.SHORTSTOP)
            val request = EjectionRequest(ejectedPlayerId = 10L, reason = EjectionReason.INJURY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns ejectedPlayer
            val eventSlot = slot<GameEvent>()
            every { gameEventRepository.save(capture(eventSlot)) } answers { eventSlot.captured }

            // when
            val result = service.ejectPlayer(gameId, request, scorerId)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.EJECTION)
            verify { gamePlayerRepository.save(ejectedPlayer) }
        }

        @Test
        fun `경기가 진행 중이 아니면 예외가 발생한다`() {
            // given
            val game = createMockGame(GameStatus.SCHEDULED)
            val request = EjectionRequest(ejectedPlayerId = 10L, reason = EjectionReason.INJURY)

            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when/then
            assertThatThrownBy {
                service.ejectPlayer(gameId, request, scorerId)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("진행 중인 경기만")
        }

        @Test
        fun `선수를 찾을 수 없으면 예외가 발생한다`() {
            // given
            val game = createMockGame(GameStatus.IN_PROGRESS)
            val request = EjectionRequest(ejectedPlayerId = 999L, reason = EjectionReason.INJURY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(999L) } returns null

            // when/then
            assertThatThrownBy {
                service.ejectPlayer(gameId, request, scorerId)
            }.isInstanceOf(GamePlayerNotFoundException::class.java)
        }

        @Test
        fun `출전 중이 아닌 선수 퇴장 시 예외가 발생한다`() {
            // given
            val game = createMockGame(GameStatus.IN_PROGRESS)
            val benchPlayer = createMockPlayer(10L, isPlaying = false, position = Position.LEFT_FIELD)
            val request = EjectionRequest(ejectedPlayerId = 10L, reason = EjectionReason.INJURY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns benchPlayer

            // when/then
            assertThatThrownBy {
                service.ejectPlayer(gameId, request, scorerId)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("현재 출전 중인 선수만")
        }

        @Test
        fun `주자 상태인 선수 퇴장 시 베이스를 비운다`() {
            // given
            val gameState = GameState()
            gameState.setRunner(Base.SECOND, 10L, 50L)
            val game = createMockGame(GameStatus.IN_PROGRESS, gameState = gameState)
            val ejectedPlayer = createMockPlayer(10L, isPlaying = true, position = Position.SHORTSTOP)
            val request = EjectionRequest(ejectedPlayerId = 10L, reason = EjectionReason.INJURY)

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns ejectedPlayer
            val eventSlot = slot<GameEvent>()
            every { gameEventRepository.save(capture(eventSlot)) } answers { eventSlot.captured }

            // when
            service.ejectPlayer(gameId, request, scorerId)

            // then
            assertThat(game.gameState.getRunner(Base.SECOND)).isNull()
        }

        @Test
        fun `경기를 찾을 수 없으면 예외가 발생한다`() {
            // given
            val request = EjectionRequest(ejectedPlayerId = 10L, reason = EjectionReason.INJURY)
            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when/then
            assertThatThrownBy {
                service.ejectPlayer(gameId, request, scorerId)
            }.isInstanceOf(GameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("ejectAndSubstitute()")
    inner class EjectAndSubstituteTest {
        @Test
        fun `퇴장과 교체를 원자적으로 처리한다`() {
            // given
            val game = createMockGame(GameStatus.IN_PROGRESS)
            val ejectedPlayer =
                createMockPlayer(10L, isPlaying = true, position = Position.SHORTSTOP, battingOrder = 6)
            val replacementPlayer =
                createMockPlayer(20L, isPlaying = false, position = Position.SHORTSTOP, hasExited = false)
            val request =
                EjectAndSubstituteRequest(
                    ejectedPlayerId = 10L,
                    replacementPlayerId = 20L,
                    reason = EjectionReason.INJURY,
                    newPosition = Position.SHORTSTOP,
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns ejectedPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns replacementPlayer
            every { battingRecordRepository.findByGamePlayerId(any()) } returns null
            every { pitchingRecordRepository.findByGamePlayerId(any()) } returns null
            val eventSlot = slot<GameEvent>()
            every { gameEventRepository.save(capture(eventSlot)) } answers { eventSlot.captured }

            // when
            val result = service.ejectAndSubstitute(gameId, request, scorerId)

            // then
            assertThat(result.eventType).isEqualTo(GameEventType.EMERGENCY_SUBSTITUTION)
            verify { gamePlayerRepository.save(ejectedPlayer) }
            verify { gamePlayerRepository.save(replacementPlayer) }
        }

        @Test
        fun `주자 상태 선수 퇴장 시 교체 선수가 베이스를 계승한다`() {
            // given
            val gameState = GameState()
            gameState.setRunner(Base.FIRST, 10L, 50L)
            val game = createMockGame(GameStatus.IN_PROGRESS, gameState = gameState)
            val ejectedPlayer =
                createMockPlayer(10L, isPlaying = true, position = Position.LEFT_FIELD, battingOrder = 7)
            val replacementPlayer =
                createMockPlayer(20L, isPlaying = false, position = Position.LEFT_FIELD, hasExited = false)
            val request =
                EjectAndSubstituteRequest(
                    ejectedPlayerId = 10L,
                    replacementPlayerId = 20L,
                    reason = EjectionReason.INJURY,
                    newPosition = Position.LEFT_FIELD,
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns ejectedPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns replacementPlayer
            every { battingRecordRepository.findByGamePlayerId(any()) } returns null
            every { pitchingRecordRepository.findByGamePlayerId(any()) } returns null
            val eventSlot = slot<GameEvent>()
            every { gameEventRepository.save(capture(eventSlot)) } answers { eventSlot.captured }

            // when
            service.ejectAndSubstitute(gameId, request, scorerId)

            // then — 교체 선수가 1루 주자를 계승, 담당 투수 ID 유지
            assertThat(game.gameState.getRunner(Base.FIRST)).isEqualTo(20L)
            assertThat(game.gameState.getRunnerPitcherId(Base.FIRST)).isEqualTo(50L)
        }

        @Test
        fun `투수 부상 퇴장 시 교체 투수로 currentPitcherId를 갱신한다`() {
            // given
            val gameState = GameState()
            gameState.currentPitcherId = 10L
            val game = createMockGame(GameStatus.IN_PROGRESS, gameState = gameState)
            val ejectedPitcher =
                createMockPlayer(10L, isPlaying = true, position = Position.STARTING_PITCHER, battingOrder = null)
            val replacementPitcher =
                createMockPlayer(20L, isPlaying = false, position = Position.RELIEF_PITCHER, hasExited = false)
            val request =
                EjectAndSubstituteRequest(
                    ejectedPlayerId = 10L,
                    replacementPlayerId = 20L,
                    reason = EjectionReason.INJURY,
                    newPosition = Position.RELIEF_PITCHER,
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns ejectedPitcher
            every { gamePlayerRepository.findByIdOrNull(20L) } returns replacementPitcher
            every { battingRecordRepository.findByGamePlayerId(any()) } returns null
            every { pitchingRecordRepository.findByGamePlayerId(any()) } returns null
            val eventSlot = slot<GameEvent>()
            every { gameEventRepository.save(capture(eventSlot)) } answers { eventSlot.captured }

            // when
            service.ejectAndSubstitute(gameId, request, scorerId)

            // then
            assertThat(game.gameState.currentPitcherId).isEqualTo(20L)
        }

        @Test
        fun `이미 퇴장한 선수를 교체 투입하면 예외가 발생한다`() {
            // given
            val game = createMockGame(GameStatus.IN_PROGRESS)
            val ejectedPlayer =
                createMockPlayer(10L, isPlaying = true, position = Position.SHORTSTOP, battingOrder = 6)
            val alreadyExitedPlayer =
                createMockPlayer(20L, isPlaying = false, position = Position.SHORTSTOP, hasExited = true)
            val request =
                EjectAndSubstituteRequest(
                    ejectedPlayerId = 10L,
                    replacementPlayerId = 20L,
                    reason = EjectionReason.INJURY,
                    newPosition = Position.SHORTSTOP,
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns ejectedPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns alreadyExitedPlayer

            // when/then
            assertThatThrownBy {
                service.ejectAndSubstitute(gameId, request, scorerId)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("이미 퇴장한 선수")
        }

        @Test
        fun `이미 출전 중인 선수를 교체 투입하면 예외가 발생한다`() {
            // given
            val game = createMockGame(GameStatus.IN_PROGRESS)
            val ejectedPlayer =
                createMockPlayer(10L, isPlaying = true, position = Position.SHORTSTOP, battingOrder = 6)
            val alreadyPlayingPlayer =
                createMockPlayer(20L, isPlaying = true, position = Position.LEFT_FIELD, hasExited = false)
            val request =
                EjectAndSubstituteRequest(
                    ejectedPlayerId = 10L,
                    replacementPlayerId = 20L,
                    reason = EjectionReason.INJURY,
                    newPosition = Position.SHORTSTOP,
                )

            every { gameRepository.findByIdOrNull(gameId) } returns game
            every { gamePlayerRepository.findByIdOrNull(10L) } returns ejectedPlayer
            every { gamePlayerRepository.findByIdOrNull(20L) } returns alreadyPlayingPlayer

            // when/then
            assertThatThrownBy {
                service.ejectAndSubstitute(gameId, request, scorerId)
            }.isInstanceOf(InvalidGameStateException::class.java)
                .hasMessageContaining("이미 출전 중인 선수")
        }
    }

    private fun createMockGame(
        status: GameStatus,
        gameState: GameState = GameState(),
    ): Game {
        val game = mockk<Game>(relaxed = true)
        every { game.id } returns gameId
        every { game.status } returns status
        every { game.gameState } returns gameState
        every { game.currentInning } returns 3
        every { game.isTopInning } returns true
        every { game.scorerLock.scorerId } returns scorerId
        every { game.validateScorer(scorerId) } returns Unit
        return game
    }

    private fun createMockPlayer(
        id: Long,
        isPlaying: Boolean,
        position: Position,
        battingOrder: Int? = null,
        hasExited: Boolean = false,
    ): GamePlayer {
        val player = mockk<GamePlayer>(relaxed = true)
        val playerEntity = mockk<Player>(relaxed = true)
        every { player.id } returns id
        every { player.isCurrentlyPlaying } returns isPlaying
        every { player.position } returns position
        every { player.battingOrder } returns battingOrder
        every { player.hasExited } returns hasExited
        every { player.isPitcher } returns (position.category == PositionCategory.PITCHER)
        every { player.player } returns playerEntity
        every { playerEntity.name } returns "테스트선수$id"
        return player
    }
}
