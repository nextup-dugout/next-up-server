package com.nextup.core.service.stats

import com.nextup.core.domain.game.BattingRecord
import com.nextup.core.domain.game.Game
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import com.nextup.core.domain.stats.CareerBattingStats
import com.nextup.core.domain.stats.CareerPitchingStats
import com.nextup.core.domain.stats.SeasonBattingStats
import com.nextup.core.domain.stats.SeasonPitchingStats
import com.nextup.core.port.repository.BattingRecordRepositoryPort
import com.nextup.core.port.repository.CareerBattingStatsRepositoryPort
import com.nextup.core.port.repository.CareerPitchingStatsRepositoryPort
import com.nextup.core.port.repository.PitchingRecordRepositoryPort
import com.nextup.core.port.repository.PlayerRepositoryPort
import com.nextup.core.port.repository.SeasonBattingStatsRepositoryPort
import com.nextup.core.port.repository.SeasonPitchingStatsRepositoryPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@DisplayName("PlayerStatsService 테스트")
class PlayerStatsServiceTest {
    private lateinit var playerStatsService: PlayerStatsService
    private lateinit var playerRepository: PlayerRepositoryPort
    private lateinit var seasonBattingStatsRepository: SeasonBattingStatsRepositoryPort
    private lateinit var seasonPitchingStatsRepository: SeasonPitchingStatsRepositoryPort
    private lateinit var careerBattingStatsRepository: CareerBattingStatsRepositoryPort
    private lateinit var careerPitchingStatsRepository: CareerPitchingStatsRepositoryPort
    private lateinit var battingRecordRepository: BattingRecordRepositoryPort
    private lateinit var pitchingRecordRepository: PitchingRecordRepositoryPort

    private val testPlayer =
        Player(
            name = "홍길동",
            primaryPosition = Position.CATCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    private val playerId = 1L
    private val year = 2024

    @BeforeEach
    fun setUp() {
        playerRepository = mockk()
        seasonBattingStatsRepository = mockk()
        seasonPitchingStatsRepository = mockk()
        careerBattingStatsRepository = mockk()
        careerPitchingStatsRepository = mockk()
        battingRecordRepository = mockk()
        pitchingRecordRepository = mockk()

        playerStatsService =
            PlayerStatsService(
                playerRepository,
                seasonBattingStatsRepository,
                seasonPitchingStatsRepository,
                careerBattingStatsRepository,
                careerPitchingStatsRepository,
                battingRecordRepository,
                pitchingRecordRepository,
            )
    }

    @Nested
    @DisplayName("시즌 타격 통계 조회")
    inner class GetSeasonBattingStats {
        @Test
        fun `should return season batting stats when exists`() {
            // given
            val stats = SeasonBattingStats.create(testPlayer, year)
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns stats

            // when
            val result = playerStatsService.getSeasonBattingStats(playerId, year)

            // then
            assertThat(result).isEqualTo(stats)
            verify { seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year) }
        }

        @Test
        fun `should throw exception when season batting stats not found`() {
            // given
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns null

            // when & then
            assertThrows<IllegalArgumentException> {
                playerStatsService.getSeasonBattingStats(playerId, year)
            }
        }
    }

    @Nested
    @DisplayName("시즌 투수 통계 조회")
    inner class GetSeasonPitchingStats {
        @Test
        fun `should return season pitching stats when exists`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, year)
            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns stats

            // when
            val result = playerStatsService.getSeasonPitchingStats(playerId, year)

