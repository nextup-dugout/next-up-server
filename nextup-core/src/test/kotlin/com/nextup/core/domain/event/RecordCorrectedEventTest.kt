package com.nextup.core.domain.event

import com.nextup.core.domain.game.CorrectionType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RecordCorrectedEvent 테스트")
class RecordCorrectedEventTest {
    @Test
    fun `이벤트 생성 시 모든 필드가 올바르게 설정됨`() {
        val event =
            RecordCorrectedEvent(
                gameId = 10L,
                correctionType = CorrectionType.BATTING,
                playerId = 1L,
                fieldName = "hits",
                oldValue = "2",
                newValue = "3",
            )

        assertThat(event.gameId).isEqualTo(10L)
        assertThat(event.correctionType).isEqualTo(CorrectionType.BATTING)
        assertThat(event.playerId).isEqualTo(1L)
        assertThat(event.fieldName).isEqualTo("hits")
        assertThat(event.oldValue).isEqualTo("2")
        assertThat(event.newValue).isEqualTo("3")
    }

    @Test
    fun `투수 정정 이벤트 생성`() {
        val event =
            RecordCorrectedEvent(
                gameId = 20L,
                correctionType = CorrectionType.PITCHING,
                playerId = 2L,
                fieldName = "earnedRuns",
                oldValue = "3",
                newValue = "1",
            )

        assertThat(event.correctionType).isEqualTo(CorrectionType.PITCHING)
        assertThat(event.fieldName).isEqualTo("earnedRuns")
    }
}
