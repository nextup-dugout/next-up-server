package com.nextup.infrastructure.service.game

import com.nextup.common.exception.GameNotFoundException
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("GameStateQueryServiceImpl 테스트")
class GameStateQueryServiceImplTest {
    private lateinit var gameRepository: GameRepositoryPort
    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var gameTeamRepository: GameTeamRepositoryPort
    private lateinit var service: GameStateQueryServiceImpl

    @BeforeEach
    fun setUp() {
        gameRepository = mockk()
        gamePlayerRepository = mockk()
        gameTeamRepository = mockk()
        service =
            GameStateQueryServiceImpl(
                gameRepository,
                gamePlayerRepository,
                gameTeamRepository,
            )
    }

    @Nested
    @DisplayName("getGame")
    inner class GetGameTest {
        @Test
        @DisplayName("경기가 존재하면 Game을 반환한다")
        fun returnsGameWhenFound() {
            // given
            val gameId = 1L
            val game = mockk<Game>()
            every { gameRepository.findByIdOrNull(gameId) } returns game

            // when
            val result = service.getGame(gameId)

            // then
            assertThat(result).isEqualTo(game)
            verify(exactly = 1) { gameRepository.findByIdOrNull(gameId) }
        }

        @Test
        @DisplayName("경기가 존재하지 않으면 GameNotFoundException을 던진다")
        fun throwsExceptionWhenNotFound() {
            // given
            val gameId = 999L
            every { gameRepository.findByIdOrNull(gameId) } returns null

            // when & then
            assertThatThrownBy { service.getGame(gameId) }
                .isInstanceOf(GameNotFoundException::class.java)

            verify(exactly = 1) { gameRepository.findByIdOrNull(gameId) }
        }
    }

    @Nested
    @DisplayName("getCurrentLineup")
    inner class GetCurrentLineupTest {
        @Test
        @DisplayName("현재 출전 중인 선수 목록을 반환한다")
        fun returnsCurrentlyPlayingPlayers() {
            // given
            val gameId = 1L
            val players = listOf(mockk<GamePlayer>(), mockk<GamePlayer>())
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(gameId) } returns players

            // when
            val result = service.getCurrentLineup(gameId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(players)
            verify(exactly = 1) { gamePlayerRepository.findCurrentlyPlayingByGameId(gameId) }
        }

        @Test
        @DisplayName("출전 중인 선수가 없으면 빈 리스트를 반환한다")
        fun returnsEmptyListWhenNoPlayers() {
            // given
            val gameId = 1L
            every { gamePlayerRepository.findCurrentlyPlayingByGameId(gameId) } returns emptyList()

            // when
            val result = service.getCurrentLineup(gameId)

            // then
            assertThat(result).isEmpty()
            verify(exactly = 1) { gamePlayerRepository.findCurrentlyPlayingByGameId(gameId) }
        }
    }

    @Nested
    @DisplayName("getGameTeams")
    inner class GetGameTeamsTest {
        @Test
        @DisplayName("경기의 GameTeam 목록을 반환한다")
        fun returnsGameTeams() {
            // given
            val gameId = 1L
            val gameTeams = listOf(mockk<GameTeam>(), mockk<GameTeam>())
            every { gameTeamRepository.findAllByGameId(gameId) } returns gameTeams

            // when
            val result = service.getGameTeams(gameId)

            // then
            assertThat(result).hasSize(2)
            assertThat(result).isEqualTo(gameTeams)
            verify(exactly = 1) { gameTeamRepository.findAllByGameId(gameId) }
        }

        @Test
        @DisplayName("GameTeam이 없으면 빈 리스트를 반환한다")
        fun returnsEmptyListWhenNoGameTeams() {
            // given
            val gameId = 1L
            every { gameTeamRepository.findAllByGameId(gameId) } returns emptyList()

            // when
            val result = service.getGameTeams(gameId)

            // then
            assertThat(result).isEmpty()
            verify(exactly = 1) { gameTeamRepository.findAllByGameId(gameId) }
        }
    }
}
