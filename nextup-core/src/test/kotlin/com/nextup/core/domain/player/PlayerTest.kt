package com.nextup.core.domain.player

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Player 엔티티 테스트")
class PlayerTest {
    private fun createPlayer(
        name: String = "홍길동",
        birthDate: LocalDate? = LocalDate.of(1995, 5, 15),
        debutYear: Int? = 2018,
        retirementYear: Int? = null,
    ): Player =
        Player(
            name = name,
            birthDate = birthDate,
            primaryPosition = Position.SHORTSTOP,
            debutYear = debutYear,
            retirementYear = retirementYear,
        )

    @Nested
    @DisplayName("은퇴 처리")
    inner class Retire {
        @Test
        fun `현역 선수를 은퇴 처리할 수 있다`() {
            // given
            val player = createPlayer(debutYear = 2018)

            // when
            player.retire(2024)

            // then
            assertThat(player.retirementYear).isEqualTo(2024)
            assertThat(player.isActive).isFalse()
        }

        @Test
        fun `이미 은퇴한 선수는 다시 은퇴할 수 없다`() {
            // given
            val player = createPlayer(debutYear = 2018, retirementYear = 2023)

            // when & then
            assertThatThrownBy { player.retire(2024) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이미 은퇴한 선수")
        }

        @Test
        fun `은퇴 연도가 데뷔 연도보다 이전이면 예외가 발생한다`() {
            // given
            val player = createPlayer(debutYear = 2018)

            // when & then
            assertThatThrownBy { player.retire(2015) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("데뷔 연도 이후")
        }
    }

    @Nested
    @DisplayName("신체 정보 수정")
    inner class UpdatePhysicalInfo {
        @Test
        fun `키와 몸무게를 수정할 수 있다`() {
            // given
            val player = createPlayer()

            // when
            player.updatePhysicalInfo(height = 182, weight = 78)

            // then
            assertThat(player.height).isEqualTo(182)
            assertThat(player.weight).isEqualTo(78)
        }

        @Test
        fun `키만 수정할 수 있다`() {
            // given
            val player =
                createPlayer().apply {
                    updatePhysicalInfo(height = 180, weight = 75)
                }

            // when
            player.updatePhysicalInfo(height = 183)

            // then
            assertThat(player.height).isEqualTo(183)
            assertThat(player.weight).isEqualTo(75)
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    inner class UpdateProfile {
        @Test
        fun `프로필 이미지 URL을 수정할 수 있다`() {
            // given
            val player = createPlayer()

            // when
            player.updateProfile("https://example.com/profile.jpg")

            // then
            assertThat(player.profileImageUrl).isEqualTo("https://example.com/profile.jpg")
        }

        @Test
        fun `프로필 이미지를 null로 설정할 수 있다`() {
            // given
            val player =
                createPlayer().apply {
                    updateProfile("https://example.com/profile.jpg")
                }

            // when
            player.updateProfile(null)

            // then
            assertThat(player.profileImageUrl).isNull()
        }
    }

    @Nested
    @DisplayName("나이 계산")
    inner class CalculateAge {
        @Test
        fun `생년월일로 나이를 계산할 수 있다`() {
            // given
            val player = createPlayer(birthDate = LocalDate.of(1995, 5, 15))
            val baseDate = LocalDate.of(2024, 12, 1)

            // when
            val age = player.calculateAge(baseDate)

            // then
            assertThat(age).isEqualTo(29)
        }

        @Test
        fun `생일이 지나지 않았으면 1살 적게 계산된다`() {
            // given
            val player = createPlayer(birthDate = LocalDate.of(1995, 5, 15))
            val baseDate = LocalDate.of(2024, 3, 1)

            // when
            val age = player.calculateAge(baseDate)

            // then
            assertThat(age).isEqualTo(28)
        }

        @Test
        fun `생년월일이 없으면 null을 반환한다`() {
            // given
            val player = createPlayer(birthDate = null)

            // when
            val age = player.calculateAge()

            // then
            assertThat(age).isNull()
        }
    }

    @Nested
    @DisplayName("활동 상태 확인")
    inner class IsActive {
        @Test
        fun `은퇴하지 않은 선수는 현역이다`() {
            // given
            val player = createPlayer(retirementYear = null)

            // then
            assertThat(player.isActive).isTrue()
        }

        @Test
        fun `은퇴한 선수는 현역이 아니다`() {
            // given
            val player = createPlayer(retirementYear = 2023)

            // then
            assertThat(player.isActive).isFalse()
        }
    }
}
