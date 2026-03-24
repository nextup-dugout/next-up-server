package com.nextup.core.domain

import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.event.CompetitionCompletedEvent
import com.nextup.core.domain.event.OwnerKickedEvent
import com.nextup.core.domain.event.StadiumClosedEvent
import com.nextup.core.domain.game.FieldingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GameStatus
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.domain.team.Team
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("L-1~L-13 향후 개선사항 통합 테스트")
class FutureImprovementsTest {

    // === Test helpers ===

    private fun createLeague(): League =
        League(
            name = "테스트 리그",
            association = mockk(relaxed = true),
            foundedYear = 2020,
        )

    private fun createCompetition(gameRules: GameRules = GameRules()): Competition =
        Competition(
            league = createLeague(),
            name = "테스트 대회",
            year = 2026,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2026, 3, 1),
            gameRules = gameRules,
        )

    private fun createTeam(
        id: Long,
        name: String = "팀$id"
    ): Team {
        val team = mockk<Team>(relaxed = true)
        every { team.id } returns id
        every { team.name } returns name
        return team
    }

    private fun createPlayer(id: Long): Player {
        val player = mockk<Player>(relaxed = true)
        every { player.id } returns id
        return player
    }

    // === L-1: 포지션별 수비 기록 분리 ===

    @Nested
    @DisplayName("L-1: 포지션별 수비 기록 분리")
    inner class L1FieldingRecordPositionTest {
        @Test
        fun `수비 기록 생성 시 포지션을 지정할 수 있다`() {
            // given
            val gamePlayer = mockk<com.nextup.core.domain.game.GamePlayer>(relaxed = true)

            // when
            val record = FieldingRecord.create(gamePlayer, Position.SHORTSTOP)

            // then
            assertThat(record.position).isEqualTo(Position.SHORTSTOP)
        }

        @Test
        fun `포지션 없이 수비 기록을 생성하면 position은 null이다`() {
            // given
            val gamePlayer = mockk<com.nextup.core.domain.game.GamePlayer>(relaxed = true)

            // when
            val record = FieldingRecord.create(gamePlayer)

            // then
            assertThat(record.position).isNull()
        }
    }

    // === L-2: 타격방해 vs 주루방해 구분 ===

    @Nested
    @DisplayName("L-2: 타격방해 vs 주루방해 구분")
    inner class L2InterferenceTest {
        @Test
        fun `BATTER_INTERFERENCE는 비타수이며 출루한다`() {
            val result = PlateAppearanceResult.BATTER_INTERFERENCE
            assertThat(result.isAtBat).isFalse()
            assertThat(result.isOnBase).isTrue()
        }

        @Test
        fun `RUNNER_INTERFERENCE는 비타수이며 출루하지 않는다`() {
            val result = PlateAppearanceResult.RUNNER_INTERFERENCE
            assertThat(result.isAtBat).isFalse()
            assertThat(result.isOnBase).isFalse()
        }

        @Test
        fun `기존 INTERFERENCE는 그대로 유지된다`() {
            val result = PlateAppearanceResult.INTERFERENCE
            assertThat(result.isAtBat).isFalse()
            assertThat(result.isOnBase).isTrue()
        }
    }

    // === L-4: 더블헤더 이닝 자동 축소 ===

    @Nested
    @DisplayName("L-4: 더블헤더 이닝 자동 축소")
    inner class L4DoubleheaderInningReductionTest {
        @Test
        fun `더블헤더 경기 생성 시 이닝이 자동 축소된다`() {
            // given
            val competition = createCompetition(GameRules(defaultInnings = 9))
            val homeTeam = createTeam(1L)
            val awayTeam = createTeam(2L)

            // when
            val game =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.now(),
                    isDoubleheader = true,
                    gameNumber = 1,
                )

            // then: 9 - 2 = 7이닝
            assertThat(game.totalInnings).isEqualTo(7)
        }

        @Test
        fun `일반 경기는 이닝 축소가 적용되지 않는다`() {
            // given
            val competition = createCompetition(GameRules(defaultInnings = 9))
            val homeTeam = createTeam(1L)
            val awayTeam = createTeam(2L)

            // when
            val game =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.now(),
                    isDoubleheader = false,
                )

            // then
            assertThat(game.totalInnings).isEqualTo(9)
        }

        @Test
        fun `더블헤더 축소 시 최소 3이닝을 보장한다`() {
            // given
            val competition = createCompetition(GameRules(defaultInnings = 3, doubleheaderInnings = 3))
            val homeTeam = createTeam(1L)
            val awayTeam = createTeam(2L)

            // when
            val game =
                Game.create(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.now(),
                    isDoubleheader = true,
                    gameNumber = 1,
                )

            // then: max(3, 3-2) = 3이닝
            assertThat(game.totalInnings).isEqualTo(3)
        }
    }

    // === L-5: 이닝 축소 정정 시 validate 보완 ===

    @Nested
    @DisplayName("L-5: 이닝 축소 정정 시 validate 보완")
    inner class L5InningReductionValidationTest {
        @Test
        fun `이닝 축소 시 기존 기록과 충돌하면 예외가 발생한다`() {
            // given
            val gamePlayer = mockk<com.nextup.core.domain.game.GamePlayer>(relaxed = true)
            val record = PitchingRecord.create(gamePlayer)
            // 5이닝 = 15 아웃 기록
            repeat(15) { record.recordInningOut() }

            // when & then: 4이닝(12 아웃)으로 축소 시도
            assertThatThrownBy { record.validateInningReduction(4) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("충돌")
        }

        @Test
        fun `이닝 축소 시 기존 기록과 충돌이 없으면 통과한다`() {
            // given
            val gamePlayer = mockk<com.nextup.core.domain.game.GamePlayer>(relaxed = true)
            val record = PitchingRecord.create(gamePlayer)
            // 3이닝 = 9 아웃 기록
            repeat(9) { record.recordInningOut() }

            // when & then (예외 없이 통과)
            record.validateInningReduction(5) // 5이닝 = 15 아웃이므로 OK
        }
    }

    // === L-7: 경기 종료 시 SeasonBattingStats 최종 정합성 검증 ===

    @Nested
    @DisplayName("L-7: 경기 종료 시 SeasonBattingStats 정합성 검증")
    inner class L7ConsistencyVerificationTest {
        @Test
        fun `시즌 통계가 BoxScore 합산과 일치하면 빈 리스트를 반환한다`() {
            // given
            val player = createPlayer(1L)
            val stats = SeasonBattingStats.create(player, 2026)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)

            // when
            val mismatches =
                stats.verifyConsistency(
                    totalPlateAppearances = 2,
                    totalHits = 1,
                    totalAtBats = 1,
                )

            // then
            assertThat(mismatches).isEmpty()
        }

        @Test
        fun `시즌 통계가 BoxScore 합산과 불일치하면 불일치 항목을 반환한다`() {
            // given
            val player = createPlayer(1L)
            val stats = SeasonBattingStats.create(player, 2026)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            // when
            val mismatches =
                stats.verifyConsistency(
                    totalPlateAppearances = 5,
                    totalHits = 3,
                    totalAtBats = 4,
                )

            // then
            assertThat(mismatches).hasSize(3)
            assertThat(mismatches[0]).contains("타석")
            assertThat(mismatches[1]).contains("안타")
            assertThat(mismatches[2]).contains("타수")
        }
    }

    // === L-8: 시즌 통계 아카이브/확정 메커니즘 ===

    @Nested
    @DisplayName("L-8: 시즌 통계 확정 메커니즘")
    inner class L8FinalizeTest {
        @Test
        fun `시즌 타격 통계를 확정할 수 있다`() {
            // given
            val player = createPlayer(1L)
            val stats = SeasonBattingStats.create(player, 2026)

            // when
            stats.finalize()

            // then
            assertThat(stats.isFinalized).isTrue()
        }

        @Test
        fun `이미 확정된 통계를 다시 확정하면 예외가 발생한다`() {
            // given
            val player = createPlayer(1L)
            val stats = SeasonBattingStats.create(player, 2026)
            stats.finalize()

            // when & then
            assertThatThrownBy { stats.finalize() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이미 확정")
        }

        @Test
        fun `확정된 통계를 해제할 수 있다`() {
            // given
            val player = createPlayer(1L)
            val stats = SeasonBattingStats.create(player, 2026)
            stats.finalize()

            // when
            stats.unfinalize()

            // then
            assertThat(stats.isFinalized).isFalse()
        }

        @Test
        fun `시즌 투수 통계를 확정할 수 있다`() {
            // given
            val player = createPlayer(1L)
            val stats = SeasonPitchingStats.create(player, 2026)

            // when
            stats.finalize()

            // then
            assertThat(stats.isFinalized).isTrue()
        }

        @Test
        fun `확정되지 않은 투수 통계를 해제하면 예외가 발생한다`() {
            // given
            val player = createPlayer(1L)
            val stats = SeasonPitchingStats.create(player, 2026)

            // when & then
            assertThatThrownBy { stats.unfinalize() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("확정되지 않은")
        }
    }

    // === L-9: 대회 완료 알림 ===

    @Nested
    @DisplayName("L-9: 대회 완료 이벤트")
    inner class L9CompetitionCompletedEventTest {
        @Test
        fun `대회 완료 이벤트를 생성할 수 있다`() {
            val event =
                CompetitionCompletedEvent(
                    competitionId = 1L,
                    competitionName = "2026 춘계 리그",
                    leagueId = 10L,
                )

            assertThat(event.competitionId).isEqualTo(1L)
            assertThat(event.competitionName).isEqualTo("2026 춘계 리그")
            assertThat(event.leagueId).isEqualTo(10L)
        }
    }

    // === L-10: OWNER 강퇴 시 자동 선거 트리거 ===

    @Nested
    @DisplayName("L-10: OWNER 강퇴 이벤트")
    inner class L10OwnerKickedEventTest {
        @Test
        fun `OWNER 강퇴 이벤트를 생성할 수 있다`() {
            val event =
                OwnerKickedEvent(
                    teamId = 1L,
                    kickedPlayerId = 100L,
                    kickedMemberId = 200L,
                )

            assertThat(event.teamId).isEqualTo(1L)
            assertThat(event.kickedPlayerId).isEqualTo(100L)
            assertThat(event.kickedMemberId).isEqualTo(200L)
        }
    }

    // === L-11: SUSPENDED 경기 자동 타임아웃 ===

    @Nested
    @DisplayName("L-11: SUSPENDED 경기 자동 타임아웃")
    inner class L11SuspendedTimeoutTest {
        @Test
        fun `중단 상태가 아닌 경기는 타임아웃이 아니다`() {
            // given
            val competition = createCompetition()
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = createTeam(1L),
                    awayTeam = createTeam(2L),
                    status = GameStatus.SCHEDULED,
                )

            // when & then
            assertThat(game.isSuspendedTimeout()).isFalse()
        }

        @Test
        fun `중단된 경기를 타임아웃으로 취소할 수 있다`() {
            // given
            val competition = createCompetition()
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = createTeam(1L),
                    awayTeam = createTeam(2L),
                    status = GameStatus.SUSPENDED,
                    currentInning = 5,
                )

            // when
            game.cancelByTimeout()

            // then
            assertThat(game.status).isEqualTo(GameStatus.CANCELLED)
            assertThat(game.note).contains("타임아웃으로 자동 취소")
        }

        @Test
        fun `중단 상태가 아닌 경기를 타임아웃 취소하면 예외가 발생한다`() {
            // given
            val competition = createCompetition()
            val game =
                Game.createForTest(
                    competition = competition,
                    homeTeam = createTeam(1L),
                    awayTeam = createTeam(2L),
                    status = GameStatus.SCHEDULED,
                )

            // when & then
            assertThatThrownBy { game.cancelByTimeout() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("중단 상태")
        }
    }

    // === L-12: 구장 폐업 시 기존 예약 미처리 ===

    @Nested
    @DisplayName("L-12: 구장 폐업 이벤트")
    inner class L12StadiumClosedEventTest {
        @Test
        fun `구장 폐업 이벤트를 생성할 수 있다`() {
            val event =
                StadiumClosedEvent(
                    stadiumId = 1L,
                    stadiumName = "잠실 야구장",
                )

            assertThat(event.stadiumId).isEqualTo(1L)
            assertThat(event.stadiumName).isEqualTo("잠실 야구장")
        }
    }

    // === L-13 is tested in StandingsServiceImplTest ===
}
