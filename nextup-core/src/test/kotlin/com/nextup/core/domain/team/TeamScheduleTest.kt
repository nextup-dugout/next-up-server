package com.nextup.core.domain.team

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("TeamSchedule 엔티티 테스트")
class TeamScheduleTest {
    private val team: Team = mockk(relaxed = true)
    private val baseStartAt: LocalDateTime = LocalDateTime.of(2026, 3, 20, 10, 0)
    private val baseEndAt: LocalDateTime = LocalDateTime.of(2026, 3, 20, 12, 0)

    @Nested
    @DisplayName("팀 일정 생성")
    inner class Create {
        @Test
        fun `유효한 파라미터로 팀 일정을 생성할 수 있다`() {
            // when
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "정기 연습",
                    description = "월요일 연습입니다",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                    endAt = baseEndAt,
                    location = "잠실 야구장",
                )

            // then
            assertThat(schedule.team).isEqualTo(team)
            assertThat(schedule.title).isEqualTo("정기 연습")
            assertThat(schedule.description).isEqualTo("월요일 연습입니다")
            assertThat(schedule.scheduleType).isEqualTo(TeamScheduleType.PRACTICE)
            assertThat(schedule.startAt).isEqualTo(baseStartAt)
            assertThat(schedule.endAt).isEqualTo(baseEndAt)
            assertThat(schedule.location).isEqualTo("잠실 야구장")
        }

        @Test
        fun `endAt 없이도 팀 일정을 생성할 수 있다`() {
            // when
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "팀 모임",
                    scheduleType = TeamScheduleType.MEETING,
                    startAt = baseStartAt,
                )

            // then
            assertThat(schedule.endAt).isNull()
        }

        @Test
        fun `빈 title로 생성하면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                TeamSchedule.create(
                    team = team,
                    title = "",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                )
            }
        }

        @Test
        fun `공백만 있는 title로 생성하면 예외가 발생한다`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                TeamSchedule.create(
                    team = team,
                    title = "   ",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                )
            }
        }

        @Test
        fun `endAt이 startAt보다 이전이면 예외가 발생한다`() {
            // given
            val invalidEndAt = baseStartAt.minusHours(1)

            // when & then
            assertThrows<IllegalArgumentException> {
                TeamSchedule.create(
                    team = team,
                    title = "연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                    endAt = invalidEndAt,
                )
            }
        }

        @Test
        fun `endAt이 startAt과 같으면 생성에 성공한다`() {
            // when
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "즉시 종료 이벤트",
                    scheduleType = TeamScheduleType.EVENT,
                    startAt = baseStartAt,
                    endAt = baseStartAt,
                )

            // then
            assertThat(schedule.endAt).isEqualTo(baseStartAt)
        }
    }

    @Nested
    @DisplayName("팀 일정 수정")
    inner class Update {
        @Test
        fun `모든 필드를 부분적으로 업데이트할 수 있다`() {
            // given
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "원래 제목",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                )

            // when
            schedule.update(
                title = "수정된 제목",
                scheduleType = TeamScheduleType.MEETING,
            )

            // then
            assertThat(schedule.title).isEqualTo("수정된 제목")
            assertThat(schedule.scheduleType).isEqualTo(TeamScheduleType.MEETING)
            assertThat(schedule.startAt).isEqualTo(baseStartAt)
        }

        @Test
        fun `null 파라미터는 기존 값을 유지한다`() {
            // given
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "원래 제목",
                    description = "원래 설명",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                    endAt = baseEndAt,
                    location = "잠실 야구장",
                )

            // when
            schedule.update(title = "수정된 제목")

            // then
            assertThat(schedule.description).isEqualTo("원래 설명")
            assertThat(schedule.scheduleType).isEqualTo(TeamScheduleType.PRACTICE)
            assertThat(schedule.startAt).isEqualTo(baseStartAt)
            assertThat(schedule.endAt).isEqualTo(baseEndAt)
            assertThat(schedule.location).isEqualTo("잠실 야구장")
        }

        @Test
        fun `update 시 빈 title이면 예외가 발생한다`() {
            // given
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "원래 제목",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                )

            // when & then
            assertThrows<IllegalArgumentException> {
                schedule.update(title = "")
            }
        }

        @Test
        fun `update 시 공백만 있는 title이면 예외가 발생한다`() {
            // given
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "원래 제목",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                )

            // when & then
            assertThrows<IllegalArgumentException> {
                schedule.update(title = "   ")
            }
        }

        @Test
        fun `update 시 endAt이 startAt보다 이전이면 예외가 발생한다`() {
            // given
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                    endAt = baseEndAt,
                )
            val invalidEndAt = baseStartAt.minusMinutes(30)

            // when & then
            assertThrows<IllegalArgumentException> {
                schedule.update(endAt = invalidEndAt)
            }
        }

        @Test
        fun `startAt과 endAt을 함께 업데이트할 수 있다`() {
            // given
            val schedule =
                TeamSchedule.create(
                    team = team,
                    title = "연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = baseStartAt,
                    endAt = baseEndAt,
                )
            val newStartAt = LocalDateTime.of(2026, 3, 21, 9, 0)
            val newEndAt = LocalDateTime.of(2026, 3, 21, 11, 0)

            // when
            schedule.update(startAt = newStartAt, endAt = newEndAt)

            // then
            assertThat(schedule.startAt).isEqualTo(newStartAt)
            assertThat(schedule.endAt).isEqualTo(newEndAt)
        }
    }
}
