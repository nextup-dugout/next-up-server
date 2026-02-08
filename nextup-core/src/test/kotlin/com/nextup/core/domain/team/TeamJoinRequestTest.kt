package com.nextup.core.domain.team

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("TeamJoinRequest 엔티티 테스트")
class TeamJoinRequestTest {
    private lateinit var team: Team
    private lateinit var user: User
    private lateinit var player: Player
    private lateinit var approver: User

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)

        user = User.createLocalUser("applicant@example.com", "password", "지원자")
        player = Player(name = "지원자", primaryPosition = Position.SHORTSTOP)
        approver = User.createLocalUser("approver@example.com", "password", "승인자")
    }

    @Nested
    @DisplayName("가입 신청 생성")
    inner class Create {
        @Test
        fun `should create join request with PENDING status`() {
            // when
            val request =
                TeamJoinRequest.create(
                    team = team,
                    user = user,
                    player = player,
                    desiredUniformNumber = 7,
                    requestMessage = "잘 부탁드립니다",
                )

            // then
            assertThat(request.team).isEqualTo(team)
            assertThat(request.user).isEqualTo(user)
            assertThat(request.player).isEqualTo(player)
            assertThat(request.desiredUniformNumber).isEqualTo(7)
            assertThat(request.requestMessage).isEqualTo("잘 부탁드립니다")
            assertThat(request.status).isEqualTo(JoinRequestStatus.PENDING)
            assertThat(request.isPending).isTrue()
        }

        @Test
        fun `should throw exception when uniform number is out of range`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                TeamJoinRequest.create(team, user, player, 0)
            }

            assertThrows<IllegalArgumentException> {
                TeamJoinRequest.create(team, user, player, 100)
            }
        }
    }

    @Nested
    @DisplayName("가입 승인")
    inner class Approve {
        @Test
        fun `should approve pending request`() {
            // given
            val request = TeamJoinRequest.create(team, user, player, 7)

            // when
            request.approve(approver, "환영합니다")

            // then
            assertThat(request.status).isEqualTo(JoinRequestStatus.APPROVED)
            assertThat(request.processedAt).isNotNull()
            assertThat(request.processedBy).isEqualTo(approver)
            assertThat(request.responseMessage).isEqualTo("환영합니다")
        }

        @Test
        fun `should throw when approving non-pending request`() {
            // given
            val request = TeamJoinRequest.create(team, user, player, 7)
            request.approve(approver)

            // when & then
            assertThrows<IllegalStateException> {
                request.approve(approver)
            }
        }
    }

    @Nested
    @DisplayName("가입 거부")
    inner class Reject {
        @Test
        fun `should reject pending request with reason`() {
            // given
            val request = TeamJoinRequest.create(team, user, player, 7)

            // when
            request.reject(approver, "인원이 충원되었습니다")

            // then
            assertThat(request.status).isEqualTo(JoinRequestStatus.REJECTED)
            assertThat(request.processedAt).isNotNull()
            assertThat(request.processedBy).isEqualTo(approver)
            assertThat(request.responseMessage).isEqualTo("인원이 충원되었습니다")
        }

        @Test
        fun `should throw when rejecting non-pending request`() {
            // given
            val request = TeamJoinRequest.create(team, user, player, 7)
            request.reject(approver)

            // when & then
            assertThrows<IllegalStateException> {
                request.reject(approver)
            }
        }
    }

    @Nested
    @DisplayName("가입 취소")
    inner class Cancel {
        @Test
        fun `should cancel pending request`() {
            // given
            val request = TeamJoinRequest.create(team, user, player, 7)

            // when
            request.cancel()

            // then
            assertThat(request.status).isEqualTo(JoinRequestStatus.REJECTED)
            assertThat(request.processedAt).isNotNull()
            assertThat(request.responseMessage).isEqualTo("신청자가 취소함")
        }

        @Test
        fun `should throw when canceling non-pending request`() {
            // given
            val request = TeamJoinRequest.create(team, user, player, 7)
            request.approve(approver)

            // when & then
            assertThrows<IllegalStateException> {
                request.cancel()
            }
        }
    }
}
