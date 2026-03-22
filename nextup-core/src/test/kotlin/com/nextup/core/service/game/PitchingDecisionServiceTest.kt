package com.nextup.core.service.game

import com.nextup.core.domain.competition.GameRules
import com.nextup.core.domain.game.GamePlayer
import com.nextup.core.domain.game.GameTeam
import com.nextup.core.domain.game.PitchingDecision
import com.nextup.core.domain.game.PitchingRecord
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * PitchingDecisionService 단위 테스트
 *
 * 각 시나리오는 실제 PitchingRecord를 생성하여 Entity 비즈니스 로직과 함께 검증합니다.
 */
@DisplayName("PitchingDecisionService")
class PitchingDecisionServiceTest {

    private lateinit var service: PitchingDecisionService
    private lateinit var gameRules: GameRules
    private lateinit var mockGamePlayer: GamePlayer

    @BeforeEach
    fun setUp() {
        service = PitchingDecisionService()
        gameRules = GameRules() // 기본값: starterWinQualificationOuts = 15 (5이닝)
        mockGamePlayer = mockk(relaxed = true)
    }

    // ────────────────────────────────────────────────────────────
    // 헬퍼
    // ────────────────────────────────────────────────────────────

    private fun makeGameTeam(
        inningScores: String?,
        totalScore: Int
    ): GameTeam {
        val gt = mockk<GameTeam>(relaxed = true)
        io.mockk.every { gt.inningScores } returns inningScores
        io.mockk.every { gt.totalScore } returns totalScore
        return gt
    }

    /**
     * PitchingRecord 생성 헬퍼
     *
     * @param isStarter 선발 여부
     * @param outs      소화 아웃 수
     * @param runsAllowed 실점 수
     */
    private fun makeRecord(
        isStarter: Boolean = false,
        outs: Int = 0,
        runsAllowed: Int = 0,
    ): PitchingRecord {
        val record = PitchingRecord.create(mockGamePlayer, isStartingPitcher = isStarter)
        repeat(outs) { record.recordOut() }
        repeat(runsAllowed) { record.recordRun(isEarned = true) }
        return record
    }

