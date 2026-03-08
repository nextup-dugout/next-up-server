package com.nextup.core.service.player

import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.common.exception.PlayerNotLinkedException
import com.nextup.common.exception.UserNotFoundException
import com.nextup.core.common.PageCommand
import com.nextup.core.common.PageResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.user.User
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.UserRepositoryPort
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
    private lateinit var userRepository: UserRepositoryPort
    private lateinit var playerService: PlayerService

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        userRepository = mockk()
        playerService = PlayerService(playerRepository, userRepository)
    }

    private fun createPlayer(
        id: Long = 1L,
        name: String = "김철수",
        position: Position = Position.STARTING_PITCHER,
    ): Player =
        Player(
            name = name,
            primaryPosition = position,
        ).apply {
            val idField = Player::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    private fun createUserWithPlayer(
        userId: Long = 1L,
        player: Player,
    ): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, userId)
        user.linkPlayer(player)
        return user
    }

    private fun createUserWithoutPlayer(userId: Long = 1L): User {
        val user =
            User.createLocalUser(
                email = "test@example.com",
                encodedPassword = "encoded",
                nickname = "테스터",
            )
        val idField = user.javaClass.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(user, userId)
        return user
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

    @Nested
    @DisplayName("getLinkedPlayer")
    inner class GetLinkedPlayer {
        @Test
        fun `should return linked player when user has linked player`() {
            // given
            val player = createPlayer()
            val user = createUserWithPlayer(userId = 1L, player = player)
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result = playerService.getLinkedPlayer(1L)

            // then
            assertThat(result.id).isEqualTo(player.id)
            assertThat(result.name).isEqualTo("김철수")
        }

        @Test
        fun `should throw UserNotFoundException when user not found`() {
            // given
            every { userRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy { playerService.getLinkedPlayer(999L) }
                .isInstanceOf(UserNotFoundException::class.java)
        }

        @Test
        fun `should throw PlayerNotLinkedException when user has no linked player`() {
            // given
            val user = createUserWithoutPlayer(userId = 1L)
            every { userRepository.findByIdOrNull(1L) } returns user

            // when & then
            assertThatThrownBy { playerService.getLinkedPlayer(1L) }
                .isInstanceOf(PlayerNotLinkedException::class.java)
        }
    }

    @Nested
    @DisplayName("updatePlayerProfile")
    inner class UpdatePlayerProfile {
        @Test
        fun `should update player profile successfully`() {
            // given
            val player = createPlayer()
            val user = createUserWithPlayer(userId = 1L, player = player)
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result =
                playerService.updatePlayerProfile(
                    userId = 1L,
                    primaryPosition = Position.CATCHER,
                    throwingHand = ThrowingHand.RIGHT,
                    battingHand = BattingHand.LEFT,
                    height = 185,
                    weight = 82,
                )

            // then
            assertThat(result.primaryPosition).isEqualTo(Position.CATCHER)
            assertThat(result.throwingHand).isEqualTo(ThrowingHand.RIGHT)
            assertThat(result.battingHand).isEqualTo(BattingHand.LEFT)
            assertThat(result.height).isEqualTo(185)
            assertThat(result.weight).isEqualTo(82)
        }

        @Test
        fun `should keep existing position when primaryPosition is null`() {
            // given
            val player = createPlayer(position = Position.SHORTSTOP)
            val user = createUserWithPlayer(userId = 1L, player = player)
            every { userRepository.findByIdOrNull(1L) } returns user

            // when
            val result =
                playerService.updatePlayerProfile(
                    userId = 1L,
                    primaryPosition = null,
                    throwingHand = ThrowingHand.LEFT,
                    battingHand = null,
                    height = null,
                    weight = null,
                )

            // then
            assertThat(result.primaryPosition).isEqualTo(Position.SHORTSTOP)
            assertThat(result.throwingHand).isEqualTo(ThrowingHand.LEFT)
        }

        @Test
        fun `should throw PlayerNotLinkedException when user has no linked player`() {
            // given
            val user = createUserWithoutPlayer(userId = 1L)
            every { userRepository.findByIdOrNull(1L) } returns user

            // when & then
            assertThatThrownBy {
                playerService.updatePlayerProfile(
                    userId = 1L,
                    primaryPosition = Position.CATCHER,
                    throwingHand = null,
                    battingHand = null,
                    height = null,
                    weight = null,
                )
            }.isInstanceOf(PlayerNotLinkedException::class.java)
        }
    }
}
