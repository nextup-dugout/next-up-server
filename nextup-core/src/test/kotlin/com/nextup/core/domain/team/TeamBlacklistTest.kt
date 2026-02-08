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
import java.time.Duration

@DisplayName("TeamBlacklist 엔티티 테스트")
class TeamBlacklistTest {
    private lateinit var team: Team
    private lateinit var user: User
    private lateinit var player: Player
    private lateinit var registrar: User

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team = Team(league = league, name = "타이거즈", city = "서울", foundedYear = 2015)

        user = User.createLocalUser("blacklisted@example.com", "password", "블랙리스트 회원")
        player = Player(name = "블랙리스트 회원", primaryPosition = Position.SHORTSTOP)
        registrar = User.createLocalUser("admin@example.com", "password", "관리자")
    }

    @Nested
    @DisplayName("영구 블랙리스트 생성")
    inner class CreatePermanent {
        @Test
        fun `should create permanent blacklist`() {
            // when
            val blacklist =
                TeamBlacklist.createPermanent(
                    team = team,
                    user = user,
                    player = player,
                    reason = "회비 미납 및 연락 두절",
                    registeredBy = registrar,
                )

            // then
            assertThat(blacklist.team).isEqualTo(team)
            assertThat(blacklist.user).isEqualTo(user)
            assertThat(blacklist.player).isEqualTo(player)
            assertThat(blacklist.reason).isEqualTo("회비 미납 및 연락 두절")
            assertThat(blacklist.registeredBy).isEqualTo(registrar)
            assertThat(blacklist.expiresAt).isNull()
            assertThat(blacklist.isPermanent).isTrue()
            assertThat(blacklist.isActive).isTrue()
            assertThat(blacklist.isExpired).isFalse()
        }

        @Test
        fun `should throw exception when reason is blank`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                TeamBlacklist.createPermanent(team, user, player, "", registrar)
            }
        }
    }

    @Nested
    @DisplayName("기한부 블랙리스트 생성")
    inner class CreateTemporary {
        @Test
        fun `should create temporary blacklist`() {
            // given
            val duration = Duration.ofDays(30)

            // when
            val blacklist =
                TeamBlacklist.createTemporary(
                    team = team,
                    user = user,
                    player = player,
                    reason = "경고 1회",
                    registeredBy = registrar,
                    duration = duration,
                )

            // then
            assertThat(blacklist.expiresAt).isNotNull()
            assertThat(blacklist.isPermanent).isFalse()
            assertThat(blacklist.isActive).isTrue()
            assertThat(blacklist.isExpired).isFalse()
        }

        @Test
        fun `should throw exception when duration is negative`() {
            // when & then
            assertThrows<IllegalArgumentException> {
                TeamBlacklist.createTemporary(
                    team,
                    user,
                    player,
                    "test",
                    registrar,
                    Duration.ofDays(-1),
                )
            }
        }
    }

    @Nested
    @DisplayName("블랙리스트 만료 확인")
    inner class ExpirationCheck {
        @Test
        fun `should check if blacklist is expired`() {
            // given - 매우 짧은 기간으로 생성하고 시간이 지나도록 설정
            val veryShortDuration = Duration.ofMillis(1)
            val blacklist =
                TeamBlacklist.createTemporary(
                    team,
                    user,
                    player,
                    "test",
                    registrar,
                    veryShortDuration,
                )

            // when - expiresAt을 과거로 설정 (리플렉션)
            val expiresAtField = TeamBlacklist::class.java.getDeclaredField("expiresAt")
            expiresAtField.isAccessible = true
            expiresAtField.set(blacklist, java.time.LocalDateTime.now().minusDays(1))

            // then
            assertThat(blacklist.isExpired).isTrue()
            assertThat(blacklist.isActive).isFalse()
        }

        @Test
        fun `should check if blacklist is active`() {
            // given
            val futureDuration = Duration.ofDays(30)
            val blacklist =
                TeamBlacklist.createTemporary(
                    team,
                    user,
                    player,
                    "test",
                    registrar,
                    futureDuration,
                )

            // then
            assertThat(blacklist.isExpired).isFalse()
            assertThat(blacklist.isActive).isTrue()
        }

        @Test
        fun `should permanent blacklist always be active`() {
            // given
            val blacklist = TeamBlacklist.createPermanent(team, user, player, "test", registrar)

            // then
            assertThat(blacklist.isActive).isTrue()
            assertThat(blacklist.isExpired).isFalse()
        }
    }
}
