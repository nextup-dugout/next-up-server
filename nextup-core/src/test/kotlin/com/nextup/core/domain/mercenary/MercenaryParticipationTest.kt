package com.nextup.core.domain.mercenary

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MercenaryParticipationTest {
    @Test
    fun `용병 참가 기록을 생성할 수 있다`() {
        // when
        val participation =
            MercenaryParticipation.create(
                gameId = 10L,
                playerId = 100L,
                teamId = 1L,
            )

        // then
        assertThat(participation.gameId).isEqualTo(10L)
        assertThat(participation.playerId).isEqualTo(100L)
        assertThat(participation.teamId).isEqualTo(1L)
    }
}
