package com.nextup.api.mapper.game

import com.nextup.core.domain.game.GameStatus
import com.nextup.core.service.game.dto.GameAggregateDto
import com.nextup.core.service.game.dto.GameDetailDto
import com.nextup.core.service.game.dto.GameTimelineDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("GameAggregateMapper")
class GameAggregateMapperTest {
    @Test
    fun `GameAggregateDto를 GameAggregateResponse로 변환한다`() {
        // given
        val gameDetail =
            GameDetailDto(
                gameId = 1L,
                competitionId = 100L,
                competitionName = "2026 사회인야구 리그",
                homeTeamId = 10L,
                homeTeamName = "Tigers",
                awayTeamId = 20L,
                awayTeamName = "Lions",
                scheduledAt = LocalDateTime.of(2026, 5, 1, 14, 0),
                status = GameStatus.FINISHED,
                homeScore = 5,
                awayScore = 3,
                location = "서울",
                fieldName = "잠실야구장",
                gameNumber = 1,
                currentInning = "경기 종료",
                totalInnings = 9,
                startedAt = LocalDateTime.of(2026, 5, 1, 14, 5),
                endedAt = LocalDateTime.of(2026, 5, 1, 17, 0),
                note = null,
                forfeitReason = null,
            )
        val timeline = GameTimelineDto(gameId = 1L, events = emptyList(), totalEvents = 0)
        val dto =
            GameAggregateDto(
                gameDetail = gameDetail,
                boxScore = null,
                timeline = timeline,
                scoresheet = null,
            )

        // when
        val response = dto.toResponse()

        // then
        assertThat(response.gameInfo.gameId).isEqualTo(1L)
        assertThat(response.gameInfo.competitionName).isEqualTo("2026 사회인야구 리그")
        assertThat(response.gameInfo.homeTeam.teamId).isEqualTo(10L)
        assertThat(response.gameInfo.homeTeam.teamName).isEqualTo("Tigers")
        assertThat(response.gameInfo.homeTeam.score).isEqualTo(5)
        assertThat(response.gameInfo.awayTeam.teamId).isEqualTo(20L)
        assertThat(response.gameInfo.awayTeam.score).isEqualTo(3)
        assertThat(response.gameInfo.status).isEqualTo(GameStatus.FINISHED)
        assertThat(response.gameInfo.statusDisplayName).isEqualTo(GameStatus.FINISHED.displayName)
        assertThat(response.boxScore).isNull()
        assertThat(response.timeline.totalEvents).isEqualTo(0)
        assertThat(response.scoresheet).isNull()
    }

    @Test
    fun `GameDetailDto를 GameAggregateInfoResponse로 변환한다`() {
        // given
        val gameDetail =
            GameDetailDto(
                gameId = 2L,
                competitionId = 200L,
                competitionName = "대회명",
                homeTeamId = 30L,
                homeTeamName = "Bears",
                awayTeamId = 40L,
                awayTeamName = "Eagles",
                scheduledAt = LocalDateTime.of(2026, 6, 15, 10, 0),
                status = GameStatus.SCHEDULED,
                homeScore = 0,
                awayScore = 0,
                location = null,
                fieldName = null,
                gameNumber = null,
                currentInning = "경기 전",
                totalInnings = 9,
                startedAt = null,
                endedAt = null,
                note = "우천 시 취소",
                forfeitReason = null,
            )

        // when
        val response = gameDetail.toAggregateInfoResponse()

        // then
        assertThat(response.gameId).isEqualTo(2L)
        assertThat(response.competitionId).isEqualTo(200L)
        assertThat(response.homeTeam.teamId).isEqualTo(30L)
        assertThat(response.awayTeam.teamId).isEqualTo(40L)
        assertThat(response.status).isEqualTo(GameStatus.SCHEDULED)
        assertThat(response.location).isNull()
        assertThat(response.note).isEqualTo("우천 시 취소")
        assertThat(response.startedAt).isNull()
    }
}
