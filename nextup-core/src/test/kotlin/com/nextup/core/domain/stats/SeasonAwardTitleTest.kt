package com.nextup.core.domain.stats

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SeasonAwardTitle 테스트")
class SeasonAwardTitleTest {
    @Nested
    @DisplayName("enum 속성 검증")
    inner class EnumProperties {
        @Test
        fun `BATTING_CHAMPION은 타격왕이다`() {
            assertThat(SeasonAwardTitle.BATTING_CHAMPION.displayName).isEqualTo("타격왕")
            assertThat(SeasonAwardTitle.BATTING_CHAMPION.description).isEqualTo("규정타석 이상 최고 타율")
        }

        @Test
        fun `HOME_RUN_KING은 홈런왕이다`() {
            assertThat(SeasonAwardTitle.HOME_RUN_KING.displayName).isEqualTo("홈런왕")
            assertThat(SeasonAwardTitle.HOME_RUN_KING.description).isEqualTo("최다 홈런")
        }

        @Test
        fun `RBI_KING은 타점왕이다`() {
            assertThat(SeasonAwardTitle.RBI_KING.displayName).isEqualTo("타점왕")
            assertThat(SeasonAwardTitle.RBI_KING.description).isEqualTo("최다 타점")
        }

        @Test
        fun `STOLEN_BASE_KING은 도루왕이다`() {
            assertThat(SeasonAwardTitle.STOLEN_BASE_KING.displayName).isEqualTo("도루왕")
            assertThat(SeasonAwardTitle.STOLEN_BASE_KING.description).isEqualTo("최다 도루")
        }

        @Test
        fun `WINS_LEADER는 다승왕이다`() {
            assertThat(SeasonAwardTitle.WINS_LEADER.displayName).isEqualTo("다승왕")
            assertThat(SeasonAwardTitle.WINS_LEADER.description).isEqualTo("최다 승리")
        }

        @Test
        fun `ERA_TITLE은 방어율 1위이다`() {
            assertThat(SeasonAwardTitle.ERA_TITLE.displayName).isEqualTo("방어율 1위")
            assertThat(SeasonAwardTitle.ERA_TITLE.description).isEqualTo("규정이닝 이상 최저 방어율")
        }

        @Test
        fun `STRIKEOUT_KING은 탈삼진왕이다`() {
            assertThat(SeasonAwardTitle.STRIKEOUT_KING.displayName).isEqualTo("탈삼진왕")
            assertThat(SeasonAwardTitle.STRIKEOUT_KING.description).isEqualTo("최다 탈삼진")
        }

        @Test
        fun `SAVES_LEADER는 세이브왕이다`() {
            assertThat(SeasonAwardTitle.SAVES_LEADER.displayName).isEqualTo("세이브왕")
            assertThat(SeasonAwardTitle.SAVES_LEADER.description).isEqualTo("최다 세이브")
        }

        @Test
        fun `HITS_LEADER는 최다안타이다`() {
            assertThat(SeasonAwardTitle.HITS_LEADER.displayName).isEqualTo("최다안타")
            assertThat(SeasonAwardTitle.HITS_LEADER.description).isEqualTo("최다 안타")
        }

        @Test
        fun `MVP는 최우수선수이다`() {
            assertThat(SeasonAwardTitle.MVP.displayName).isEqualTo("MVP")
            assertThat(SeasonAwardTitle.MVP.description).isEqualTo("최우수선수")
        }
    }

    @Nested
    @DisplayName("enum 전체 검증")
    inner class EnumValues {
        @Test
        fun `타이틀은 총 10개이다`() {
            assertThat(SeasonAwardTitle.entries).hasSize(10)
        }

        @Test
        fun `모든 타이틀은 displayName과 description이 비어있지 않다`() {
            SeasonAwardTitle.entries.forEach { title ->
                assertThat(title.displayName).isNotBlank()
                assertThat(title.description).isNotBlank()
            }
        }

        @Test
        fun `valueOf로 문자열에서 enum을 찾을 수 있다`() {
            assertThat(SeasonAwardTitle.valueOf("BATTING_CHAMPION")).isEqualTo(SeasonAwardTitle.BATTING_CHAMPION)
            assertThat(SeasonAwardTitle.valueOf("ERA_TITLE")).isEqualTo(SeasonAwardTitle.ERA_TITLE)
            assertThat(SeasonAwardTitle.valueOf("MVP")).isEqualTo(SeasonAwardTitle.MVP)
        }
    }
}
