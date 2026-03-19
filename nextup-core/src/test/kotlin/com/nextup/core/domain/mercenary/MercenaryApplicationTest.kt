package com.nextup.core.domain.mercenary

import com.nextup.core.domain.player.Position
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MercenaryApplicationTest {
    @Test
    fun `용병 지원을 생성할 수 있다`() {
        // when
        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER, Position.FIRST_BASE),
                message = "포수 경력 5년입니다",
            )

        // then
        assertThat(application.requestId).isEqualTo(1L)
        assertThat(application.playerId).isEqualTo(100L)
        assertThat(application.preferredPositions).containsExactlyInAnyOrder(
            Position.CATCHER,
            Position.FIRST_BASE,
        )
        assertThat(application.status).isEqualTo(MercenaryApplicationStatus.PENDING)
        assertThat(application.message).isEqualTo("포수 경력 5년입니다")
    }

    @Test
    fun `선호 포지션이 비어있으면 생성 시 예외가 발생한다`() {
        // when & then
        assertThrows<IllegalArgumentException> {
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = emptySet(),
            )
        }
    }

    @Test
    fun `PENDING 상태의 지원을 수락할 수 있다`() {
        // given
        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        // when
        application.accept()

        // then
        assertThat(application.status).isEqualTo(MercenaryApplicationStatus.ACCEPTED)
    }

    @Test
    fun `PENDING 상태가 아닌 지원을 수락하면 예외가 발생한다`() {
        // given
        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )
        application.reject()

        // when & then
        assertThrows<IllegalStateException> {
            application.accept()
        }
    }

    @Test
    fun `PENDING 상태의 지원을 거절할 수 있다`() {
        // given
        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )

        // when
        application.reject()

        // then
        assertThat(application.status).isEqualTo(MercenaryApplicationStatus.REJECTED)
    }

    @Test
    fun `PENDING 상태가 아닌 지원을 거절하면 예외가 발생한다`() {
        // given
        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.CATCHER),
            )
        application.accept()

        // when & then
        assertThrows<IllegalStateException> {
            application.reject()
        }
    }

    @Test
    fun `메시지 없이 지원을 생성할 수 있다`() {
        // when
        val application =
            MercenaryApplication.create(
                requestId = 1L,
                playerId = 100L,
                preferredPositions = setOf(Position.SHORTSTOP),
            )

        // then
        assertThat(application.message).isNull()
    }
}
