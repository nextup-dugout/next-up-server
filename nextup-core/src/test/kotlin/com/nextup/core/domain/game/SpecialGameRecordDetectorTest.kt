package com.nextup.core.domain.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("SpecialGameRecordDetector 테스트")
class SpecialGameRecordDetectorTest {
    private lateinit var competition: Competition
    private lateinit var homeTeam: Team
    private lateinit var awayTeam: Team
    private lateinit var game: Game
    private lateinit var homeGameTeam: GameTeam
    private lateinit var awayGameTeam: GameTeam

    @BeforeEach
    fun setUp() {
        val association = Association(name = "서울시야구협회", region = "서울")
        val league = League(association = association, name = "1부 리그", foundedYear = 2020)
        homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020, id = 1L)
        awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020, id = 2L)

        competition =
            Competition(
                league = league,
                name = "테스트 대회",
                year = 2025,
                season = 1,
                type = CompetitionType.LEAGUE,
                startDate = LocalDate.of(2025, 3, 1),
                status = CompetitionStatus.IN_PROGRESS,
            )

        game =
            Game.createForTest(
                competition = competition,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                status = GameStatus.FINISHED,
                currentInning = 9,
                isTopInning = false,
                totalInnings = 9,
                id = 10L,
            )

        homeGameTeam = game.gameTeams.first { it.homeAway == HomeAway.HOME }
        awayGameTeam = game.gameTeams.first { it.homeAway == HomeAway.AWAY }
    }

    private fun createGamePlayer(
        gameTeam: GameTeam,
        position: Position = Position.STARTING_PITCHER,
        id: Long = 0L,
    ): GamePlayer {
        val player =
            Player(
                name = "테스트선수",
                birthDate = LocalDate.of(1990, 1, 1),
                primaryPosition = position,
                id = id,
            )
        return GamePlayer(
            gameTeam = gameTeam,
            player = player,
            position = position,
            battingOrder = 1,
            id = id,
        )
    }

    @Nested
    @DisplayName("퍼펙트게임 감지")
    inner class PerfectGameDetection {
        @Test
        fun `안타 0, 볼넷 0, 사구 0, 실책 0이면 퍼펙트게임으로 감지`() {
            // given - 홈팀이 원정팀을 상대로 퍼펙트게임 달성
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
            homeGameTeam.updateScore(totalScore = 3, totalHits = 5, totalErrors = 0)

            val homePitcher = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)
            // 투수 기록: 볼넷 0, 사구 0
            // (기본값이 모두 0이므로 추가 설정 불필요)

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)
            // 수비 기록: 실책 0 (기본값)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam, awayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = mapOf(homeTeam.id to listOf(homePitchingRecord)),
                    fieldingRecordsByTeamId = mapOf(homeTeam.id to listOf(homeFieldingRecord)),
                )

            // then
            assertThat(results).hasSize(1)
            assertThat(results[0].record).isEqualTo(SpecialGameRecord.PERFECT_GAME)
            assertThat(results[0].teamId).isEqualTo(homeTeam.id)
            assertThat(results[0].opponentTeamId).isEqualTo(awayTeam.id)
        }
    }

    @Nested
    @DisplayName("노히트노런 감지")
    inner class NoHitterDetection {
        @Test
        fun `안타 0이지만 볼넷이 있으면 노히트노런으로 감지`() {
            // given - 홈팀이 원정팀을 상대로 노히트 (볼넷 허용)
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
            homeGameTeam.updateScore(totalScore = 5, totalHits = 8, totalErrors = 0)

            val homePitcher = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)
            homePitchingRecord.recordWalk() // 볼넷 1개 허용

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam, awayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = mapOf(homeTeam.id to listOf(homePitchingRecord)),
                    fieldingRecordsByTeamId = mapOf(homeTeam.id to listOf(homeFieldingRecord)),
                )

            // then
            assertThat(results).hasSize(1)
            assertThat(results[0].record).isEqualTo(SpecialGameRecord.NO_HITTER)
            assertThat(results[0].teamId).isEqualTo(homeTeam.id)
        }

        @Test
        fun `안타 0이지만 사구가 있으면 노히트노런으로 감지`() {
            // given
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
            homeGameTeam.updateScore(totalScore = 2, totalHits = 4, totalErrors = 0)

            val homePitcher = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)
            homePitchingRecord.recordHitByPitch() // 사구 1개

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam, awayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = mapOf(homeTeam.id to listOf(homePitchingRecord)),
                    fieldingRecordsByTeamId = mapOf(homeTeam.id to listOf(homeFieldingRecord)),
                )

            // then
            assertThat(results).hasSize(1)
            assertThat(results[0].record).isEqualTo(SpecialGameRecord.NO_HITTER)
        }

        @Test
        fun `안타 0이지만 실책이 있으면 노히트노런으로 감지`() {
            // given
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 1)
            homeGameTeam.updateScore(totalScore = 3, totalHits = 5, totalErrors = 0)

            val homePitcher = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)
            homeFieldingRecord.recordError() // 실책 1개

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam, awayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = mapOf(homeTeam.id to listOf(homePitchingRecord)),
                    fieldingRecordsByTeamId = mapOf(homeTeam.id to listOf(homeFieldingRecord)),
                )

            // then
            assertThat(results).hasSize(1)
            assertThat(results[0].record).isEqualTo(SpecialGameRecord.NO_HITTER)
        }
    }

    @Nested
    @DisplayName("미감지 케이스")
    inner class NoDetection {
        @Test
        fun `안타가 있으면 특수 기록 미감지`() {
            // given - 정상 경기 (양 팀 모두 안타 있음)
            homeGameTeam.updateScore(totalScore = 5, totalHits = 8, totalErrors = 1)
            awayGameTeam.updateScore(totalScore = 3, totalHits = 6, totalErrors = 0)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam, awayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = emptyMap(),
                    fieldingRecordsByTeamId = emptyMap(),
                )

            // then
            assertThat(results).isEmpty()
        }

        @Test
        fun `몰수승 경기에서는 특수 기록 미감지`() {
            // given
            val forfeitedGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.FORFEITED,
                    id = 20L,
                )

            val fHomeGameTeam = forfeitedGame.gameTeams.first { it.homeAway == HomeAway.HOME }
            val fAwayGameTeam = forfeitedGame.gameTeams.first { it.homeAway == HomeAway.AWAY }
            fAwayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = forfeitedGame,
                    gameTeams = listOf(fHomeGameTeam, fAwayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = emptyMap(),
                    fieldingRecordsByTeamId = emptyMap(),
                )

            // then
            assertThat(results).isEmpty()
        }

        @Test
        fun `취소 경기에서는 특수 기록 미감지`() {
            // given
            val cancelledGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.CANCELLED,
                    id = 30L,
                )

            val cHomeGameTeam = cancelledGame.gameTeams.first { it.homeAway == HomeAway.HOME }
            val cAwayGameTeam = cancelledGame.gameTeams.first { it.homeAway == HomeAway.AWAY }

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = cancelledGame,
                    gameTeams = listOf(cHomeGameTeam, cAwayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = emptyMap(),
                    fieldingRecordsByTeamId = emptyMap(),
                )

            // then
            assertThat(results).isEmpty()
        }

        @Test
        fun `중단 경기에서는 특수 기록 미감지`() {
            // given
            val suspendedGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.SUSPENDED,
                    id = 40L,
                )

            val sHomeGameTeam = suspendedGame.gameTeams.first { it.homeAway == HomeAway.HOME }
            val sAwayGameTeam = suspendedGame.gameTeams.first { it.homeAway == HomeAway.AWAY }

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = suspendedGame,
                    gameTeams = listOf(sHomeGameTeam, sAwayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = emptyMap(),
                    fieldingRecordsByTeamId = emptyMap(),
                )

            // then
            assertThat(results).isEmpty()
        }

        @Test
        fun `팀이 2개가 아니면 특수 기록 미감지`() {
            // given
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = emptyMap(),
                    fieldingRecordsByTeamId = emptyMap(),
                )

            // then
            assertThat(results).isEmpty()
        }
    }

    @Nested
    @DisplayName("콜드게임 특수 기록 감지")
    inner class CalledGameDetection {
        @Test
        fun `콜드게임에서도 특수 기록 감지 가능`() {
            // given
            val calledGame =
                Game.createForTest(
                    competition = competition,
                    homeTeam = homeTeam,
                    awayTeam = awayTeam,
                    scheduledAt = LocalDateTime.of(2025, 5, 10, 14, 0),
                    status = GameStatus.CALLED,
                    currentInning = 7,
                    isTopInning = false,
                    totalInnings = 9,
                    id = 50L,
                )

            val cHomeGameTeam = calledGame.gameTeams.first { it.homeAway == HomeAway.HOME }
            val cAwayGameTeam = calledGame.gameTeams.first { it.homeAway == HomeAway.AWAY }

            cAwayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
            cHomeGameTeam.updateScore(totalScore = 10, totalHits = 12, totalErrors = 0)

            val homePitcher = createGamePlayer(cHomeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)

            val homeFielder = createGamePlayer(cHomeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = calledGame,
                    gameTeams = listOf(cHomeGameTeam, cAwayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId = mapOf(homeTeam.id to listOf(homePitchingRecord)),
                    fieldingRecordsByTeamId = mapOf(homeTeam.id to listOf(homeFieldingRecord)),
                )

            // then
            assertThat(results).hasSize(1)
            assertThat(results[0].record).isEqualTo(SpecialGameRecord.PERFECT_GAME)
        }
    }

    @Nested
    @DisplayName("양 팀 동시 감지")
    inner class BothTeamsDetection {
        @Test
        fun `양 팀 모두 노히트 달성 시 두 건 감지`() {
            // given - 극히 드문 양 팀 동시 노히트 (0-0 무안타)
            homeGameTeam.updateScore(totalScore = 1, totalHits = 0, totalErrors = 0)
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)

            val homePitcher = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord =
                PitchingRecord.create(homePitcher, isStartingPitcher = true)

            val awayPitcher = createGamePlayer(awayGameTeam, Position.STARTING_PITCHER, 200L)
            val awayPitchingRecord =
                PitchingRecord.create(awayPitcher, isStartingPitcher = true)
            awayPitchingRecord.recordWalk() // 홈팀에 볼넷 허용 (1점은 볼넷+도루+희생플라이 등으로 가정)

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)

            val awayFielder = createGamePlayer(awayGameTeam, Position.SHORTSTOP, 201L)
            val awayFieldingRecord = FieldingRecord.create(awayFielder)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam, awayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId =
                        mapOf(
                            homeTeam.id to listOf(homePitchingRecord),
                            awayTeam.id to listOf(awayPitchingRecord),
                        ),
                    fieldingRecordsByTeamId =
                        mapOf(
                            homeTeam.id to listOf(homeFieldingRecord),
                            awayTeam.id to listOf(awayFieldingRecord),
                        ),
                )

            // then
            assertThat(results).hasSize(2)
            val homeResult = results.first { it.teamId == homeTeam.id }
            val awayResult = results.first { it.teamId == awayTeam.id }
            assertThat(homeResult.record).isEqualTo(SpecialGameRecord.PERFECT_GAME)
            assertThat(awayResult.record).isEqualTo(SpecialGameRecord.NO_HITTER)
        }
    }

    @Nested
    @DisplayName("복수 투수 시나리오")
    inner class MultiplePitchersScenario {
        @Test
        fun `복수 투수 중 한 명이 볼넷 허용 시 노히트로 감지`() {
            // given
            awayGameTeam.updateScore(totalScore = 0, totalHits = 0, totalErrors = 0)
            homeGameTeam.updateScore(totalScore = 4, totalHits = 7, totalErrors = 0)

            val homePitcher1 = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 100L)
            val homePitchingRecord1 =
                PitchingRecord.create(homePitcher1, isStartingPitcher = true)
            // 선발 투수: 볼넷 0, 사구 0

            val homePitcher2 = createGamePlayer(homeGameTeam, Position.STARTING_PITCHER, 102L)
            val homePitchingRecord2 =
                PitchingRecord.create(homePitcher2, isStartingPitcher = false)
            homePitchingRecord2.recordWalk() // 계투 투수가 볼넷 1개 허용

            val homeFielder = createGamePlayer(homeGameTeam, Position.SHORTSTOP, 101L)
            val homeFieldingRecord = FieldingRecord.create(homeFielder)

            // when
            val results =
                SpecialGameRecordDetector.detect(
                    game = game,
                    gameTeams = listOf(homeGameTeam, awayGameTeam),
                    battingRecordsByTeamId = emptyMap(),
                    pitchingRecordsByTeamId =
                        mapOf(
                            homeTeam.id to
                                listOf(
                                    homePitchingRecord1,
                                    homePitchingRecord2,
                                ),
                        ),
                    fieldingRecordsByTeamId = mapOf(homeTeam.id to listOf(homeFieldingRecord)),
                )

            // then
            assertThat(results).hasSize(1)
            assertThat(results[0].record).isEqualTo(SpecialGameRecord.NO_HITTER)
        }
    }
}
