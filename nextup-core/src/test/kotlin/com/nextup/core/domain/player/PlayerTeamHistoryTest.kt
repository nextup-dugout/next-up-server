package com.nextup.core.domain.player

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("PlayerTeamHistory 테스트")
class PlayerTeamHistoryTest {

    private lateinit var player: Player
    private lateinit var team: Team

    @BeforeEach
    fun setUp() {
        player = Player(
            name = "테스트선수",
            primaryPosition = Position.SHORTSTOP
        )

        val association = Association(
            name = "테스트협회",
            region = "서울"
        )

        val league = League(
            association = association,
            name = "테스트리그",
            foundedYear = 2020
        )

        team = Team(
            league = league,
            name = "테스트팀",
            city = "서울",
            foundedYear = 2020
        )
    }

    @Nested
    @DisplayName("상태 관리")
    inner class StatusManagement {

        @Test
        fun `should have ACTIVE status by default`() {
            // given
            val history = createHistory()

            // then
            assertThat(history.status).isEqualTo(PlayerTeamStatus.ACTIVE)
        }

        @Test
        fun `should transfer player successfully when status is ACTIVE`() {
            // given
            val history = createHistory()
            val transferDate = LocalDate.now()

            // when
            history.transfer(transferDate)

            // then
            assertThat(history.status).isEqualTo(PlayerTeamStatus.TRANSFERRED)
            assertThat(history.endDate).isEqualTo(transferDate)
        }

        @Test
        fun `should throw exception when transferring already transferred player`() {
            // given
            val history = createHistory()
            history.transfer(LocalDate.now())

            // when & then
            assertThrows<IllegalStateException> {
                history.transfer(LocalDate.now())
            }
        }

        @Test
        fun `should throw exception when transferring inactive player`() {
            // given
            val history = createHistory()
            history.deactivate(LocalDate.now())

            // when & then
            assertThrows<IllegalStateException> {
                history.transfer(LocalDate.now())
            }
        }

        @Test
        fun `should deactivate player successfully when status is ACTIVE`() {
            // given
            val history = createHistory()
            val deactivateDate = LocalDate.now()

            // when
            history.deactivate(deactivateDate)

            // then
            assertThat(history.status).isEqualTo(PlayerTeamStatus.INACTIVE)
            assertThat(history.endDate).isEqualTo(deactivateDate)
        }

        @Test
        fun `should throw exception when deactivating already inactive player`() {
            // given
            val history = createHistory()
            history.deactivate(LocalDate.now())

            // when & then
            assertThrows<IllegalStateException> {
                history.deactivate(LocalDate.now())
            }
        }

        @Test
        fun `should throw exception when deactivating transferred player`() {
            // given
            val history = createHistory()
            history.transfer(LocalDate.now())

            // when & then
            assertThrows<IllegalStateException> {
                history.deactivate(LocalDate.now())
            }
        }
    }

