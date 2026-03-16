package com.nextup.scorer.controller

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.HomeAway
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.scorer.dto.websocket.InningScoresDto
import com.nextup.scorer.dto.websocket.ScoreboardMessage
import com.nextup.scorer.dto.websocket.TeamScoreDto
import com.nextup.scorer.websocket.mapper.ScoreboardMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ScoreboardController 테스트")
class ScoreboardControllerTest {
    private val gameRepository: GameRepositoryPort = mockk()
    private val gameTeamRepository: GameTeamRepositoryPort = mockk()
    private val scoreboardMapper: ScoreboardMapper = mockk()

    private lateinit var controller: ScoreboardController

    @BeforeEach
    fun setUp() {
        controller =
            ScoreboardController(
                gameRepository = gameRepository,
                gameTeamRepository = gameTeamRepository,
                scoreboardMapper = scoreboardMapper,
            )
    }

    @Test
    fun `스코어보드를 정상 조회한다`() {
        // given
        val gameId = 1L
        val game: Game = mockk()
        val homeTeam: GameTeam =
            mockk {
                every { homeAway } returns HomeAway.HOME
            }
        val awayTeam: GameTeam =
            mockk {
                every { homeAway } returns HomeAway.AWAY
            }
        val scoreboardMessage =
            ScoreboardMessage(
                gameId = gameId,
                homeTeam = TeamScoreDto(1L, "홈팀", 5, 8, 1),
                awayTeam = TeamScoreDto(2L, "원정팀", 3, 6, 0),
                inningScores =
                    InningScoresDto(
                        homeScores = listOf(0, 1, 2, 0, 2, 0, 0, 0, 0),
                        awayScores = listOf(1, 0, 0, 1, 0, 1, 0, 0, 0),
                    ),
                currentInning = 9,
                isTopInning = false,
            )

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(homeTeam, awayTeam)
        every { scoreboardMapper.toScoreboardMessage(game, homeTeam, awayTeam) } returns scoreboardMessage

        // when
        val response = controller.getScoreboard(gameId)

        // then
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body?.success).isTrue()
        assertThat(response.body?.data?.gameId).isEqualTo(gameId)
        assertThat(response.body?.data?.homeTeam?.runs).isEqualTo(5)
        assertThat(response.body?.data?.awayTeam?.runs).isEqualTo(3)

        verify { gameRepository.findByIdOrNull(gameId) }
        verify { gameTeamRepository.findAllByGameId(gameId) }
        verify { scoreboardMapper.toScoreboardMessage(game, homeTeam, awayTeam) }
    }

    @Test
    fun `존재하지 않는 경기 조회 시 예외를 발생시킨다`() {
        // given
        val gameId = 999L
        every { gameRepository.findByIdOrNull(gameId) } returns null

        // when & then
        assertThatThrownBy {
            controller.getScoreboard(gameId)
        }.isInstanceOf(GameNotFoundException::class.java)
    }

    @Test
    fun `홈팀 정보가 없으면 예외를 발생시킨다`() {
        // given
        val gameId = 1L
        val game: Game = mockk()
        val awayTeam: GameTeam =
            mockk {
                every { homeAway } returns HomeAway.AWAY
            }

        every { gameRepository.findByIdOrNull(gameId) } returns game
        every { gameTeamRepository.findAllByGameId(gameId) } returns listOf(awayTeam)

        // when & then
        assertThatThrownBy {
            controller.getScoreboard(gameId)
        }.isInstanceOf(GameNotFoundException::class.java)
    }
}
