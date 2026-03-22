package com.nextup.core.domain.stats

import com.nextup.common.exception.StatsValidationException
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import com.nextup.core.domain.game.PlateAppearanceResult
import com.nextup.core.domain.player.BattingHand
import com.nextup.core.domain.player.Player
import com.nextup.core.domain.player.Position
import com.nextup.core.domain.player.ThrowingHand
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

@DisplayName("SeasonPitchingStats 테스트")
class SeasonPitchingStatsTest {
    private val testPlayer =
        Player(
            name = "박찬호",
            primaryPosition = Position.STARTING_PITCHER,
            throwingHand = ThrowingHand.RIGHT,
            battingHand = BattingHand.RIGHT,
            id = 1L,
        )

    @Nested
    @DisplayName("시즌 투수 통계 생성")
    inner class Create {
        @Test
        fun `should create season pitching stats successfully`() {
            // when
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // then
            assertThat(stats.player).isEqualTo(testPlayer)
            assertThat(stats.year).isEqualTo(2024)
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.wins).isZero
            assertThat(stats.inningsPitchedOuts).isZero
        }

        @Test
        fun `should throw exception when year is invalid`() {
            // when & then
            assertThrows<StatsValidationException> {
                SeasonPitchingStats.create(testPlayer, -1)
            }
        }

