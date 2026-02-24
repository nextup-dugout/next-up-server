package com.nextup.core.domain.competition

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("CompetitionPlayer 엔티티 테스트")
class CompetitionPlayerTest {
    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var competition: Competition
    private lateinit var team: Team
    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        association = Association(name = "서울시야구협회", abbreviation = "SBA", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition =
            Competition(
                league = league,
                name = "2025 춘계대회",
                year = 2025,
                startDate = LocalDate.of(2025, 3, 1),
            )
        team = Team(league = league, name = "Tigers", city = "서울", foundedYear = 2020)
        player = Player(name = "홍길동", primaryPosition = Position.SHORTSTOP, id = 1L)
    }

    @Nested
    @DisplayName("대회 선수 등록")
    inner class Register {
        @Test
        fun `선수를 대회에 등록할 수 있다`() {
            // when
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)

            // then
            assertThat(competitionPlayer.competition).isEqualTo(competition)
            assertThat(competitionPlayer.team).isEqualTo(team)
            assertThat(competitionPlayer.player).isEqualTo(player)
            assertThat(competitionPlayer.status).isEqualTo(CompetitionPlayerStatus.ACTIVE)
        }

        @Test
        fun `신규 등록 선수는 ACTIVE 상태이다`() {
            // when
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)

            // then
            assertThat(competitionPlayer.status).isEqualTo(CompetitionPlayerStatus.ACTIVE)
            assertThat(competitionPlayer.isEligible).isTrue()
        }
    }

    @Nested
    @DisplayName("출전 정지")
    inner class Suspend {
        @Test
        fun `활성 상태의 선수를 출전 정지할 수 있다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)

            // when
            competitionPlayer.suspend()

            // then
            assertThat(competitionPlayer.status).isEqualTo(CompetitionPlayerStatus.SUSPENDED)
            assertThat(competitionPlayer.isEligible).isFalse()
        }

        @Test
        fun `이미 출전 정지된 선수를 다시 정지할 수 없다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)
            competitionPlayer.suspend()

            // when & then
            assertThatThrownBy { competitionPlayer.suspend() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("활성 상태의 선수만 출전 정지할 수 있습니다. 현재 상태: 출전 정지")
        }

        @Test
        fun `등록 취소된 선수를 출전 정지할 수 없다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)
            competitionPlayer.withdraw()

            // when & then
            assertThatThrownBy { competitionPlayer.suspend() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("출전 정지 복귀")
    inner class Reinstate {
        @Test
        fun `출전 정지 상태의 선수를 복귀시킬 수 있다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)
            competitionPlayer.suspend()

            // when
            competitionPlayer.reinstate()

            // then
            assertThat(competitionPlayer.status).isEqualTo(CompetitionPlayerStatus.ACTIVE)
            assertThat(competitionPlayer.isEligible).isTrue()
        }

        @Test
        fun `활성 상태의 선수를 복귀시킬 수 없다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)

            // when & then
            assertThatThrownBy { competitionPlayer.reinstate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("출전 정지 상태의 선수만 복귀시킬 수 있습니다. 현재 상태: 활성")
        }
    }

    @Nested
    @DisplayName("등록 취소")
    inner class Withdraw {
        @Test
        fun `활성 상태의 선수를 등록 취소할 수 있다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)

            // when
            competitionPlayer.withdraw()

            // then
            assertThat(competitionPlayer.status).isEqualTo(CompetitionPlayerStatus.WITHDRAWN)
            assertThat(competitionPlayer.isEligible).isFalse()
        }

        @Test
        fun `출전 정지 상태의 선수도 등록 취소할 수 있다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)
            competitionPlayer.suspend()

            // when
            competitionPlayer.withdraw()

            // then
            assertThat(competitionPlayer.status).isEqualTo(CompetitionPlayerStatus.WITHDRAWN)
        }

        @Test
        fun `이미 등록 취소된 선수를 다시 취소할 수 없다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)
            competitionPlayer.withdraw()

            // when & then
            assertThatThrownBy { competitionPlayer.withdraw() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessage("이미 등록 취소된 선수입니다.")
        }
    }

    @Nested
    @DisplayName("출전 가능 여부")
    inner class IsEligible {
        @Test
        fun `ACTIVE 상태만 출전 가능하다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)

            // then
            assertThat(competitionPlayer.isEligible).isTrue()
        }

        @Test
        fun `SUSPENDED 상태는 출전 불가능하다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)
            competitionPlayer.suspend()

            // then
            assertThat(competitionPlayer.isEligible).isFalse()
        }

        @Test
        fun `WITHDRAWN 상태는 출전 불가능하다`() {
            // given
            val competitionPlayer = CompetitionPlayer.register(competition, team, player)
            competitionPlayer.withdraw()

            // then
            assertThat(competitionPlayer.isEligible).isFalse()
        }
    }
}
