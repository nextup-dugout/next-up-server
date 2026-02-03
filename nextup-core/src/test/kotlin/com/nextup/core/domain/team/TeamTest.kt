package com.nextup.core.domain.team

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Team 엔티티 테스트")
class TeamTest {

    private lateinit var league: League

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
    }

    private fun createTeam(
        name: String = "타이거즈",
        city: String = "서울",
        isActive: Boolean = true
    ): Team {
        return Team(
            league = league,
            name = name,
            city = city,
            foundedYear = 2015,
            isActive = isActive
        )
    }

    @Nested
    @DisplayName("전체 이름")
    inner class FullName {

        @Test
        fun `도시명과 팀명을 합쳐서 반환한다`() {
            // given
            val team = createTeam(name = "타이거즈", city = "서울")

            // then
            assertThat(team.fullName).isEqualTo("서울 타이거즈")
        }
    }

    @Nested
    @DisplayName("비활성화")
    inner class Deactivate {

        @Test
        fun `팀을 비활성화할 수 있다`() {
            // given
            val team = createTeam(isActive = true)

            // when
            team.deactivate()

            // then
            assertThat(team.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("활성화")
    inner class Activate {

        @Test
        fun `팀을 활성화할 수 있다`() {
            // given
            val team = createTeam(isActive = false)

            // when
            team.activate()

            // then
            assertThat(team.isActive).isTrue()
        }
    }

    @Nested
    @DisplayName("정보 수정")
    inner class UpdateInfo {

        @Test
        fun `모든 정보를 수정할 수 있다`() {
            // given
            val team = createTeam()

            // when
            team.updateInfo(
                logoUrl = "https://example.com/logo.png",
                primaryColor = "#FF0000",
                secondaryColor = "#FFFFFF"
            )

            // then
            assertThat(team.logoUrl).isEqualTo("https://example.com/logo.png")
            assertThat(team.primaryColor).isEqualTo("#FF0000")
            assertThat(team.secondaryColor).isEqualTo("#FFFFFF")
        }

        @Test
        fun `일부 정보만 수정할 수 있다`() {
            // given
            val team = createTeam().apply {
                updateInfo(
                    logoUrl = "https://example.com/old-logo.png",
                    primaryColor = "#0000FF",
                    secondaryColor = "#000000"
                )
            }

            // when
            team.updateInfo(primaryColor = "#FF0000")

            // then
            assertThat(team.logoUrl).isEqualTo("https://example.com/old-logo.png")
            assertThat(team.primaryColor).isEqualTo("#FF0000")
            assertThat(team.secondaryColor).isEqualTo("#000000")
        }

        @Test
        fun `정보를 null로 설정할 수 있다`() {
            // given
            val team = createTeam().apply {
                updateInfo(logoUrl = "https://example.com/logo.png")
            }

            // when
            team.updateInfo(logoUrl = null)

            // then
            assertThat(team.logoUrl).isNull()
        }
    }
}
