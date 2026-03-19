package com.nextup.api.mapper.player

import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.PlayerTeamHistory
import com.nextup.core.domain.player.PlayerTeamStatus
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.service.player.dto.PlayerDashboardDto
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PlayerDashboardMapper")
class PlayerDashboardMapperTest {
    @Test
    fun `PlayerDashboardDto를 PlayerDashboardResponse로 변환한다`() {
        // given
        val player =
            Player(
                id = 1L,
                name = "홍길동",
                primaryPosition = Position.STARTING_PITCHER,
                profileImageUrl = "https://example.com/profile.jpg",
            )
        val dto =
            PlayerDashboardDto(
                player = player,
                currentHistory = null,
                seasonBattingStats = null,
                seasonPitchingStats = null,
                careerBattingStats = null,
                careerPitchingStats = null,
                recentBattingForm = null,
                recentPitchingForm = null,
                teamHistory = emptyList(),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.profile.id).isEqualTo(1L)
        assertThat(response.profile.name).isEqualTo("홍길동")
        assertThat(response.profile.profileImageUrl).isEqualTo("https://example.com/profile.jpg")
        assertThat(response.currentTeam).isNull()
        assertThat(response.seasonBattingStats).isNull()
        assertThat(response.careerBattingStats).isNull()
        assertThat(response.recentBattingForm).isNull()
        assertThat(response.teamHistory).isEmpty()
    }

    @Test
    fun `현재 소속 팀이 있는 경우 currentTeam이 포함된다`() {
        // given
        val player =
            Player(
                id = 1L,
                name = "박선수",
                primaryPosition = Position.LEFT_FIELD,
            )
        val team = mockk<Team>()
        every { team.id } returns 10L
        every { team.name } returns "Tigers"
        every { team.logoUrl } returns null

        val currentHistory =
            PlayerTeamHistory(
                player = player,
                team = team,
                startDate = LocalDate.of(2024, 1, 1),
                position = Position.LEFT_FIELD,
                uniformNumber = 7,
                status = PlayerTeamStatus.ACTIVE,
            )
        val dto =
            PlayerDashboardDto(
                player = player,
                currentHistory = currentHistory,
                seasonBattingStats = null,
                seasonPitchingStats = null,
                careerBattingStats = null,
                careerPitchingStats = null,
                recentBattingForm = null,
                recentPitchingForm = null,
                teamHistory = listOf(currentHistory),
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.profile.backNumber).isEqualTo(7)
        assertThat(response.currentTeam).isNotNull
        assertThat(response.currentTeam!!.teamId).isEqualTo(10L)
        assertThat(response.currentTeam!!.teamName).isEqualTo("Tigers")
        assertThat(response.currentTeam!!.isActive).isTrue()
        assertThat(response.teamHistory).hasSize(1)
    }

    @Test
    fun `PlayerTeamHistory를 TeamHistoryItemResponse로 변환한다`() {
        // given
        val player =
            Player(
                id = 1L,
                name = "김선수",
                primaryPosition = Position.CATCHER,
            )
        val team = mockk<Team>()
        every { team.id } returns 5L
        every { team.name } returns "Lions"
        every { team.logoUrl } returns null

        val history =
            PlayerTeamHistory(
                player = player,
                team = team,
                startDate = LocalDate.of(2022, 3, 1),
                endDate = LocalDate.of(2023, 12, 31),
                position = Position.CATCHER,
                uniformNumber = 21,
                status = PlayerTeamStatus.TRANSFERRED,
            )

        // when
        val response = history.toHistoryItemResponse()

        // then
        assertThat(response.teamId).isEqualTo(5L)
        assertThat(response.teamName).isEqualTo("Lions")
        assertThat(response.uniformNumber).isEqualTo(21)
        assertThat(response.startDate).isEqualTo(LocalDate.of(2022, 3, 1))
        assertThat(response.endDate).isEqualTo(LocalDate.of(2023, 12, 31))
        assertThat(response.isActive).isFalse()
    }
}
