package com.nextup.core.domain.player

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PlayerCareer 엔티티 테스트")
class PlayerCareerTest {

    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        player = Player(
            name = "홍길동",
            primaryPosition = Position.SHORTSTOP
        )
    }

    private fun createCareer(
        type: CareerType = CareerType.UNIVERSITY,
        organization: String = "서울대학교",
        startDate: LocalDate = LocalDate.of(2015, 3, 1),
        endDate: LocalDate? = null
    ): PlayerCareer {
        return PlayerCareer(
            player = player,
            type = type,
            organization = organization,
            startDate = startDate,
            endDate = endDate
        )
    }

    @Nested
    @DisplayName("활동 상태 확인")
    inner class IsActive {

        @Test
        fun `종료일이 없으면 활동 중이다`() {
            // given
            val career = createCareer(endDate = null)

            // then
            assertThat(career.isActive).isTrue()
        }

        @Test
        fun `종료일이 있으면 활동 중이 아니다`() {
            // given
            val career = createCareer(endDate = LocalDate.of(2019, 2, 28))

            // then
            assertThat(career.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("프로 경력 확인")
    inner class IsProfessional {

        @Test
        fun `KBO 경력은 프로 경력이다`() {
            // given
            val career = createCareer(type = CareerType.KBO)

            // then
            assertThat(career.isProfessional).isTrue()
        }

        @Test
        fun `MLB 경력은 프로 경력이다`() {
            // given
            val career = createCareer(type = CareerType.MLB)

            // then
            assertThat(career.isProfessional).isTrue()
        }

        @Test
        fun `NPB 경력은 프로 경력이다`() {
            // given
            val career = createCareer(type = CareerType.NPB)

            // then
            assertThat(career.isProfessional).isTrue()
        }

        @Test
        fun `독립리그 경력은 프로 경력이다`() {
            // given
            val career = createCareer(type = CareerType.INDEPENDENT)

            // then
            assertThat(career.isProfessional).isTrue()
        }

        @Test
        fun `대학교 경력은 프로 경력이 아니다`() {
            // given
            val career = createCareer(type = CareerType.UNIVERSITY)

            // then
            assertThat(career.isProfessional).isFalse()
        }

        @Test
        fun `고등학교 경력은 프로 경력이 아니다`() {
            // given
            val career = createCareer(type = CareerType.HIGH_SCHOOL)

            // then
            assertThat(career.isProfessional).isFalse()
        }

        @Test
        fun `사회인 경력은 프로 경력이 아니다`() {
            // given
            val career = createCareer(type = CareerType.AMATEUR)

            // then
            assertThat(career.isProfessional).isFalse()
        }
    }

    @Nested
    @DisplayName("경력 종료")
    inner class EndCareer {

        @Test
        fun `경력을 종료할 수 있다`() {
            // given
            val career = createCareer(startDate = LocalDate.of(2015, 3, 1))

            // when
            career.endCareer(LocalDate.of(2019, 2, 28))

            // then
            assertThat(career.endDate).isEqualTo(LocalDate.of(2019, 2, 28))
            assertThat(career.isActive).isFalse()
        }

        @Test
        fun `종료일이 시작일보다 이전이면 예외가 발생한다`() {
            // given
            val career = createCareer(startDate = LocalDate.of(2015, 3, 1))

            // when & then
            assertThatThrownBy { career.endCareer(LocalDate.of(2014, 1, 1)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("시작일 이후")
        }
    }

    @Nested
    @DisplayName("특정 날짜 활동 여부 확인")
    inner class IsActiveAt {

        @Test
        fun `시작일 이전에는 활동 중이 아니다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = LocalDate.of(2019, 2, 28)
            )

            // then
            assertThat(career.isActiveAt(LocalDate.of(2014, 12, 31))).isFalse()
        }

        @Test
        fun `시작일에는 활동 중이다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = LocalDate.of(2019, 2, 28)
            )

            // then
            assertThat(career.isActiveAt(LocalDate.of(2015, 3, 1))).isTrue()
        }

        @Test
        fun `기간 중에는 활동 중이다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = LocalDate.of(2019, 2, 28)
            )

            // then
            assertThat(career.isActiveAt(LocalDate.of(2017, 6, 15))).isTrue()
        }

        @Test
        fun `종료일에는 활동 중이다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = LocalDate.of(2019, 2, 28)
            )

            // then
            assertThat(career.isActiveAt(LocalDate.of(2019, 2, 28))).isTrue()
        }

        @Test
        fun `종료일 이후에는 활동 중이 아니다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = LocalDate.of(2019, 2, 28)
            )

            // then
            assertThat(career.isActiveAt(LocalDate.of(2019, 3, 1))).isFalse()
        }

        @Test
        fun `종료일이 없으면 현재까지 활동 중이다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = null
            )

            // then
            assertThat(career.isActiveAt(LocalDate.of(2030, 12, 31))).isTrue()
        }
    }

    @Nested
    @DisplayName("경력 기간 계산")
    inner class CalculateYears {

        @Test
        fun `경력 기간을 년 단위로 계산한다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = LocalDate.of(2019, 2, 28)
            )

            // then
            assertThat(career.calculateYears()).isEqualTo(4)
        }

        @Test
        fun `종료일이 없으면 기준일까지 계산한다`() {
            // given
            val career = createCareer(
                startDate = LocalDate.of(2015, 3, 1),
                endDate = null
            )
            val baseDate = LocalDate.of(2024, 3, 1)

            // then
            assertThat(career.calculateYears(baseDate)).isEqualTo(9)
        }
    }
}
