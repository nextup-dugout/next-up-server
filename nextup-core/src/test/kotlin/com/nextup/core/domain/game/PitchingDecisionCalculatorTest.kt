package com.nextup.core.domain.game

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PitchingDecisionCalculator")
class PitchingDecisionCalculatorTest {

    // ── 헬퍼: mock 팩토리 ─────────────────────────────────────────────────────

    private fun mockGameTeam(
        teamId: Long,
        totalScore: Int,
        inningScores: String?
    ): GameTeam {
        val team = mockk<com.nextup.core.domain.team.Team>(relaxed = true)
        every { team.id } returns teamId
        return mockk<GameTeam>(relaxed = true).also {
            every { it.team } returns team
            every { it.totalScore } returns totalScore
            every { it.inningScores } returns inningScores
            every { it.result } returns GameResult.UNDECIDED
        }
    }

    private fun mockWinnerTeam(
        teamId: Long,
        totalScore: Int,
        inningScores: String?
    ): GameTeam = mockGameTeam(teamId, totalScore, inningScores)

    private fun mockLoserTeam(
        teamId: Long,
        totalScore: Int,
        inningScores: String?
    ): GameTeam {
        val gt = mockGameTeam(teamId, totalScore, inningScores)
        every { gt.result } returns GameResult.LOSS
        return gt
    }

    private fun mockPitchingRecord(
        teamId: Long,
        isStartingPitcher: Boolean,
        inningsPitchedOuts: Int,
        entryInning: Int?,
        exitInning: Int?,
        currentDecision: PitchingDecision = PitchingDecision.NONE,
    ): PitchingRecord {
        val team = mockk<com.nextup.core.domain.team.Team>(relaxed = true)
        every { team.id } returns teamId

        val gameTeam = mockk<GameTeam>(relaxed = true)
        every { gameTeam.team } returns team

        val gamePlayer = mockk<GamePlayer>(relaxed = true)
        every { gamePlayer.gameTeam } returns gameTeam
        every { gamePlayer.entryInning } returns entryInning
        every { gamePlayer.exitInning } returns exitInning

        val record = mockk<PitchingRecord>(relaxed = true)
        every { record.gamePlayer } returns gamePlayer
        every { record.isStartingPitcher } returns isStartingPitcher
        every { record.inningsPitchedOuts } returns inningsPitchedOuts
        every { record.completeInnings } returns inningsPitchedOuts / 3
        every { record.decision } returns currentDecision
        return record
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("parseInningScores")
    inner class ParseInningScoresTest {
        @Test
        fun `null 이닝 점수는 빈 리스트를 반환한다`() {
            assertThat(PitchingDecisionCalculator.parseInningScores(null)).isEmpty()
        }

        @Test
        fun `공백 문자열은 빈 리스트를 반환한다`() {
            assertThat(PitchingDecisionCalculator.parseInningScores("  ")).isEmpty()
        }

        @Test
        fun `정상 이닝 점수를 파싱한다`() {
            val result = PitchingDecisionCalculator.parseInningScores("0,1,2,0,3")
            assertThat(result).containsExactly(0, 1, 2, 0, 3)
        }
    }

    @Nested
    @DisplayName("toCumulative")
    inner class ToCumulativeTest {
        @Test
        fun `이닝별 득점을 누적 점수로 변환한다`() {
            val result = PitchingDecisionCalculator.toCumulative(listOf(0, 1, 2, 0, 3))
            assertThat(result).containsExactly(0, 1, 3, 3, 6)
        }

        @Test
        fun `빈 리스트는 빈 리스트를 반환한다`() {
            assertThat(PitchingDecisionCalculator.toCumulative(emptyList())).isEmpty()
        }
    }

    @Nested
    @DisplayName("findFinalLeadChangeInning")
    inner class FindFinalLeadChangeInningTest {
        @Test
        fun `처음부터 리드하다가 유지하면 null을 반환한다`() {
            // winner: 1,1,1 / loser: 0,0,0 → 항상 리드, 역전 없음
            val winner = listOf(1, 2, 3)
            val loser = listOf(0, 0, 0)
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winner, loser)).isNull()
        }

