package com.nextup.core.service.team

import com.nextup.common.exception.TeamNotFoundException
import com.nextup.common.exception.TeamScheduleNotFoundException
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.team.TeamSchedule
import com.nextup.core.domain.team.TeamScheduleType
import com.nextup.core.port.repository.TeamRepositoryPort
import com.nextup.core.port.repository.TeamScheduleRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@DisplayName("TeamScheduleService")
class TeamScheduleServiceTest {
    private lateinit var teamScheduleRepository: TeamScheduleRepositoryPort
    private lateinit var teamRepository: TeamRepositoryPort
    private lateinit var service: TeamScheduleService

    private lateinit var mockTeam: Team

    @BeforeEach
    fun setUp() {
        teamScheduleRepository = mockk()
        teamRepository = mockk()
        service =
            TeamScheduleService(
                teamScheduleRepository = teamScheduleRepository,
                teamRepository = teamRepository,
            )

        mockTeam = mockk(relaxed = true)
    }

    @Nested
    @DisplayName("create - 팀 일정 생성")
    inner class CreateTest {
        @Test
        fun `팀이 존재하면 일정을 생성하고 저장한다`() {
            // given
            val teamId = 1L
            val startAt = LocalDateTime.now().plusDays(1)
            val schedule =
                TeamSchedule.create(
                    team = mockTeam,
                    title = "연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = startAt,
                )
            every { teamRepository.findByIdOrNull(teamId) } returns mockTeam
            every { teamScheduleRepository.save(any()) } returns schedule

            // when
            val result =
                service.create(
                    teamId = teamId,
                    title = "연습",
                    description = null,
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = startAt,
                    endAt = null,
                    location = null,
                )

            // then
            assertThat(result).isNotNull
            assertThat(result.title).isEqualTo("연습")
            verify(exactly = 1) { teamScheduleRepository.save(any()) }
        }

        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException을 던진다`() {
            // given
            val teamId = 999L
            every { teamRepository.findByIdOrNull(teamId) } returns null

            // when & then
            assertThrows<TeamNotFoundException> {
                service.create(
                    teamId = teamId,
                    title = "연습",
                    description = null,
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = LocalDateTime.now().plusDays(1),
                    endAt = null,
                    location = null,
                )
            }
            verify(exactly = 0) { teamScheduleRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("getByTeamId - 팀 일정 목록 조회")
    inner class GetByTeamIdTest {
        @Test
        fun `팀이 존재하면 해당 팀의 일정 목록을 반환한다`() {
            // given
            val teamId = 1L
            val startAt = LocalDateTime.now().plusDays(1)
            val schedules =
                listOf(
                    TeamSchedule.create(
                        team = mockTeam,
                        title = "연습",
                        scheduleType = TeamScheduleType.PRACTICE,
                        startAt = startAt,
                    ),
                    TeamSchedule.create(
                        team = mockTeam,
                        title = "모임",
                        scheduleType = TeamScheduleType.MEETING,
                        startAt = startAt.plusDays(1),
                    ),
                )
            every { teamRepository.findByIdOrNull(teamId) } returns mockTeam
            every { teamScheduleRepository.findByTeamId(teamId) } returns schedules

            // when
            val result = service.getByTeamId(teamId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result[0].title).isEqualTo("연습")
            assertThat(result[1].title).isEqualTo("모임")
        }

        @Test
        fun `팀이 존재하지 않으면 TeamNotFoundException을 던진다`() {
            // given
            val teamId = 999L
            every { teamRepository.findByIdOrNull(teamId) } returns null

            // when & then
            assertThrows<TeamNotFoundException> {
                service.getByTeamId(teamId)
            }
        }
    }

    @Nested
    @DisplayName("getById - ID로 팀 일정 조회")
    inner class GetByIdTest {
        @Test
        fun `존재하지 않는 ID이면 TeamScheduleNotFoundException을 던진다`() {
            // given
            val scheduleId = 999L
            every { teamScheduleRepository.findByIdOrNull(scheduleId) } returns null

            // when & then
            assertThrows<TeamScheduleNotFoundException> {
                service.getById(scheduleId)
            }
        }
    }

    @Nested
    @DisplayName("update - 팀 일정 수정")
    inner class UpdateTest {
        @Test
        fun `존재하는 일정을 수정한다`() {
            // given
            val scheduleId = 1L
            val startAt = LocalDateTime.now().plusDays(1)
            val schedule =
                TeamSchedule.create(
                    team = mockTeam,
                    title = "연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = startAt,
                )
            every { teamScheduleRepository.findByIdOrNull(scheduleId) } returns schedule

            // when
            val result =
                service.update(
                    id = scheduleId,
                    title = "정기 연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                )

            // then
            assertThat(result.title).isEqualTo("정기 연습")
        }
    }

    @Nested
    @DisplayName("delete - 팀 일정 삭제")
    inner class DeleteTest {
        @Test
        fun `존재하는 일정을 삭제한다`() {
            // given
            val scheduleId = 1L
            val startAt = LocalDateTime.now().plusDays(1)
            val schedule =
                TeamSchedule.create(
                    team = mockTeam,
                    title = "연습",
                    scheduleType = TeamScheduleType.PRACTICE,
                    startAt = startAt,
                )
            every { teamScheduleRepository.findByIdOrNull(scheduleId) } returns schedule
            every { teamScheduleRepository.deleteById(schedule.id) } returns Unit

            // when
            service.delete(scheduleId)

            // then
            verify(exactly = 1) { teamScheduleRepository.deleteById(schedule.id) }
        }
    }
}
