package com.nextup.infrastructure.service.eventgame

import com.nextup.common.exception.EventGameNotFoundException
import com.nextup.core.domain.eventgame.EventGame
import com.nextup.core.domain.eventgame.EventGameParticipant
import com.nextup.core.domain.eventgame.EventGameParticipantStatus
import com.nextup.core.domain.eventgame.EventGameStatus
import com.nextup.core.port.repository.EventGameParticipantRepositoryPort
import com.nextup.core.port.repository.EventGameRepositoryPort
import com.nextup.core.service.eventgame.CreateEventGameCommand
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("EventGameService 테스트")
class EventGameServiceTest {
    private lateinit var eventGameRepository: EventGameRepositoryPort
    private lateinit var participantRepository: EventGameParticipantRepositoryPort
    private lateinit var service: EventGameServiceImpl

    @BeforeEach
    fun setUp() {
        eventGameRepository = mockk()
        participantRepository = mockk()
        service = EventGameServiceImpl(eventGameRepository, participantRepository)
    }

    private fun createEventGame(
        id: Long = 1L,
        maxParticipants: Int = 20,
    ): EventGame {
        val game =
            EventGame.create(
                organizerId = 100L,
                title = "주말 픽업 게임",
                scheduledAt = LocalDateTime.now().plusDays(7),
                maxParticipants = maxParticipants,
            )
        return game
    }

    @Nested
    @DisplayName("createEventGame")
    inner class CreateEventGameTest {
        @Test
        fun `이벤트 게임 생성 성공`() {
            val slot = slot<EventGame>()
            every { eventGameRepository.save(capture(slot)) } answers { slot.captured }

            val result =
                service.createEventGame(
                    CreateEventGameCommand(
                        organizerId = 100L,
                        title = "주말 픽업 게임",
                        scheduledAt = LocalDateTime.now().plusDays(7),
                        maxParticipants = 20,
                    ),
                )

            assertThat(result.title).isEqualTo("주말 픽업 게임")
            assertThat(result.status).isEqualTo(EventGameStatus.RECRUITING)
            assertThat(result.organizerId).isEqualTo(100L)
        }
    }

    @Nested
    @DisplayName("getEventGame")
    inner class GetEventGameTest {
        @Test
        fun `존재하지 않는 이벤트 게임 조회 시 예외`() {
            every { eventGameRepository.findByIdOrNull(999L) } returns null

            assertThatThrownBy { service.getEventGame(999L) }
                .isInstanceOf(EventGameNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("joinEventGame")
    inner class JoinEventGameTest {
        @Test
        fun `참가 신청 성공`() {
            val game = createEventGame()
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { eventGameRepository.save(any()) } answers { firstArg() }

            val result = service.joinEventGame(1L, 10L, "참가합니다")

            assertThat(result.playerId).isEqualTo(10L)
            assertThat(result.status).isEqualTo(EventGameParticipantStatus.APPLIED)
            assertThat(result.message).isEqualTo("참가합니다")
        }
    }

    @Nested
    @DisplayName("confirmParticipant")
    inner class ConfirmParticipantTest {
        @Test
        fun `참가 확정 성공`() {
            val game = createEventGame()
            val participant = EventGameParticipant.create(game, 10L)
            every { eventGameRepository.findByIdOrNull(any()) } returns game
            every { participantRepository.findByIdOrNull(any()) } returns participant
            every { participantRepository.save(any()) } answers { firstArg() }

            val result = service.confirmParticipant(1L, 1L)

            assertThat(result.status).isEqualTo(EventGameParticipantStatus.CONFIRMED)
        }
    }

    @Nested
    @DisplayName("getRecruitingEventGames")
    inner class GetRecruitingTest {
        @Test
        fun `모집 중인 이벤트 게임 목록 조회`() {
            val games = listOf(createEventGame(), createEventGame())
            every { eventGameRepository.findByStatus(EventGameStatus.RECRUITING) } returns games

            val result = service.getRecruitingEventGames()

            assertThat(result).hasSize(2)
        }
    }
}