    @Nested
    @DisplayName("활성 상태 확인")
    inner class ActiveStatusCheck {

        @Test
        fun `isActive should return true when status is ACTIVE`() {
            // given
            val history = createHistory()

            // then
            assertThat(history.isActive).isTrue()
        }

        @Test
        fun `isActive should return false when status is TRANSFERRED`() {
            // given
            val history = createHistory()
            history.transfer(LocalDate.now())

            // then
            assertThat(history.isActive).isFalse()
        }

        @Test
        fun `isActive should return false when status is INACTIVE`() {
            // given
            val history = createHistory()
            history.deactivate(LocalDate.now())

            // then
            assertThat(history.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("날짜 유효성 검증")
    inner class DateValidation {

        @Test
        fun `should throw exception when transfer date is before start date`() {
            // given
            val startDate = LocalDate.of(2024, 6, 1)
            val history = createHistory(startDate = startDate)
            val transferDate = LocalDate.of(2024, 5, 1)

            // when & then
            assertThrows<IllegalArgumentException> {
                history.transfer(transferDate)
            }
        }

        @Test
        fun `should throw exception when deactivate date is before start date`() {
            // given
            val startDate = LocalDate.of(2024, 6, 1)
            val history = createHistory(startDate = startDate)
            val deactivateDate = LocalDate.of(2024, 5, 1)

            // when & then
            assertThrows<IllegalArgumentException> {
                history.deactivate(deactivateDate)
            }
        }

        @Test
        fun `should allow transfer on the same day as start date`() {
            // given
            val startDate = LocalDate.of(2024, 6, 1)
            val history = createHistory(startDate = startDate)

            // when
            history.transfer(startDate)

            // then
            assertThat(history.status).isEqualTo(PlayerTeamStatus.TRANSFERRED)
            assertThat(history.endDate).isEqualTo(startDate)
        }

        @Test
        fun `should allow deactivate on the same day as start date`() {
            // given
            val startDate = LocalDate.of(2024, 6, 1)
            val history = createHistory(startDate = startDate)

            // when
            history.deactivate(startDate)

            // then
            assertThat(history.status).isEqualTo(PlayerTeamStatus.INACTIVE)
            assertThat(history.endDate).isEqualTo(startDate)
        }
    }

    @Nested
    @DisplayName("기존 기능 유지")
    inner class ExistingFunctionality {

        @Test
        fun `endAffiliation should still work independently of status`() {
            // given
            val history = createHistory()
            val endDate = LocalDate.now()

            // when
            history.endAffiliation(endDate)

            // then
            assertThat(history.endDate).isEqualTo(endDate)
            // 상태는 변경되지 않음 (기존 동작 유지)
            assertThat(history.status).isEqualTo(PlayerTeamStatus.ACTIVE)
        }

        @Test
        fun `isCurrentAffiliation should return false when endDate is set`() {
            // given
            val history = createHistory()
            history.endAffiliation(LocalDate.now())

            // then
            assertThat(history.isCurrentAffiliation).isFalse()
        }

        @Test
        fun `isCurrentAffiliation should return true when endDate is null`() {
            // given
            val history = createHistory()

            // then
            assertThat(history.isCurrentAffiliation).isTrue()
        }

        @Test
        fun `changeUniformNumber should work correctly`() {
            // given
            val history = createHistory()

            // when
            history.changeUniformNumber(7)

            // then
            assertThat(history.uniformNumber).isEqualTo(7)
        }

        @Test
        fun `changePosition should work correctly`() {
            // given
            val history = createHistory()

            // when
            history.changePosition(Position.SECOND_BASE)

            // then
            assertThat(history.position).isEqualTo(Position.SECOND_BASE)
        }

        @Test
        fun `isActiveAt should return true when date is within range`() {
            // given
            val startDate = LocalDate.of(2024, 1, 1)
            val history = createHistory(startDate = startDate)

            // when & then
            assertThat(history.isActiveAt(LocalDate.of(2024, 6, 1))).isTrue()
            assertThat(history.isActiveAt(startDate)).isTrue()
        }

        @Test
        fun `isActiveAt should return false when date is before start date`() {
            // given
            val startDate = LocalDate.of(2024, 6, 1)
            val history = createHistory(startDate = startDate)

            // when & then
            assertThat(history.isActiveAt(LocalDate.of(2024, 5, 1))).isFalse()
        }

        @Test
        fun `durationInDays should return null when endDate is null`() {
            // given
            val history = createHistory()

            // then
            assertThat(history.durationInDays).isNull()
        }

        @Test
        fun `durationInDays should return correct days when endDate is set`() {
            // given
            val startDate = LocalDate.of(2024, 1, 1)
            val endDate = LocalDate.of(2024, 1, 11)
            val history = createHistory(startDate = startDate)
            history.endAffiliation(endDate)

            // then
            assertThat(history.durationInDays).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("PlayerTeamStatus enum 테스트")
    inner class PlayerTeamStatusTest {

        @Test
        fun `ACTIVE should have correct display name`() {
            assertThat(PlayerTeamStatus.ACTIVE.displayName).isEqualTo("활동중")
        }

        @Test
        fun `INACTIVE should have correct display name`() {
            assertThat(PlayerTeamStatus.INACTIVE.displayName).isEqualTo("비활동")
        }

        @Test
        fun `TRANSFERRED should have correct display name`() {
            assertThat(PlayerTeamStatus.TRANSFERRED.displayName).isEqualTo("이적")
        }
    }

    // Helper methods

    private fun createHistory(
        startDate: LocalDate = LocalDate.now()
    ): PlayerTeamHistory {
        return PlayerTeamHistory(
            player = player,
            team = team,
            startDate = startDate,
            position = Position.SHORTSTOP
        )
    }
}
