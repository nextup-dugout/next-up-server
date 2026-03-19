package com.nextup.core.domain.mercenary

import com.nextup.core.domain.player.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant
import java.time.temporal.ChronoUnit

class MercenaryRequestTest {
    private val futureDeadline = Instant.now().plus(7, ChronoUnit.DAYS)

    @Test
    fun `용병 요청을 생성할 수 있다`() {
        // when
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER, Position.SHORTSTOP),
                maxCount = 2,
                deadline = futureDeadline,
                description = "포수, 유격수 구합니다",
            )

        // then
        assertThat(request.requestingTeamId).isEqualTo(1L)
        assertThat(request.gameId).isEqualTo(10L)
        assertThat(request.positions).containsExactlyInAnyOrder(Position.CATCHER, Position.SHORTSTOP)
        assertThat(request.maxCount).isEqualTo(2)
        assertThat(request.status).isEqualTo(MercenaryRequestStatus.OPEN)
        assertThat(request.deadline).isEqualTo(futureDeadline)
        assertThat(request.description).isEqualTo("포수, 유격수 구합니다")
    }

    @Test
    fun `포지션이 비어있으면 생성 시 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = emptySet(),
                maxCount = 2,
                deadline = futureDeadline,
            )
        }
    }

    @Test
    fun `최대 모집 인원이 0 이하이면 생성 시 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 0,
                deadline = futureDeadline,
            )
        }
    }

    @Test
    fun `마감 시한이 과거이면 생성 시 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 2,
                deadline = Instant.now().minus(1, ChronoUnit.DAYS),
            )
        }
    }

    @Test
    fun `OPEN 상태의 요청을 마감할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        // when
        request.close()

        // then
        assertThat(request.status).isEqualTo(MercenaryRequestStatus.CLOSED)
    }

    @Test
    fun `OPEN 상태가 아닌 요청을 마감하면 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )
        request.cancel()

        // when & then
        assertThrows<IllegalStateException> {
            request.close()
        }
    }

    @Test
    fun `OPEN 상태의 요청을 취소할 수 있다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        // when
        request.cancel()

        // then
        assertThat(request.status).isEqualTo(MercenaryRequestStatus.CANCELLED)
    }

    @Test
    fun `OPEN 상태가 아닌 요청을 취소하면 예외가 발생한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )
        request.close()

        // when & then
        assertThrows<IllegalStateException> {
            request.cancel()
        }
    }

    @Test
    fun `지원을 받을 수 있는 상태인지 확인한다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )

        // when & then
        assertThat(request.canAcceptApplication()).isTrue()
    }

    @Test
    fun `마감된 요청은 지원을 받을 수 없다`() {
        // given
        val request =
            MercenaryRequest.create(
                requestingTeamId = 1L,
                gameId = 10L,
                positions = setOf(Position.CATCHER),
                maxCount = 1,
                deadline = futureDeadline,
            )
        request.close()

        // when & then
        assertThat(request.canAcceptApplication()).isFalse()
    }
}
