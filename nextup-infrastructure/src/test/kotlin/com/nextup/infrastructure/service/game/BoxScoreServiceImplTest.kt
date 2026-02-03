package com.nextup.infrastructure.service.game

import com.nextup.core.domain.association.Association
import com.nextup.core.domain.competition.Competition
import com.nextup.core.domain.competition.CompetitionStatus
import com.nextup.core.domain.competition.CompetitionType
import com.nextup.core.domain.game.*
import com.nextup.core.domain.league.League
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GamePlayerRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("BoxScoreServiceImpl 테스트")
class BoxScoreServiceImplTest {

    private lateinit var gamePlayerRepository: GamePlayerRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort
    private lateinit var boxScoreService: BoxScoreServiceImpl

    private lateinit var association: Association
    private lateinit var league: League
    private lateinit var competition: Competition
    private lateinit var game: Game
    private lateinit var homeTeam: Team
    private lateinit var awayTeam: Team
    private lateinit var homeGameTeam: GameTeam
    private lateinit var awayGameTeam: GameTeam

    @BeforeEach
    fun setUp() {
        gamePlayerRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()
        boxScoreService = BoxScoreServiceImpl(
            gamePlayerRepository,
            battingRecordRepository,
            pitchingRecordRepository
        )

        association = Association(name = "서울시야구협회", region = "서울")
        league = League(association = association, name = "1부 리그", foundedYear = 2020)
        competition = Competition(
            league = league,
            name = "2025 춘계대회",
            year = 2025,
            season = 1,
            type = CompetitionType.LEAGUE,
            startDate = LocalDate.of(2025, 3, 1),
            status = CompetitionStatus.IN_PROGRESS
        )
        game = Game(
            competition = competition,
            scheduledAt = LocalDateTime.of(2025, 4, 15, 14, 0),
            location = "잠실야구장",
            status = GameStatus.IN_PROGRESS
        )
        homeTeam = Team(league = league, name = "홈팀", city = "서울", foundedYear = 2020)
        awayTeam = Team(league = league, name = "원정팀", city = "부산", foundedYear = 2020)
        homeGameTeam = GameTeam(game = game, team = homeTeam, homeAway = HomeAway.HOME)
        awayGameTeam = GameTeam(game = game, team = awayTeam, homeAway = HomeAway.AWAY)
    }

    private fun createPlayer(name: String): Player {
        return Player(
            name = name,
            birthDate = LocalDate.of(1995, 1, 1),
            primaryPosition = Position.FIRST_BASE
        )
    }

