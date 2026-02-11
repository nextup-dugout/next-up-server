package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.PlayerNotFoundException
import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.team.Team
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.GameRepositoryPort
import com.nextup.core.port.repository.GameTeamRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.service.stats.dto.FormTrend
import com.nextup.core.service.stats.dto.FormType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("RecentFormServiceImpl 테스트")
class RecentFormServiceImplTest {
    private lateinit var playerRepositoryPort: PlayerRepositoryPort
    private lateinit var battingRecordRepositoryPort: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepositoryPort: PitchingRecordRepositoryPort
    private lateinit var gameRepositoryPort: GameRepositoryPort
    private lateinit var gameTeamRepositoryPort: GameTeamRepositoryPort
    private lateinit var recentFormService: RecentFormServiceImpl

    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        playerRepositoryPort = mockk()
        battingRecordRepositoryPort = mockk()
        pitchingRecordRepositoryPort = mockk()
        gameRepositoryPort = mockk()
        gameTeamRepositoryPort = mockk()
        recentFormService =
            RecentFormServiceImpl(
                playerRepositoryPort,
                battingRecordRepositoryPort,
                pitchingRecordRepositoryPort,
                gameRepositoryPort,
                gameTeamRepositoryPort,
            )

        player =
            mockk<Player>().apply {
                every { id } returns 1L
                every { name } returns "홍길동"
            }
    }

    private fun createMockTeam(
        teamId: Long,
        teamName: String = "테스트팀",
    ): Team =
        mockk<Team>().apply {
            every { id } returns teamId
            every { name } returns teamName
        }

    private fun createMockGame(
        gameId: Long,
        scheduledAt: LocalDateTime = LocalDateTime.now(),
    ): Game =
        mockk<Game>().apply {
            every { id } returns gameId
            every { this@apply.scheduledAt } returns scheduledAt
        }

    private fun createMockGameTeam(
        game: Game,
        team: Team,
    ): GameTeam =
        mockk<GameTeam>().apply {
            every { this@apply.game } returns game
            every { this@apply.team } returns team
        }

    private fun createMockGamePlayer(gameTeam: GameTeam): GamePlayer =
        mockk<GamePlayer>().apply {
            every { this@apply.gameTeam } returns gameTeam
        }

    private fun createMockBattingRecord(
        gamePlayer: GamePlayer,
        atBats: Int,
        hits: Int,
        homeRuns: Int = 0,
        rbi: Int = 0,
        runs: Int = 0,
        walks: Int = 0,
        strikeouts: Int = 0,
    ): BattingRecord =
        mockk<BattingRecord>().apply {
            every { this@apply.gamePlayer } returns gamePlayer
            every { this@apply.atBats } returns atBats
            every { this@apply.hits } returns hits
            every { this@apply.homeRuns } returns homeRuns
            every { this@apply.runsBattedIn } returns rbi
            every { this@apply.runs } returns runs
            every { this@apply.walks } returns walks
            every { this@apply.strikeouts } returns strikeouts
        }

    private fun createMockPitchingRecord(
        gamePlayer: GamePlayer,
        inningsPitchedOuts: Int,
        earnedRuns: Int,
        strikeouts: Int = 0,
        walksAllowed: Int = 0,
        hitsAllowed: Int = 0,
        decision: PitchingDecision = PitchingDecision.NONE,
    ): PitchingRecord =
        mockk<PitchingRecord>().apply {
            every { this@apply.gamePlayer } returns gamePlayer
            every { this@apply.inningsPitchedOuts } returns inningsPitchedOuts
            every { inningsPitchedDisplay } returns "${inningsPitchedOuts / 3}.${inningsPitchedOuts % 3}"
            every { this@apply.earnedRuns } returns earnedRuns
            every { this@apply.strikeouts } returns strikeouts
            every { this@apply.walksAllowed } returns walksAllowed
            every { this@apply.hitsAllowed } returns hitsAllowed
            every { this@apply.decision } returns decision
        }

    @Nested
    @DisplayName("getRecentForm 공통")
    inner class GetRecentFormCommon {
        @Test
        fun `조회할 경기 수가 0 이하이면 예외를 발생시킨다`() {
            // when & then
            assertThatThrownBy {
                recentFormService.getRecentForm(1L, 0, FormType.BATTING)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("1~20 사이")
        }

        @Test
        fun `조회할 경기 수가 20 초과이면 예외를 발생시킨다`() {
            // when & then
            assertThatThrownBy {
                recentFormService.getRecentForm(1L, 21, FormType.BATTING)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("1~20 사이")
        }

        @Test
        fun `선수가 존재하지 않으면 예외를 발생시킨다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns null

            // when & then
            assertThatThrownBy {
                recentFormService.getRecentForm(1L, 5, FormType.BATTING)
            }.isInstanceOf(PlayerNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("타격 폼 분석")
    inner class BattingForm {
        @Test
        fun `타격 기록이 없으면 예외를 발생시킨다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every { battingRecordRepositoryPort.findRecentByPlayerId(1L, 5) } returns emptyList()

            // when & then
            assertThatThrownBy {
                recentFormService.getRecentForm(1L, 5, FormType.BATTING)
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("타격 기록이 없습니다")
        }

        @Test
        fun `타격 폼을 분석하여 반환한다`() {
            // given
            val team = createMockTeam(10L, "테스트팀")
            val opponentTeam = createMockTeam(20L, "상대팀")
            val game = createMockGame(1L, LocalDateTime.of(2025, 4, 15, 14, 0))
            val gameTeam = createMockGameTeam(game, team)
            val opponentGameTeam = createMockGameTeam(game, opponentTeam)
            val gamePlayer = createMockGamePlayer(gameTeam)

            val battingRecord = createMockBattingRecord(gamePlayer, 4, 2, 1, 2, 1, 0, 1)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every { battingRecordRepositoryPort.findRecentByPlayerId(1L, 5) } returns listOf(battingRecord)
            every { battingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(battingRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L)) } returns listOf(game)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L)) } returns listOf(gameTeam, opponentGameTeam)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.BATTING)

            // then
            assertThat(result.playerName).isEqualTo("홍길동")
            assertThat(result.type).isEqualTo(FormType.BATTING)
            assertThat(result.gamesRequested).isEqualTo(5)
            assertThat(result.gamesFound).isEqualTo(1)
            assertThat(result.batting).isNotNull
            assertThat(result.batting?.totalAtBats).isEqualTo(4)
            assertThat(result.batting?.totalHits).isEqualTo(2)
            assertThat(result.pitching).isNull()
        }

        @Test
        fun `기록이 1개 이하이면 STABLE 트렌드를 반환한다`() {
            // given
            val team = createMockTeam(10L)
            val game = createMockGame(1L)
            val gameTeam = createMockGameTeam(game, team)
            val gamePlayer = createMockGamePlayer(gameTeam)
            val battingRecord = createMockBattingRecord(gamePlayer, 4, 2)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every { battingRecordRepositoryPort.findRecentByPlayerId(1L, 5) } returns listOf(battingRecord)
            every { battingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(battingRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L)) } returns listOf(game)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L)) } returns listOf(gameTeam)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.BATTING)

            // then
            assertThat(result.trend).isEqualTo(FormTrend.STABLE)
        }

        @Test
        fun `최근 타율이 이전보다 높으면 UP 트렌드를 반환한다`() {
            // given
            val team = createMockTeam(10L)
            val game1 = createMockGame(1L, LocalDateTime.of(2025, 4, 16, 14, 0))
            val game2 = createMockGame(2L, LocalDateTime.of(2025, 4, 15, 14, 0))
            val gameTeam1 = createMockGameTeam(game1, team)
            val gameTeam2 = createMockGameTeam(game2, team)
            val gamePlayer1 = createMockGamePlayer(gameTeam1)
            val gamePlayer2 = createMockGamePlayer(gameTeam2)

            // 최근 경기: 높은 타율 (3/4 = 0.750)
            val recentRecord = createMockBattingRecord(gamePlayer1, 4, 3)
            // 이전 경기: 낮은 타율 (1/4 = 0.250)
            val earlierRecord = createMockBattingRecord(gamePlayer2, 4, 1)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every {
                battingRecordRepositoryPort.findRecentByPlayerId(1L, 5)
            } returns listOf(recentRecord, earlierRecord)
            every { battingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(recentRecord, earlierRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L, 2L)) } returns listOf(game1, game2)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L, 2L)) } returns listOf(gameTeam1, gameTeam2)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.BATTING)

            // then
            assertThat(result.trend).isEqualTo(FormTrend.UP)
            assertThat(result.trendDescription).contains("상승세")
        }

        @Test
        fun `최근 타율이 이전보다 낮으면 DOWN 트렌드를 반환한다`() {
            // given
            val team = createMockTeam(10L)
            val game1 = createMockGame(1L, LocalDateTime.of(2025, 4, 16, 14, 0))
            val game2 = createMockGame(2L, LocalDateTime.of(2025, 4, 15, 14, 0))
            val gameTeam1 = createMockGameTeam(game1, team)
            val gameTeam2 = createMockGameTeam(game2, team)
            val gamePlayer1 = createMockGamePlayer(gameTeam1)
            val gamePlayer2 = createMockGamePlayer(gameTeam2)

            // 최근 경기: 낮은 타율 (1/4 = 0.250)
            val recentRecord = createMockBattingRecord(gamePlayer1, 4, 1)
            // 이전 경기: 높은 타율 (3/4 = 0.750)
            val earlierRecord = createMockBattingRecord(gamePlayer2, 4, 3)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every {
                battingRecordRepositoryPort.findRecentByPlayerId(1L, 5)
            } returns listOf(recentRecord, earlierRecord)
            every { battingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(recentRecord, earlierRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L, 2L)) } returns listOf(game1, game2)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L, 2L)) } returns listOf(gameTeam1, gameTeam2)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.BATTING)

            // then
            assertThat(result.trend).isEqualTo(FormTrend.DOWN)
            assertThat(result.trendDescription).contains("하락세")
        }
    }

    @Nested
    @DisplayName("투수 폼 분석")
    inner class PitchingForm {
        @Test
        fun `투수 기록이 없으면 예외를 발생시킨다`() {
            // given
            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every { pitchingRecordRepositoryPort.findRecentByPlayerId(1L, 5) } returns emptyList()

            // when & then
            assertThatThrownBy {
                recentFormService.getRecentForm(1L, 5, FormType.PITCHING)
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("투수 기록이 없습니다")
        }

        @Test
        fun `투수 폼을 분석하여 반환한다`() {
            // given
            val team = createMockTeam(10L, "테스트팀")
            val opponentTeam = createMockTeam(20L, "상대팀")
            val game = createMockGame(1L, LocalDateTime.of(2025, 4, 15, 14, 0))
            val gameTeam = createMockGameTeam(game, team)
            val opponentGameTeam = createMockGameTeam(game, opponentTeam)
            val gamePlayer = createMockGamePlayer(gameTeam)

            val pitchingRecord = createMockPitchingRecord(gamePlayer, 21, 2, 8, 2, 5, PitchingDecision.WIN)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every { pitchingRecordRepositoryPort.findRecentByPlayerId(1L, 5) } returns listOf(pitchingRecord)
            every { pitchingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(pitchingRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L)) } returns listOf(game)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L)) } returns listOf(gameTeam, opponentGameTeam)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.PITCHING)

            // then
            assertThat(result.playerName).isEqualTo("홍길동")
            assertThat(result.type).isEqualTo(FormType.PITCHING)
            assertThat(result.gamesRequested).isEqualTo(5)
            assertThat(result.gamesFound).isEqualTo(1)
            assertThat(result.pitching).isNotNull
            assertThat(result.pitching?.totalInningsPitchedOuts).isEqualTo(21)
            assertThat(result.pitching?.totalEarnedRuns).isEqualTo(2)
            assertThat(result.pitching?.totalStrikeouts).isEqualTo(8)
            assertThat(result.batting).isNull()
        }

        @Test
        fun `최근 ERA가 이전보다 낮으면 UP 트렌드를 반환한다`() {
            // given
            val team = createMockTeam(10L)
            val game1 = createMockGame(1L, LocalDateTime.of(2025, 4, 16, 14, 0))
            val game2 = createMockGame(2L, LocalDateTime.of(2025, 4, 15, 14, 0))
            val gameTeam1 = createMockGameTeam(game1, team)
            val gameTeam2 = createMockGameTeam(game2, team)
            val gamePlayer1 = createMockGamePlayer(gameTeam1)
            val gamePlayer2 = createMockGamePlayer(gameTeam2)

            // 최근 경기: 낮은 ERA (1 ER / 7 IP)
            val recentRecord = createMockPitchingRecord(gamePlayer1, 21, 1, 8, 1, 4, PitchingDecision.WIN)
            // 이전 경기: 높은 ERA (5 ER / 7 IP)
            val earlierRecord = createMockPitchingRecord(gamePlayer2, 21, 5, 4, 3, 8, PitchingDecision.LOSS)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every {
                pitchingRecordRepositoryPort.findRecentByPlayerId(1L, 5)
            } returns listOf(recentRecord, earlierRecord)
            every { pitchingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(recentRecord, earlierRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L, 2L)) } returns listOf(game1, game2)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L, 2L)) } returns listOf(gameTeam1, gameTeam2)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.PITCHING)

            // then
            assertThat(result.trend).isEqualTo(FormTrend.UP)
            assertThat(result.trendDescription).contains("상승세")
        }

        @Test
        fun `최근 ERA가 이전보다 높으면 DOWN 트렌드를 반환한다`() {
            // given
            val team = createMockTeam(10L)
            val game1 = createMockGame(1L, LocalDateTime.of(2025, 4, 16, 14, 0))
            val game2 = createMockGame(2L, LocalDateTime.of(2025, 4, 15, 14, 0))
            val gameTeam1 = createMockGameTeam(game1, team)
            val gameTeam2 = createMockGameTeam(game2, team)
            val gamePlayer1 = createMockGamePlayer(gameTeam1)
            val gamePlayer2 = createMockGamePlayer(gameTeam2)

            // 최근 경기: 높은 ERA (5 ER / 7 IP)
            val recentRecord = createMockPitchingRecord(gamePlayer1, 21, 5, 4, 3, 8, PitchingDecision.LOSS)
            // 이전 경기: 낮은 ERA (1 ER / 7 IP)
            val earlierRecord = createMockPitchingRecord(gamePlayer2, 21, 1, 8, 1, 4, PitchingDecision.WIN)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every {
                pitchingRecordRepositoryPort.findRecentByPlayerId(1L, 5)
            } returns listOf(recentRecord, earlierRecord)
            every { pitchingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(recentRecord, earlierRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L, 2L)) } returns listOf(game1, game2)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L, 2L)) } returns listOf(gameTeam1, gameTeam2)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.PITCHING)

            // then
            assertThat(result.trend).isEqualTo(FormTrend.DOWN)
            assertThat(result.trendDescription).contains("하락세")
        }

        @Test
        fun `이닝이 0이면 STABLE 트렌드를 반환한다`() {
            // given
            val team = createMockTeam(10L)
            val game1 = createMockGame(1L, LocalDateTime.of(2025, 4, 16, 14, 0))
            val game2 = createMockGame(2L, LocalDateTime.of(2025, 4, 15, 14, 0))
            val gameTeam1 = createMockGameTeam(game1, team)
            val gameTeam2 = createMockGameTeam(game2, team)
            val gamePlayer1 = createMockGamePlayer(gameTeam1)
            val gamePlayer2 = createMockGamePlayer(gameTeam2)

            // 최근 경기: 0이닝
            val recentRecord = createMockPitchingRecord(gamePlayer1, 0, 0, 0, 0, 0, PitchingDecision.NONE)
            // 이전 경기
            val earlierRecord = createMockPitchingRecord(gamePlayer2, 21, 2, 6, 2, 5, PitchingDecision.WIN)

            every { playerRepositoryPort.findByIdOrNull(1L) } returns player
            every {
                pitchingRecordRepositoryPort.findRecentByPlayerId(1L, 5)
            } returns listOf(recentRecord, earlierRecord)
            every { pitchingRecordRepositoryPort.findAllByPlayerId(1L) } returns listOf(recentRecord, earlierRecord)
            every { gameRepositoryPort.findAllByIds(listOf(1L, 2L)) } returns listOf(game1, game2)
            every { gameTeamRepositoryPort.findAllByGameIds(listOf(1L, 2L)) } returns listOf(gameTeam1, gameTeam2)

            // when
            val result = recentFormService.getRecentForm(1L, 5, FormType.PITCHING)

            // then
            assertThat(result.trend).isEqualTo(FormTrend.STABLE)
        }
    }
}