            // then
            assertThat(result).isEqualTo(stats)
            verify { seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year) }
        }

        @Test
        fun `should throw exception when season pitching stats not found`() {
            // given
            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns null

            // when & then
            assertThrows<IllegalArgumentException> {
                playerStatsService.getSeasonPitchingStats(playerId, year)
            }
        }
    }

    @Nested
    @DisplayName("통산 타격 통계 조회")
    inner class GetCareerBattingStats {
        @Test
        fun `should return career batting stats when exists`() {
            // given
            val stats = CareerBattingStats.create(testPlayer)
            every { careerBattingStatsRepository.findByPlayerId(playerId) } returns stats

            // when
            val result = playerStatsService.getCareerBattingStats(playerId)

            // then
            assertThat(result).isEqualTo(stats)
            verify { careerBattingStatsRepository.findByPlayerId(playerId) }
        }

        @Test
        fun `should throw exception when career batting stats not found`() {
            // given
            every { careerBattingStatsRepository.findByPlayerId(playerId) } returns null

            // when & then
            assertThrows<IllegalArgumentException> {
                playerStatsService.getCareerBattingStats(playerId)
            }
        }
    }

    @Nested
    @DisplayName("통산 투수 통계 조회")
    inner class GetCareerPitchingStats {
        @Test
        fun `should return career pitching stats when exists`() {
            // given
            val stats = CareerPitchingStats.create(testPlayer)
            every { careerPitchingStatsRepository.findByPlayerId(playerId) } returns stats

            // when
            val result = playerStatsService.getCareerPitchingStats(playerId)

            // then
            assertThat(result).isEqualTo(stats)
            verify { careerPitchingStatsRepository.findByPlayerId(playerId) }
        }

        @Test
        fun `should throw exception when career pitching stats not found`() {
            // given
            every { careerPitchingStatsRepository.findByPlayerId(playerId) } returns null

            // when & then
            assertThrows<IllegalArgumentException> {
                playerStatsService.getCareerPitchingStats(playerId)
            }
        }
    }

    @Nested
    @DisplayName("모든 시즌 통계 조회")
    inner class GetAllSeasonStats {
        @Test
        fun `should return all season batting stats`() {
            // given
            val stats1 = SeasonBattingStats.create(testPlayer, 2023)
            val stats2 = SeasonBattingStats.create(testPlayer, 2024)
            every { seasonBattingStatsRepository.findAllByPlayerId(playerId) } returns listOf(stats1, stats2)

            // when
            val result = playerStatsService.getAllSeasonBattingStats(playerId)

            // then
            assertThat(result).hasSize(2)
            verify { seasonBattingStatsRepository.findAllByPlayerId(playerId) }
        }

        @Test
        fun `should return all season pitching stats`() {
            // given
            val stats1 = SeasonPitchingStats.create(testPlayer, 2023)
            val stats2 = SeasonPitchingStats.create(testPlayer, 2024)
            every { seasonPitchingStatsRepository.findAllByPlayerId(playerId) } returns listOf(stats1, stats2)

            // when
            val result = playerStatsService.getAllSeasonPitchingStats(playerId)

            // then
            assertThat(result).hasSize(2)
            verify { seasonPitchingStatsRepository.findAllByPlayerId(playerId) }
        }

        @Test
        fun `should return empty list when no season stats exist`() {
            // given
            every { seasonBattingStatsRepository.findAllByPlayerId(playerId) } returns emptyList()

            // when
            val result = playerStatsService.getAllSeasonBattingStats(playerId)

            // then
            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("시즌 타격 통계 갱신")
    inner class UpdatePlayerBattingStats {
        @Test
        fun `should create and return new season batting stats when not exists`() {
            // given
            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns null
            every { battingRecordRepository.findAllByPlayerIdAndYear(playerId, year) } returns emptyList()
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updatePlayerBattingStats(playerId, year)

            // then
            assertThat(result.player).isEqualTo(testPlayer)
            assertThat(result.year).isEqualTo(year)
            verify { seasonBattingStatsRepository.save(any()) }
        }

        @Test
        fun `should update existing season batting stats with records`() {
            // given
            val existingStats = SeasonBattingStats.create(testPlayer, year)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                BattingRecord.create(gamePlayer).apply {
                    setStats(pa = 4, ab = 4, h = 2)
                }

            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { seasonBattingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns existingStats
            every { battingRecordRepository.findAllByPlayerIdAndYear(playerId, year) } returns listOf(record)
            every { seasonBattingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updatePlayerBattingStats(playerId, year)

            // then
            assertThat(result.gamesPlayed).isEqualTo(1)
            assertThat(result.hits).isEqualTo(2)
            assertThat(result.atBats).isEqualTo(4)
            verify { seasonBattingStatsRepository.save(any()) }
        }

        @Test
        fun `should throw exception when player not found`() {
            // given
            every { playerRepository.findByIdOrNull(playerId) } returns null

            // when & then
            assertThrows<IllegalArgumentException> {
                playerStatsService.updatePlayerBattingStats(playerId, year)
            }
        }
    }

    @Nested
    @DisplayName("시즌 투수 통계 갱신")
    inner class UpdatePlayerPitchingStats {
        @Test
        fun `should create and return new season pitching stats when not exists`() {
            // given
            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns null
            every { pitchingRecordRepository.findAllByPlayerIdAndYear(playerId, year) } returns emptyList()
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updatePlayerPitchingStats(playerId, year)

            // then
            assertThat(result.player).isEqualTo(testPlayer)
            assertThat(result.year).isEqualTo(year)
            verify { seasonPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `should update existing season pitching stats with records`() {
            // given
            val existingStats = SeasonPitchingStats.create(testPlayer, year)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 2,
                        runsAllowed = 2,
                        strikeouts = 6,
                        decision = PitchingDecision.WIN,
                    )
                }

            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { seasonPitchingStatsRepository.findByPlayerIdAndYear(playerId, year) } returns existingStats
            every { pitchingRecordRepository.findAllByPlayerIdAndYear(playerId, year) } returns listOf(record)
            every { seasonPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updatePlayerPitchingStats(playerId, year)

            // then
            assertThat(result.gamesPlayed).isEqualTo(1)
            assertThat(result.gamesStarted).isEqualTo(1)
            assertThat(result.wins).isEqualTo(1)
            assertThat(result.strikeouts).isEqualTo(6)
            verify { seasonPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `should throw exception when player not found`() {
            // given
            every { playerRepository.findByIdOrNull(playerId) } returns null

            // when & then
            assertThrows<IllegalArgumentException> {
                playerStatsService.updatePlayerPitchingStats(playerId, year)
            }
        }
    }

    @Nested
    @DisplayName("통산 타격 통계 갱신")
    inner class UpdateCareerBattingStats {
        @Test
        fun `should create and return new career batting stats when not exists`() {
            // given
            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { careerBattingStatsRepository.findByPlayerId(playerId) } returns null
            every { battingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updateCareerBattingStats(playerId)

            // then
            assertThat(result.player).isEqualTo(testPlayer)
            assertThat(result.seasonsPlayed).isZero
            verify { careerBattingStatsRepository.save(any()) }
        }

        @Test
        fun `should update career batting stats with records from multiple seasons`() {
            // given
            val existingStats = CareerBattingStats.create(testPlayer)

            val game2023 = createMockGame(2023)
            val game2024 = createMockGame(2024)

            val record1 = createMockBattingRecord(game2023, 4, 4, 2)
            val record2 = createMockBattingRecord(game2024, 4, 3, 1)

            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { careerBattingStatsRepository.findByPlayerId(playerId) } returns existingStats
            every { battingRecordRepository.findAllByPlayerId(playerId) } returns listOf(record1, record2)
            every { careerBattingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updateCareerBattingStats(playerId)

            // then
            assertThat(result.gamesPlayed).isEqualTo(2)
            assertThat(result.hits).isEqualTo(3)
            assertThat(result.atBats).isEqualTo(7)
            assertThat(result.seasonsPlayed).isEqualTo(2)
            verify { careerBattingStatsRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("통산 투수 통계 갱신")
    inner class UpdateCareerPitchingStats {
        @Test
        fun `should create and return new career pitching stats when not exists`() {
            // given
            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { careerPitchingStatsRepository.findByPlayerId(playerId) } returns null
            every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns emptyList()
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updateCareerPitchingStats(playerId)

            // then
            assertThat(result.player).isEqualTo(testPlayer)
            assertThat(result.seasonsPlayed).isZero
            verify { careerPitchingStatsRepository.save(any()) }
        }

        @Test
        fun `should update career pitching stats with records from multiple seasons`() {
            // given
            val existingStats = CareerPitchingStats.create(testPlayer)

            val game2023 = createMockGame(2023)
            val game2024 = createMockGame(2024)

            val record1 = createMockPitchingRecord(game2023, 18, 2, 2, 6, true, PitchingDecision.WIN)
            val record2 = createMockPitchingRecord(game2024, 21, 3, 3, 5, true, PitchingDecision.LOSS)

            every { playerRepository.findByIdOrNull(playerId) } returns testPlayer
            every { careerPitchingStatsRepository.findByPlayerId(playerId) } returns existingStats
            every { pitchingRecordRepository.findAllByPlayerId(playerId) } returns listOf(record1, record2)
            every { careerPitchingStatsRepository.save(any()) } answers { firstArg() }

            // when
            val result = playerStatsService.updateCareerPitchingStats(playerId)

            // then
            assertThat(result.gamesPlayed).isEqualTo(2)
            assertThat(result.gamesStarted).isEqualTo(2)
            assertThat(result.wins).isEqualTo(1)
            assertThat(result.losses).isEqualTo(1)
            assertThat(result.strikeouts).isEqualTo(11)
            assertThat(result.seasonsPlayed).isEqualTo(2)
            verify { careerPitchingStatsRepository.save(any()) }
        }
    }

    @Nested
    @DisplayName("시즌 타격 리더보드")
    inner class GetSeasonBattingLeaders {
        @Test
        fun `should return batting average leaders`() {
            // given
            val stats = listOf(SeasonBattingStats.create(testPlayer, year))
            every { seasonBattingStatsRepository.findTopByBattingAverage(year, 50, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonBattingLeaders(year, BattingCategory.AVG)

            // then
            assertThat(result).hasSize(1)
            verify { seasonBattingStatsRepository.findTopByBattingAverage(year, 50, 10) }
        }

        @Test
        fun `should return home run leaders`() {
            // given
            val stats = listOf(SeasonBattingStats.create(testPlayer, year))
            every { seasonBattingStatsRepository.findTopByHomeRuns(year, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonBattingLeaders(year, BattingCategory.HR)

            // then
            assertThat(result).hasSize(1)
            verify { seasonBattingStatsRepository.findTopByHomeRuns(year, 10) }
        }

        @Test
        fun `should return RBI leaders`() {
            // given
            val stats = listOf(SeasonBattingStats.create(testPlayer, year))
            every { seasonBattingStatsRepository.findTopByRunsBattedIn(year, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonBattingLeaders(year, BattingCategory.RBI)

            // then
            assertThat(result).hasSize(1)
            verify { seasonBattingStatsRepository.findTopByRunsBattedIn(year, 10) }
        }

        @Test
        fun `should return OPS leaders`() {
            // given
            val stats = listOf(SeasonBattingStats.create(testPlayer, year))
            every { seasonBattingStatsRepository.findTopByOps(year, 50, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonBattingLeaders(year, BattingCategory.OPS)

            // then
            assertThat(result).hasSize(1)
            verify { seasonBattingStatsRepository.findTopByOps(year, 50, 10) }
        }

        @Test
        fun `should use custom minAtBats and limit`() {
            // given
            val stats = listOf(SeasonBattingStats.create(testPlayer, year))
            every { seasonBattingStatsRepository.findTopByBattingAverage(year, 100, 5) } returns stats

            // when
            val result = playerStatsService.getSeasonBattingLeaders(year, BattingCategory.AVG, 100, 5)

            // then
            assertThat(result).hasSize(1)
            verify { seasonBattingStatsRepository.findTopByBattingAverage(year, 100, 5) }
        }
    }

    @Nested
    @DisplayName("시즌 투수 리더보드")
    inner class GetSeasonPitchingLeaders {
        @Test
        fun `should return ERA leaders`() {
            // given
            val stats = listOf(SeasonPitchingStats.create(testPlayer, year))
            every { seasonPitchingStatsRepository.findTopByEra(year, 45, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonPitchingLeaders(year, PitchingCategory.ERA)

            // then
            assertThat(result).hasSize(1)
            verify { seasonPitchingStatsRepository.findTopByEra(year, 45, 10) }
        }

        @Test
        fun `should return wins leaders`() {
            // given
            val stats = listOf(SeasonPitchingStats.create(testPlayer, year))
            every { seasonPitchingStatsRepository.findTopByWins(year, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonPitchingLeaders(year, PitchingCategory.WINS)

            // then
            assertThat(result).hasSize(1)
            verify { seasonPitchingStatsRepository.findTopByWins(year, 10) }
        }

        @Test
        fun `should return strikeout leaders`() {
            // given
            val stats = listOf(SeasonPitchingStats.create(testPlayer, year))
            every { seasonPitchingStatsRepository.findTopByStrikeouts(year, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonPitchingLeaders(year, PitchingCategory.STRIKEOUTS)

            // then
            assertThat(result).hasSize(1)
            verify { seasonPitchingStatsRepository.findTopByStrikeouts(year, 10) }
        }

        @Test
        fun `should return saves leaders`() {
            // given
            val stats = listOf(SeasonPitchingStats.create(testPlayer, year))
            every { seasonPitchingStatsRepository.findTopBySaves(year, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonPitchingLeaders(year, PitchingCategory.SAVES)

            // then
            assertThat(result).hasSize(1)
            verify { seasonPitchingStatsRepository.findTopBySaves(year, 10) }
        }

        @Test
        fun `should return WHIP leaders`() {
            // given
            val stats = listOf(SeasonPitchingStats.create(testPlayer, year))
            every { seasonPitchingStatsRepository.findTopByWhip(year, 45, 10) } returns stats

            // when
            val result = playerStatsService.getSeasonPitchingLeaders(year, PitchingCategory.WHIP)

            // then
            assertThat(result).hasSize(1)
            verify { seasonPitchingStatsRepository.findTopByWhip(year, 45, 10) }
        }

        @Test
        fun `should use custom minInnings and limit`() {
            // given
            val stats = listOf(SeasonPitchingStats.create(testPlayer, year))
            every { seasonPitchingStatsRepository.findTopByEra(year, 90, 5) } returns stats

            // when
            val result = playerStatsService.getSeasonPitchingLeaders(year, PitchingCategory.ERA, 30, 5)

            // then
            assertThat(result).hasSize(1)
            verify { seasonPitchingStatsRepository.findTopByEra(year, 90, 5) }
        }
    }

    // Helper methods

    private fun BattingRecord.setStats(
        pa: Int = 0,
        ab: Int = 0,
        h: Int = 0,
    ) {
        setField("plateAppearances", pa)
        setField("atBats", ab)
        setField("hits", h)
    }

    private fun BattingRecord.setField(
        fieldName: String,
        value: Int,
    ) {
        val field = BattingRecord::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun PitchingRecord.setStats(
        inningsPitchedOuts: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        strikeouts: Int = 0,
        decision: PitchingDecision = PitchingDecision.NONE,
    ) {
        setField("inningsPitchedOuts", inningsPitchedOuts)
        setField("earnedRuns", earnedRuns)
        setField("runsAllowed", runsAllowed)
        setField("strikeouts", strikeouts)
        setField("decision", decision)
    }

    private fun PitchingRecord.setField(
        fieldName: String,
        value: Any,
    ) {
        val field = PitchingRecord::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun createMockGame(year: Int): Game =
        mockk {
            every { scheduledAt } returns java.time.LocalDateTime.of(year, 6, 1, 14, 0, 0)
        }

    private fun createMockBattingRecord(
        game: Game,
        pa: Int,
        ab: Int,
        h: Int,
    ): BattingRecord {
        val gameTeam =
            mockk<GameTeam> {
                every { this@mockk.game } returns game
            }
        val gamePlayer =
            mockk<GamePlayer> {
                every { this@mockk.gameTeam } returns gameTeam
            }
        return BattingRecord.create(gamePlayer).apply {
            setStats(pa, ab, h)
        }
    }

    private fun createMockPitchingRecord(
        game: Game,
        inningsPitchedOuts: Int,
        earnedRuns: Int,
        runsAllowed: Int,
        strikeouts: Int,
        isStartingPitcher: Boolean,
        decision: PitchingDecision,
    ): PitchingRecord {
        val gameTeam =
            mockk<GameTeam> {
                every { this@mockk.game } returns game
            }
        val gamePlayer =
            mockk<GamePlayer> {
                every { this@mockk.gameTeam } returns gameTeam
            }
        return PitchingRecord.create(gamePlayer, isStartingPitcher).apply {
            setStats(inningsPitchedOuts, earnedRuns, runsAllowed, strikeouts, decision)
        }
    }
}
