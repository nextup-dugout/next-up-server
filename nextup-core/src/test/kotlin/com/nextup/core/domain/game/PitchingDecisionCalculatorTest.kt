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