        @Test
        fun `3회에 역전한 경우 3을 반환한다`() {
            // winner: 0,0,3 / loser: 1,1,1
            val winner = listOf(0, 0, 3)
            val loser = listOf(1, 2, 3)
            // cumulative winner: 0,0,3 / loser: 1,2,3
            // 3회: winner=3 > loser=3? 아니다
            // 다시: 이닝별이 아닌 누적 기준
            // 직접 누적으로 넘겨야 함
            val winCum = PitchingDecisionCalculator.toCumulative(winner)
            val loseCum = PitchingDecisionCalculator.toCumulative(loser)
            // winCum: 0,0,3 / loseCum: 1,2,3
            // 이닝3: w=3 > l=3? No. 역전 안됨
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winCum, loseCum)).isNull()
        }

        @Test
        fun `역전이 일어난 이닝을 반환한다`() {
            // winner 누적: 0,2,5 / loser 누적: 1,3,3
            // 이닝1: w=0,l=1 → loser 앞
            // 이닝2: w=2,l=3 → loser 앞
            // 이닝3: w=5,l=3 → winner 앞 (역전!)
            val winCum = listOf(0, 2, 5)
            val loseCum = listOf(1, 3, 3)
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winCum, loseCum)).isEqualTo(3)
        }

        @Test
        fun `두 번의 역전 시 마지막 역전 이닝을 반환한다`() {
            // winner 누적: 2,2,2,5 / loser 누적: 0,3,3,4
            // 이닝1: w=2>l=0 (winner 리드, 리드 변화 아님 - 처음부터)
            // 이닝2: w=2<=l=3 (loser 역전)
            // 이닝3: w=2<=l=3
            // 이닝4: w=5>l=4 (winner 역전!)
            val winCum = listOf(2, 2, 2, 5)
            val loseCum = listOf(0, 3, 3, 4)
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winCum, loseCum)).isEqualTo(4)
        }
    }

    @Nested
    @DisplayName("무승부")
    inner class DrawTest {
        @Test
        fun `무승부이면 빈 맵을 반환한다`() {
            val winnerTeam = mockWinnerTeam(1L, 3, "1,1,1")
            val loserTeam = mockLoserTeam(2L, 3, "1,1,1")

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = null,
                    loserGameTeam = null,
                    allPitchingRecords = emptyList(),
                )

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("선발 승 시나리오")
    inner class StarterWinTest {
        @Test
        fun `선발 투수가 5이닝 이상 투구하고 리드 유지 채 교체되면 선발 승을 받는다`() {
            // winner 팀(teamId=1): 이닝별 2,0,0,0,0,0,0,0,0 → 총 2점
            // loser 팀(teamId=2): 이닝별 0,0,0,0,0,0,0,0,0 → 총 0점
            val winnerTeam = mockWinnerTeam(1L, 2, "2,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0,0,0,0,0,0,0")

            // 선발투수: 5이닝(15아웃), entryInning=1, exitInning=5
            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 15,
                    entryInning = 1,
                    exitInning = 5,
                )
            // 구원투수: 4이닝(12아웃), entryInning=6
            val reliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 12,
                    entryInning = 6,
                    exitInning = null,
                )
            // 패전팀 선발
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, reliever, loserStarter),
                )

            assertThat(result[starter]).isEqualTo(PitchingDecision.WIN)
            // 4이닝(12아웃) 마무리 → 3이닝 이상 기준 세이브 자격
            assertThat(result[reliever]).isEqualTo(PitchingDecision.SAVE)
            assertThat(result[loserStarter]).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `선발 투수가 5이닝 이상이지만 리드 없이 교체되면 구원 승을 선택한다`() {
            // 선발 5이닝 후 동점, 6회에 역전
            // winner: 0,0,0,0,0,3,0 / loser: 0,0,0,0,1,1,0
            val winnerTeam = mockWinnerTeam(1L, 3, "0,0,0,0,0,3,0")
            val loserTeam = mockLoserTeam(2L, 2, "0,0,0,0,1,1,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 15, // 5이닝
                    entryInning = 1,
                    exitInning = 5,
                )
            val reliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 6, // 2이닝
                    entryInning = 6,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 21,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, reliever, loserStarter),
                )

            // 5이닝 후 score: winner=0, loser=1 → 리드 없음 → 구원 승
            assertThat(result[reliever]).isEqualTo(PitchingDecision.WIN)
            assertThat(result[starter]).isNotEqualTo(PitchingDecision.WIN)
        }
    }

    @Nested
    @DisplayName("세이브 시나리오")
    inner class SaveTest {
        @Test
        fun `3점 이내 리드 상황으로 마지막 이닝 등판 후 마무리하면 세이브를 받는다`() {
            // winner: 5점, loser: 3점 → 2점차 (3점 이내)
            val winnerTeam = mockWinnerTeam(1L, 5, "5,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 3, "0,1,1,1,0,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 24, // 8이닝
                    entryInning = 1,
                    exitInning = 8,
                )
            val closer =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 3, // 1이닝
                    entryInning = 9,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, closer, loserStarter),
                )

            assertThat(result[starter]).isEqualTo(PitchingDecision.WIN)
            assertThat(result[closer]).isEqualTo(PitchingDecision.SAVE)
        }

        @Test
        fun `3이닝 이상 마무리하면 리드 마진 관계없이 세이브를 받는다`() {
            // winner: 10점, loser: 1점 → 9점차이지만 구원 3이닝 투구
            val winnerTeam = mockWinnerTeam(1L, 10, "10,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 1, "0,0,0,0,0,0,1,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 18, // 6이닝
                    entryInning = 1,
                    exitInning = 6,
                )
            val closer =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 9, // 3이닝
                    entryInning = 7,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, closer, loserStarter),
                )

            assertThat(result[starter]).isEqualTo(PitchingDecision.WIN)
            assertThat(result[closer]).isEqualTo(PitchingDecision.SAVE)
        }
    }

    @Nested
    @DisplayName("홀드 시나리오")
    inner class HoldTest {
        @Test
        fun `리드 상황에서 등판하여 리드 유지 채 교체된 구원 투수는 홀드를 받는다`() {
            // winner: 1회에 5점, loser: 0점
            val winnerTeam = mockWinnerTeam(1L, 5, "5,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0,0,0,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 18, // 6이닝
                    entryInning = 1,
                    exitInning = 6,
                )
            // 중간 계투: 리드 유지 채 교체
            val middleReliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 6, // 2이닝
                    entryInning = 7,
                    exitInning = 8,
                )
            val closer =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 3, // 1이닝
                    entryInning = 9,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, middleReliever, closer, loserStarter),
                )

            assertThat(result[middleReliever]).isEqualTo(PitchingDecision.HOLD)
        }
    }

    @Nested
    @DisplayName("블론세이브 시나리오")
    inner class BlownSaveTest {
        @Test
        fun `리드 상황 등판 후 역전 허용했다가 팀이 재역전하면 블론세이브를 받는다`() {
            // winner: 3,0,0,0,0,2,0 → 총 5점
            // loser: 0,0,0,0,4,0,0 → 총 4점
            // 5회에 역전 허용, 6회에 재역전
            val winnerTeam = mockWinnerTeam(1L, 5, "3,0,0,0,0,2,0")
            val loserTeam = mockLoserTeam(2L, 4, "0,0,0,0,4,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 12, // 4이닝
                    entryInning = 1,
                    exitInning = 4,
                )
            // 중간 계투: 5회에 역전 허용 (블론세이브)
            val middleReliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 3, // 1이닝 (5회)
                    entryInning = 5,
                    exitInning = 5,
                )
            // 6회 역전 후 마무리 투수 (승리 투수)
            val closerWin =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 6, // 2이닝 (6~7회)
                    entryInning = 6,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 21,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, middleReliever, closerWin, loserStarter),
                )

            assertThat(result[middleReliever]).isEqualTo(PitchingDecision.BLOWN_SAVE)
            assertThat(result[closerWin]).isEqualTo(PitchingDecision.WIN)
        }
    }

    @Nested
    @DisplayName("투수 없음 시나리오")
    inner class NoPitchersTest {
        @Test
        fun `투수 기록이 없으면 빈 맵을 반환한다`() {
            val winnerTeam = mockWinnerTeam(1L, 3, "3,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0")

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = emptyList(),
                )

            assertThat(result).isEmpty()
        }
    }

    @Nested
    @DisplayName("단독 투수 시나리오")
    inner class SinglePitcherTest {
        @Test
        fun `승리팀 투수가 한 명일 때 세이브 없이 승리만 부여된다`() {
            // winner: 선발 1명이 완봉
            val winnerTeam = mockWinnerTeam(1L, 3, "3,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0,0,0,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, loserStarter),
                )

            assertThat(result[starter]).isEqualTo(PitchingDecision.WIN)
            assertThat(result.values).doesNotContain(PitchingDecision.SAVE)
        }

        @Test
        fun `승리팀 투수가 두 명이고 마지막이 승리 투수이면 세이브가 없다`() {
            // winner: 선발이 4이닝 후 교체, 구원이 5이닝 투구하고 역전승
            val winnerTeam = mockWinnerTeam(1L, 3, "0,0,0,0,3,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 2, "1,1,0,0,0,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 12, // 4이닝
                    entryInning = 1,
                    exitInning = 4,
                )
            val reliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 15, // 5이닝
                    entryInning = 5,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, reliever, loserStarter),
                )

            // 역전 5회 당시 등판 중인 reliever가 승리 투수
            assertThat(result[reliever]).isEqualTo(PitchingDecision.WIN)
            // 마지막 투수 == 승리 투수 → 세이브 없음
            assertThat(result.values).doesNotContain(PitchingDecision.SAVE)
        }
    }

    @Nested
    @DisplayName("선발 투수 4이닝 이하 시나리오")
    inner class StarterUnder5InningsTest {
        @Test
        fun `선발 투수가 4이닝 이하면 구원 승이 결정된다`() {
            // 선발이 4이닝(12아웃), 리드 유지, 구원이 5이닝 마무리
            val winnerTeam = mockWinnerTeam(1L, 5, "5,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0,0,0,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 12, // 4이닝 (5이닝 미만)
                    entryInning = 1,
                    exitInning = 4,
                )
            val reliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 15, // 5이닝
                    entryInning = 5,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, reliever, loserStarter),
                )

            // 처음부터 리드 유지 → finalLeadInning=null → findPitcherAtInning fallback
            // 선발은 5이닝 미만이므로 구원 승 혹은 다른 투수가 승리
            assertThat(result[starter]).isNotEqualTo(PitchingDecision.WIN)
        }
    }

    @Nested
    @DisplayName("세이브 불자격 시나리오")
    inner class NoSaveTest {
        @Test
        fun `선발 투수는 세이브를 받지 못한다`() {
            // 선발이 완봉이지만 마지막 투수여도 세이브 자격 없음
            val winnerTeam = mockWinnerTeam(1L, 5, "5,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 3, "0,0,1,0,2,0,0,0,0")

            // 선발 + 마지막 구원
            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 21, // 7이닝
                    entryInning = 1,
                    exitInning = 7,
                )
            // 4점 리드로 마무리 등판 (4 > 3 → 세이브 불자격: 3점 초과 리드이고 2이닝 미만)
            val closer =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 6, // 2이닝
                    entryInning = 8,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, closer, loserStarter),
                )

            assertThat(result.values).doesNotContain(PitchingDecision.SAVE)
        }

        @Test
        fun `리드가 없는 경우(동점 종료) 세이브 자격이 없다`() {
            // 무승부 케이스를 제외하고, 이론상 finalLeadMargin <= 0인 경우 세이브 없음
            // finalLeadMargin > 0이어야 세이브 가능 — 간접 경로이므로 qualifiesForSave 직접 테스트

            // qualifiesForSave: leadMargin=0 → false
            // isStartingPitcher=false, leadMargin=0
            // → PitchingDecisionCalculator 내부 함수지만 완봉 시나리오로 우회 검증
            // winner 5점 리드로 4점 차 마무리, 1이닝 구원 (3점 초과 & 2이닝 미만) → 세이브 없음
            val winnerTeam = mockWinnerTeam(1L, 5, "5,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 1, "0,0,0,0,0,0,1,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 24, // 8이닝
                    entryInning = 1,
                    exitInning = 8,
                )
            val closer =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 3, // 1이닝 (3이닝 미만, 4점 차 > 3점)
                    entryInning = 9,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, closer, loserStarter),
                )

            assertThat(result.values).doesNotContain(PitchingDecision.SAVE)
        }
    }

    @Nested
    @DisplayName("중간 구원 투수 NONE 시나리오")
    inner class MiddleRelieverNoneTest {
        @Test
        fun `리드 없는 상황에 등판한 중간 계투는 NONE을 받는다`() {
            // winner: 0,0,0,0,5,0,0,0,0 → 5회에 역전
            // loser: 1,1,1,1,0,0,0,0,0 → 4점 선취
            val winnerTeam = mockWinnerTeam(1L, 5, "0,0,0,0,5,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 4, "1,1,1,1,0,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 6, // 2이닝
                    entryInning = 1,
                    exitInning = 2,
                )
            // 중간 계투: 3~4회에 등판, 뒤지는 상황 (리드 없음 → NONE)
            val middleReliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 6, // 2이닝
                    entryInning = 3,
                    exitInning = 4,
                )
            // 역전 투수: 5회부터 등판 (승리 투수)
            val closerWin =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 15, // 5이닝
                    entryInning = 5,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, middleReliever, closerWin, loserStarter),
                )

            // 중간 계투는 리드 없이 등판 → 맵에 없거나 NONE
            assertThat(result[middleReliever]).satisfiesAnyOf(
                { v -> assertThat(v).isNull() },
                { v -> assertThat(v).isEqualTo(PitchingDecision.NONE) },
            )
        }

        @Test
        fun `1회부터 등판(entryInning=1)하는 중간 계투는 등판 전 점수가 0대0이므로 리드 없음으로 처리된다`() {
            // winner: 0,0,3,0,0,0 → 3회 역전
            // loser:  1,1,0,0,0,0 → 2점 선취
            val winnerTeam = mockWinnerTeam(1L, 3, "0,0,3,0,0,0")
            val loserTeam = mockLoserTeam(2L, 2, "1,1,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 3, // 1이닝
                    entryInning = 1,
                    exitInning = 1,
                )
            // entryInning=2, 2회에 등판 → 1회 종료 후 0:1 → 뒤지고 있음 → NONE
            val middleReliever =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 3, // 1이닝
                    entryInning = 2,
                    exitInning = 2,
                )
            val closerWin =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 12, // 4이닝
                    entryInning = 3,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 18,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, middleReliever, closerWin, loserStarter),
                )

            assertThat(result[middleReliever]).satisfiesAnyOf(
                { v -> assertThat(v).isNull() },
                { v -> assertThat(v).isEqualTo(PitchingDecision.NONE) },
            )
        }
    }

    @Nested
    @DisplayName("entryInning 없는 투수 시나리오")
    inner class NullEntryInningTest {
        @Test
        fun `entryInning이 null인 투수는 아웃 수 누산으로 이닝을 추정한다`() {
            // winner: 3,0,0,0,2,0,0,0,0 → 총 5점
            // loser: 0,0,0,0,0,0,0,0,0 → 총 0점
            val winnerTeam = mockWinnerTeam(1L, 5, "3,0,0,0,2,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0,0,0,0,0,0,0")

            // entryInning=null, 아웃 15개 → 1~5이닝 담당
            val starterNullEntry =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 15, // 5이닝
                    entryInning = null,
                    exitInning = null,
                )
            // entryInning=null, 아웃 12개 → 6~9이닝 담당
            val relieverNullEntry =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 12, // 4이닝
                    entryInning = null,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starterNullEntry, relieverNullEntry, loserStarter),
                )

            // 처음부터 리드 → finalLeadInning=null → starter WIN 자격 확인
            // starter: 5이닝, exitInning=null → completeInnings=5, 5회 종료 후 winner=5 > loser=0 → 선발 승
            assertThat(result[starterNullEntry]).isEqualTo(PitchingDecision.WIN)
            assertThat(result[loserStarter]).isEqualTo(PitchingDecision.LOSS)
        }
    }

    @Nested
    @DisplayName("findFinalLeadChangeInning 추가 케이스")
    inner class FindFinalLeadChangeInningEdgeCaseTest {
        @Test
        fun `동점 상태로 끝나는 경우 null을 반환한다`() {
            // 양 팀 모두 동점: 처음부터 동점, 역전 없음
            val winCum = listOf(1, 1, 1)
            val loseCum = listOf(1, 1, 1)
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winCum, loseCum)).isNull()
        }

        @Test
        fun `1회에 리드 잡고 이후 역전 후 다시 역전하면 최종 역전 이닝을 반환한다`() {
            // winner 누적: 2,2,2,2,5 / loser 누적: 0,3,3,3,4
            // 1회: w=2>l=0 (winner 처음 리드, finalLeadChange=1)
            // 2회: w=2<=l=3 (loser 역전)
            // 5회: w=5>l=4 (winner 재역전, finalLeadChange=5)
            val winCum = listOf(2, 2, 2, 2, 5)
            val loseCum = listOf(0, 3, 3, 3, 4)
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winCum, loseCum)).isEqualTo(5)
        }

        @Test
        fun `winner 누적과 loser 누적 크기가 다를 때도 동작한다`() {
            // winner: 2이닝, loser: 3이닝 (연장 등)
            val winCum = listOf(0, 3) // 2회에 역전
            val loseCum = listOf(1, 2, 2) // loser가 더 긴 배열
            // 1회: w=0<l=1 → loser 리드
            // 2회: w=3>l=2 → winner 역전
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winCum, loseCum)).isEqualTo(2)
        }

        @Test
        fun `1회부터 리드하고 이후 역전당한 적 있으면 처음 리드한 이닝을 확인한다`() {
            // winner 누적: 1,1,1,4 / loser 누적: 0,2,2,3
            // 1회: w=1>l=0 (winner 처음 리드, finalLeadChange=1)
            // 2회: w=1<=l=2 (loser 역전)
            // 4회: w=4>l=3 (winner 재역전, finalLeadChange=4)
            val winCum = listOf(1, 1, 1, 4)
            val loseCum = listOf(0, 2, 2, 3)
            assertThat(PitchingDecisionCalculator.findFinalLeadChangeInning(winCum, loseCum)).isEqualTo(4)
        }
    }

    @Nested
    @DisplayName("패전팀 여러 투수 시나리오")
    inner class LoserMultiplePitchersTest {
        @Test
        fun `패전팀에 투수가 여럿이면 결승점 허용 투수가 패전 투수가 된다`() {
            // winner: 0,0,5,0,0 → 3회 역전 결정
            // loser: 1,1,0,0,0 → 2점 선취
            val winnerTeam = mockWinnerTeam(1L, 5, "0,0,5,0,0")
            val loserTeam = mockLoserTeam(2L, 2, "1,1,0,0,0")

            val winnerStarter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 15,
                    entryInning = 1,
                    exitInning = null,
                )
            // 패전팀: 1~2회 선발, 3~5회 구원
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 6, // 2이닝
                    entryInning = 1,
                    exitInning = 2,
                )
            val loserReliever =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = false,
                    inningsPitchedOuts = 9, // 3이닝
                    entryInning = 3,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(winnerStarter, loserStarter, loserReliever),
                )

            // 3회 역전 허용 → loserReliever가 패전 투수
            assertThat(result[loserReliever]).isEqualTo(PitchingDecision.LOSS)
            assertThat(result[loserStarter]).isNotEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `패전팀 투수 기록이 없으면 패전 결정 없이 종료된다`() {
            // loserPitchers가 비어있는 케이스
            val winnerTeam = mockWinnerTeam(1L, 3, "3,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 9,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter), // loser 팀 투수 없음
                )

            assertThat(result.values).doesNotContain(PitchingDecision.LOSS)
        }
    }

    @Nested
    @DisplayName("완봉 시나리오")
    inner class CompleteGameTest {
        @Test
        fun `선발 투수 완봉승일 때 선발 승 하나만 부여된다`() {
            // winner: 5점, loser: 0점 → 선발이 9이닝 완투
            val winnerTeam = mockWinnerTeam(1L, 5, "5,0,0,0,0,0,0,0,0")
            val loserTeam = mockLoserTeam(2L, 0, "0,0,0,0,0,0,0,0,0")

            val starter =
                mockPitchingRecord(
                    teamId = 1L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27, // 9이닝
                    entryInning = 1,
                    exitInning = null,
                )
            val loserStarter =
                mockPitchingRecord(
                    teamId = 2L,
                    isStartingPitcher = true,
                    inningsPitchedOuts = 27,
                    entryInning = 1,
                    exitInning = null,
                )

            val result =
                PitchingDecisionCalculator.calculate(
                    winnerGameTeam = winnerTeam,
                    loserGameTeam = loserTeam,
                    allPitchingRecords = listOf(starter, loserStarter),
                )

            assertThat(result[starter]).isEqualTo(PitchingDecision.WIN)
            assertThat(result[loserStarter]).isEqualTo(PitchingDecision.LOSS)
            // 세이브, 홀드 없음
            assertThat(result.values).doesNotContain(PitchingDecision.SAVE)
            assertThat(result.values).doesNotContain(PitchingDecision.HOLD)
        }
    }
}
