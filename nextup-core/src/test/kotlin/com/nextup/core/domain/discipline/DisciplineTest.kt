package com.nextup.core.domain.discipline

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Discipline 엔티티 테스트")
class DisciplineTest {
    private lateinit var player: Player
    private lateinit var competition: Competition

    @BeforeEach
    fun setUp() {
        val association =
            Association(
                name = "서울시야구협회",
                abbreviation = "SBA",
                region = "서울",
            )

        val league =
            League(
                association = association,
                name = "1부 리그",
                abbreviation = "1st",
                foundedYear = 2020,
                divisionLevel = 1,
            )

        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
            )

        player =
            Player(
                name = "홍길동",
                birthDate = LocalDate.of(1995, 5, 15),
                primaryPosition = Position.CATCHER,
                throwingHand = ThrowingHand.RIGHT,
                battingHand = BattingHand.RIGHT,
            )
    }

    @Nested
    @DisplayName("경고 징계 생성")
    inner class CreateWarning {
        @Test
        fun `경고 징계를 생성할 수 있다`() {
            // when
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "과도한 항의",
                    issuedBy = "심판장",
                )

            // then
            assertThat(discipline.type).isEqualTo(DisciplineType.WARNING)
            assertThat(discipline.reason).isEqualTo("과도한 항의")
            assertThat(discipline.issuedBy).isEqualTo("심판장")
            assertThat(discipline.status).isEqualTo(DisciplineStatus.ACTIVE)
            assertThat(discipline.suspensionGames).isNull()
        }

        @Test
        fun `만료일이 설정된 경고 징계를 생성할 수 있다`() {
            // given
            val expiresAt = LocalDateTime.now().plusMonths(3)

            // when
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                    expiresAt = expiresAt,
                )

            // then
            assertThat(discipline.expiresAt).isEqualTo(expiresAt)
        }

        @Test
        fun `징계 사유가 비어있으면 생성할 수 없다`() {
            // when & then
            assertThatThrownBy {
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "",
                    issuedBy = "심판장",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("징계 사유는 필수입니다.")
        }

        @Test
        fun `발급자가 비어있으면 생성할 수 없다`() {
            // when & then
            assertThatThrownBy {
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("징계 발급자는 필수입니다.")
        }
    }

    @Nested
    @DisplayName("출장 정지 징계 생성")
    inner class CreateSuspension {
        @Test
        fun `출장 정지 징계를 생성할 수 있다`() {
            // when
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )

            // then
            assertThat(discipline.type).isEqualTo(DisciplineType.SUSPENSION)
            assertThat(discipline.reason).isEqualTo("폭력 행위")
            assertThat(discipline.suspensionGames).isEqualTo(3)
            assertThat(discipline.servedGames).isEqualTo(0)
            assertThat(discipline.status).isEqualTo(DisciplineStatus.ACTIVE)
        }

        @Test
        fun `출장 정지 경기 수가 0이면 생성할 수 없다`() {
            // when & then
            assertThatThrownBy {
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 0,
                    issuedBy = "기술위원장",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("출장 정지 경기 수는 1 이상이어야 합니다.")
        }

        @Test
        fun `출장 정지 경기 수가 음수이면 생성할 수 없다`() {
            // when & then
            assertThatThrownBy {
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = -1,
                    issuedBy = "기술위원장",
                )
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("영구 제재 징계 생성")
    inner class CreateBan {
        @Test
        fun `영구 제재 징계를 생성할 수 있다`() {
            // when
            val discipline =
                Discipline.createBan(
                    player = player,
                    competition = competition,
                    reason = "승부 조작",
                    issuedBy = "협회장",
                )

            // then
            assertThat(discipline.type).isEqualTo(DisciplineType.BAN)
            assertThat(discipline.reason).isEqualTo("승부 조작")
            assertThat(discipline.status).isEqualTo(DisciplineStatus.ACTIVE)
            assertThat(discipline.suspensionGames).isNull()
        }
    }

    @Nested
    @DisplayName("징계 취소")
    inner class Cancel {
        @Test
        fun `활성 상태의 징계를 취소할 수 있다`() {
            // given
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            // when
            discipline.cancel()

            // then
            assertThat(discipline.status).isEqualTo(DisciplineStatus.CANCELLED)
        }

        @Test
        fun `이행 완료된 징계는 취소할 수 없다`() {
            // given
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 1,
                    issuedBy = "기술위원장",
                )
            discipline.incrementServedGames() // 이행 완료 처리

            // when & then
            assertThatThrownBy { discipline.cancel() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("활성 상태의 징계만 취소할 수 있습니다.")
        }

        @Test
        fun `취소된 징계는 다시 취소할 수 없다`() {
            // given
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )
            discipline.cancel()

            // when & then
            assertThatThrownBy { discipline.cancel() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("유효성 확인")
    inner class IsEffective {
        @Test
        fun `활성 상태의 징계는 유효하다`() {
            // given
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            // then
            assertThat(discipline.isEffective()).isTrue()
        }

        @Test
        fun `취소된 징계는 유효하지 않다`() {
            // given
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )
            discipline.cancel()

            // then
            assertThat(discipline.isEffective()).isFalse()
        }

        @Test
        fun `만료된 징계는 유효하지 않다`() {
            // given
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                    expiresAt = LocalDateTime.now().minusDays(1),
                )

            // then
            assertThat(discipline.isEffective()).isFalse()
        }

        @Test
        fun `모든 경기를 소화한 출장 정지는 유효하지 않다`() {
            // given
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 2,
                    issuedBy = "기술위원장",
                )
            discipline.incrementServedGames()
            discipline.incrementServedGames()

            // then
            assertThat(discipline.isEffective()).isFalse()
        }
    }

    @Nested
    @DisplayName("소화 경기 수 증가")
    inner class IncrementServedGames {
        @Test
        fun `출장 정지 징계의 소화 경기 수를 증가시킬 수 있다`() {
            // given
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )

            // when
            discipline.incrementServedGames()

            // then
            assertThat(discipline.servedGames).isEqualTo(1)
            assertThat(discipline.status).isEqualTo(DisciplineStatus.ACTIVE)
        }

        @Test
        fun `모든 경기를 소화하면 이행 완료 상태가 된다`() {
            // given
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 2,
                    issuedBy = "기술위원장",
                )

            // when
            discipline.incrementServedGames()
            discipline.incrementServedGames()

            // then
            assertThat(discipline.servedGames).isEqualTo(2)
            assertThat(discipline.status).isEqualTo(DisciplineStatus.SERVED)
        }

        @Test
        fun `경고 징계는 소화 경기 수를 증가시킬 수 없다`() {
            // given
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            // when & then
            assertThatThrownBy { discipline.incrementServedGames() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("출장 정지 징계만 경기를 소화할 수 있습니다.")
        }

        @Test
        fun `취소된 징계는 소화 경기 수를 증가시킬 수 없다`() {
            // given
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )
            discipline.cancel()

            // when & then
            assertThatThrownBy { discipline.incrementServedGames() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("활성 상태의 징계만 경기를 소화할 수 있습니다.")
        }
    }

    @Nested
    @DisplayName("이행 완료 처리")
    inner class MarkServed {
        @Test
        fun `출장 정지 징계를 이행 완료 처리할 수 있다`() {
            // given
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )

            // when
            discipline.markServed()

            // then
            assertThat(discipline.status).isEqualTo(DisciplineStatus.SERVED)
        }

        @Test
        fun `경고 징계는 이행 완료 처리할 수 없다`() {
            // given
            val discipline =
                Discipline.createWarning(
                    player = player,
                    competition = competition,
                    reason = "경고",
                    issuedBy = "심판장",
                )

            // when & then
            assertThatThrownBy { discipline.markServed() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("출장 정지 징계만 이행 완료 처리할 수 있습니다.")
        }

        @Test
        fun `취소된 징계는 이행 완료 처리할 수 없다`() {
            // given
            val discipline =
                Discipline.createSuspension(
                    player = player,
                    competition = competition,
                    reason = "폭력 행위",
                    suspensionGames = 3,
                    issuedBy = "기술위원장",
                )
            discipline.cancel()

            // when & then
            assertThatThrownBy { discipline.markServed() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }
}
