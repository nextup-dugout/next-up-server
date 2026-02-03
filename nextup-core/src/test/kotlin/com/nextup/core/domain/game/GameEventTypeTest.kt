package com.nextup.core.domain.game

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GameEventType enum 테스트")
class GameEventTypeTest {

    @Nested
    @DisplayName("이벤트 타입 분류")
    inner class EventTypeClassification {

        @Test
        fun `PLATE_APPEARANCE는 타석 결과 이벤트이다`() {
            assertThat(GameEventType.PLATE_APPEARANCE.isPlateAppearance).isTrue()
            assertThat(GameEventType.PLATE_APPEARANCE.isBaseRunning).isFalse()
            assertThat(GameEventType.PLATE_APPEARANCE.isSubstitution).isFalse()
        }

        @Test
        fun `BASE_RUNNING은 주루 이벤트이다`() {
            assertThat(GameEventType.BASE_RUNNING.isBaseRunning).isTrue()
            assertThat(GameEventType.BASE_RUNNING.isPlateAppearance).isFalse()
        }

        @Test
        fun `SUBSTITUTION은 선수 교체 이벤트이다`() {
            assertThat(GameEventType.SUBSTITUTION.isSubstitution).isTrue()
            assertThat(GameEventType.SUBSTITUTION.isGameProgress).isFalse()
        }

        @Test
        fun `경기 진행 관련 이벤트들을 구분할 수 있다`() {
            assertThat(GameEventType.GAME_START.isGameProgress).isTrue()
            assertThat(GameEventType.GAME_END.isGameProgress).isTrue()
            assertThat(GameEventType.INNING_START.isGameProgress).isTrue()
            assertThat(GameEventType.INNING_END.isGameProgress).isTrue()
            assertThat(GameEventType.HALF_INNING_CHANGE.isGameProgress).isTrue()
            assertThat(GameEventType.GAME_CALLED.isGameProgress).isTrue()
        }

        @Test
        fun `기타 이벤트들은 경기 진행 이벤트가 아니다`() {
            assertThat(GameEventType.TIMEOUT.isGameProgress).isFalse()
            assertThat(GameEventType.MOUND_VISIT.isGameProgress).isFalse()
            assertThat(GameEventType.REVIEW.isGameProgress).isFalse()
            assertThat(GameEventType.INJURY.isGameProgress).isFalse()
        }
    }

    @Nested
    @DisplayName("이벤트 타입 속성")
    inner class EventTypeProperties {

        @Test
        fun `모든 이벤트 타입은 표시 이름을 가진다`() {
            GameEventType.entries.forEach { type ->
                assertThat(type.displayName).isNotBlank()
            }
        }

        @Test
        fun `모든 이벤트 타입은 설명을 가진다`() {
            GameEventType.entries.forEach { type ->
                assertThat(type.description).isNotBlank()
            }
        }
    }
}
