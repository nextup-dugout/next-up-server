package com.nextup.core.domain.game

import com.nextup.common.exception.InvalidLineupBattingOrderCountException
import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.event.PitchCountWarningEvent
import com.nextup.core.domain.event.PitchCountWarningType
import com.nextup.core.domain.event.TimeLimitWarningEvent
import com.nextup.core.domain.event.TimeLimitWarningType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.domain.user.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("야구 도메인 세부 규칙 보완 테스트 (#344)")
class BaseballDomainRulesTest {
    private lateinit var league: League
    private lateinit var team1: Team
    private lateinit var team2: Team

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        team1 = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        team2 = Team(league = league, name = "원정팀", city = "서울", foundedYear = 2020, id = 2L)
    }

    private fun createCompetition(rules: GameRules = GameRules()): Competition =
        Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            status = CompetitionStatus.IN_PROGRESS,
            gameRules = rules,
        )

    private fun createGame(
        competition: Competition,
        currentInning: Int = 1,
        isTopInning: Boolean = true,
        startedAt: LocalDateTime? = null,
    ): Game =
        Game.createForTest(
            competition = competition,
            homeTeam = team1,
            awayTeam = team2,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            status = GameStatus.IN_PROGRESS,
            currentInning = currentInning,
            isTopInning = isTopInning,
            startedAt = startedAt,
        )

    // ── M-1: 타이브레이크 주자의 담당 투수 ID 설정 ───────────────────

    @Nested
    @DisplayName("M-1: 타이브레이크 주자의 담당 투수 ID 설정")
    inner class TiebreakerPitcherId {

        @Test
        fun `타이브레이크 주자 배치 시 현재 투수 ID가 담당 투수로 설정된다`() {
            // given
            val gameState = GameState(currentPitcherId = 42L)

            // when
            gameState.setupTiebreaker(firstRunnerId = 100L, secondRunnerId = 200L)

            // then
            assertThat(gameState.runnerOnFirstId).isEqualTo(100L)
            assertThat(gameState.runnerOnSecondId).isEqualTo(200L)
            assertThat(gameState.runnerOnFirstPitcherId).isEqualTo(42L)
            assertThat(gameState.runnerOnSecondPitcherId).isEqualTo(42L)
            assertThat(gameState.runnerOnThirdId).isNull()
            assertThat(gameState.runnerOnThirdPitcherId).isNull()
        }

        @Test
        fun `타이브레이크 주자 ID가 null이면 담당 투수 ID도 null이다`() {
            // given
            val gameState = GameState(currentPitcherId = 42L)

            // when
            gameState.setupTiebreaker(firstRunnerId = null, secondRunnerId = null)

            // then
            assertThat(gameState.runnerOnFirstPitcherId).isNull()
            assertThat(gameState.runnerOnSecondPitcherId).isNull()
        }

        @Test
        fun `타이브레이크 주자 배치 시 기존 3루 담당 투수 ID도 제거된다`() {
            // given
            val gameState =
                GameState(
                    currentPitcherId = 42L,
                    runnerOnThirdId = 300L,
                    runnerOnThirdPitcherId = 10L,
                )

            // when
            gameState.setupTiebreaker(firstRunnerId = 100L, secondRunnerId = 200L)

            // then
            assertThat(gameState.runnerOnThirdPitcherId).isNull()
        }

        @Test
        fun `현재 투수 ID가 null이면 담당 투수 ID도 null로 설정된다`() {
            // given
            val gameState = GameState(currentPitcherId = null)

            // when
            gameState.setupTiebreaker(firstRunnerId = 100L, secondRunnerId = 200L)

            // then
            assertThat(gameState.runnerOnFirstPitcherId).isNull()
            assertThat(gameState.runnerOnSecondPitcherId).isNull()
        }
    }

    // ── M-3: 낫아웃 삼진(Dropped Third Strike) ──────────────────

    @Nested
    @DisplayName("M-3: 낫아웃 삼진(Dropped Third Strike)")
    inner class DroppedThirdStrike {

        @Test
        fun `낫아웃 삼진은 삼진이다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isStrikeout).isTrue()
        }

        @Test
        fun `낫아웃 삼진은 타수에 포함된다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isAtBat).isTrue()
        }

        @Test
        fun `낫아웃 삼진은 안타가 아니다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isHit).isFalse()
        }

        @Test
        fun `낫아웃 삼진은 출루에 성공한다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isOnBase).isTrue()
        }

        @Test
        fun `낫아웃 삼진은 isDroppedThirdStrike이다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.isDroppedThirdStrike).isTrue()
        }

        @Test
        fun `일반 삼진은 isDroppedThirdStrike이 아니다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT.isDroppedThirdStrike).isFalse()
        }

        @Test
        fun `일반 삼진은 출루에 실패한다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT.isOnBase).isFalse()
        }

        @Test
        fun `낫아웃 삼진의 루타 수는 0이다`() {
            assertThat(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD.totalBases).isEqualTo(0)
        }
    }

    // ── M-4: FieldingRecord triplePlays 필드 ────────────────────

    @Nested
    @DisplayName("M-4: FieldingRecord triplePlays 필드")
    inner class FieldingRecordTriplePlays {

        private fun createFieldingRecord(): FieldingRecord {
            val competition = createCompetition()
            val game = createGame(competition)
            val gameTeam = GameTeam(game = game, team = team1, homeAway = HomeAway.HOME)
            val player = Player(name = "테스트선수", primaryPosition = Position.SHORTSTOP)
            val gamePlayer = GamePlayer.createStarter(gameTeam, player, Position.SHORTSTOP, 6)
            return FieldingRecord.create(gamePlayer)
        }

        @Test
        fun `삼중살 관여를 기록할 수 있다`() {
            // given
            val record = createFieldingRecord()

            // when
            record.recordTriplePlay()

            // then
            assertThat(record.triplePlays).isEqualTo(1)
        }

        @Test
        fun `삼중살 관여를 여러 번 기록할 수 있다`() {
            // given
            val record = createFieldingRecord()

            // when
            record.recordTriplePlay()
            record.recordTriplePlay()

            // then
            assertThat(record.triplePlays).isEqualTo(2)
        }

        @Test
        fun `삼중살 관여를 취소할 수 있다`() {
            // given
            val record = createFieldingRecord()
            record.recordTriplePlay()

            // when
            record.revertTriplePlay()

            // then
            assertThat(record.triplePlays).isEqualTo(0)
        }

        @Test
        fun `삼중살 관여가 없을 때 취소하면 예외가 발생한다`() {
            // given
            val record = createFieldingRecord()

            // when & then
            assertThatThrownBy { record.revertTriplePlay() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("취소할 삼중살 관여 기록이 없습니다.")
        }

        @Test
        fun `기본값은 0이다`() {
            // given
            val record = createFieldingRecord()

            // then
            assertThat(record.triplePlays).isEqualTo(0)
        }
    }

    // ── M-5: 시간 제한 경고/알림 ────────────────────────────────

    @Nested
    @DisplayName("M-5: 시간 제한 경고/알림")
    inner class TimeLimitWarning {

        @Test
        fun `시간 제한이 없으면 null을 반환한다`() {
            // given
            val competition = createCompetition(GameRules(timeLimitMinutes = null))
            val game =
                createGame(competition, startedAt = LocalDateTime.of(2025, 4, 15, 14, 0))

            // when
            val status =
                game.checkTimeLimitStatus(now = LocalDateTime.of(2025, 4, 15, 16, 30))

            // then
            assertThat(status).isNull()
        }

        @Test
        fun `경기가 시작되지 않으면 null을 반환한다`() {
            // given
            val competition = createCompetition(GameRules(timeLimitMinutes = 120))
            val game = createGame(competition, startedAt = null)

            // when
            val status =
                game.checkTimeLimitStatus(now = LocalDateTime.of(2025, 4, 15, 16, 30))

            // then
            assertThat(status).isNull()
        }

        @Test
        fun `시간 제한 임박 시 APPROACHING_LIMIT을 반환한다`() {
            // given: 120분 제한, 115분 경과
            val competition = createCompetition(GameRules(timeLimitMinutes = 120))
            val game =
                createGame(competition, startedAt = LocalDateTime.of(2025, 4, 15, 14, 0))

            // when
            val status =
                game.checkTimeLimitStatus(
                    now = LocalDateTime.of(2025, 4, 15, 15, 55),
                )

            // then
            assertThat(status).isEqualTo(TimeLimitStatus.APPROACHING_LIMIT)
        }

        @Test
        fun `시간 제한 도달 시 LIMIT_REACHED를 반환한다`() {
            // given: 120분 제한, 120분 경과
            val competition = createCompetition(GameRules(timeLimitMinutes = 120))
            val game =
                createGame(competition, startedAt = LocalDateTime.of(2025, 4, 15, 14, 0))

            // when
            val status =
                game.checkTimeLimitStatus(
                    now = LocalDateTime.of(2025, 4, 15, 16, 0),
                )

            // then
            assertThat(status).isEqualTo(TimeLimitStatus.LIMIT_REACHED)
        }

        @Test
        fun `시간 제한 초과 시에도 LIMIT_REACHED를 반환한다`() {
            // given: 120분 제한, 130분 경과
            val competition = createCompetition(GameRules(timeLimitMinutes = 120))
            val game =
                createGame(competition, startedAt = LocalDateTime.of(2025, 4, 15, 14, 0))

            // when
            val status =
                game.checkTimeLimitStatus(
                    now = LocalDateTime.of(2025, 4, 15, 16, 10),
                )

            // then
            assertThat(status).isEqualTo(TimeLimitStatus.LIMIT_REACHED)
        }

        @Test
        fun `시간 제한 이내이면 null을 반환한다`() {
            // given: 120분 제한, 60분 경과
            val competition = createCompetition(GameRules(timeLimitMinutes = 120))
            val game =
                createGame(competition, startedAt = LocalDateTime.of(2025, 4, 15, 14, 0))

            // when
            val status =
                game.checkTimeLimitStatus(
                    now = LocalDateTime.of(2025, 4, 15, 15, 0),
                )

            // then
            assertThat(status).isNull()
        }

        @Test
        fun `TimeLimitWarningEvent 남은 시간이 올바르게 계산된다`() {
            // given
            val event =
                TimeLimitWarningEvent(
                    gameId = 1L,
                    startedAt = java.time.Instant.now(),
                    timeLimitMinutes = 120,
                    elapsedMinutes = 115,
                    warningType = TimeLimitWarningType.APPROACHING_LIMIT,
                )

            // then
            assertThat(event.remainingMinutes).isEqualTo(5)
        }

        @Test
        fun `TimeLimitWarningEvent 시간 초과 시 남은 시간이 음수이다`() {
            // given
            val event =
                TimeLimitWarningEvent(
                    gameId = 1L,
                    startedAt = java.time.Instant.now(),
                    timeLimitMinutes = 120,
                    elapsedMinutes = 130,
                    warningType = TimeLimitWarningType.LIMIT_REACHED,
                )

            // then
            assertThat(event.remainingMinutes).isEqualTo(-10)
        }
    }

    // ── M-6: 도루/견제 투수/포수 기록 반영 ──────────────────────

    @Nested
    @DisplayName("M-6: PitchingRecord 도루/견제 기록 반영")
    inner class PitchingRecordBaseRunning {

        private fun createPitchingRecord(): PitchingRecord {
            val competition = createCompetition()
            val game = createGame(competition)
            val gameTeam = GameTeam(game = game, team = team1, homeAway = HomeAway.HOME)
            val player = Player(name = "투수", primaryPosition = Position.STARTING_PITCHER)
            val gamePlayer =
                GamePlayer.createStarter(gameTeam, player, Position.STARTING_PITCHER, null)
            return PitchingRecord.create(gamePlayer, isStartingPitcher = true)
        }

        @Test
        fun `도루 허용을 기록할 수 있다`() {
            // given
            val record = createPitchingRecord()

            // when
            record.recordStolenBaseAllowed()
            record.recordStolenBaseAllowed()

            // then
            assertThat(record.stolenBasesAllowed).isEqualTo(2)
        }

        @Test
        fun `도루 저지를 기록할 수 있다`() {
            // given
            val record = createPitchingRecord()

            // when
            record.recordCaughtStealing()

            // then
            assertThat(record.runnersCaughtStealing).isEqualTo(1)
        }

        @Test
        fun `견제 아웃을 기록할 수 있다`() {
            // given
            val record = createPitchingRecord()

            // when
            record.recordPickoff()

            // then
            assertThat(record.pickoffs).isEqualTo(1)
        }

        @Test
        fun `도루 허용을 취소할 수 있다`() {
            // given
            val record = createPitchingRecord()
            record.recordStolenBaseAllowed()

            // when
            record.revertStolenBaseAllowed()

            // then
            assertThat(record.stolenBasesAllowed).isEqualTo(0)
        }

        @Test
        fun `도루 허용이 없을 때 취소하면 예외가 발생한다`() {
            // given
            val record = createPitchingRecord()

            // when & then
            assertThatThrownBy { record.revertStolenBaseAllowed() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 도루 허용 기록이 없습니다.")
        }

        @Test
        fun `도루 저지를 취소할 수 있다`() {
            // given
            val record = createPitchingRecord()
            record.recordCaughtStealing()

            // when
            record.revertCaughtStealing()

            // then
            assertThat(record.runnersCaughtStealing).isEqualTo(0)
        }

        @Test
        fun `도루 저지가 없을 때 취소하면 예외가 발생한다`() {
            // given
            val record = createPitchingRecord()

            // when & then
            assertThatThrownBy { record.revertCaughtStealing() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 도루 저지 기록이 없습니다.")
        }

        @Test
        fun `견제 아웃을 취소할 수 있다`() {
            // given
            val record = createPitchingRecord()
            record.recordPickoff()

            // when
            record.revertPickoff()

            // then
            assertThat(record.pickoffs).isEqualTo(0)
        }

        @Test
        fun `견제 아웃이 없을 때 취소하면 예외가 발생한다`() {
            // given
            val record = createPitchingRecord()

            // when & then
            assertThatThrownBy { record.revertPickoff() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 견제 아웃 기록이 없습니다.")
        }

        @Test
        fun `기록 정정으로 stolenBasesAllowed를 수정할 수 있다`() {
            // given
            val record = createPitchingRecord()

            // when
            val oldValue = record.correctField("stolenBasesAllowed", "5")

            // then
            assertThat(record.stolenBasesAllowed).isEqualTo(5)
            assertThat(oldValue).isEqualTo("0")
        }

        @Test
        fun `기록 정정으로 runnersCaughtStealing을 수정할 수 있다`() {
            // given
            val record = createPitchingRecord()

            // when
            val oldValue = record.correctField("runnersCaughtStealing", "3")

            // then
            assertThat(record.runnersCaughtStealing).isEqualTo(3)
            assertThat(oldValue).isEqualTo("0")
        }

        @Test
        fun `기록 정정으로 pickoffs를 수정할 수 있다`() {
            // given
            val record = createPitchingRecord()

            // when
            val oldValue = record.correctField("pickoffs", "2")

            // then
            assertThat(record.pickoffs).isEqualTo(2)
            assertThat(oldValue).isEqualTo("0")
        }
    }

    // ── M-7: DH 해제 시 타순 인원 검증 ──────────────────────────

    @Nested
    @DisplayName("M-7: DH 해제 시 타순 인원 검증")
    inner class DhReleaseBattingOrderCount {

        private fun createLineupEntries(
            count: Int,
            hasDh: Boolean = false,
        ): List<LineupEntry> {
            val competition = createCompetition()
            val game = createGame(competition)
            val gameTeam = GameTeam(game = game, team = team1, homeAway = HomeAway.HOME)
            val manager =
                User.createLocalUser(
                    email = "manager@test.com",
                    encodedPassword = "encoded",
                    nickname = "감독",
                )
            val submission = LineupSubmission.create(game, team1, manager)

            val positions =
                listOf(
                    Position.CATCHER,
                    Position.FIRST_BASE,
                    Position.SECOND_BASE,
                    Position.SHORTSTOP,
                    Position.THIRD_BASE,
                    Position.LEFT_FIELD,
                    Position.CENTER_FIELD,
                    Position.RIGHT_FIELD,
                    if (hasDh) Position.DESIGNATED_HITTER else Position.STARTING_PITCHER,
                )

            return (1..count).map { i ->
                LineupEntry(
                    submission = submission,
                    player =
                        Player(
                            name = "선수$i",
                            primaryPosition = positions.getOrElse(i - 1) { Position.LEFT_FIELD },
                            id = i.toLong(),
                        ),
                    position = positions.getOrElse(i - 1) { Position.LEFT_FIELD },
                    battingOrder = i,
                    backNumber = i,
                    isStarter = true,
                )
            }
        }

        @Test
        fun `타순에 9명이 배치되면 검증을 통과한다`() {
            // given
            val entries = createLineupEntries(9)

            // when & then (no exception)
            LineupValidator.validate(entries)
        }

        @Test
        fun `타순에 8명만 배치되면 예외가 발생한다`() {
            // given
            val entries = createLineupEntries(8)

            // when & then
            assertThatThrownBy { LineupValidator.validate(entries) }
                .isInstanceOf(InvalidLineupBattingOrderCountException::class.java)
                .hasMessageContaining("9명")
                .hasMessageContaining("8명")
        }

        @Test
        fun `타순에 10명 배치되면 예외가 발생한다`() {
            // given
            val entries = createLineupEntries(10)

            // when & then
            assertThatThrownBy { LineupValidator.validate(entries) }
                .isInstanceOf(InvalidLineupBattingOrderCountException::class.java)
                .hasMessageContaining("9명")
                .hasMessageContaining("10명")
        }

        @Test
        fun `8인 경기 규칙에서 타순에 8명이 배치되면 검증을 통과한다`() {
            // given
            val entries = createLineupEntries(8)

            // when & then (minBattingOrderCount=8인 대회에서는 통과)
            LineupValidator.validate(entries, requiredBattingOrderCount = 8)
        }

        @Test
        fun `8인 경기 규칙에서 타순에 7명만 배치되면 예외가 발생한다`() {
            // given
            val entries = createLineupEntries(7)

            // when & then
            assertThatThrownBy {
                LineupValidator.validate(entries, requiredBattingOrderCount = 8)
            }
                .isInstanceOf(InvalidLineupBattingOrderCountException::class.java)
                .hasMessageContaining("8명")
                .hasMessageContaining("7명")
        }
    }

    // ── M-8: 투구수 제한 도달 시 교체 권고 알림 ──────────────────

    @Nested
    @DisplayName("M-8: 투구수 제한 도달 시 교체 권고 알림")
    inner class PitchCountLimitSubstitution {

        @Test
        fun `투구수 제한 도달 시 교체 권고 메시지가 생성된다`() {
            // given
            val event =
                PitchCountWarningEvent(
                    gameId = 1L,
                    gamePlayerId = 10L,
                    playerId = 100L,
                    pitchesThrown = 80,
                    pitchCountLimit = 80,
                    warningType = PitchCountWarningType.LIMIT_REACHED,
                )

            // then
            assertThat(event.isSubstitutionRecommended).isTrue()
            assertThat(event.substitutionMessage).isNotNull()
            assertThat(event.substitutionMessage).contains("투수 교체를 권고합니다")
            assertThat(event.substitutionMessage).contains("80구")
        }

        @Test
        fun `투구수 제한 임박 시 교체 권고가 아니다`() {
            // given
            val event =
                PitchCountWarningEvent(
                    gameId = 1L,
                    gamePlayerId = 10L,
                    playerId = 100L,
                    pitchesThrown = 72,
                    pitchCountLimit = 80,
                    warningType = PitchCountWarningType.APPROACHING_LIMIT,
                )

            // then
            assertThat(event.isSubstitutionRecommended).isFalse()
            assertThat(event.substitutionMessage).isNull()
        }

        @Test
        fun `투구수 제한 초과 시 남은 투구 수가 음수이다`() {
            // given
            val event =
                PitchCountWarningEvent(
                    gameId = 1L,
                    gamePlayerId = 10L,
                    playerId = 100L,
                    pitchesThrown = 85,
                    pitchCountLimit = 80,
                    warningType = PitchCountWarningType.LIMIT_REACHED,
                )

            // then
            assertThat(event.remainingPitches).isEqualTo(-5)
            assertThat(event.isSubstitutionRecommended).isTrue()
        }

        @Test
        fun `PitchingRecord에서 투구수 상태 확인 시 제한 도달을 감지한다`() {
            // given
            val competition = createCompetition()
            val game = createGame(competition)
            val gameTeam = GameTeam(game = game, team = team1, homeAway = HomeAway.HOME)
            val player = Player(name = "투수", primaryPosition = Position.STARTING_PITCHER)
            val gamePlayer =
                GamePlayer.createStarter(gameTeam, player, Position.STARTING_PITCHER, null)
            val record = PitchingRecord.create(gamePlayer, isStartingPitcher = true)

            record.recordPitchCount(totalPitches = 80, strikes = 50)

            // when
            val status = record.checkPitchCountStatus(limit = 80, warningThreshold = 10)

            // then
            assertThat(status).isEqualTo(PitchCountStatus.LIMIT_REACHED)
        }
    }
}
