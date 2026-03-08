package com.nextup.api.controller.player

import com.nextup.common.exception.PlayerNotLinkedException
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.service.player.PlayerService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PlayerProfileController 테스트")
class PlayerProfileControllerTest {
    private lateinit var playerService: PlayerService
    private lateinit var controller: PlayerProfileController

    @BeforeEach
    fun setUp() {
        playerService = mockk()
        controller = PlayerProfileController(playerService)
    }

    private fun createPlayer(
        id: Long = 1L,
        name: String = "김철수",
        position: Position = Position.STARTING_PITCHER,
        throwingHand: ThrowingHand? = ThrowingHand.RIGHT,
        battingHand: BattingHand? = BattingHand.RIGHT,
        height: Int? = 180,
        weight: Int? = 75,
    ): Player =
        Player(
            name = name,
            primaryPosition = position,
            throwingHand = throwingHand,
            battingHand = battingHand,
            height = height,
            weight = weight,
        ).apply {
            val idField = Player::class.java.getDeclaredField("id")
            idField.isAccessible = true
            idField.set(this, id)
        }

    @Nested
    @DisplayName("GET /api/v1/players/me")
    inner class GetMyPlayerProfile {
        @Test
        fun `should return my player profile successfully`() {
            // given
            val userId = 1L
            val player = createPlayer()
            every { playerService.getLinkedPlayer(userId) } returns player

            // when
            val response = controller.getMyPlayerProfile(userId)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.id).isEqualTo(1L)
            assertThat(response.data?.name).isEqualTo("김철수")
            assertThat(response.data?.primaryPosition).isEqualTo("선발투수")
            assertThat(response.data?.throwingHand).isEqualTo("우투")
            assertThat(response.data?.battingHand).isEqualTo("우타")
            assertThat(response.data?.height).isEqualTo(180)
            assertThat(response.data?.weight).isEqualTo(75)
            verify { playerService.getLinkedPlayer(userId) }
        }

        @Test
        fun `should throw PlayerNotLinkedException when no linked player`() {
            // given
            val userId = 1L
            every { playerService.getLinkedPlayer(userId) } throws
                PlayerNotLinkedException(userId)

            // when & then
            assertThatThrownBy { controller.getMyPlayerProfile(userId) }
                .isInstanceOf(PlayerNotLinkedException::class.java)
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/players/me")
    inner class UpdateMyPlayerProfile {
        @Test
        fun `should update player profile successfully`() {
            // given
            val userId = 1L
            val updatedPlayer =
                createPlayer(
                    position = Position.CATCHER,
                    throwingHand = ThrowingHand.LEFT,
                    battingHand = BattingHand.SWITCH,
                    height = 185,
                    weight = 82,
                )
            every {
                playerService.updatePlayerProfile(
                    userId = userId,
                    primaryPosition = Position.CATCHER,
                    throwingHand = ThrowingHand.LEFT,
                    battingHand = BattingHand.SWITCH,
                    height = 185,
                    weight = 82,
                )
            } returns updatedPlayer

            val request =
                com.nextup.api.dto.player.UpdatePlayerProfileRequest(
                    primaryPosition = Position.CATCHER,
                    throwingHand = ThrowingHand.LEFT,
                    battingHand = BattingHand.SWITCH,
                    height = 185,
                    weight = 82,
                )

            // when
            val response = controller.updateMyPlayerProfile(userId, request)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.primaryPosition).isEqualTo("포수")
            assertThat(response.data?.throwingHand).isEqualTo("좌투")
            assertThat(response.data?.battingHand).isEqualTo("양타")
            assertThat(response.data?.height).isEqualTo(185)
            assertThat(response.data?.weight).isEqualTo(82)
        }

        @Test
        fun `should update with partial fields`() {
            // given
            val userId = 1L
            val updatedPlayer =
                createPlayer(
                    position = Position.STARTING_PITCHER,
                    throwingHand = ThrowingHand.RIGHT,
                    battingHand = BattingHand.RIGHT,
                    height = 185,
                    weight = 75,
                )
            every {
                playerService.updatePlayerProfile(
                    userId = userId,
                    primaryPosition = null,
                    throwingHand = null,
                    battingHand = null,
                    height = 185,
                    weight = null,
                )
            } returns updatedPlayer

            val request =
                com.nextup.api.dto.player.UpdatePlayerProfileRequest(
                    height = 185,
                )

            // when
            val response = controller.updateMyPlayerProfile(userId, request)

            // then
            assertThat(response.success).isTrue()
            assertThat(response.data?.height).isEqualTo(185)
        }

        @Test
        fun `should throw PlayerNotLinkedException when no linked player`() {
            // given
            val userId = 1L
            every {
                playerService.updatePlayerProfile(
                    userId = userId,
                    primaryPosition = any(),
                    throwingHand = any(),
                    battingHand = any(),
                    height = any(),
                    weight = any(),
                )
            } throws PlayerNotLinkedException(userId)

            val request =
                com.nextup.api.dto.player.UpdatePlayerProfileRequest(
                    primaryPosition = Position.CATCHER,
                )

            // when & then
            assertThatThrownBy {
                controller.updateMyPlayerProfile(userId, request)
            }.isInstanceOf(PlayerNotLinkedException::class.java)
        }
    }
}
