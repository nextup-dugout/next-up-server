package com.nextup.core.domain.player

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("PlayerTeamHistory 엔티티 테스트")
class PlayerTeamHistoryTest {
    private lateinit var player: Player
    private lateinit var team: Team
    private lateinit var otherTeam: Team

    @BeforeEach
    fun setUp() {
        player =
            Player(
                name = "홍길동",
                primaryPosition = Position.SHORTSTOP,
            ).apply {
                setId(this, 1L)
            }

        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)

        team =
            Team(
                league = league,
                name = "타이거즈",
                city = "서울",
                foundedYear = 2015,
            )

        otherTeam =
            Team(
                league = league,
                name = "라이온즈",
                city = "부산",
                foundedYear = 2016,
            )
    }

    private fun setId(
        entity: Any,
        id: Long,
    ) {
        val idField = entity::class.java.getDeclaredField("id")
        idField.isAccessible = true
        idField.set(entity, id)
    }

    private fun createHistory(
        startDate: LocalDate = LocalDate.of(2020, 3, 1),
        endDate: LocalDate? = null,
        uniformNumber: Int? = 7,
        position: Position = Position.SHORTSTOP,
    ): PlayerTeamHistory =
        PlayerTeamHistory(
            player = player,
            team = team,
            startDate = startDate,
            endDate = endDate,
            uniformNumber = uniformNumber,
            position = position,
        )

    @Nested
    @DisplayName("현재 소속 확인")
    inner class IsCurrentAffiliation {
        @Test
        fun `종료일이 없으면 현재 소속이다`() {
            // given
            val history = createHistory(endDate = null)

            // then
            assertThat(history.isCurrentAffiliation).isTrue()
        }

        @Test
        fun `종료일이 있으면 현재 소속이 아니다`() {
            // given
            val history = createHistory(endDate = LocalDate.of(2023, 12, 31))

            // then
            assertThat(history.isCurrentAffiliation).isFalse()
        }
    }

    @Nested
    @DisplayName("소속 기간 계산")
    inner class DurationInDays {
        @Test
        fun `소속 기간을 일 단위로 계산한다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2020, 1, 11),
                )

            // then
            assertThat(history.durationInDays).isEqualTo(10)
        }

        @Test
        fun `종료일이 없으면 null을 반환한다`() {
            // given
            val history = createHistory(endDate = null)

            // then
            assertThat(history.durationInDays).isNull()
        }
    }

    @Nested
    @DisplayName("소속 종료")
    inner class EndAffiliation {
        @Test
        fun `소속을 종료할 수 있다`() {
            // given
            val history = createHistory(startDate = LocalDate.of(2020, 3, 1))

            // when
            history.endAffiliation(LocalDate.of(2023, 12, 31))

            // then
            assertThat(history.endDate).isEqualTo(LocalDate.of(2023, 12, 31))
            assertThat(history.isCurrentAffiliation).isFalse()
        }

        @Test
        fun `종료일이 시작일보다 이전이면 예외가 발생한다`() {
            // given
            val history = createHistory(startDate = LocalDate.of(2020, 3, 1))

            // when & then
            assertThatThrownBy { history.endAffiliation(LocalDate.of(2019, 1, 1)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("시작일 이후")
        }
    }

    @Nested
    @DisplayName("등번호 변경")
    inner class ChangeUniformNumber {
        @Test
        fun `등번호를 변경할 수 있다`() {
            // given
            val history = createHistory(uniformNumber = 7)

            // when
            history.changeUniformNumber(10)

            // then
            assertThat(history.uniformNumber).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("포지션 변경")
    inner class ChangePosition {
        @Test
        fun `포지션을 변경할 수 있다`() {
            // given
            val history = createHistory(position = Position.SHORTSTOP)

            // when
            history.changePosition(Position.THIRD_BASE)

            // then
            assertThat(history.position).isEqualTo(Position.THIRD_BASE)
        }
    }

    @Nested
    @DisplayName("특정 날짜 활동 여부 확인")
    inner class IsActiveAt {
        @Test
        fun `시작일 이전에는 활동 중이 아니다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 3, 1),
                    endDate = LocalDate.of(2023, 12, 31),
                )

            // then
            assertThat(history.isActiveAt(LocalDate.of(2020, 2, 28))).isFalse()
        }

        @Test
        fun `기간 중에는 활동 중이다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 3, 1),
                    endDate = LocalDate.of(2023, 12, 31),
                )

            // then
            assertThat(history.isActiveAt(LocalDate.of(2022, 6, 15))).isTrue()
        }

        @Test
        fun `종료일 이후에는 활동 중이 아니다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 3, 1),
                    endDate = LocalDate.of(2023, 12, 31),
                )

            // then
            assertThat(history.isActiveAt(LocalDate.of(2024, 1, 1))).isFalse()
        }

        @Test
        fun `종료일이 없으면 현재까지 활동 중이다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 3, 1),
                    endDate = null,
                )

            // then
            assertThat(history.isActiveAt(LocalDate.of(2030, 12, 31))).isTrue()
        }
    }

    @Nested
    @DisplayName("이적 처리")
    inner class Transfer {
        @Test
        fun `ACTIVE 상태에서 이적할 수 있다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                )

            // when
            history.transfer(LocalDate.of(2023, 6, 30))

            // then
            assertThat(history.status).isEqualTo(PlayerTeamStatus.TRANSFERRED)
            assertThat(history.endDate).isEqualTo(LocalDate.of(2023, 6, 30))
        }

        @Test
        fun `ACTIVE 상태가 아니면 이적할 수 없다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                )
            history.deactivate(LocalDate.of(2023, 1, 1))

            // when & then
            assertThatThrownBy { history.transfer(LocalDate.of(2023, 6, 30)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("ACTIVE 상태인 선수만")
        }

        @Test
        fun `이적일이 시작일보다 이전이면 예외가 발생한다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                )

            // when & then
            assertThatThrownBy { history.transfer(LocalDate.of(2019, 12, 31)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이적일은")
        }
    }

    @Nested
    @DisplayName("비활동 처리")
    inner class Deactivate {
        @Test
        fun `ACTIVE 상태에서 비활동 처리할 수 있다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                )

            // when
            history.deactivate(LocalDate.of(2023, 6, 30))

            // then
            assertThat(history.status).isEqualTo(PlayerTeamStatus.INACTIVE)
            assertThat(history.endDate).isEqualTo(LocalDate.of(2023, 6, 30))
        }

        @Test
        fun `ACTIVE 상태가 아니면 비활동 처리할 수 없다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                )
            history.transfer(LocalDate.of(2023, 1, 1))

            // when & then
            assertThatThrownBy { history.deactivate(LocalDate.of(2023, 6, 30)) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("ACTIVE 상태인 선수만")
        }

        @Test
        fun `비활동일이 시작일보다 이전이면 예외가 발생한다`() {
            // given
            val history =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                )

            // when & then
            assertThatThrownBy { history.deactivate(LocalDate.of(2019, 12, 31)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("비활동일은")
        }
    }

    @Nested
    @DisplayName("활성 상태 확인")
    inner class IsActive {
        @Test
        fun `ACTIVE 상태면 true를 반환한다`() {
            // given
            val history = createHistory()

            // then
            assertThat(history.isActive).isTrue()
        }

        @Test
        fun `INACTIVE 상태면 false를 반환한다`() {
            // given
            val history = createHistory()
            history.deactivate(LocalDate.now())

            // then
            assertThat(history.isActive).isFalse()
        }

        @Test
        fun `TRANSFERRED 상태면 false를 반환한다`() {
            // given
            val history = createHistory()
            history.transfer(LocalDate.now())

            // then
            assertThat(history.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("기간 중복 확인")
    inner class Overlaps {
        @Test
        fun `같은 선수의 기간이 겹치면 true를 반환한다`() {
            // given
            val history1 =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2021, 12, 31),
                )
            val history2 =
                PlayerTeamHistory(
                    player = player,
                    team = otherTeam,
                    startDate = LocalDate.of(2021, 6, 1),
                    endDate = LocalDate.of(2022, 12, 31),
                    position = Position.SHORTSTOP,
                )

            // then
            assertThat(history1.overlaps(history2)).isTrue()
        }

        @Test
        fun `같은 선수의 기간이 겹치지 않으면 false를 반환한다`() {
            // given
            val history1 =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2021, 12, 31),
                )
            val history2 =
                PlayerTeamHistory(
                    player = player,
                    team = otherTeam,
                    startDate = LocalDate.of(2022, 1, 1),
                    endDate = LocalDate.of(2023, 12, 31),
                    position = Position.SHORTSTOP,
                )

            // then
            assertThat(history1.overlaps(history2)).isFalse()
        }

        @Test
        fun `종료일이 없는 경우도 중복 확인이 가능하다`() {
            // given
            val history1 =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = null,
                )
            val history2 =
                PlayerTeamHistory(
                    player = player,
                    team = otherTeam,
                    startDate = LocalDate.of(2022, 1, 1),
                    endDate = null,
                    position = Position.SHORTSTOP,
                )

            // then
            assertThat(history1.overlaps(history2)).isTrue()
        }

        @Test
        fun `다른 선수의 기간은 중복되지 않는다`() {
            // given
            val otherPlayer =
                Player(name = "김철수", primaryPosition = Position.CATCHER).apply {
                    setId(this, 2L)
                }
            val history1 =
                createHistory(
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2021, 12, 31),
                )
            val history2 =
                PlayerTeamHistory(
                    player = otherPlayer,
                    team = team,
                    startDate = LocalDate.of(2020, 6, 1),
                    endDate = LocalDate.of(2022, 12, 31),
                    position = Position.CATCHER,
                )

            // then
            assertThat(history1.overlaps(history2)).isFalse()
        }
    }
}
