package com.nextup.core.domain.competition

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Competition 엔티티 테스트")
class CompetitionTest {
    private lateinit var association: Association
    private lateinit var league: League

    @BeforeEach
    fun setUp() {
        association =
            Association(
                name = "서울시야구협회",
                abbreviation = "SBA",
                region = "서울",
            )
        league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = "1st",
                foundedYear = 2020,
                divisionLevel = 1,
            )
    }

    private fun createCompetition(
        status: CompetitionStatus = CompetitionStatus.SCHEDULED,
        startDate: LocalDate = LocalDate.of(2025, 3, 1),
        endDate: LocalDate? = null,
    ): Competition =
        Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = startDate,
            endDate = endDate,
            status = status,
        )

    @Nested
    @DisplayName("대회 시작")
    inner class Start {
        @Test
        fun `예정된 대회를 시작할 수 있다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.SCHEDULED)

            // when
            competition.start()

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.IN_PROGRESS)
        }

        @Test
        fun `진행 중인 대회는 시작할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.IN_PROGRESS)

            // when & then
            assertThatThrownBy { competition.start() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("예정된 대회만 시작할 수 있습니다.")
        }

        @Test
        fun `완료된 대회는 시작할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.COMPLETED)

            // when & then
            assertThatThrownBy { competition.start() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("대회 완료")
    inner class Complete {
        @Test
        fun `진행 중인 대회를 완료할 수 있다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.IN_PROGRESS)
            val endDate = LocalDate.of(2025, 6, 30)

            // when
            competition.complete(endDate)

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.COMPLETED)
            assertThat(competition.endDate).isEqualTo(endDate)
        }

        @Test
        fun `예정된 대회는 완료할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.SCHEDULED)

            // when & then
            assertThatThrownBy { competition.complete() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("진행 중인 대회만 완료할 수 있습니다.")
        }

        @Test
        fun `종료일이 시작일보다 이전이면 완료할 수 없다`() {
            // given
            val competition =
                createCompetition(
                    status = CompetitionStatus.IN_PROGRESS,
                    startDate = LocalDate.of(2025, 3, 1),
                )

            // when & then
            assertThatThrownBy { competition.complete(LocalDate.of(2025, 2, 1)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("종료일은 시작일 이후여야 합니다.")
        }
    }

    @Nested
    @DisplayName("대회 취소")
    inner class Cancel {
        @Test
        fun `예정된 대회를 취소할 수 있다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.SCHEDULED)

            // when
            competition.cancel()

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.CANCELLED)
        }

        @Test
        fun `진행 중인 대회를 취소할 수 있다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.IN_PROGRESS)

            // when
            competition.cancel()

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.CANCELLED)
        }

        @Test
        fun `완료된 대회는 취소할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.COMPLETED)

            // when & then
            assertThatThrownBy { competition.cancel() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("완료된 대회는 취소할 수 없습니다.")
        }
    }

    @Nested
    @DisplayName("대회 연기")
    inner class Postpone {
        @Test
        fun `예정된 대회를 연기할 수 있다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.SCHEDULED)

            // when
            competition.postpone()

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.POSTPONED)
        }

        @Test
        fun `진행 중인 대회를 연기할 수 있다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.IN_PROGRESS)

            // when
            competition.postpone()

            // then
            assertThat(competition.status).isEqualTo(CompetitionStatus.POSTPONED)
        }

        @Test
        fun `완료된 대회는 연기할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.COMPLETED)

            // when & then
            assertThatThrownBy { competition.postpone() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("예정 중이거나 진행 중인 대회만 연기할 수 있습니다.")
        }

        @Test
        fun `취소된 대회는 연기할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.CANCELLED)

            // when & then
            assertThatThrownBy { competition.postpone() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("예정 중이거나 진행 중인 대회만 연기할 수 있습니다.")
        }
    }

    @Nested
    @DisplayName("대회 정보 업데이트")
    inner class UpdateInfo {
        @Test
        fun `대회명을 변경할 수 있다`() {
            // given
            val competition = createCompetition()
            val newName = "2025 추계대회"

            // when
            competition.updateInfo(name = newName)

            // then
            assertThat(competition.name).isEqualTo(newName)
        }

        @Test
        fun `설명을 변경할 수 있다`() {
            // given
            val competition = createCompetition()
            val newDescription = "2025 정규 리그 추계 시즌"

            // when
            competition.updateInfo(description = newDescription)

            // then
            assertThat(competition.description).isEqualTo(newDescription)
        }

        @Test
        fun `종료일을 변경할 수 있다`() {
            // given
            val competition = createCompetition(startDate = LocalDate.of(2025, 3, 1))
            val newEndDate = LocalDate.of(2025, 9, 30)

            // when
            competition.updateInfo(endDate = newEndDate)

            // then
            assertThat(competition.endDate).isEqualTo(newEndDate)
        }

        @Test
        fun `종료일이 시작일보다 이전이면 업데이트할 수 없다`() {
            // given
            val competition = createCompetition(startDate = LocalDate.of(2025, 3, 1))

            // when & then
            assertThatThrownBy { competition.updateInfo(endDate = LocalDate.of(2025, 2, 28)) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("종료일은 시작일 이후여야 합니다.")
        }

        @Test
        fun `여러 필드를 동시에 업데이트할 수 있다`() {
            // given
            val competition = createCompetition(startDate = LocalDate.of(2025, 3, 1))
            val newName = "2025 추계대회"
            val newDescription = "추계 시즌"
            val newEndDate = LocalDate.of(2025, 10, 31)

            // when
            competition.updateInfo(name = newName, description = newDescription, endDate = newEndDate)

            // then
            assertThat(competition.name).isEqualTo(newName)
            assertThat(competition.description).isEqualTo(newDescription)
            assertThat(competition.endDate).isEqualTo(newEndDate)
        }

        @Test
        fun `null 값은 기존 값을 유지한다`() {
            // given
            val competition =
                createCompetition(startDate = LocalDate.of(2025, 3, 1)).apply {
                    updateInfo(description = "원본 설명")
                }

            // when
            competition.updateInfo(name = null, description = null, endDate = null)

            // then
            assertThat(competition.name).isEqualTo("2025 춘계대회")
            assertThat(competition.description).isEqualTo("원본 설명")
        }
    }

    @Nested
    @DisplayName("활성 상태 확인")
    inner class IsActive {
        @Test
        fun `진행 중인 대회는 활성 상태이다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.IN_PROGRESS)

            // then
            assertThat(competition.isActive).isTrue()
        }

        @Test
        fun `예정된 대회는 활성 상태가 아니다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.SCHEDULED)

            // then
            assertThat(competition.isActive).isFalse()
        }

        @Test
        fun `완료된 대회는 활성 상태가 아니다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.COMPLETED)

            // then
            assertThat(competition.isActive).isFalse()
        }
    }

    @Nested
    @DisplayName("특정 날짜 활성 상태 확인")
    inner class IsActiveAt {
        @Test
        fun `진행 중인 대회의 시작일과 종료일 사이 날짜는 활성이다`() {
            // given
            val competition =
                createCompetition(
                    status = CompetitionStatus.IN_PROGRESS,
                    startDate = LocalDate.of(2025, 3, 1),
                    endDate = LocalDate.of(2025, 6, 30),
                )

            // then
            assertThat(competition.isActiveAt(LocalDate.of(2025, 4, 15))).isTrue()
        }

        @Test
        fun `대회 시작일 이전 날짜는 활성이 아니다`() {
            // given
            val competition =
                createCompetition(
                    status = CompetitionStatus.IN_PROGRESS,
                    startDate = LocalDate.of(2025, 3, 1),
                )

            // then
            assertThat(competition.isActiveAt(LocalDate.of(2025, 2, 28))).isFalse()
        }

        @Test
        fun `예정된 대회는 어떤 날짜에도 활성이 아니다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.SCHEDULED)

            // then
            assertThat(competition.isActiveAt(LocalDate.of(2025, 4, 15))).isFalse()
        }
    }

    @Nested
    @DisplayName("경기 규칙 업데이트")
    inner class UpdateGameRules {
        @Test
        fun `기본 GameRules가 설정된다`() {
            // given
            val competition = createCompetition()

            // then
            assertThat(competition.gameRules.defaultInnings).isEqualTo(9)
            assertThat(competition.gameRules.forfeitScore).isEqualTo(7)
        }

        @Test
        fun `예정 상태의 대회는 규칙을 변경할 수 있다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.SCHEDULED)
            val newRules = GameRules(defaultInnings = 7, forfeitScore = 9)

            // when
            competition.updateGameRules(newRules)

            // then
            assertThat(competition.gameRules.defaultInnings).isEqualTo(7)
            assertThat(competition.gameRules.forfeitScore).isEqualTo(9)
        }

        @Test
        fun `진행 중인 대회는 규칙을 변경할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.IN_PROGRESS)
            val newRules = GameRules(defaultInnings = 7)

            // when & then
            assertThatThrownBy { competition.updateGameRules(newRules) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("예정 상태")
        }

        @Test
        fun `완료된 대회는 규칙을 변경할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.COMPLETED)
            val newRules = GameRules(defaultInnings = 7)

            // when & then
            assertThatThrownBy { competition.updateGameRules(newRules) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("예정 상태")
        }

        @Test
        fun `취소된 대회는 규칙을 변경할 수 없다`() {
            // given
            val competition = createCompetition(status = CompetitionStatus.CANCELLED)
            val newRules = GameRules(defaultInnings = 7)

            // when & then
            assertThatThrownBy { competition.updateGameRules(newRules) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("예정 상태")
        }

        @Test
        fun `사용자 정의 GameRules로 대회를 생성할 수 있다`() {
            // given
            val customRules =
                GameRules(
                    defaultInnings = 7,
                    mercyRuleEnabled = true,
                    mercyRunDifference = 10,
                    mercyMinimumInning = 5,
                    forfeitScore = 9,
                )
            val competition =
                Competition(
                    league = league,
                    name = "2025 사회인 대회",
                    year = 2025,
                    season = 1,
                    type = CompetitionType.LEAGUE,
                    startDate = LocalDate.of(2025, 3, 1),
                    gameRules = customRules,
                )

            // then
            assertThat(competition.gameRules.defaultInnings).isEqualTo(7)
            assertThat(competition.gameRules.mercyRuleEnabled).isTrue()
            assertThat(competition.gameRules.forfeitScore).isEqualTo(9)
        }
    }
}