        @Test
        fun `should create season pitching stats with teamId`() {
            // when
            val stats = SeasonPitchingStats.create(testPlayer, 2024, teamId = 7L)

            // then
            assertThat(stats.player).isEqualTo(testPlayer)
            assertThat(stats.year).isEqualTo(2024)
            assertThat(stats.teamId).isEqualTo(7L)
            assertThat(stats.gamesPlayed).isZero
        }
    }

    @Nested
    @DisplayName("경기 기록 누적")
    inner class AddGameRecord {
        @Test
        fun `should accumulate single game record for starting pitcher correctly`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    // 6이닝 (18 outs), 2실점, 2자책, 5피안타, 2볼넷, 7삼진
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 2,
                        runsAllowed = 2,
                        hitsAllowed = 5,
                        walksAllowed = 2,
                        strikeouts = 7,
                        battersFaced = 25,
                        decision = PitchingDecision.WIN,
                    )
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isEqualTo(1)
            assertThat(stats.inningsPitchedOuts).isEqualTo(18)
            assertThat(stats.earnedRuns).isEqualTo(2)
            assertThat(stats.runsAllowed).isEqualTo(2)
            assertThat(stats.hitsAllowed).isEqualTo(5)
            assertThat(stats.walksAllowed).isEqualTo(2)
            assertThat(stats.strikeouts).isEqualTo(7)
            assertThat(stats.battersFaced).isEqualTo(25)
            assertThat(stats.wins).isEqualTo(1)
            assertThat(stats.losses).isZero
        }

        @Test
        fun `should accumulate relief pitcher record correctly`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    // 2이닝 (6 outs), 0실점, 세이브
                    setStats(
                        inningsPitchedOuts = 6,
                        earnedRuns = 0,
                        runsAllowed = 0,
                        hitsAllowed = 1,
                        walksAllowed = 0,
                        strikeouts = 3,
                        battersFaced = 7,
                        decision = PitchingDecision.SAVE,
                    )
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.saves).isEqualTo(1)
            assertThat(stats.wins).isZero
        }

        @Test
        fun `should accumulate multiple game records correctly`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()

            // Game 1: 선발 6이닝 승리
            val record1 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 2,
                        runsAllowed = 2,
                        hitsAllowed = 5,
                        walksAllowed = 2,
                        strikeouts = 6,
                        battersFaced = 24,
                        decision = PitchingDecision.WIN,
                    )
                }

            // Game 2: 선발 7이닝 패배
            val record2 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 21,
                        earnedRuns = 4,
                        runsAllowed = 4,
                        hitsAllowed = 8,
                        walksAllowed = 1,
                        strikeouts = 5,
                        battersFaced = 28,
                        decision = PitchingDecision.LOSS,
                    )
                }

            // Game 3: 중계 2이닝 홀드
            val record3 =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    setStats(
                        inningsPitchedOuts = 6,
                        earnedRuns = 0,
                        runsAllowed = 0,
                        hitsAllowed = 1,
                        walksAllowed = 1,
                        strikeouts = 2,
                        battersFaced = 7,
                        decision = PitchingDecision.HOLD,
                    )
                }

            // when
            stats.addGameRecord(record1)
            stats.addGameRecord(record2)
            stats.addGameRecord(record3)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(3)
            assertThat(stats.gamesStarted).isEqualTo(2)
            assertThat(stats.inningsPitchedOuts).isEqualTo(45) // 18 + 21 + 6
            assertThat(stats.wins).isEqualTo(1)
            assertThat(stats.losses).isEqualTo(1)
            assertThat(stats.holds).isEqualTo(1)
            assertThat(stats.earnedRuns).isEqualTo(6)
            assertThat(stats.hitsAllowed).isEqualTo(14)
            assertThat(stats.strikeouts).isEqualTo(13)
        }

        @Test
        fun `should accumulate pitches thrown when provided`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        pitchesThrown = 95,
                        strikesThrown = 63,
                    )
                }

            // when
            stats.addGameRecord(record)

            // then
            assertThat(stats.pitchesThrown).isEqualTo(95)
            assertThat(stats.strikesThrown).isEqualTo(63)
        }
    }

    @Nested
    @DisplayName("계산 속성 검증")
    inner class CalculatedProperties {
        @Test
        fun `should calculate ERA correctly`() {
            // given: 18 outs (6이닝), 3자책 => ERA = (3/6)*9 = 4.50
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 18, earnedRuns = 3)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isEqualByComparingTo(BigDecimal("4.50"))
        }

        @Test
        fun `should return zero ERA when no innings pitched and no earned runs`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isNotNull().isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `should return null ERA when no innings pitched but earned runs exist`() {
            // given: 0이닝이지만 자책점이 있는 경우 (무한대)
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 0, earnedRuns = 3)

            // when
            val era = stats.earnedRunAverage

            // then
            assertThat(era).isNull()
        }

        @Test
        fun `should calculate WHIP correctly`() {
            // given: 18 outs (6이닝), 5피안타, 2볼넷 => WHIP = (5+2)/6 = 1.17
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 18, hitsAllowed = 5, walksAllowed = 2)

            // when
            val whip = stats.whip

            // then
            assertThat(whip).isEqualByComparingTo(BigDecimal("1.17"))
        }

        @Test
        fun `should calculate K per 9 correctly`() {
            // given: 18 outs (6이닝), 9삼진 => K/9 = (9/6)*9 = 13.50
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 18, strikeouts = 9)

            // when
            val k9 = stats.strikeoutsPer9

            // then
            assertThat(k9).isEqualByComparingTo(BigDecimal("13.50"))
        }

        @Test
        fun `should calculate BB per 9 correctly`() {
            // given: 27 outs (9이닝), 3볼넷 => BB/9 = (3/9)*9 = 3.00
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 27, walksAllowed = 3)

            // when
            val bb9 = stats.walksPer9

            // then
            assertThat(bb9).isEqualByComparingTo(BigDecimal("3.00"))
        }

        @Test
        fun `should calculate K-BB ratio correctly`() {
            // given: 9삼진, 3볼넷 => K/BB = 3.00
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, strikeouts = 9, walksAllowed = 3)

            // when
            val kbb = stats.strikeoutToWalkRatio

            // then
            assertThat(kbb).isEqualByComparingTo(BigDecimal("3.00"))
        }

        @Test
        fun `should return strikeouts when walks is zero for K-BB ratio`() {
            // given: 10삼진, 0볼넷
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, strikeouts = 10, walksAllowed = 0)

            // when
            val kbb = stats.strikeoutToWalkRatio

            // then
            assertThat(kbb).isEqualByComparingTo(BigDecimal("10.00"))
        }

        @Test
        fun `should calculate strike percentage correctly`() {
            // given: 100투구, 65스트라이크 => 65%
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, pitchesThrown = 100, strikesThrown = 65)

            // when
            val strikePct = stats.strikePercentage

            // then
            assertThat(strikePct).isNotNull
            assertThat(strikePct).isEqualByComparingTo(BigDecimal("0.650"))
        }

        @Test
        fun `should return null strike percentage when pitches not recorded`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val strikePct = stats.strikePercentage

            // then
            assertThat(strikePct).isNull()
        }

        @Test
        fun `should calculate winning percentage correctly`() {
            // given: 8승 2패 => .800
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, wins = 8, losses = 2)

            // when
            val winPct = stats.winningPercentage

            // then
            assertThat(winPct).isEqualByComparingTo(BigDecimal("0.800"))
        }

        @Test
        fun `should calculate innings pitched display correctly`() {
            // given: 20 outs = 6.2 이닝
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 20)

            // when
            val display = stats.inningsPitchedDisplay

            // then
            assertThat(display).isEqualTo("6.2")
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class Validation {
        @Test
        fun `should validate successfully with valid stats`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(
                stats,
                gamesPlayed = 10,
                gamesStarted = 5,
                inningsPitchedOuts = 60,
                earnedRuns = 10,
                runsAllowed = 12,
            )

            // when & then
            stats.validate() // Should not throw
        }

        @Test
        fun `should throw exception when games started exceeds games played`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, gamesPlayed = 5, gamesStarted = 6)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when earned runs exceed runs allowed`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, earnedRuns = 10, runsAllowed = 5)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when strikes exceed pitches thrown`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, pitchesThrown = 100, strikesThrown = 101)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }

        @Test
        fun `should throw exception when games played is negative`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, gamesPlayed = -1)

            // when & then
            assertThrows<StatsValidationException> {
                stats.validate()
            }
        }
    }

    @Nested
    @DisplayName("경기 종료 시 기록 추가 (addGameRecordForEndOfGame)")
    inner class AddGameRecordForEndOfGame {
        @Test
        fun `실시간 갱신 필드를 제외하고 경기 요약 필드만 추가한다`() {
            // given: 경기 중 applyLiveUpdate로 피안타 2, 삼진 3, 볼넷 1이 이미 반영된 상태
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)

            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 1,
                        runsAllowed = 2,
                        hitsAllowed = 2,
                        walksAllowed = 1,
                        strikeouts = 3,
                        battersFaced = 6,
                        homeRunsAllowed = 0,
                        decision = PitchingDecision.WIN,
                    )
                }

            // when
            stats.addGameRecordForEndOfGame(record)

            // then: 실시간 갱신 필드는 applyLiveUpdate 값 그대로 (중복 추가 안 됨)
            assertThat(stats.hitsAllowed).isEqualTo(2)
            assertThat(stats.strikeouts).isEqualTo(3)
            assertThat(stats.walksAllowed).isEqualTo(1)
            assertThat(stats.homeRunsAllowed).isEqualTo(0)
            assertThat(stats.hitBatsmen).isEqualTo(0)
            assertThat(stats.battersFaced).isEqualTo(6)

            // then: 경기 요약 필드는 정상 추가됨
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isEqualTo(1)
            assertThat(stats.inningsPitchedOuts).isEqualTo(18)
            assertThat(stats.earnedRuns).isEqualTo(1)
            assertThat(stats.runsAllowed).isEqualTo(2)
            assertThat(stats.wins).isEqualTo(1)
        }

        @Test
        fun `addGameRecord는 실시간 갱신 필드도 포함하여 전체 기록을 추가한다`() {
            // given: 실시간 갱신 없이 바로 경기 종료 (신규 생성 케이스)
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 1,
                        runsAllowed = 2,
                        hitsAllowed = 5,
                        walksAllowed = 2,
                        strikeouts = 7,
                        battersFaced = 25,
                        homeRunsAllowed = 1,
                        decision = PitchingDecision.WIN,
                    )
                }

            // when
            stats.addGameRecord(record)

            // then: 모든 필드가 추가됨
            assertThat(stats.hitsAllowed).isEqualTo(5)
            assertThat(stats.strikeouts).isEqualTo(7)
            assertThat(stats.walksAllowed).isEqualTo(2)
            assertThat(stats.homeRunsAllowed).isEqualTo(1)
            assertThat(stats.battersFaced).isEqualTo(25)
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.inningsPitchedOuts).isEqualTo(18)
            assertThat(stats.earnedRuns).isEqualTo(1)
            assertThat(stats.runsAllowed).isEqualTo(2)
            assertThat(stats.wins).isEqualTo(1)
        }

        @Test
        fun `addGameRecordForEndOfGame는 구원 투수 기록도 정상 처리한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    setStats(
                        inningsPitchedOuts = 3,
                        earnedRuns = 0,
                        runsAllowed = 0,
                        hitsAllowed = 0,
                        walksAllowed = 0,
                        strikeouts = 1,
                        battersFaced = 1,
                        decision = PitchingDecision.SAVE,
                    )
                }

            // when
            stats.addGameRecordForEndOfGame(record)

            // then
            assertThat(stats.gamesPlayed).isEqualTo(1)
            assertThat(stats.gamesStarted).isEqualTo(0)
            assertThat(stats.saves).isEqualTo(1)
            assertThat(stats.inningsPitchedOuts).isEqualTo(3)
            assertThat(stats.strikeouts).isEqualTo(1) // applyLiveUpdate 값만, 중복 아님
            assertThat(stats.battersFaced).isEqualTo(1) // applyLiveUpdate 값만
        }
    }

    @Nested
    @DisplayName("경기 기록 롤백 (revertGameRecord)")
    inner class RevertGameRecord {
        @Test
        fun `경기 기록을 롤백하면 누적 통계가 차감된다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 2,
                        runsAllowed = 3,
                        hitsAllowed = 5,
                        walksAllowed = 2,
                        strikeouts = 7,
                        battersFaced = 25,
                        decision = PitchingDecision.WIN,
                    )
                }
            stats.addGameRecord(record)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.inningsPitchedOuts).isZero
            assertThat(stats.earnedRuns).isZero
            assertThat(stats.runsAllowed).isZero
            assertThat(stats.hitsAllowed).isZero
            assertThat(stats.walksAllowed).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.battersFaced).isZero
            assertThat(stats.wins).isZero
        }

        @Test
        fun `릴리프 투수 기록 롤백 시 gamesStarted는 변경되지 않는다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                    setStats(
                        inningsPitchedOuts = 6,
                        decision = PitchingDecision.SAVE,
                    )
                }
            stats.addGameRecord(record)
            assertThat(stats.gamesStarted).isZero

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.gamesStarted).isZero
            assertThat(stats.saves).isZero
        }

        @Test
        fun `롤백 시 값이 0 미만으로 내려가지 않는다`() {
            // given: 이미 0인 상태에서 롤백
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer, isStartingPitcher = true).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        earnedRuns = 5,
                        runsAllowed = 5,
                        hitsAllowed = 10,
                        walksAllowed = 3,
                        strikeouts = 8,
                        battersFaced = 30,
                        homeRunsAllowed = 2,
                        decision = PitchingDecision.WIN,
                    )
                }

            // when: 아무것도 누적하지 않고 바로 롤백
            stats.revertGameRecord(record)

            // then
            assertThat(stats.gamesPlayed).isZero
            assertThat(stats.inningsPitchedOuts).isZero
            assertThat(stats.earnedRuns).isZero
            assertThat(stats.wins).isZero
        }

        @Test
        fun `투구 수 롤백이 정상 처리된다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()
            val record =
                PitchingRecord.create(gamePlayer).apply {
                    setStats(
                        inningsPitchedOuts = 18,
                        pitchesThrown = 95,
                        strikesThrown = 63,
                    )
                }
            stats.addGameRecord(record)

            // when
            stats.revertGameRecord(record)

            // then
            assertThat(stats.pitchesThrown).isEqualTo(0)
            assertThat(stats.strikesThrown).isEqualTo(0)
        }

        @Test
        fun `각 판정별 롤백이 정상 처리된다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            val gamePlayer = mockk<GamePlayer>()

            val decisions =
                listOf(
                    PitchingDecision.WIN,
                    PitchingDecision.LOSS,
                    PitchingDecision.SAVE,
                    PitchingDecision.HOLD,
                    PitchingDecision.BLOWN_SAVE,
                )

            decisions.forEach { decision ->
                val record =
                    PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                        setStats(decision = decision)
                    }
                stats.addGameRecord(record)
            }

            assertThat(stats.wins).isEqualTo(1)
            assertThat(stats.losses).isEqualTo(1)
            assertThat(stats.saves).isEqualTo(1)
            assertThat(stats.holds).isEqualTo(1)
            assertThat(stats.blownSaves).isEqualTo(1)

            // when: 각 판정별로 롤백
            decisions.forEach { decision ->
                val record =
                    PitchingRecord.create(gamePlayer, isStartingPitcher = false).apply {
                        setStats(decision = decision)
                    }
                stats.revertGameRecord(record)
            }

            // then
            assertThat(stats.wins).isZero
            assertThat(stats.losses).isZero
            assertThat(stats.saves).isZero
            assertThat(stats.holds).isZero
            assertThat(stats.blownSaves).isZero
        }
    }

    @Nested
    @DisplayName("실시간 타석 결과 반영 (applyLiveUpdate)")
    inner class ApplyLiveUpdate {
        @Test
        fun `안타 결과를 반영하면 피안타가 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.hitsAllowed).isEqualTo(1)
        }

        @Test
        fun `2루타 결과를 반영하면 피안타가 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.DOUBLE)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.hitsAllowed).isEqualTo(1)
        }

        @Test
        fun `3루타 결과를 반영하면 피안타가 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.hitsAllowed).isEqualTo(1)
        }

        @Test
        fun `홈런 결과를 반영하면 피안타와 피홈런이 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.hitsAllowed).isEqualTo(1)
            assertThat(stats.homeRunsAllowed).isEqualTo(1)
        }

        @Test
        fun `삼진 결과를 반영하면 탈삼진이 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.strikeouts).isEqualTo(1)
        }

        @Test
        fun `볼넷 결과를 반영하면 볼넷허용이 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.walksAllowed).isEqualTo(1)
        }

        @Test
        fun `고의사구 결과를 반영하면 볼넷허용이 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.walksAllowed).isEqualTo(1)
        }

        @Test
        fun `사구 결과를 반영하면 사구가 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.hitBatsmen).isEqualTo(1)
        }

        @Test
        fun `땅볼 등 기타 결과는 대면타자만 증가한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.applyLiveUpdate(PlateAppearanceResult.GROUND_OUT)

            // then
            assertThat(stats.battersFaced).isEqualTo(1)
            assertThat(stats.hitsAllowed).isZero
            assertThat(stats.strikeouts).isZero
        }
    }

    @Nested
    @DisplayName("실시간 타석 결과 역산 (revertLiveUpdate)")
    inner class RevertLiveUpdate {
        @Test
        fun `안타 역산 시 피안타가 감소한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.SINGLE)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.SINGLE)

            // then
            assertThat(stats.battersFaced).isZero
            assertThat(stats.hitsAllowed).isZero
        }

        @Test
        fun `홈런 역산 시 피안타와 피홈런이 감소한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.HOME_RUN)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(stats.battersFaced).isZero
            assertThat(stats.hitsAllowed).isZero
            assertThat(stats.homeRunsAllowed).isZero
        }

        @Test
        fun `삼진 역산 시 삼진이 감소한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.STRIKEOUT)

            // then
            assertThat(stats.strikeouts).isZero
        }

        @Test
        fun `볼넷 역산 시 볼넷허용이 감소한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.WALK)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.WALK)

            // then
            assertThat(stats.walksAllowed).isZero
        }

        @Test
        fun `고의사구 역산 시 볼넷허용이 감소한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(stats.walksAllowed).isZero
        }

        @Test
        fun `사구 역산 시 사구가 감소한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.applyLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(stats.hitBatsmen).isZero
        }

        @Test
        fun `역산 시 값이 0 미만으로 내려가지 않는다`() {
            // given: 아무것도 적용하지 않고 바로 역산
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.revertLiveUpdate(PlateAppearanceResult.SINGLE)
            stats.revertLiveUpdate(PlateAppearanceResult.HOME_RUN)
            stats.revertLiveUpdate(PlateAppearanceResult.STRIKEOUT)
            stats.revertLiveUpdate(PlateAppearanceResult.WALK)
            stats.revertLiveUpdate(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(stats.battersFaced).isZero
            assertThat(stats.hitsAllowed).isZero
            assertThat(stats.homeRunsAllowed).isZero
            assertThat(stats.strikeouts).isZero
            assertThat(stats.walksAllowed).isZero
            assertThat(stats.hitBatsmen).isZero
        }
    }

    @Nested
    @DisplayName("시즌 통계 확정/해제 (finalize/unfinalize)")
    inner class FinalizeUnfinalize {
        @Test
        fun `통계를 확정하면 isFinalized가 true가 된다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            stats.finalize()

            // then
            assertThat(stats.isFinalized).isTrue()
        }

        @Test
        fun `이미 확정된 통계를 다시 확정하면 예외가 발생한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.finalize()

            // when & then
            assertThrows<IllegalArgumentException> {
                stats.finalize()
            }
        }

        @Test
        fun `확정된 통계를 해제하면 isFinalized가 false가 된다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            stats.finalize()

            // when
            stats.unfinalize()

            // then
            assertThat(stats.isFinalized).isFalse()
        }

        @Test
        fun `확정되지 않은 통계를 해제하면 예외가 발생한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when & then
            assertThrows<IllegalArgumentException> {
                stats.unfinalize()
            }
        }
    }

    @Nested
    @DisplayName("필드별 정정 (applyFieldCorrection)")
    inner class ApplyFieldCorrection {
        @Test
        fun `이닝 아웃 수를 정정할 수 있다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 18)

            // when
            stats.applyFieldCorrection("inningsPitchedOuts", 3)

            // then
            assertThat(stats.inningsPitchedOuts).isEqualTo(21)
        }

        @Test
        fun `자책점을 정정할 수 있다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, earnedRuns = 5, runsAllowed = 10)

            // when
            stats.applyFieldCorrection("earnedRuns", -2)

            // then
            assertThat(stats.earnedRuns).isEqualTo(3)
        }

        @Test
        fun `정정 시 값이 0 미만으로 내려가지 않는다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, strikeouts = 2)

            // when
            stats.applyFieldCorrection("strikeouts", -5)

            // then
            assertThat(stats.strikeouts).isZero
        }

        @Test
        fun `유효하지 않은 필드명이면 예외가 발생한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when & then
            assertThrows<IllegalArgumentException> {
                stats.applyFieldCorrection("invalidField", 1)
            }
        }

        @Test
        fun `모든 필드를 정정할 수 있다`() {
            // given: validate()를 통과하도록 충분한 값을 미리 설정
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(
                stats,
                gamesPlayed = 10,
                gamesStarted = 5,
                inningsPitchedOuts = 60,
                earnedRuns = 5,
                runsAllowed = 10,
                hitsAllowed = 10,
                walksAllowed = 5,
                strikeouts = 20,
                pitchesThrown = 200,
                strikesThrown = 100,
            )
            val clazz = SeasonPitchingStats::class.java
            setField(clazz, stats, "homeRunsAllowed", 3)
            setField(clazz, stats, "hitBatsmen", 2)
            setField(clazz, stats, "wildPitches", 1)
            setField(clazz, stats, "balks", 1)
            setField(clazz, stats, "battersFaced", 50)

            // when & then: 각 필드 정정이 예외 없이 동작
            // runsAllowed를 먼저 늘려서 earnedRuns 정정이 validate를 통과하도록 함
            stats.applyFieldCorrection("runsAllowed", 1)
            stats.applyFieldCorrection("earnedRuns", 1)
            stats.applyFieldCorrection("inningsPitchedOuts", 1)
            stats.applyFieldCorrection("hitsAllowed", 1)
            stats.applyFieldCorrection("walksAllowed", 1)
            stats.applyFieldCorrection("strikeouts", 1)
            stats.applyFieldCorrection("homeRunsAllowed", 1)
            stats.applyFieldCorrection("hitBatsmen", 1)
            stats.applyFieldCorrection("wildPitches", 1)
            stats.applyFieldCorrection("balks", 1)
            stats.applyFieldCorrection("battersFaced", 1)
            // pitchesThrown을 먼저 늘려서 strikesThrown 정정이 validate를 통과하도록 함
            stats.applyFieldCorrection("pitchesThrown", 1)
            stats.applyFieldCorrection("strikesThrown", 1)
        }

        @Test
        fun `투구 수 정정이 nullable 필드에서 정상 동작한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            assertThat(stats.pitchesThrown).isNull()

            // when
            stats.applyFieldCorrection("pitchesThrown", 50)

            // then
            assertThat(stats.pitchesThrown).isEqualTo(50)
        }

        @Test
        fun `정정 후 유효성 검증이 실행된다`() {
            // given: earnedRuns > runsAllowed 상태가 되도록 정정하면 예외 발생
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, earnedRuns = 5, runsAllowed = 5)

            // when & then
            assertThrows<StatsValidationException> {
                stats.applyFieldCorrection("earnedRuns", 1) // earnedRuns = 6 > runsAllowed = 5
            }
        }
    }

    @Nested
    @DisplayName("추가 계산 속성 검증")
    inner class AdditionalCalculatedProperties {
        @Test
        fun `WHIP는 이닝이 0일 때 0을 반환한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val whip = stats.whip

            // then
            assertThat(whip).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `K per 9는 이닝이 0일 때 0을 반환한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val k9 = stats.strikeoutsPer9

            // then
            assertThat(k9).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `BB per 9는 이닝이 0일 때 0을 반환한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val bb9 = stats.walksPer9

            // then
            assertThat(bb9).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `K-BB 비율은 삼진과 볼넷 모두 0일 때 0을 반환한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val kbb = stats.strikeoutToWalkRatio

            // then
            assertThat(kbb).isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `비자책 실점이 올바르게 계산된다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, runsAllowed = 10, earnedRuns = 7)

            // when & then
            assertThat(stats.unearnedRuns).isEqualTo(3)
        }

        @Test
        fun `승률은 승패 모두 0일 때 0을 반환한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)

            // when
            val winPct = stats.winningPercentage

            // then
            assertThat(winPct).isEqualByComparingTo(BigDecimal("0.000"))
        }

        @Test
        fun `스트라이크 비율은 투구 수가 0일 때 null을 반환한다`() {
            // given
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, pitchesThrown = 0, strikesThrown = 0)

            // when
            val strikePct = stats.strikePercentage

            // then
            assertThat(strikePct).isNull()
        }

        @Test
        fun `이닝 표시 문자열이 올바르다 - 정수 이닝`() {
            // given: 27 outs = 9.0 이닝
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 27)

            // when & then
            assertThat(stats.inningsPitchedDisplay).isEqualTo("9.0")
            assertThat(stats.completeInnings).isEqualTo(9)
            assertThat(stats.remainingOuts).isZero
        }

        @Test
        fun `이닝 표시 문자열이 올바르다 - 소수 이닝`() {
            // given: 19 outs = 6.1 이닝
            val stats = SeasonPitchingStats.create(testPlayer, 2024)
            setStatsDirectly(stats, inningsPitchedOuts = 19)

            // when & then
            assertThat(stats.inningsPitchedDisplay).isEqualTo("6.1")
            assertThat(stats.completeInnings).isEqualTo(6)
            assertThat(stats.remainingOuts).isEqualTo(1)
        }
    }

    // Helper methods

    private fun PitchingRecord.setStats(
        inningsPitchedOuts: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        hitsAllowed: Int = 0,
        walksAllowed: Int = 0,
        strikeouts: Int = 0,
        battersFaced: Int = 0,
        homeRunsAllowed: Int = 0,
        decision: PitchingDecision = PitchingDecision.NONE,
        pitchesThrown: Int? = null,
        strikesThrown: Int? = null,
    ) {
        setField("inningsPitchedOuts", inningsPitchedOuts)
        setField("earnedRuns", earnedRuns)
        setField("runsAllowed", runsAllowed)
        setField("hitsAllowed", hitsAllowed)
        setField("walksAllowed", walksAllowed)
        setField("strikeouts", strikeouts)
        setField("battersFaced", battersFaced)
        setField("homeRunsAllowed", homeRunsAllowed)
        setField("decision", decision)
        setField("pitchesThrown", pitchesThrown)
        setField("strikesThrown", strikesThrown)
    }

    private fun PitchingRecord.setField(
        fieldName: String,
        value: Any?,
    ) {
        val field = PitchingRecord::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(this, value)
    }

    private fun setStatsDirectly(
        stats: SeasonPitchingStats,
        gamesPlayed: Int = 0,
        gamesStarted: Int = 0,
        inningsPitchedOuts: Int = 0,
        wins: Int = 0,
        losses: Int = 0,
        earnedRuns: Int = 0,
        runsAllowed: Int = 0,
        hitsAllowed: Int = 0,
        walksAllowed: Int = 0,
        strikeouts: Int = 0,
        pitchesThrown: Int? = null,
        strikesThrown: Int? = null,
    ) {
        val clazz = SeasonPitchingStats::class.java
        setField(clazz, stats, "gamesPlayed", gamesPlayed)
        setField(clazz, stats, "gamesStarted", gamesStarted)
        setField(clazz, stats, "inningsPitchedOuts", inningsPitchedOuts)
        setField(clazz, stats, "wins", wins)
        setField(clazz, stats, "losses", losses)
        setField(clazz, stats, "earnedRuns", earnedRuns)
        setField(clazz, stats, "runsAllowed", runsAllowed)
        setField(clazz, stats, "hitsAllowed", hitsAllowed)
        setField(clazz, stats, "walksAllowed", walksAllowed)
        setField(clazz, stats, "strikeouts", strikeouts)
        setField(clazz, stats, "pitchesThrown", pitchesThrown)
        setField(clazz, stats, "strikesThrown", strikesThrown)
    }

    private fun setField(
        clazz: Class<*>,
        obj: Any,
        fieldName: String,
        value: Any?,
    ) {
        val field = clazz.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(obj, value)
    }
}