    @Nested
    @DisplayName("getBoxScore")
    inner class GetBoxScore {

        @Test
        fun `경기의 박스스코어를 조회할 수 있다`() {
            // given
            val player1 = createPlayer("홈선수1")
            val player2 = createPlayer("원정선수1")

            val homeGamePlayer = GamePlayer(
                gameTeam = homeGameTeam,
                player = player1,
                position = Position.FIRST_BASE,
                battingOrder = 1
            )
            val awayGamePlayer = GamePlayer(
                gameTeam = awayGameTeam,
                player = player2,
                position = Position.STARTING_PITCHER,
                battingOrder = null
            )

            val homeBattingRecord = BattingRecord.create(homeGamePlayer)
            val awayPitchingRecord = PitchingRecord.create(awayGamePlayer, isStartingPitcher = true)

            every { gamePlayerRepository.findAllByGameId(1L) } returns listOf(homeGamePlayer, awayGamePlayer)
            every { battingRecordRepository.findByGamePlayer(homeGamePlayer) } returns homeBattingRecord
            every { battingRecordRepository.findByGamePlayer(awayGamePlayer) } returns null
            every { pitchingRecordRepository.findByGamePlayer(homeGamePlayer) } returns null
            every { pitchingRecordRepository.findByGamePlayer(awayGamePlayer) } returns awayPitchingRecord

            // when
            val result = boxScoreService.getBoxScore(1L)

            // then
            assertThat(result.gameId).isEqualTo(1L)
            assertThat(result.homeTeam.batters).hasSize(1)
            assertThat(result.awayTeam.pitchers).hasSize(1)
        }

        @Test
        fun `출전 선수가 없으면 예외를 발생시킨다`() {
            // given
            every { gamePlayerRepository.findAllByGameId(1L) } returns emptyList()

            // when & then
            assertThatThrownBy { boxScoreService.getBoxScore(1L) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("출전 선수가 없습니다")
        }

        @Test
        fun `이닝별 점수를 파싱할 수 있다`() {
            // given
            homeGameTeam.addRunInInning(1, 1)
            homeGameTeam.addRunInInning(2, 2)
            homeGameTeam.addRunInInning(3, 0)
            homeGameTeam.addRunInInning(4, 3)
            val player = createPlayer("홈선수1")
            val homeGamePlayer = GamePlayer(
                gameTeam = homeGameTeam,
                player = player,
                position = Position.FIRST_BASE,
                battingOrder = 1
            )
            val awayPlayer = createPlayer("원정선수1")
            val awayGamePlayer = GamePlayer(
                gameTeam = awayGameTeam,
                player = awayPlayer,
                position = Position.STARTING_PITCHER
            )

            every { gamePlayerRepository.findAllByGameId(1L) } returns listOf(homeGamePlayer, awayGamePlayer)
            every { battingRecordRepository.findByGamePlayer(any()) } returns null
            every { pitchingRecordRepository.findByGamePlayer(any()) } returns null

            // when
            val result = boxScoreService.getBoxScore(1L)

            // then
            assertThat(result.homeTeam.inningScores).containsExactly(1, 2, 0, 3)
        }

        @Test
        fun `타자는 타순으로 정렬된다`() {
            // given
            val player1 = createPlayer("타자1")
            val player2 = createPlayer("타자2")
            val player3 = createPlayer("타자3")

            val gp1 = GamePlayer(homeGameTeam, player1, Position.CENTER_FIELD, battingOrder = 3)
            val gp2 = GamePlayer(homeGameTeam, player2, Position.FIRST_BASE, battingOrder = 1)
            val gp3 = GamePlayer(homeGameTeam, player3, Position.SECOND_BASE, battingOrder = 2)
            val awayPlayer = createPlayer("원정선수")
            val awayGp = GamePlayer(awayGameTeam, awayPlayer, Position.STARTING_PITCHER)

            every { gamePlayerRepository.findAllByGameId(1L) } returns listOf(gp1, gp2, gp3, awayGp)
            every { battingRecordRepository.findByGamePlayer(any()) } returns null
            every { pitchingRecordRepository.findByGamePlayer(any()) } returns null

            // when
            val result = boxScoreService.getBoxScore(1L)

            // then
            assertThat(result.homeTeam.batters).hasSize(3)
            assertThat(result.homeTeam.batters[0].battingOrder).isEqualTo(1)
            assertThat(result.homeTeam.batters[1].battingOrder).isEqualTo(2)
            assertThat(result.homeTeam.batters[2].battingOrder).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("updateOnPlateAppearance")
    inner class UpdateOnPlateAppearance {

        @Test
        fun `타석 결과를 반영하여 타자와 투수 기록을 갱신한다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val batterGameTeam = mockk<GameTeam>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { batter.gameTeam } returns batterGameTeam
            every { pitcher.id } returns 2L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord

            // when
            boxScoreService.updateOnPlateAppearance(
                gameId = 1L,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.SINGLE,
                rbis = 1,
                runsScored = emptyList(),
                inning = 3
            )

            // then
            verify { battingRecord.applyPlateAppearanceResult(PlateAppearanceResult.SINGLE, 1) }
            verify { pitchingRecord.applyBatterFaced(PlateAppearanceResult.SINGLE) }
            verify { batterGameTeam.addHit() }
        }

        @Test
        fun `타자 기록이 없으면 예외를 발생시킨다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)

            every { batter.id } returns 1L
            every { battingRecordRepository.findByGamePlayer(batter) } returns null

            // when & then
            assertThatThrownBy {
                boxScoreService.updateOnPlateAppearance(
                    gameId = 1L,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SINGLE,
                    rbis = 0,
                    runsScored = emptyList(),
                    inning = 1
                )
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("타격 기록을 찾을 수 없습니다")
        }

        @Test
        fun `투수 기록이 없으면 예외를 발생시킨다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { pitcher.id } returns 2L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns null

            // when & then
            assertThatThrownBy {
                boxScoreService.updateOnPlateAppearance(
                    gameId = 1L,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SINGLE,
                    rbis = 0,
                    runsScored = emptyList(),
                    inning = 1
                )
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("투수 기록을 찾을 수 없습니다")
        }

        @Test
        fun `득점한 주자의 득점을 기록한다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val runner = mockk<GamePlayer>(relaxed = true)
            val batterGameTeam = mockk<GameTeam>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)
            val runnerBattingRecord = mockk<BattingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { batter.gameTeam } returns batterGameTeam
            every { pitcher.id } returns 2L
            every { runner.id } returns 3L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gamePlayerRepository.findByIdOrNull(3L) } returns runner
            every { battingRecordRepository.findByGamePlayer(runner) } returns runnerBattingRecord

            // when
            boxScoreService.updateOnPlateAppearance(
                gameId = 1L,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.SINGLE,
                rbis = 1,
                runsScored = listOf(3L),
                inning = 3
            )

            // then
            verify { runnerBattingRecord.recordRun() }
            verify { batterGameTeam.addRunInInning(3, 1) }
            verify { pitchingRecord.recordEarnedRun(1) }
        }

