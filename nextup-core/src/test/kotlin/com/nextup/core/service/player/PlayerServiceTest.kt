package com.nextup.core.service.player

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.port.repository.PlayerRepositoryPort
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PlayerService")
class PlayerServiceTest {
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var playerService: PlayerService

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        playerService = PlayerService(playerRepository)
    }

    @Nested
    @DisplayName("search")
    inner class Search {
        @Test
        fun `should return paginated results`() {
            // given
            val player =
                Player(
                    name = "김철수",
                    primaryPosition = Position.STARTING_PITCHER,
                )
            val pageCommand = PageCommand(page = 0, size = 20)

            every {
                playerRepository.search(
                    name = "김",
                    teamId = null,
                    position = null,
                    pageCommand = pageCommand,
                )
            } returns
                PageResult(
                    content = listOf(player),
                    page = 0,
                    size = 20,
                    totalElements = 1,
                    totalPages = 1,
                )

            // when
            val result =
                playerService.search(
                    name = "김",
                    teamId = null,
                    position = null,
                    pageCommand = pageCommand,
                )

            // then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].name).isEqualTo("김철수")
        }

        @Test
        fun `should pass all filter parameters to repository`() {
            // given
            val pageCommand = PageCommand(page = 0, size = 10)

            every {
                playerRepository.search(
                    name = "이",
                    teamId = 5L,
                    position = Position.SHORTSTOP,
                    pageCommand = pageCommand,
                )
            } returns
                PageResult(
                    content = emptyList(),
                    page = 0,
                    size = 10,
                    totalElements = 0,
                    totalPages = 0,
                )

            // when
            val result =
                playerService.search(
                    name = "이",
                    teamId = 5L,
                    position = Position.SHORTSTOP,
                    pageCommand = pageCommand,
                )

            // then
            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getById")
    inner class GetById {
        @Test
        fun `should throw PlayerNotFoundException when player not found`() {
            // given
            every { playerRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy { playerService.getById(999L) }
                .isInstanceOf(PlayerNotFoundException::class.java)
        }
    }
}