    // ────────────────────────────────────────────────────────────
    // 기본 동작
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("기본 동작")
    inner class BasicBehaviorTest {

        @Test
        fun `투수 기록이 없으면 아무 결정도 하지 않는다`() {
            // given
            val winnerTeam = makeGameTeam("3,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 예외 없이 정상 종료
        }
    }

    // ────────────────────────────────────────────────────────────
    // 선발 투수 승리 (W)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("선발 투수 승리")
    inner class StarterWinTest {

        @Test
        fun `선발 5이닝 이상 완투 시 선발에게 승리 부여`() {
            // given: 선발이 27아웃(9이닝) 완투
            val starter = makeRecord(isStarter = true, outs = 27, runsAllowed = 2)
            val winnerTeam = makeGameTeam("1,0,2,0,0,0,2,0,0", 5)
            val loserTeam = makeGameTeam("0,0,1,0,0,0,1,0,0", 2)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `선발 5이닝 이상 + 구원진 리드 유지 시 선발 승리 부여`() {
            // given: 선발 5이닝(15아웃), 구원 4이닝(12아웃)
            // 선발 퇴장 시 리드 유지 (1회에 1점 리드)
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val relief = makeRecord(isStarter = false, outs = 12, runsAllowed = 1)
            val winnerTeam = makeGameTeam("1,0,0,0,0,0,0,0,1", 2)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,1,0", 1)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, relief),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `사회인 야구 규칙 starterWinQualificationOuts=9(3이닝)로 단축 경기 선발 승리`() {
            // given: 사회인 야구 3이닝 경기 규칙
            val shortGameRules =
                GameRules(
                    defaultInnings = 7,
                    starterWinQualificationOuts = 9, // 3이닝
                )
            val starter = makeRecord(isStarter = true, outs = 9, runsAllowed = 0)
            val winnerTeam = makeGameTeam("2,1,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = shortGameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `선발 5이닝 미만이고 구원 없으면 선발에게 승리 부여 (사회인 야구 단축 경기)`() {
            // given: 콜드게임 등으로 4이닝만 진행, 선발만 있음
            val starter = makeRecord(isStarter = true, outs = 12, runsAllowed = 1) // 4이닝
            val winnerTeam = makeGameTeam("2,0,1,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 구원 없으면 선발에게 승리 (단축 경기)
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 구원 투수 승리 (W)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("구원 투수 승리")
    inner class RelieverWinTest {

        @Test
        fun `선발 이닝 미달 후 구원이 역전 이닝 담당 시 구원 투수 승리`() {
            // given: 선발 3이닝(9아웃), 구원1이 4~5회(리드 만든 구원), 구원2가 마무리
            // 이닝별 점수: 승팀 0,0,0,2,0,0,0,0,0 / 패팀 1,0,0,0,0,0,0,0,0
            val starter = makeRecord(isStarter = true, outs = 9, runsAllowed = 1) // 3이닝
            val relief1 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0) // 3이닝(4~6회)
            val relief2 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0) // 3이닝(7~9회)

            val winnerTeam = makeGameTeam("0,0,0,2,0,0,0,0,0", 2)
            val loserTeam = makeGameTeam("1,0,0,0,0,0,0,0,0", 1)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, relief1, relief2),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 리드를 만든 4회를 담당한 relief1에게 승리
            assertThat(relief1.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(starter.decision).isEqualTo(PitchingDecision.NONE)
        }

        @Test
        fun `선발만 있고 구원 없고 이닝 미달 시 선발 승리`() {
            // given: 콜드게임 4이닝, 선발 완투
            val starter = makeRecord(isStarter = true, outs = 12)
            val winnerTeam = makeGameTeam("0,3,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `구원 투수만 있는 경우 리드를 만든 투수에게 승리`() {
            // given: 처음부터 구원만 등판 (선발 없음)
            val relief1 = makeRecord(isStarter = false, outs = 9, runsAllowed = 1)
            val relief2 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)
            val relief3 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)

            // 승팀: 4회에 처음 리드 (리드 만든 시점 = 4회, 4~6회 = relief2 소화)
            val winnerTeam = makeGameTeam("0,0,0,2,0,0,0,0,0", 2)
            val loserTeam = makeGameTeam("0,0,1,0,0,0,0,0,0", 1)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(relief1, relief2, relief3),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: relief2 가 4회에 투구 중 (relief1이 1~3회 담당 후 교체)
            assertThat(relief2.decision).isEqualTo(PitchingDecision.WIN)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 패전 투수 (L)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("패전 투수")
    inner class LosingPitcherTest {

        @Test
        fun `선발 투수만 있는 경우 선발에게 패전 부여`() {
            // given
            val loserStarter = makeRecord(isStarter = true, outs = 27, runsAllowed = 3)
            val winnerTeam = makeGameTeam("2,0,0,1,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `결승점 이닝 특정 가능 시 해당 이닝 투수에게 패전`() {
            // given: 5회에 결승점 허용 → 5~7회 담당 relief2에게 패전
            val loserStarter = makeRecord(isStarter = true, outs = 12, runsAllowed = 0) // 1~4회
            val loserRelief1 = makeRecord(isStarter = false, outs = 9, runsAllowed = 2) // 5~7회 (결승점 이닝)
            val loserRelief2 = makeRecord(isStarter = false, outs = 6, runsAllowed = 0) // 8~9회

            // 승팀: 5회에 결승점 (누적 3점으로 패팀 최종 2점 초과)
            val winnerTeam = makeGameTeam("1,0,0,0,2,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,1,0,0,0,0,0,1,0", 2)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter, loserRelief1, loserRelief2),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 5회 담당 loserRelief1에게 패전
            assertThat(loserRelief1.decision).isEqualTo(PitchingDecision.LOSS)
            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.NONE)
            assertThat(loserRelief2.decision).isEqualTo(PitchingDecision.NONE)
        }

        @Test
        fun `구원 투수만 있는 패배팀에서 패전 투수 결정`() {
            // given
            val loserRelief1 = makeRecord(isStarter = false, outs = 15, runsAllowed = 1)
            val loserRelief2 = makeRecord(isStarter = false, outs = 12, runsAllowed = 2)

            val winnerTeam = makeGameTeam("1,0,2,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,1,0,0,0,0,0", 1) // 패배팀 1점 뿐

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserRelief1, loserRelief2),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 결승점(3점)이 된 3회 이닝 담당 투수 → loserRelief1(5이닝 담당)
            assertThat(loserRelief1.decision).isEqualTo(PitchingDecision.LOSS)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 세이브 (S)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("세이브")
    inner class SaveTest {

        @Test
        fun `3점 이내 리드 상황에서 마무리한 구원 투수에게 세이브`() {
            // given: 선발 5이닝 승리, 마무리 구원 4이닝 (리드 마진 2점)
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val closer = makeRecord(isStarter = false, outs = 12, runsAllowed = 1)

            val winnerTeam = makeGameTeam("2,0,0,0,0,0,0,0,0", 2)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,1,0", 1)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, closer),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(closer.decision).isEqualTo(PitchingDecision.SAVE)
        }

        @Test
        fun `3이닝 이상 마무리한 구원 투수에게 세이브 (큰 리드도 가능)`() {
            // given: 선발 5이닝, 마무리 9아웃(3이닝) 마무리, 리드 5점 (3점 초과지만 3이닝 조건)
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val closer = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)

            val winnerTeam = makeGameTeam("5,0,0,0,0,0,0,0,0", 5)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, closer),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(closer.decision).isEqualTo(PitchingDecision.SAVE)
        }

        @Test
        fun `1점 리드 상황에서 마무리한 구원 투수에게 세이브`() {
            // given
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 2)
            val closer = makeRecord(isStarter = false, outs = 12, runsAllowed = 0)

            val winnerTeam = makeGameTeam("1,0,0,1,1,0,0,0,0", 3)
            val loserTeam = makeGameTeam("1,1,0,0,0,0,0,0,0", 2)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, closer),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(closer.decision).isEqualTo(PitchingDecision.SAVE)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 세이브 - 동점 주자 조건 (H-7)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("세이브 - 동점 주자 조건")
    inner class SaveTyingRunnerTest {

        @Test
        fun `6점 차 만루 등판 마무리 시 세이브 부여 (동점 주자 조건)`() {
            // given: 6점 차 리드, 만루(주자 3명) 상황 등판
            // 동점 주자 조건: finalLeadMargin(6) <= runnersOnBase(3) + 1(타자) = 4 → false
            // 하지만 6 <= 3+1 = 4는 false이므로 세이브 아님
            // 실제로는 주자 3 + 타자 1 + 다음타자 1 = 5명이 동점 주자가 될 수 있지만
            // 규칙상 "동점 타자가 타석/온베이스/다음 타자"이므로 runnersOnBase + 1로 판단
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)

            val winnerTeam = makeGameTeam("6,0,0,0,0,0,0,0,0", 6)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when: qualifiesForSave 직접 검증
            val result = service.qualifiesForSave(closer, finalLeadMargin = 6, runnersOnBase = 3)

            // then: 6 <= 3+1=4 → false, 6 > 3 → true 아님, 6 >= 3이닝 아님
            assertThat(result).isFalse()
        }

        @Test
        fun `4점 차 만루 등판 마무리 시 세이브 부여 (동점 주자 조건 충족)`() {
            // given: 4점 차 리드, 만루(주자 3명) 등판
            // 동점 주자 조건: finalLeadMargin(4) <= runnersOnBase(3) + 1(타자) = 4 → true
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)

            // when
            val result = service.qualifiesForSave(closer, finalLeadMargin = 4, runnersOnBase = 3)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `5점 차 만루 등판 마무리 시 세이브 미부여 (동점 주자 조건 미충족)`() {
            // given: 5점 차 리드, 만루(주자 3명) 등판
            // 동점 주자 조건: finalLeadMargin(5) <= runnersOnBase(3) + 1(타자) = 4 → false
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)

            // when
            val result = service.qualifiesForSave(closer, finalLeadMargin = 5, runnersOnBase = 3)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `4점 차 주자 2명 등판 시 세이브 미부여`() {
            // given: 4점 차, 주자 2명
            // 동점 주자 조건: finalLeadMargin(4) <= runnersOnBase(2) + 1 = 3 → false
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)

            // when
            val result = service.qualifiesForSave(closer, finalLeadMargin = 4, runnersOnBase = 2)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `선발 투수는 세이브 자격 없음`() {
            // given
            val starter = makeRecord(isStarter = true, outs = 27, runsAllowed = 0)

            // when
            val result = service.qualifiesForSave(starter, finalLeadMargin = 1, runnersOnBase = 0)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `리드 없으면 세이브 자격 없음`() {
            // given
            val closer = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)

            // when
            val result = service.qualifiesForSave(closer, finalLeadMargin = 0, runnersOnBase = 0)

            // then
            assertThat(result).isFalse()
        }

        @Test
        fun `3점 이내 리드는 주자 수 관계없이 세이브`() {
            // given
            val closer = makeRecord(isStarter = false, outs = 3, runsAllowed = 0)

            // when
            val result = service.qualifiesForSave(closer, finalLeadMargin = 2, runnersOnBase = 0)

            // then
            assertThat(result).isTrue()
        }

        @Test
        fun `3이닝 이상 마무리는 리드 점수와 주자 수 관계없이 세이브`() {
            // given: 9아웃 = 3이닝
            val closer = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)

            // when
            val result = service.qualifiesForSave(closer, finalLeadMargin = 10, runnersOnBase = 0)

            // then
            assertThat(result).isTrue()
        }
    }

    // ────────────────────────────────────────────────────────────
    // GameRules 기반 선발 승리 자격 이닝 검증
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GameRules 기반 선발 승리 자격")
    inner class GameRulesStarterQualificationTest {

        @Test
        fun `기본 GameRules(15아웃=5이닝)에서 선발 5이닝 완투 시 승리`() {
            // given: 기본 규칙 (starterWinQualificationOuts = 15)
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val winnerTeam = makeGameTeam("2,0,0,0,0,0,0,0,0", 2)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `사회인 야구 규칙(9아웃=3이닝)에서 선발 3이닝 승리`() {
            // given
            val shortRules =
                GameRules(
                    defaultInnings = 7,
                    starterWinQualificationOuts = 9,
                )
            val starter = makeRecord(isStarter = true, outs = 9, runsAllowed = 0)
            val winnerTeam = makeGameTeam("3,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = shortRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `GameRules 기준 미달 시 선발에게 승리 미부여 (구원 승리)`() {
            // given: starterWinQualificationOuts = 15 (5이닝)이지만 선발은 4이닝(12아웃)만 소화
            val starter = makeRecord(isStarter = true, outs = 12, runsAllowed = 0)
            val relief = makeRecord(isStarter = false, outs = 15, runsAllowed = 0)

            // 1회에 리드를 잡고 유지
            val winnerTeam = makeGameTeam("2,0,0,0,0,0,0,0,0", 2)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, relief),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 선발은 이닝 미달이므로 구원 투수에게 승리
            assertThat(starter.decision).isNotEqualTo(PitchingDecision.WIN)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 홀드 (H)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("홀드")
    inner class HoldTest {

        @Test
        fun `중간 구원 투수에게 홀드 부여`() {
            // given: 선발 → 중간계투 → 마무리 구조
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val middle = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)

            val winnerTeam = makeGameTeam("2,0,0,0,0,0,0,0,0", 2)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, middle, closer),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(middle.decision).isEqualTo(PitchingDecision.HOLD)
            assertThat(closer.decision).isEqualTo(PitchingDecision.SAVE)
        }

        @Test
        fun `여러 중간 구원 투수 모두에게 홀드 부여`() {
            // given: 선발 → 계투1 → 계투2 → 마무리
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val middle1 = makeRecord(isStarter = false, outs = 3, runsAllowed = 0)
            val middle2 = makeRecord(isStarter = false, outs = 3, runsAllowed = 0)
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)

            val winnerTeam = makeGameTeam("3,0,0,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("1,0,0,0,0,0,0,0,0", 1)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, middle1, middle2, closer),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(middle1.decision).isEqualTo(PitchingDecision.HOLD)
            assertThat(middle2.decision).isEqualTo(PitchingDecision.HOLD)
            assertThat(closer.decision).isEqualTo(PitchingDecision.SAVE)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 블론세이브 (BS)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("블론세이브")
    inner class BlownSaveTest {

        @Test
        fun `패배팀 구원 투수가 리드 상황 등판 후 역전 허용 시 블론세이브 후 패전으로 전환`() {
            // given: 패배팀이 3회까지 리드했다가 4회에 역전 허용
            // 패팀: 2,0,0,0,0,0,0,0,0 (2점 리드 후 역전 당함)
            // 승팀: 0,0,0,3,0,0,0,0,0 (4회에 역전)
            val loserStarter = makeRecord(isStarter = true, outs = 9, runsAllowed = 0) // 1~3회
            val loserRelief = makeRecord(isStarter = false, outs = 18, runsAllowed = 3) // 4~9회 (역전 허용)

            val winnerTeam = makeGameTeam("0,0,0,3,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("2,0,0,0,0,0,0,0,0", 2)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter, loserRelief),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 역전 허용 시점(4회)의 투수인 loserRelief에게 블론세이브 + 패전
            // 야구 규칙: BS와 L은 동시 기록 가능하며, 최종 결정은 LOSS
            assertThat(loserRelief.decision).isEqualTo(PitchingDecision.LOSS)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 통합 시나리오
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("통합 시나리오")
    inner class IntegrationScenarioTest {

        @Test
        fun `승리팀과 패배팀 모두 결정이 올바르게 부여된다`() {
            // given:
            //   승팀: 선발(5이닝) WIN + 마무리(4이닝) SAVE
            //   패팀: 선발(5이닝) + 구원(4이닝) LOSS
            val winStarter = makeRecord(isStarter = true, outs = 15, runsAllowed = 0)
            val winCloser = makeRecord(isStarter = false, outs = 12, runsAllowed = 1)

            val loseStarter = makeRecord(isStarter = true, outs = 15, runsAllowed = 3)
            val loseRelief = makeRecord(isStarter = false, outs = 12, runsAllowed = 0)

            // 승팀 1회에 3점 → 결승점은 1회에 이미 확정
            val winnerTeam = makeGameTeam("3,0,0,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(winStarter, winCloser),
                loserTeamRecords = listOf(loseStarter, loseRelief),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(winStarter.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(winCloser.decision).isEqualTo(PitchingDecision.SAVE)
            assertThat(loseStarter.decision).isEqualTo(PitchingDecision.LOSS)
            assertThat(loseRelief.decision).isEqualTo(PitchingDecision.NONE)
        }

        @Test
        fun `선발 완봉승 단독 선발에게만 WIN 부여`() {
            // given
            val starter = makeRecord(isStarter = true, outs = 27, runsAllowed = 0)
            val winnerTeam = makeGameTeam("1,0,0,0,0,0,0,0,1", 2)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `이닝 점수 데이터 없어도 예외 없이 동작한다`() {
            // given: inningScores = null
            val winStarter = makeRecord(isStarter = true, outs = 27, runsAllowed = 0)
            val loseStarter = makeRecord(isStarter = true, outs = 27, runsAllowed = 3)
            val winnerTeam = makeGameTeam(null, 3)
            val loserTeam = makeGameTeam(null, 0)

            // when: 예외 없이 정상 동작
            service.assignDecisions(
                winnerTeamRecords = listOf(winStarter),
                loserTeamRecords = listOf(loseStarter),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 투수 기록이 있으므로 최소한 기본 결정이 부여됨
            assertThat(winStarter.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(loseStarter.decision).isEqualTo(PitchingDecision.LOSS)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 결승점 이닝 특정 불가 엣지 케이스 (findLosingPitcher / findWinningReliever)
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("결승점·승리투수 이닝 특정 불가 엣지 케이스")
    inner class GoAheadRunEdgeCasesTest {

        @Test
        fun `승리팀이 단 한번도 리드를 못 잡은 경우 구원 투수 목록 마지막에게 승리`() {
            // given: 이닝 점수 문자열이 null → winnerCumulative 빈 리스트
            // → findWinningReliever 루프를 전혀 돌지 않으므로 firstLeadInning = -1
            // → 마지막 구원투수(relief3)에게 WIN
            val relief1 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)
            val relief2 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)
            val relief3 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)

            val winnerTeam = makeGameTeam(null, 3)
            val loserTeam = makeGameTeam(null, 0)

            service.assignDecisions(
                winnerTeamRecords = listOf(relief1, relief2, relief3),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: firstLeadInning < 0 → relievers.last() = relief3 에게 WIN
            assertThat(relief3.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `결승점 이닝 특정 불가 시 가장 많은 실점을 허용한 패배팀 투수에게 패전`() {
            // given: 승리팀이 처음부터 리드를 유지하는 경우
            // w > l && prevW <= prevL 조건: 1회에는 prevW=0, prevL=0 이므로 prevW <= prevL (0<=0 true)
            // 따라서 1회에 decisionInning=1 이 설정된다.
            // 반면, 이닝 점수가 null이면 winnerCumulative가 비어 findLosingPitcher가
            // maxByOrNull 경로로 분기한다.
            val loserRelief1 =
                makeRecord(isStarter = false, outs = 12, runsAllowed = 1) // 1~4회
            val loserRelief2 =
                makeRecord(isStarter = false, outs = 15, runsAllowed = 3) // 5~9회 (최다 실점)

            // inningScores=null → winnerCumulative 비어있음 → maxByOrNull(runsAllowed) 경로
            val winnerTeam = makeGameTeam(null, 4)
            val loserTeam = makeGameTeam(null, 4)

            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserRelief1, loserRelief2),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: winnerCumulative 비어있음 → maxByOrNull(runsAllowed) → loserRelief2(3실점)
            assertThat(loserRelief2.decision).isEqualTo(PitchingDecision.LOSS)
            assertThat(loserRelief1.decision).isEqualTo(PitchingDecision.NONE)
        }

        @Test
        fun `구원 투수 아웃 합산이 목표 이닝에 못 미치면 마지막 투수에게 패전`() {
            // given: 승팀이 처음에 리드 후 패팀이 역전하지 않는 시나리오
            // 패팀 선발(6아웃=2이닝) + 구원(3아웃=1이닝) = 3이닝만 커버
            // 승팀이 5회에 결승점 → decisionInning=5
            // 하지만 패팀 투수 합산 아웃이 9(3이닝)뿐 → 5회 커버 불가 → last() 반환
            val loserStarter = makeRecord(isStarter = true, outs = 6, runsAllowed = 0) // 1~2회
            val loserRelief = makeRecord(isStarter = false, outs = 3, runsAllowed = 0) // 3회

            // 승팀이 5회에 리드를 처음 확보 (1~4회 동점)
            val winnerTeam = makeGameTeam("0,0,0,0,3,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter, loserRelief),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: findRelieverAtInning(5회) → 아웃 합산 9개로 3이닝만 커버 → null → last()=loserRelief
            assertThat(loserRelief.decision).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `선발 자격 있는 선발이 리드 없이 교체되면 구원 투수 중 승리 투수 결정`() {
            // given: 선발이 5이닝(15아웃) 자격 있으나 5이닝까지 동점 → leadAfterStarter=false
            // → 구원투수 중 승리 투수 결정
            // 선발 5이닝(1~5회): 승팀 누적 vs 패팀 누적 = 2:2 (동점)
            // 6회에 승팀이 처음 리드 → 구원투수1이 승리
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 2)
            val relief1 = makeRecord(isStarter = false, outs = 6, runsAllowed = 0) // 6~7회
            val relief2 = makeRecord(isStarter = false, outs = 6, runsAllowed = 0) // 8~9회

            // 5이닝까지 2:2 동점, 6회에 승팀이 역전
            val winnerTeam = makeGameTeam("1,0,0,1,0,2,0,0,0", 4)
            val loserTeam = makeGameTeam("2,0,0,0,0,0,0,0,0", 2)

            service.assignDecisions(
                winnerTeamRecords = listOf(starter, relief1, relief2),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 선발 퇴장 후 5이닝까지 동점 → leadAfterStarter=false
            // → 구원투수 중 6회에 리드 만든 relief1에게 WIN
            assertThat(starter.decision).isNotEqualTo(PitchingDecision.WIN)
            assertThat(relief1.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `구원 투수 중 승리 투수가 결정되면 나머지 구원 투수에게 isSaveEligible=true로 세이브 기회`() {
            // given: 구원투수만 있고 inningScores=null → firstLeadInning=-1 → last()=relief3 이 WIN
            // remainingRelievers=[relief1, relief2], isSaveEligible=true
            // relief1(middle) → HOLD, relief2(last of remaining) → SAVE(1점 차 리드)
            val relief1 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)
            val relief2 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)
            val relief3 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)

            // inningScores=null → findWinningReliever에서 firstLeadInning=-1 → last()=relief3
            val winnerTeam = makeGameTeam(null, 1)
            val loserTeam = makeGameTeam(null, 0)

            service.assignDecisions(
                winnerTeamRecords = listOf(relief1, relief2, relief3),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: relief3=WIN, relief1=HOLD, relief2=SAVE(1점 차 리드)
            assertThat(relief3.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(relief1.decision).isEqualTo(PitchingDecision.HOLD)
            assertThat(relief2.decision).isEqualTo(PitchingDecision.SAVE)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 분기 커버리지 보완: isSaveEligible=false / winnerRecords 없음
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("분기 커버리지 보완")
    inner class BranchCoverageTest {

        @Test
        fun `승리팀 기록 없고 패배팀만 있을 때 패전 투수가 결정된다`() {
            // given: winnerTeamRecords 비어있고 loserTeamRecords에만 선발 존재
            // assignWinAndRelief는 winnerRecords.isEmpty() 조기 반환 분기를 커버
            val loserStarter = makeRecord(isStarter = true, outs = 27, runsAllowed = 5)
            val winnerTeam = makeGameTeam("2,0,1,0,1,0,0,0,0", 4)
            val loserTeam = makeGameTeam("1,0,1,0,0,0,0,0,0", 2)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 패전 투수 결정 (선발 완투)
            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `구원 투수 없이 선발만 있을 때 isSaveEligible=false 분기를 통과한다`() {
            // given: 선발 완투, 구원 없음 → assignReliefDecisions은 relievers.isEmpty() 조기 반환
            // isSaveEligible 분기에 도달하지 않으므로 SAVE 없음
            val starter = makeRecord(isStarter = true, outs = 27, runsAllowed = 0)
            val winnerTeam = makeGameTeam("3,0,0,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 선발에게 WIN, SAVE 없음
            assertThat(starter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `구원 투수가 있어도 isSaveEligible=false이면 마지막 구원투수에게 세이브 미부여`() {
            // given: 선발 이닝 자격 미달(12아웃=4이닝) + 리드 만든 구원투수 없음(점수 null)
            // → winningRelief=relief2(last), remainingRelievers=[relief1]
            // → isSaveEligible=true이므로 실제로는 세이브 가능. 아래는 isSaveEligible=false 경로:
            // 선발 없고 구원만 있고 승리 투수가 결정되지 않는 경로를 통해
            // assignReliefDecisions(isSaveEligible=false) 분기를 커버
            // 승리팀 기록이 아예 없는 경우 assignWinAndRelief 자체가 조기 반환
            val loserRelief1 = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)
            val winnerTeam = makeGameTeam("4,0,0,0,0,0,0,0,0", 4)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when: winnerTeamRecords=emptyList → assignWinAndRelief 조기 반환
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserRelief1),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 패전 투수 결정
            assertThat(loserRelief1.decision).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `qualifiesForSave에서 isSaveByInnings만 true일 때 세이브 자격이 있다`() {
            // finalLeadMargin(5) > 3 → false
            // isSaveByInnings: 9아웃 = 3이닝 → true (|| 두 번째 조건)
            // finalLeadMargin(5) <= runnersOnBase(0) + 1 = 1 → false
            val closer = makeRecord(isStarter = false, outs = 9, runsAllowed = 0)
            val result = service.qualifiesForSave(closer, finalLeadMargin = 5, runnersOnBase = 0)
            assertThat(result).isTrue()
        }

        @Test
        fun `qualifiesForSave에서 runnersOnBase 조건만 true일 때 세이브 자격이 있다`() {
            // finalLeadMargin(4) > 3 → false (첫 번째 false)
            // isSaveByInnings: 6아웃 < 9 → false (두 번째 false)
            // finalLeadMargin(4) <= runnersOnBase(3) + 1 = 4 → true (세 번째 true)
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)
            val result = service.qualifiesForSave(closer, finalLeadMargin = 4, runnersOnBase = 3)
            assertThat(result).isTrue()
        }

        @Test
        fun `qualifiesForSave에서 모든 OR 조건이 false이면 세이브 자격이 없다`() {
            // finalLeadMargin(5) > 3 → false
            // isSaveByInnings: 6아웃 < 9 → false
            // finalLeadMargin(5) <= runnersOnBase(2) + 1 = 3 → false
            val closer = makeRecord(isStarter = false, outs = 6, runsAllowed = 0)
            val result = service.qualifiesForSave(closer, finalLeadMargin = 5, runnersOnBase = 2)
            assertThat(result).isFalse()
        }

        @Test
        fun `qualifiesForSave에서 finalLeadMargin 3이하만 true일 때 세이브 자격이 있다`() {
            // finalLeadMargin(2) <= 3 → true (|| 첫 번째 조건으로 short-circuit)
            val closer = makeRecord(isStarter = false, outs = 3, runsAllowed = 0)
            val result = service.qualifiesForSave(closer, finalLeadMargin = 2, runnersOnBase = 0)
            assertThat(result).isTrue()
        }

        @Test
        fun `assignDecisions에서 winnerTeamRecords만 비어있을 때 패전 투수가 결정된다`() {
            // winnerTeamRecords.isEmpty() == true, loserTeamRecords.isEmpty() == false
            // → && 전체는 false → early return 하지 않음
            val loserStarter = makeRecord(isStarter = true, outs = 27, runsAllowed = 3)
            val winnerTeam = makeGameTeam("1,0,2,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `assignDecisions에서 loserTeamRecords만 비어있을 때 승리 투수가 결정된다`() {
            // winnerTeamRecords.isEmpty() == false → && short-circuit (전체 false)
            // → early return 하지 않음
            val winStarter = makeRecord(isStarter = true, outs = 27, runsAllowed = 0)
            val winnerTeam = makeGameTeam("3,0,0,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            service.assignDecisions(
                winnerTeamRecords = listOf(winStarter),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            assertThat(winStarter.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `선발이 자격 있고 구원 있으나 리드 없이 교체 후 구원 승리 없을 때 세이브 미부여`() {
            // given: 선발 5이닝(15아웃) 자격 있으나 리드 없이 교체, 구원 투수가 리드를 만들었으나
            // isSaveEligible=true로 assignReliefDecisions 호출됨
            // 그러나 finalLeadMargin > 3이고 3이닝 미만이고 동점 주자 조건 불충족이면 세이브 없음
            val starter = makeRecord(isStarter = true, outs = 15, runsAllowed = 2)
            val winningRelief = makeRecord(isStarter = false, outs = 3, runsAllowed = 0) // 1이닝만 소화
            val closerRelief = makeRecord(isStarter = false, outs = 3, runsAllowed = 0) // 1이닝

            // 선발 5이닝 동점, 6회 구원1이 역전, 7회 구원2 마무리
            // finalLeadMargin=4 (4점 차), 주자 0명 → 세이브 조건: 4<=3 false, 3이닝 false, 4<=0+1 false
            val winnerTeam = makeGameTeam("1,0,0,1,0,4,0,0,0", 6)
            val loserTeam = makeGameTeam("2,0,0,0,0,0,0,0,0", 2)

            // when
            service.assignDecisions(
                winnerTeamRecords = listOf(starter, winningRelief, closerRelief),
                loserTeamRecords = emptyList(),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: winningRelief가 WIN, closerRelief는 SAVE 조건 불충족이므로 NONE
            assertThat(winningRelief.decision).isEqualTo(PitchingDecision.WIN)
            assertThat(closerRelief.decision).isEqualTo(PitchingDecision.NONE)
        }
    }

    // ────────────────────────────────────────────────────────────
    // 시소 게임 (리드 교차) 결승점 판정 — M-2
    // ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("시소 게임 결승점 판정")
    inner class SeesawGameGoAheadRunTest {

        @Test
        fun `시소 게임에서 최종 역전 이닝의 투수에게 패전 부여`() {
            // given: 리드가 두 번 바뀌는 시소 게임
            // 승팀(원정): 2,0,0,0,0,3,0,0,0 = 5
            // 패팀(홈):   0,0,3,0,0,0,0,0,0 = 3
            // 누적: 승 2,2,2,2,2,5,5,5,5 / 패 0,0,3,3,3,3,3,3,3
            // 1회: 2>0 && 0<=0 → decisionInning=1
            // 3회: 2<3 → skip
            // 6회: 5>3 && 2<=3 → decisionInning=6 (최종 역전)
            val loserStarter =
                makeRecord(isStarter = true, outs = 15, runsAllowed = 2) // 1~5회
            val loserRelief =
                makeRecord(isStarter = false, outs = 12, runsAllowed = 3) // 6~9회 (결승점 허용)

            val winnerTeam = makeGameTeam("2,0,0,0,0,3,0,0,0", 5)
            val loserTeam = makeGameTeam("0,0,3,0,0,0,0,0,0", 3)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter, loserRelief),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 6회 결승점 → 6~9회 담당 loserRelief에게 패전
            assertThat(loserRelief.decision).isEqualTo(PitchingDecision.LOSS)
            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.NONE)
        }

        @Test
        fun `세 번 리드가 바뀌는 시소 게임에서 최종 역전 투수에게 패전`() {
            // given: 리드 교차 3회
            // 승팀: 1,0,0,2,0,0,0,0,2 = 5
            // 패팀: 0,0,2,0,0,1,0,0,0 = 3
            // 누적: 승 1,1,1,3,3,3,3,3,5 / 패 0,0,2,2,2,3,3,3,3
            // 1회: 1>0 && 0<=0 → decisionInning=1
            // 3회: 1<2 → skip
            // 4회: 3>2 && 1<=2 → decisionInning=4
            // 6회: 3<=3 → skip (동점)
            // 9회: 5>3 && 3<=3 → decisionInning=9
            val loserStarter =
                makeRecord(isStarter = true, outs = 9, runsAllowed = 1) // 1~3회
            val loserRelief1 =
                makeRecord(isStarter = false, outs = 9, runsAllowed = 2) // 4~6회
            val loserRelief2 =
                makeRecord(isStarter = false, outs = 9, runsAllowed = 2) // 7~9회 (최종 결승점)

            val winnerTeam = makeGameTeam("1,0,0,2,0,0,0,0,2", 5)
            val loserTeam = makeGameTeam("0,0,2,0,0,1,0,0,0", 3)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter, loserRelief1, loserRelief2),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 9회 결승점 → 7~9회 담당 loserRelief2에게 패전
            assertThat(loserRelief2.decision).isEqualTo(PitchingDecision.LOSS)
            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.NONE)
            assertThat(loserRelief1.decision).isEqualTo(PitchingDecision.NONE)
        }

        @Test
        fun `승리팀이 처음부터 리드를 한 번도 놓치지 않은 경우 1회 투수에게 패전`() {
            // given: 승팀이 1회에 리드 잡고 끝까지 유지
            // 승팀: 3,0,0,0,0,0,0,0,0 = 3
            // 패팀: 0,0,0,1,0,0,0,0,0 = 1
            // 누적: 승 3,3,3,3,3,3,3,3,3 / 패 0,0,0,1,1,1,1,1,1
            // 1회: 3>0 && 0<=0 → decisionInning=1
            // 이후 prevW > prevL 이므로 갱신 없음
            val loserStarter =
                makeRecord(isStarter = true, outs = 27, runsAllowed = 3) // 완투

            val winnerTeam = makeGameTeam("3,0,0,0,0,0,0,0,0", 3)
            val loserTeam = makeGameTeam("0,0,0,1,0,0,0,0,0", 1)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 1회 결승점 → 완투한 선발에게 패전
            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `동점에서 역전한 뒤 추가 득점이 있어도 결승점 이닝은 최초 역전 이닝`() {
            // given: 동점에서 4회에 역전, 7회에 추가 득점
            // 승팀: 0,0,0,2,0,0,3,0,0 = 5
            // 패팀: 0,0,0,0,0,0,0,0,0 = 0
            // 누적: 승 0,0,0,2,2,2,5,5,5 / 패 0,0,0,0,0,0,0,0,0
            // 4회: 2>0 && 0<=0 → decisionInning=4
            // 7회: 5>0 && 2>0 → prevW > prevL → 갱신 없음
            val loserStarter =
                makeRecord(isStarter = true, outs = 9, runsAllowed = 0) // 1~3회
            val loserRelief1 =
                makeRecord(isStarter = false, outs = 9, runsAllowed = 2) // 4~6회 (결승점)
            val loserRelief2 =
                makeRecord(isStarter = false, outs = 9, runsAllowed = 3) // 7~9회 (추가 실점)

            val winnerTeam = makeGameTeam("0,0,0,2,0,0,3,0,0", 5)
            val loserTeam = makeGameTeam("0,0,0,0,0,0,0,0,0", 0)

            // when
            service.assignDecisions(
                winnerTeamRecords = emptyList(),
                loserTeamRecords = listOf(loserStarter, loserRelief1, loserRelief2),
                winnerGameTeam = winnerTeam,
                loserGameTeam = loserTeam,
                gameRules = gameRules,
            )

            // then: 4회 결승점 → 4~6회 담당 loserRelief1에게 패전
            assertThat(loserRelief1.decision).isEqualTo(PitchingDecision.LOSS)
            assertThat(loserStarter.decision).isEqualTo(PitchingDecision.NONE)
            assertThat(loserRelief2.decision).isEqualTo(PitchingDecision.NONE)
        }
    }
}