        @Test
        fun `주자를 찾을 수 없으면 예외를 발생시킨다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val batterGameTeam = mockk<GameTeam>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { batter.gameTeam } returns batterGameTeam
            every { pitcher.id } returns 2L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gamePlayerRepository.findByIdOrNull(999L) } returns null

            // when & then
            assertThatThrownBy {
                boxScoreService.updateOnPlateAppearance(
                    gameId = 1L,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SINGLE,
                    rbis = 1,
                    runsScored = listOf(999L),
                    inning = 3
                )
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("주자를 찾을 수 없습니다")
        }

        @Test
        fun `주자의 타격 기록이 없으면 예외를 발생시킨다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val runner = mockk<GamePlayer>(relaxed = true)
            val batterGameTeam = mockk<GameTeam>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { batter.gameTeam } returns batterGameTeam
            every { pitcher.id } returns 2L
            every { runner.id } returns 3L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord
            every { gamePlayerRepository.findByIdOrNull(3L) } returns runner
            every { battingRecordRepository.findByGamePlayer(runner) } returns null

            // when & then
            assertThatThrownBy {
                boxScoreService.updateOnPlateAppearance(
                    gameId = 1L,
                    batter = batter,
                    pitcher = pitcher,
                    result = PlateAppearanceResult.SINGLE,
                    rbis = 1,
                    runsScored = listOf(3L),
                    inning = 3
                )
            }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("주자의 타격 기록을 찾을 수 없습니다")
        }

        @Test
        fun `아웃인 경우 투수의 아웃 카운트를 증가시킨다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val batterGameTeam = mockk<GameTeam>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { batter.gameTeam } returns batterGameTeam
            every { pitcher.id } returns 2L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord

            // when
            boxScoreService.updateOnPlateAppearance(
                gameId = 1L,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.GROUND_OUT,
                rbis = 0,
                runsScored = emptyList(),
                inning = 1
            )

            // then
            verify { pitchingRecord.recordOut() }
        }

        @Test
        fun `홈런인 경우 타자 자신의 득점과 팀 점수를 증가시킨다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val batterGameTeam = mockk<GameTeam>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { batter.gameTeam } returns batterGameTeam
            every { pitcher.id } returns 2L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord

            // when
            boxScoreService.updateOnPlateAppearance(
                gameId = 1L,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.HOME_RUN,
                rbis = 3,
                runsScored = listOf(),
                inning = 5
            )

            // then
            verify { batterGameTeam.addRunInInning(5, 1) }
            verify { pitchingRecord.recordEarnedRun(1) }
            verify { batterGameTeam.addHit() }
        }

        @Test
        fun `안타인 경우 팀 안타 수를 증가시킨다`() {
            // given
            val batter = mockk<GamePlayer>(relaxed = true)
            val pitcher = mockk<GamePlayer>(relaxed = true)
            val batterGameTeam = mockk<GameTeam>(relaxed = true)
            val battingRecord = mockk<BattingRecord>(relaxed = true)
            val pitchingRecord = mockk<PitchingRecord>(relaxed = true)

            every { batter.id } returns 1L
            every { batter.gameTeam } returns batterGameTeam
            every { pitcher.id } returns 2L
            every { battingRecordRepository.findByGamePlayer(batter) } returns battingRecord
            every { pitchingRecordRepository.findByGamePlayer(pitcher) } returns pitchingRecord

            // when
            boxScoreService.updateOnPlateAppearance(
                gameId = 1L,
                batter = batter,
                pitcher = pitcher,
                result = PlateAppearanceResult.DOUBLE,
                rbis = 0,
                runsScored = emptyList(),
                inning = 2
            )

            // then
            verify { batterGameTeam.addHit() }
        }
    }
}
