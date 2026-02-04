package com.nextup.infrastructure.service.stats

import com.nextup.common.exception.InvalidInputException
import com.nextup.common.exception.NotFoundException
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.service.stats.PlayerStatsService
import com.nextup.core.service.stats.dto.RecordScope
import com.nextup.core.service.stats.dto.RecordType
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("PlayerRecordServiceImpl 테스트")
class PlayerRecordServiceImplTest {
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var playerStatsService: PlayerStatsService
    private lateinit var playerRecordService: PlayerRecordServiceImpl

    private lateinit var player: Player

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        playerStatsService = mockk()
        playerRecordService = PlayerRecordServiceImpl(playerRepository, playerStatsService)

        player =
            mockk<Player>().apply {
                every { id } returns 1L
                every { name } returns "홍길동"
            }
    }

    private fun createMockSeasonBattingStats(): SeasonBattingStats =
        mockk<SeasonBattingStats>().apply {
            every { gamesPlayed } returns 50
            every { plateAppearances } returns 200
            every { atBats } returns 180
            every { hits } returns 54
            every { doubles } returns 10
            every { triples } returns 2
            every { homeRuns } returns 5
            every { runs } returns 25
            every { runsBattedIn } returns 30
            every { walks } returns 15
            every { strikeouts } returns 35
            every { stolenBases } returns 5
            every { battingAverage } returns BigDecimal("0.300")
            every { onBasePercentage } returns BigDecimal("0.370")
            every { sluggingPercentage } returns BigDecimal("0.450")
            every { ops } returns BigDecimal("0.820")
        }

    private fun createMockSeasonPitchingStats(): SeasonPitchingStats =
        mockk<SeasonPitchingStats>().apply {
            every { gamesPlayed } returns 20
            every { gamesStarted } returns 18
            every { inningsPitchedDisplay } returns "120.0"
            every { wins } returns 10
            every { losses } returns 5
            every { saves } returns 0
            every { holds } returns 0
            every { earnedRuns } returns 35
            every { hitsAllowed } returns 100
            every { walksAllowed } returns 30
            every { strikeouts } returns 100
            every { homeRunsAllowed } returns 8
            every { earnedRunAverage } returns BigDecimal("2.63")
            every { whip } returns BigDecimal("1.08")
        }

    private fun createMockCareerBattingStats(): CareerBattingStats =
        mockk<CareerBattingStats>().apply {
            every { gamesPlayed } returns 300
            every { plateAppearances } returns 1200
            every { atBats } returns 1000
            every { hits } returns 300
            every { doubles } returns 60
            every { triples } returns 10
            every { homeRuns } returns 30
            every { runs } returns 150
            every { runsBattedIn } returns 180
            every { walks } returns 150
            every { strikeouts } returns 200
            every { stolenBases } returns 30
            every { battingAverage } returns BigDecimal("0.300")
            every { onBasePercentage } returns BigDecimal("0.400")
            every { sluggingPercentage } returns BigDecimal("0.500")
            every { ops } returns BigDecimal("0.900")
        }

    private fun createMockCareerPitchingStats(): CareerPitchingStats =
        mockk<CareerPitchingStats>().apply {
            every { gamesPlayed } returns 100
            every { gamesStarted } returns 80
            every { inningsPitchedDisplay } returns "500.0"
            every { wins } returns 40
            every { losses } returns 25
            every { saves } returns 0
            every { holds } returns 0
            every { earnedRuns } returns 150
            every { hitsAllowed } returns 450
            every { walksAllowed } returns 100
            every { strikeouts } returns 400
            every { homeRunsAllowed } returns 40
            every { earnedRunAverage } returns BigDecimal("2.70")
            every { whip } returns BigDecimal("1.10")
        }

    @Nested
    @DisplayName("getPlayerRecord")
    inner class GetPlayerRecord {
        @Test
        fun `선수가 존재하지 않으면 예외를 발생시킨다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns null

            // when & then
            assertThatThrownBy {
                playerRecordService.getPlayerRecord(1L, RecordScope.CAREER, RecordType.ALL, null, null)
            }.isInstanceOf(NotFoundException::class.java)
                .hasMessageContaining("선수를 찾을 수 없습니다")
        }

        @Test
        fun `대회 범위는 아직 구현되지 않아 예외를 발생시킨다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player

            // when & then
            assertThatThrownBy {
                playerRecordService.getPlayerRecord(1L, RecordScope.COMPETITION, RecordType.ALL, null, null)
            }.isInstanceOf(InvalidInputException::class.java)
                .hasMessageContaining("대회별 통계는 추후 구현 예정")
        }
    }

    @Nested
    @DisplayName("시즌 기록 조회")
    inner class SeasonRecord {
        @Test
        fun `시즌 타격 기록을 조회한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerStatsService.getSeasonBattingStats(1L, 2025) } returns createMockSeasonBattingStats()

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.SEASON, RecordType.BATTING, 2025, null)

            // then
            assertThat(result.playerName).isEqualTo("홍길동")
            assertThat(result.scope).isEqualTo(RecordScope.SEASON)
            assertThat(result.type).isEqualTo(RecordType.BATTING)
            assertThat(result.year).isEqualTo(2025)
            assertThat(result.battingStats).isNotNull
            assertThat(result.battingStats?.gamesPlayed).isEqualTo(50)
            assertThat(result.battingStats?.hits).isEqualTo(54)
            assertThat(result.pitchingStats).isNull()
        }

        @Test
        fun `시즌 투수 기록을 조회한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerStatsService.getSeasonPitchingStats(1L, 2025) } returns createMockSeasonPitchingStats()

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.SEASON, RecordType.PITCHING, 2025, null)

            // then
            assertThat(result.scope).isEqualTo(RecordScope.SEASON)
            assertThat(result.type).isEqualTo(RecordType.PITCHING)
            assertThat(result.battingStats).isNull()
            assertThat(result.pitchingStats).isNotNull
            assertThat(result.pitchingStats?.gamesPlayed).isEqualTo(20)
            assertThat(result.pitchingStats?.wins).isEqualTo(10)
        }

        @Test
        fun `시즌 ALL 타입으로 타격과 투수 기록을 모두 조회한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerStatsService.getSeasonBattingStats(1L, 2025) } returns createMockSeasonBattingStats()
            every { playerStatsService.getSeasonPitchingStats(1L, 2025) } returns createMockSeasonPitchingStats()

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.SEASON, RecordType.ALL, 2025, null)

            // then
            assertThat(result.type).isEqualTo(RecordType.ALL)
            assertThat(result.battingStats).isNotNull
            assertThat(result.pitchingStats).isNotNull
        }

        @Test
        fun `시즌 타격 기록이 없으면 null을 반환한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every {
                playerStatsService.getSeasonBattingStats(1L, 2025)
            } throws IllegalArgumentException("통계가 없습니다")

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.SEASON, RecordType.BATTING, 2025, null)

            // then
            assertThat(result.battingStats).isNull()
        }

        @Test
        fun `연도를 지정하지 않으면 현재 연도를 사용한다`() {
            // given
            val currentYear = LocalDate.now().year
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerStatsService.getSeasonBattingStats(1L, currentYear) } returns createMockSeasonBattingStats()

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.SEASON, RecordType.BATTING, null, null)

            // then
            assertThat(result.year).isEqualTo(currentYear)
        }
    }

    @Nested
    @DisplayName("통산 기록 조회")
    inner class CareerRecord {
        @Test
        fun `통산 타격 기록을 조회한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerStatsService.getCareerBattingStats(1L) } returns createMockCareerBattingStats()

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.CAREER, RecordType.BATTING, null, null)

            // then
            assertThat(result.scope).isEqualTo(RecordScope.CAREER)
            assertThat(result.type).isEqualTo(RecordType.BATTING)
            assertThat(result.year).isNull()
            assertThat(result.battingStats).isNotNull
            assertThat(result.battingStats?.gamesPlayed).isEqualTo(300)
            assertThat(result.pitchingStats).isNull()
        }

        @Test
        fun `통산 투수 기록을 조회한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerStatsService.getCareerPitchingStats(1L) } returns createMockCareerPitchingStats()

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.CAREER, RecordType.PITCHING, null, null)

            // then
            assertThat(result.scope).isEqualTo(RecordScope.CAREER)
            assertThat(result.type).isEqualTo(RecordType.PITCHING)
            assertThat(result.pitchingStats).isNotNull
            assertThat(result.pitchingStats?.gamesPlayed).isEqualTo(100)
            assertThat(result.battingStats).isNull()
        }

        @Test
        fun `통산 타격 기록이 없으면 null을 반환한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every {
                playerStatsService.getCareerBattingStats(1L)
            } throws IllegalArgumentException("통계가 없습니다")

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.CAREER, RecordType.BATTING, null, null)

            // then
            assertThat(result.battingStats).isNull()
        }

        @Test
        fun `통산 투수 기록이 없으면 null을 반환한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every {
                playerStatsService.getCareerPitchingStats(1L)
            } throws IllegalArgumentException("통계가 없습니다")

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.CAREER, RecordType.PITCHING, null, null)

            // then
            assertThat(result.pitchingStats).isNull()
        }

        @Test
        fun `통산 ALL 타입으로 타격과 투수 기록을 모두 조회한다`() {
            // given
            every { playerRepository.findByIdOrNull(1L) } returns player
            every { playerStatsService.getCareerBattingStats(1L) } returns createMockCareerBattingStats()
            every { playerStatsService.getCareerPitchingStats(1L) } returns createMockCareerPitchingStats()

            // when
            val result = playerRecordService.getPlayerRecord(1L, RecordScope.CAREER, RecordType.ALL, null, null)

            // then
            assertThat(result.type).isEqualTo(RecordType.ALL)
            assertThat(result.battingStats).isNotNull
            assertThat(result.pitchingStats).isNotNull
        }
    }
}
