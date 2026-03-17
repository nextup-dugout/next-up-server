package com.nextup.core.domain.game

import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@DisplayName("PitchingRecord")
class PitchingRecordTest {
    private lateinit var gamePlayer: GamePlayer
    private lateinit var pitchingRecord: PitchingRecord

    @BeforeEach
    fun setup() {
        gamePlayer = mockk<GamePlayer>(relaxed = true)
        pitchingRecord = PitchingRecord.create(gamePlayer)
    }

    @Nested
    @DisplayName("아웃 기록")
    inner class RecordOutTest {
        @Test
        fun `아웃을 기록하면 inningsPitchedOuts가 증가한다`() {
            // when
            pitchingRecord.recordOut(false)

            // then
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(1)
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
        }

        @Test
        fun `삼진 아웃을 기록하면 strikeouts도 증가한다`() {
            // when
            pitchingRecord.recordOut(isStrikeout = true)

            // then
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(1)
            assertThat(pitchingRecord.strikeouts).isEqualTo(1)
        }

        @Test
        fun `3아웃을 기록하면 1이닝이다`() {
            // when
            repeat(3) { pitchingRecord.recordOut() }

            // then
            assertThat(pitchingRecord.completeInnings).isEqualTo(1)
            assertThat(pitchingRecord.remainingOuts).isEqualTo(0)
            assertThat(pitchingRecord.inningsPitchedDisplay).isEqualTo("1.0")
        }
    }

    @Nested
    @DisplayName("피안타 기록")
    inner class RecordHitTest {
        @Test
        fun `피안타를 기록하면 hitsAllowed가 증가한다`() {
            // when
            pitchingRecord.recordHit()

            // then
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(1)
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
        }

        @Test
        fun `피홈런을 기록하면 homeRunsAllowed도 증가한다`() {
            // when
            pitchingRecord.recordHit(isHomeRun = true)

            // then
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(1)
            assertThat(pitchingRecord.homeRunsAllowed).isEqualTo(1)
        }

        @Test
        fun `피안타와 함께 실점을 기록할 수 있다`() {
            // when
            pitchingRecord.recordHit(runsScored = 2, earnedRuns = 1)

            // then
            assertThat(pitchingRecord.runsAllowed).isEqualTo(2)
            assertThat(pitchingRecord.earnedRuns).isEqualTo(1)
        }

        @Test
        fun `자책점이 실점보다 크면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                pitchingRecord.recordHit(runsScored = 1, earnedRuns = 2)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("자책점")
        }
    }

    @Nested
    @DisplayName("볼넷 기록")
    inner class RecordWalkTest {
        @Test
        fun `볼넷을 기록하면 walksAllowed가 증가한다`() {
            // when
            pitchingRecord.recordWalk()

            // then
            assertThat(pitchingRecord.walksAllowed).isEqualTo(1)
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("사구 기록")
    inner class RecordHitByPitchTest {
        @Test
        fun `사구를 기록하면 hitBatsmen이 증가한다`() {
            // when
            pitchingRecord.recordHitByPitch()

            // then
            assertThat(pitchingRecord.hitBatsmen).isEqualTo(1)
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("실점 기록")
    inner class RecordRunTest {
        @Test
        fun `자책점을 기록하면 earnedRuns와 runsAllowed 모두 증가한다`() {
            // when
            pitchingRecord.recordRun(isEarned = true)

            // then
            assertThat(pitchingRecord.runsAllowed).isEqualTo(1)
            assertThat(pitchingRecord.earnedRuns).isEqualTo(1)
        }

        @Test
        fun `비자책점을 기록하면 runsAllowed만 증가한다`() {
            // when
            pitchingRecord.recordRun(isEarned = false)

            // then
            assertThat(pitchingRecord.runsAllowed).isEqualTo(1)
            assertThat(pitchingRecord.earnedRuns).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("와일드피치와 보크")
    inner class WildPitchAndBalkTest {
        @Test
        fun `와일드피치를 기록할 수 있다`() {
            // when
            pitchingRecord.recordWildPitch()

            // then
            assertThat(pitchingRecord.wildPitches).isEqualTo(1)
        }

        @Test
        fun `보크를 기록할 수 있다`() {
            // when
            pitchingRecord.recordBalk()

            // then
            assertThat(pitchingRecord.balks).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("투구 수 기록")
    inner class RecordPitchCountTest {
        @Test
        fun `투구 수를 기록할 수 있다`() {
            // when
            pitchingRecord.recordPitchCount(totalPitches = 100, strikes = 65)

            // then
            assertThat(pitchingRecord.pitchesThrown).isEqualTo(100)
            assertThat(pitchingRecord.strikesThrown).isEqualTo(65)
        }

        @Test
        fun `스트라이크가 총 투구수보다 많으면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy {
                pitchingRecord.recordPitchCount(totalPitches = 50, strikes = 60)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("스트라이크 수")
        }
    }

    @Nested
    @DisplayName("투구 수 제한 상태 확인 (D-20)")
    inner class CheckPitchCountStatusTest {
        @Test
        fun `투구 수 미기록 시 null을 반환한다`() {
            // given: pitchesThrown is null by default

            // when
            val status = pitchingRecord.checkPitchCountStatus(limit = 100)

            // then
            assertThat(status).isNull()
        }

        @Test
        fun `투구 수가 제한 미만이고 경고 범위 밖이면 null을 반환한다`() {
            // given: 100구 제한, 10구 경고 임박 기준, 현재 85구 (경고 범위 밖)
            pitchingRecord.recordPitchCount(totalPitches = 85, strikes = 50)

            // when
            val status = pitchingRecord.checkPitchCountStatus(limit = 100, warningThreshold = 10)

            // then
            assertThat(status).isNull()
        }

        @Test
        fun `투구 수가 경고 임박 범위에 들어오면 APPROACHING_LIMIT을 반환한다`() {
            // given: 100구 제한, 10구 경고 임박 기준, 현재 91구 (제한까지 9구 남음)
            pitchingRecord.recordPitchCount(totalPitches = 91, strikes = 55)

            // when
            val status = pitchingRecord.checkPitchCountStatus(limit = 100, warningThreshold = 10)

            // then
            assertThat(status).isEqualTo(PitchCountStatus.APPROACHING_LIMIT)
        }

        @Test
        fun `투구 수가 경고 임박 기준 경계값이면 APPROACHING_LIMIT을 반환한다`() {
            // given: 100구 제한, 10구 경고 임박 기준, 현재 정확히 90구
            pitchingRecord.recordPitchCount(totalPitches = 90, strikes = 55)

            // when
            val status = pitchingRecord.checkPitchCountStatus(limit = 100, warningThreshold = 10)

            // then
            assertThat(status).isEqualTo(PitchCountStatus.APPROACHING_LIMIT)
        }

        @Test
        fun `투구 수가 제한에 도달하면 LIMIT_REACHED를 반환한다`() {
            // given: 100구 제한, 현재 정확히 100구
            pitchingRecord.recordPitchCount(totalPitches = 100, strikes = 65)

            // when
            val status = pitchingRecord.checkPitchCountStatus(limit = 100)

            // then
            assertThat(status).isEqualTo(PitchCountStatus.LIMIT_REACHED)
        }

        @Test
        fun `투구 수가 제한을 초과하면 LIMIT_REACHED를 반환한다`() {
            // given: 100구 제한, 현재 105구 (초과)
            pitchingRecord.recordPitchCount(totalPitches = 105, strikes = 65)

            // when
            val status = pitchingRecord.checkPitchCountStatus(limit = 100)

            // then
            assertThat(status).isEqualTo(PitchCountStatus.LIMIT_REACHED)
        }

        @Test
        fun `기본 경고 임박 기준은 10구이다`() {
            // given: 80구 제한, 현재 72구 (제한까지 8구 남음 - 기본 10구 기준 내)
            pitchingRecord.recordPitchCount(totalPitches = 72, strikes = 45)

            // when: warningThreshold 기본값 사용
            val status = pitchingRecord.checkPitchCountStatus(limit = 80)

            // then
            assertThat(status).isEqualTo(PitchCountStatus.APPROACHING_LIMIT)
        }
    }

    @Nested
    @DisplayName("이닝 계산")
    inner class InningsCalculationTest {
        @Test
        fun `0아웃은 0이닝이다`() {
            assertThat(pitchingRecord.inningsPitchedDisplay).isEqualTo("0.0")
        }

        @Test
        fun `1아웃은 0_1이닝이다`() {
            pitchingRecord.recordOut()
            assertThat(pitchingRecord.inningsPitchedDisplay).isEqualTo("0.1")
        }

        @Test
        fun `2아웃은 0_2이닝이다`() {
            repeat(2) { pitchingRecord.recordOut() }
            assertThat(pitchingRecord.inningsPitchedDisplay).isEqualTo("0.2")
        }

        @Test
        fun `5_1이닝은 16아웃이다`() {
            repeat(16) { pitchingRecord.recordOut() }
            assertThat(pitchingRecord.inningsPitchedDisplay).isEqualTo("5.1")
            assertThat(pitchingRecord.completeInnings).isEqualTo(5)
            assertThat(pitchingRecord.remainingOuts).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("ERA 계산")
    inner class EarnedRunAverageTest {
        @Test
        fun `0이닝 0자책점이면 ERA는 0_00이다`() {
            assertThat(pitchingRecord.earnedRunAverage).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `0이닝이지만 자책점이 있으면 ERA는 null이다`() {
            // given: 아웃 없이 자책점만 기록된 투수 (등판 후 즉시 교체)
            pitchingRecord.recordRun(isEarned = true)

            // then: 계산 불가 (무한대)
            assertThat(pitchingRecord.earnedRunAverage).isNull()
        }

        @Test
        fun `9이닝 3자책점이면 ERA는 3_00이다`() {
            // given: 27아웃 = 9이닝
            repeat(27) { pitchingRecord.recordOut() }
            pitchingRecord.recordRun(isEarned = true)
            pitchingRecord.recordRun(isEarned = true)
            pitchingRecord.recordRun(isEarned = true)

            // then: ERA = (3 / 9) * 9 = 3.00
            assertThat(pitchingRecord.earnedRunAverage).isEqualTo(BigDecimal("3.00"))
        }

        @Test
        fun `6이닝 2자책점이면 ERA는 3_00이다`() {
            // given: 18아웃 = 6이닝
            repeat(18) { pitchingRecord.recordOut() }
            pitchingRecord.recordRun(isEarned = true)
            pitchingRecord.recordRun(isEarned = true)

            // then: ERA = (2 / 6) * 9 = 3.00
            assertThat(pitchingRecord.earnedRunAverage).isEqualTo(BigDecimal("3.00"))
        }
    }

    @Nested
    @DisplayName("WHIP 계산")
    inner class WhipTest {
        @Test
        fun `이닝이 0이면 WHIP은 0이다`() {
            assertThat(pitchingRecord.whip).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `7이닝 7피안타 0볼넷이면 WHIP은 1_00이다`() {
            // given: 21아웃 = 7이닝
            repeat(21) { pitchingRecord.recordOut() }
            repeat(7) { pitchingRecord.recordHit() }

            // then: WHIP = 7 / 7 = 1.00
            assertThat(pitchingRecord.whip).isEqualTo(BigDecimal("1.00"))
        }

        @Test
        fun `9이닝 6피안타 3볼넷이면 WHIP은 1_00이다`() {
            // given: 27아웃 = 9이닝
            repeat(27) { pitchingRecord.recordOut() }
            repeat(6) { pitchingRecord.recordHit() }
            repeat(3) { pitchingRecord.recordWalk() }

            // then: WHIP = (6 + 3) / 9 = 1.00
            assertThat(pitchingRecord.whip).isEqualTo(BigDecimal("1.00"))
        }
    }

    @Nested
    @DisplayName("K/9 계산")
    inner class StrikeoutsPer9Test {
        @Test
        fun `이닝이 0이면 9이닝당 삼진은 0이다`() {
            assertThat(pitchingRecord.strikeoutsPer9).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `9이닝 9삼진이면 9이닝당 삼진은 9_00이다`() {
            // given: 27아웃 = 9이닝, 9개는 삼진
            repeat(9) { pitchingRecord.recordOut(isStrikeout = true) }
            repeat(18) { pitchingRecord.recordOut(isStrikeout = false) }

            // then: K/9 = (9 / 9) * 9 = 9.00
            assertThat(pitchingRecord.strikeoutsPer9).isEqualTo(BigDecimal("9.00"))
        }
    }

    @Nested
    @DisplayName("BB/9 계산")
    inner class WalksPer9Test {
        @Test
        fun `이닝이 0이면 9이닝당 볼넷은 0이다`() {
            assertThat(pitchingRecord.walksPer9).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `9이닝 3볼넷이면 9이닝당 볼넷은 3_00이다`() {
            // given: 27아웃 = 9이닝
            repeat(27) { pitchingRecord.recordOut() }
            repeat(3) { pitchingRecord.recordWalk() }

            // then: BB/9 = (3 / 9) * 9 = 3.00
            assertThat(pitchingRecord.walksPer9).isEqualTo(BigDecimal("3.00"))
        }
    }

    @Nested
    @DisplayName("K/BB 비율")
    inner class StrikeoutToWalkRatioTest {
        @Test
        fun `삼진과 볼넷이 모두 0이면 0이다`() {
            assertThat(pitchingRecord.strikeoutToWalkRatio).isEqualTo(BigDecimal("0.00"))
        }

        @Test
        fun `볼넷이 0이고 삼진이 있으면 삼진 수를 반환한다`() {
            // given
            repeat(5) { pitchingRecord.recordOut(isStrikeout = true) }

            // then
            assertThat(pitchingRecord.strikeoutToWalkRatio).isEqualTo(BigDecimal("5.00"))
        }

        @Test
        fun `6삼진 2볼넷이면 삼진대볼넷 비율은 3_00이다`() {
            // given
            repeat(6) { pitchingRecord.recordOut(isStrikeout = true) }
            repeat(2) { pitchingRecord.recordWalk() }

            // then: K/BB = 6 / 2 = 3.00
            assertThat(pitchingRecord.strikeoutToWalkRatio).isEqualTo(BigDecimal("3.00"))
        }
    }

    @Nested
    @DisplayName("스트라이크 비율")
    inner class StrikePercentageTest {
        @Test
        fun `투구 수가 없으면 null이다`() {
            assertThat(pitchingRecord.strikePercentage).isNull()
        }

        @Test
        fun `100투구 65스트라이크면 0_650이다`() {
            // given
            pitchingRecord.recordPitchCount(totalPitches = 100, strikes = 65)

            // then
            assertThat(pitchingRecord.strikePercentage).isEqualTo(BigDecimal("0.650"))
        }
    }

    @Nested
    @DisplayName("비자책점 계산")
    inner class UnearnedRunsTest {
        @Test
        fun `비자책점 = 실점 - 자책점`() {
            // given
            pitchingRecord.recordRun(isEarned = true)
            pitchingRecord.recordRun(isEarned = true)
            pitchingRecord.recordRun(isEarned = false)
            pitchingRecord.recordRun(isEarned = false)

            // then
            assertThat(pitchingRecord.unearnedRuns).isEqualTo(2)
        }
    }

    @Nested
    @DisplayName("선발 투수")
    inner class StartingPitcherTest {
        @Test
        fun `기본값은 선발투수가 아니다`() {
            assertThat(pitchingRecord.isStartingPitcher).isFalse()
        }

        @Test
        fun `선발투수로 설정할 수 있다`() {
            // when
            pitchingRecord.setAsStartingPitcher()

            // then
            assertThat(pitchingRecord.isStartingPitcher).isTrue()
        }

        @Test
        fun `create 시 선발투수 여부를 지정할 수 있다`() {
            // when
            val starterRecord = PitchingRecord.create(gamePlayer, isStartingPitcher = true)

            // then
            assertThat(starterRecord.isStartingPitcher).isTrue()
        }
    }

    @Nested
    @DisplayName("선발 승리 자격")
    inner class QualifiedForWinTest {
        @Test
        fun `선발투수가 아니면 자격이 없다`() {
            // given
            repeat(15) { pitchingRecord.recordOut() }

            // then
            assertThat(pitchingRecord.isQualifiedForWin).isFalse()
        }

        @Test
        fun `선발투수라도 5이닝 미만이면 자격이 없다`() {
            // given
            pitchingRecord.setAsStartingPitcher()
            repeat(14) { pitchingRecord.recordOut() } // 4.2이닝

            // then
            assertThat(pitchingRecord.isQualifiedForWin).isFalse()
        }

        @Test
        fun `선발투수가 5이닝 이상 던지면 자격이 있다`() {
            // given
            pitchingRecord.setAsStartingPitcher()
            repeat(15) { pitchingRecord.recordOut() } // 5.0이닝

            // then
            assertThat(pitchingRecord.isQualifiedForWin).isTrue()
        }
    }

    @Nested
    @DisplayName("승/패/세이브/홀드 결정")
    inner class DecisionTest {
        @Test
        fun `기본 결정은 NONE이다`() {
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.NONE)
        }

        @Test
        fun `승리 결정을 부여할 수 있다`() {
            // when
            pitchingRecord.assignWin()

            // then
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `패배 결정을 부여할 수 있다`() {
            // when
            pitchingRecord.assignLoss()

            // then
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.LOSS)
        }

        @Test
        fun `구원투수에게 세이브를 부여할 수 있다`() {
            // when
            pitchingRecord.assignSave()

            // then
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.SAVE)
        }

        @Test
        fun `선발투수에게 세이브를 부여하면 예외가 발생한다`() {
            // given
            pitchingRecord.setAsStartingPitcher()

            // when & then
            assertThatThrownBy { pitchingRecord.assignSave() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("선발 투수")
        }

        @Test
        fun `구원투수에게 홀드를 부여할 수 있다`() {
            // when
            pitchingRecord.assignHold()

            // then
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.HOLD)
        }

        @Test
        fun `선발투수에게 홀드를 부여하면 예외가 발생한다`() {
            // given
            pitchingRecord.setAsStartingPitcher()

            // when & then
            assertThatThrownBy { pitchingRecord.assignHold() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("선발 투수")
        }

        @Test
        fun `구원투수에게 블론세이브를 부여할 수 있다`() {
            // when
            pitchingRecord.assignBlownSave()

            // then
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.BLOWN_SAVE)
        }

        @Test
        fun `이미 패배 결정이 있는 투수에게 세이브를 부여하면 예외가 발생한다`() {
            // given
            pitchingRecord.assignLoss()

            // when & then
            assertThatThrownBy { pitchingRecord.assignSave() }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Nested
    @DisplayName("유효성 검증")
    inner class ValidationTest {
        @Test
        fun `정상적인 기록은 검증에 통과한다`() {
            // given
            repeat(18) { pitchingRecord.recordOut() }
            pitchingRecord.recordHit(runsScored = 2, earnedRuns = 1)

            // when & then
            pitchingRecord.validate()
        }

        @Test
        fun `투구 수 기록이 있으면 검증한다`() {
            // given
            pitchingRecord.recordPitchCount(100, 65)

            // when & then
            pitchingRecord.validate()
        }
    }

    @Nested
    @DisplayName("이닝 문자열 파싱")
    inner class ParseInningsToOutsTest {
        @Test
        fun `5_0을 15아웃으로 변환한다`() {
            assertThat(PitchingRecord.parseInningsToOuts("5.0")).isEqualTo(15)
        }

        @Test
        fun `5_1을 16아웃으로 변환한다`() {
            assertThat(PitchingRecord.parseInningsToOuts("5.1")).isEqualTo(16)
        }

        @Test
        fun `5_2를 17아웃으로 변환한다`() {
            assertThat(PitchingRecord.parseInningsToOuts("5.2")).isEqualTo(17)
        }

        @Test
        fun `7을 21아웃으로 변환한다`() {
            assertThat(PitchingRecord.parseInningsToOuts("7")).isEqualTo(21)
        }

        @Test
        fun `잘못된 잔여 아웃 수는 예외가 발생한다`() {
            assertThatThrownBy { PitchingRecord.parseInningsToOuts("5.3") }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("잔여 아웃 수")
        }
    }

    @Nested
    @DisplayName("applyBatterFaced - BoxScore 자동 계산")
    inner class ApplyBatterFacedTest {
        @Test
        fun `단타를 적용하면 battersFaced와 hitsAllowed가 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.SINGLE)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(1)
        }

        @Test
        fun `2루타를 적용하면 피안타가 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.DOUBLE)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(1)
        }

        @Test
        fun `홈런을 적용하면 피안타와 피홈런이 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(1)
            assertThat(pitchingRecord.homeRunsAllowed).isEqualTo(1)
        }

        @Test
        fun `삼진을 적용하면 battersFaced와 strikeouts가 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.strikeouts).isEqualTo(1)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
        }

        @Test
        fun `볼넷을 적용하면 battersFaced와 walksAllowed가 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.WALK)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.walksAllowed).isEqualTo(1)
        }

        @Test
        fun `고의4구를 적용하면 battersFaced와 walksAllowed가 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.INTENTIONAL_WALK)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.walksAllowed).isEqualTo(1)
        }

        @Test
        fun `사구를 적용하면 battersFaced와 hitBatsmen이 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.HIT_BY_PITCH)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.hitBatsmen).isEqualTo(1)
        }

        @Test
        fun `땅볼아웃을 적용하면 battersFaced만 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.GROUND_OUT)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
            assertThat(pitchingRecord.walksAllowed).isEqualTo(0)
        }

        @Test
        fun `연속된 타자 대결 결과를 올바르게 누적한다`() {
            // given
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.SINGLE)
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT)
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.WALK)
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.HOME_RUN)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(4)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(2)
            assertThat(pitchingRecord.strikeouts).isEqualTo(1)
            assertThat(pitchingRecord.walksAllowed).isEqualTo(1)
            assertThat(pitchingRecord.homeRunsAllowed).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("recordInningOut - 이닝 아웃 카운트 기록")
    inner class RecordInningOutTest {
        @Test
        fun `recordInningOut()은 이닝 아웃만 증가시킨다`() {
            // when
            pitchingRecord.recordInningOut()

            // then
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("recordEarnedRun - 자책점 기록")
    inner class RecordEarnedRunTest {
        @Test
        fun `자책점을 기록하면 earnedRuns와 runsAllowed가 증가한다`() {
            // when
            pitchingRecord.recordEarnedRun(2)

            // then
            assertThat(pitchingRecord.earnedRuns).isEqualTo(2)
            assertThat(pitchingRecord.runsAllowed).isEqualTo(2)
        }

        @Test
        fun `음수 자책점은 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                pitchingRecord.recordEarnedRun(-1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("자책점은 0 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("recordUnearnedRun - 비자책 실점 기록")
    inner class RecordUnearnedRunTest {
        @Test
        fun `비자책 실점을 기록하면 runsAllowed만 증가한다`() {
            // when
            pitchingRecord.recordUnearnedRun(2)

            // then
            assertThat(pitchingRecord.runsAllowed).isEqualTo(2)
            assertThat(pitchingRecord.earnedRuns).isEqualTo(0)
        }

        @Test
        fun `음수 비자책 실점은 허용되지 않는다`() {
            // when & then
            assertThatThrownBy {
                pitchingRecord.recordUnearnedRun(-1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("비자책 실점은 0 이상이어야 합니다")
        }

        @Test
        fun `자책점과 비자책점을 함께 기록할 수 있다`() {
            // given
            pitchingRecord.recordEarnedRun(2)
            pitchingRecord.recordUnearnedRun(1)

            // then
            assertThat(pitchingRecord.earnedRuns).isEqualTo(2)
            assertThat(pitchingRecord.runsAllowed).isEqualTo(3)
            assertThat(pitchingRecord.unearnedRuns).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("revertBatterFaced - 음수 값 방지 가드")
    inner class RevertBatterFacedNegativeGuardTest {
        @Test
        fun `대면 타자 기록이 없을 때 롤백하면 예외가 발생한다`() {
            assertThatThrownBy {
                pitchingRecord.revertBatterFaced(PlateAppearanceResult.SINGLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 대면 타자 기록이 없습니다")
        }

        @Test
        fun `피안타 기록이 없을 때 단타 롤백하면 예외가 발생한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT)

            assertThatThrownBy {
                pitchingRecord.revertBatterFaced(PlateAppearanceResult.SINGLE)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 피안타 기록이 없습니다")
        }

        @Test
        fun `피홈런 기록이 없을 때 홈런 롤백하면 예외가 발생한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.SINGLE)

            assertThatThrownBy {
                pitchingRecord.revertBatterFaced(PlateAppearanceResult.HOME_RUN)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 피홈런 기록이 없습니다")
        }

        @Test
        fun `삼진 기록이 없을 때 삼진 롤백하면 예외가 발생한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                pitchingRecord.revertBatterFaced(PlateAppearanceResult.STRIKEOUT)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 삼진 기록이 없습니다")
        }

        @Test
        fun `볼넷 기록이 없을 때 볼넷 롤백하면 예외가 발생한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                pitchingRecord.revertBatterFaced(PlateAppearanceResult.WALK)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 볼넷 기록이 없습니다")
        }

        @Test
        fun `사구 기록이 없을 때 사구 롤백하면 예외가 발생한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.GROUND_OUT)

            assertThatThrownBy {
                pitchingRecord.revertBatterFaced(PlateAppearanceResult.HIT_BY_PITCH)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 사구 기록이 없습니다")
        }

        @Test
        fun `단타 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.SINGLE)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.SINGLE)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("revertEarnedRun - 음수 값 방지 가드")
    inner class RevertEarnedRunNegativeGuardTest {
        @Test
        fun `롤백할 자책점이 현재 자책점보다 크면 예외가 발생한다`() {
            pitchingRecord.recordEarnedRun(1)

            assertThatThrownBy {
                pitchingRecord.revertEarnedRun(2)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 자책점")
        }

        @Test
        fun `자책점 롤백이 정상적으로 동작한다`() {
            pitchingRecord.recordEarnedRun(3)
            pitchingRecord.revertEarnedRun(2)

            assertThat(pitchingRecord.earnedRuns).isEqualTo(1)
            assertThat(pitchingRecord.runsAllowed).isEqualTo(1)
        }

        @Test
        fun `음수 롤백 값은 허용되지 않는다`() {
            assertThatThrownBy {
                pitchingRecord.revertEarnedRun(-1)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 자책점은 0 이상이어야 합니다")
        }
    }

    @Nested
    @DisplayName("revertInningOut - 이닝 아웃 카운트 롤백")
    inner class RevertInningOutTest {
        @Test
        fun `아웃 카운트를 롤백할 수 있다`() {
            pitchingRecord.recordInningOut()
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(1)

            pitchingRecord.revertInningOut()
            assertThat(pitchingRecord.inningsPitchedOuts).isEqualTo(0)
        }

        @Test
        fun `롤백할 아웃 카운트가 없으면 예외가 발생한다`() {
            assertThatThrownBy {
                pitchingRecord.revertInningOut()
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 아웃 카운트가 없습니다")
        }
    }

    @Nested
    @DisplayName("revertBatterFaced - 결과별 롤백 정상 동작")
    inner class RevertBatterFacedHappyPathTest {
        @Test
        fun `2루타 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.DOUBLE)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.DOUBLE)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
        }

        @Test
        fun `3루타 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.TRIPLE)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.TRIPLE)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
        }

        @Test
        fun `홈런 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.HOME_RUN)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.HOME_RUN)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
            assertThat(pitchingRecord.homeRunsAllowed).isEqualTo(0)
        }

        @Test
        fun `삼진 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.STRIKEOUT)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.strikeouts).isEqualTo(0)
        }

        @Test
        fun `볼넷 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.WALK)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.WALK)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.walksAllowed).isEqualTo(0)
        }

        @Test
        fun `고의4구 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.INTENTIONAL_WALK)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.INTENTIONAL_WALK)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.walksAllowed).isEqualTo(0)
        }

        @Test
        fun `사구 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.HIT_BY_PITCH)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.HIT_BY_PITCH)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.hitBatsmen).isEqualTo(0)
        }

        @Test
        fun `땅볼아웃 롤백이 정상적으로 동작한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.GROUND_OUT)
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.GROUND_OUT)

            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("revertBatterFaced - 중간 가드 커버리지")
    inner class RevertBatterFacedIntermediateGuardTest {
        @Test
        fun `홈런 롤백 시 피안타가 없으면 예외가 발생한다`() {
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT)

            assertThatThrownBy {
                pitchingRecord.revertBatterFaced(PlateAppearanceResult.HOME_RUN)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 피안타 기록이 없습니다")
        }
    }

    @Nested
    @DisplayName("revertEarnedRun - 중간 가드 커버리지")
    inner class RevertEarnedRunIntermediateGuardTest {
        private fun setField(
            fieldName: String,
            value: Int,
        ) {
            val field = PitchingRecord::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(pitchingRecord, value)
        }

        @Test
        fun `자책점 롤백 시 실점이 부족하면 예외가 발생한다`() {
            setField("earnedRuns", 3)
            setField("runsAllowed", 1)

            assertThatThrownBy {
                pitchingRecord.revertEarnedRun(2)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 실점")
        }
    }

    @Nested
    @DisplayName("validate - 음수 필드 검증")
    inner class ValidateNonNegativeFieldTest {
        private fun setField(
            fieldName: String,
            value: Int,
        ) {
            val field = PitchingRecord::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(pitchingRecord, value)
        }

        @Test
        fun `이닝 아웃 수가 음수이면 검증에 실패한다`() {
            setField("inningsPitchedOuts", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이닝 아웃 수")
        }

        @Test
        fun `자책점이 음수이면 검증에 실패한다`() {
            setField("earnedRuns", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("자책점")
        }

        @Test
        fun `실점이 음수이면 검증에 실패한다`() {
            setField("runsAllowed", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("실점")
        }

        @Test
        fun `피안타가 음수이면 검증에 실패한다`() {
            setField("hitsAllowed", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("피안타")
        }

        @Test
        fun `볼넷 허용이 음수이면 검증에 실패한다`() {
            setField("walksAllowed", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("볼넷 허용")
        }

        @Test
        fun `삼진이 음수이면 검증에 실패한다`() {
            setField("strikeouts", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("삼진")
        }

        @Test
        fun `피홈런이 음수이면 검증에 실패한다`() {
            setField("homeRunsAllowed", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("피홈런")
        }

        @Test
        fun `사구가 음수이면 검증에 실패한다`() {
            setField("hitBatsmen", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("사구")
        }

        @Test
        fun `와일드피치가 음수이면 검증에 실패한다`() {
            setField("wildPitches", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("와일드피치")
        }

        @Test
        fun `보크가 음수이면 검증에 실패한다`() {
            setField("balks", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("보크")
        }

        @Test
        fun `대면 타자 수가 음수이면 검증에 실패한다`() {
            setField("battersFaced", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("대면 타자 수")
        }
    }

    @Nested
    @DisplayName("도루 허용 / 도루 저지 / 견제 아웃 기록 및 롤백")
    inner class StolenBaseCaughtStealingPickoffTest {
        @Test
        fun `도루 허용을 기록하면 stolenBasesAllowed가 증가한다`() {
            // when
            pitchingRecord.recordStolenBaseAllowed()

            // then
            assertThat(pitchingRecord.stolenBasesAllowed).isEqualTo(1)
        }

        @Test
        fun `도루 허용을 여러 번 기록하면 누적된다`() {
            // when
            repeat(3) { pitchingRecord.recordStolenBaseAllowed() }

            // then
            assertThat(pitchingRecord.stolenBasesAllowed).isEqualTo(3)
        }

        @Test
        fun `도루 저지를 기록하면 runnersCaughtStealing이 증가한다`() {
            // when
            pitchingRecord.recordCaughtStealing()

            // then
            assertThat(pitchingRecord.runnersCaughtStealing).isEqualTo(1)
        }

        @Test
        fun `견제 아웃을 기록하면 pickoffs가 증가한다`() {
            // when
            pitchingRecord.recordPickoff()

            // then
            assertThat(pitchingRecord.pickoffs).isEqualTo(1)
        }

        @Test
        fun `도루 허용을 롤백하면 stolenBasesAllowed가 감소한다`() {
            // given
            pitchingRecord.recordStolenBaseAllowed()

            // when
            pitchingRecord.revertStolenBaseAllowed()

            // then
            assertThat(pitchingRecord.stolenBasesAllowed).isEqualTo(0)
        }

        @Test
        fun `도루 허용 기록이 없을 때 롤백하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy { pitchingRecord.revertStolenBaseAllowed() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 도루 허용 기록이 없습니다")
        }

        @Test
        fun `도루 저지를 롤백하면 runnersCaughtStealing이 감소한다`() {
            // given
            pitchingRecord.recordCaughtStealing()

            // when
            pitchingRecord.revertCaughtStealing()

            // then
            assertThat(pitchingRecord.runnersCaughtStealing).isEqualTo(0)
        }

        @Test
        fun `도루 저지 기록이 없을 때 롤백하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy { pitchingRecord.revertCaughtStealing() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 도루 저지 기록이 없습니다")
        }

        @Test
        fun `견제 아웃을 롤백하면 pickoffs가 감소한다`() {
            // given
            pitchingRecord.recordPickoff()

            // when
            pitchingRecord.revertPickoff()

            // then
            assertThat(pitchingRecord.pickoffs).isEqualTo(0)
        }

        @Test
        fun `견제 아웃 기록이 없을 때 롤백하면 예외가 발생한다`() {
            // when & then
            assertThatThrownBy { pitchingRecord.revertPickoff() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 견제 아웃 기록이 없습니다")
        }
    }

    @Nested
    @DisplayName("validate - 도루/견제 음수 필드 검증")
    inner class ValidateStolenBaseFieldsTest {
        private fun setField(
            fieldName: String,
            value: Int,
        ) {
            val field = PitchingRecord::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(pitchingRecord, value)
        }

        @Test
        fun `도루 허용이 음수이면 검증에 실패한다`() {
            setField("stolenBasesAllowed", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("도루 허용")
        }

        @Test
        fun `도루 저지가 음수이면 검증에 실패한다`() {
            setField("runnersCaughtStealing", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("도루 저지")
        }

        @Test
        fun `견제 아웃이 음수이면 검증에 실패한다`() {
            setField("pickoffs", -1)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("견제 아웃")
        }
    }

    @Nested
    @DisplayName("applyBatterFaced - STRIKEOUT_DROPPED_THIRD 처리")
    inner class ApplyBatterFacedDroppedThirdTest {
        @Test
        fun `낫아웃 삼진을 적용하면 battersFaced와 strikeouts가 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.strikeouts).isEqualTo(1)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
        }

        @Test
        fun `낫아웃 삼진 롤백이 정상적으로 동작한다`() {
            // given
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            // when
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.strikeouts).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("validate - 관계형 제약 검증")
    inner class ValidateRelationalTest {
        private fun setField(
            fieldName: String,
            value: Int,
        ) {
            val field = PitchingRecord::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.setInt(pitchingRecord, value)
        }

        @Test
        fun `자책점이 실점보다 크면 검증에 실패한다`() {
            setField("earnedRuns", 3)
            setField("runsAllowed", 2)
            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("자책점")
        }

        @Test
        fun `스트라이크가 투구 수보다 크면 검증에 실패한다`() {
            pitchingRecord.recordPitchCount(totalPitches = 50, strikes = 30)
            val strikesField = PitchingRecord::class.java.getDeclaredField("strikesThrown")
            strikesField.isAccessible = true
            strikesField.set(pitchingRecord, 60)

            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("스트라이크 수")
        }

        @Test
        fun `선발 투수가 승리 자격 미달인데 승리 결정이면 검증에 실패한다`() {
            pitchingRecord.setAsStartingPitcher()
            repeat(14) { pitchingRecord.recordOut() } // 4.2이닝 (5이닝 미만)
            pitchingRecord.assignWin()

            assertThatThrownBy { pitchingRecord.validate() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("선발 승리 자격")
        }
    }

    @Nested
    @DisplayName("스트라이크 비율 - 분기 커버리지 보완")
    inner class StrikePercentageBranchTest {

        @Test
        fun `투구 수가 0이면 null이다`() {
            // given: pitchesThrown=0 분기 커버
            val pitchesField = PitchingRecord::class.java.getDeclaredField("pitchesThrown")
            pitchesField.isAccessible = true
            pitchesField.set(pitchingRecord, 0)
            val strikesField = PitchingRecord::class.java.getDeclaredField("strikesThrown")
            strikesField.isAccessible = true
            strikesField.set(pitchingRecord, 0)

            // then
            assertThat(pitchingRecord.strikePercentage).isNull()
        }

        @Test
        fun `strikesThrown이 null이면 null이다`() {
            // given: pitchesThrown은 설정되고 strikesThrown은 null 분기 커버
            val pitchesField = PitchingRecord::class.java.getDeclaredField("pitchesThrown")
            pitchesField.isAccessible = true
            pitchesField.set(pitchingRecord, 100)
            // strikesThrown은 기본값 null 유지

            // then
            assertThat(pitchingRecord.strikePercentage).isNull()
        }
    }

    @Nested
    @DisplayName("applyBatterFaced - 3루타 분기 커버리지")
    inner class ApplyBatterFacedTripleTest {

        @Test
        fun `3루타를 적용하면 battersFaced와 hitsAllowed가 증가한다`() {
            // when
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(1)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(1)
            assertThat(pitchingRecord.homeRunsAllowed).isEqualTo(0)
        }

        @Test
        fun `3루타 롤백을 적용하면 battersFaced와 hitsAllowed가 감소한다`() {
            // given
            pitchingRecord.applyBatterFaced(PlateAppearanceResult.TRIPLE)

            // when
            pitchingRecord.revertBatterFaced(PlateAppearanceResult.TRIPLE)

            // then
            assertThat(pitchingRecord.battersFaced).isEqualTo(0)
            assertThat(pitchingRecord.hitsAllowed).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("도루/도루저지/견제 - 경계값 분기 커버리지")
    inner class BaseRunningBoundaryTest {

        @Test
        fun `도루 허용 0인 상태에서 revert하면 예외가 발생한다`() {
            assertThatThrownBy { pitchingRecord.revertStolenBaseAllowed() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 도루 허용 기록이 없습니다")
        }

        @Test
        fun `도루 저지 0인 상태에서 revert하면 예외가 발생한다`() {
            assertThatThrownBy { pitchingRecord.revertCaughtStealing() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 도루 저지 기록이 없습니다")
        }

        @Test
        fun `견제 아웃 0인 상태에서 revert하면 예외가 발생한다`() {
            assertThatThrownBy { pitchingRecord.revertPickoff() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("롤백할 견제 아웃 기록이 없습니다")
        }

        @Test
        fun `도루 허용을 여러 번 기록하면 누적된다`() {
            // when
            repeat(3) { pitchingRecord.recordStolenBaseAllowed() }

            // then
            assertThat(pitchingRecord.stolenBasesAllowed).isEqualTo(3)
        }

        @Test
        fun `도루 저지 여러 번 기록 후 한 번 revert하면 1 감소한다`() {
            // given
            pitchingRecord.recordCaughtStealing()
            pitchingRecord.recordCaughtStealing()

            // when
            pitchingRecord.revertCaughtStealing()

            // then
            assertThat(pitchingRecord.runnersCaughtStealing).isEqualTo(1)
        }

        @Test
        fun `견제 아웃을 여러 번 기록 후 한 번 revert하면 1 감소한다`() {
            // given
            pitchingRecord.recordPickoff()
            pitchingRecord.recordPickoff()

            // when
            pitchingRecord.revertPickoff()

            // then
            assertThat(pitchingRecord.pickoffs).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("partial branch 커버리지 - strikeoutToWalkRatio 분기")
    inner class StrikeoutToWalkRatioPartialBranchTest {

        @Test
        fun `볼넷 0이고 삼진 0이면 setScale 적용된 0_00을 반환한다`() {
            // walksAllowed == 0, strikeouts == 0 → BigDecimal.ZERO
            assertThat(pitchingRecord.strikeoutToWalkRatio)
                .isEqualByComparingTo(BigDecimal("0.00"))
            assertThat(pitchingRecord.strikeoutToWalkRatio.scale()).isEqualTo(2)
        }

        @Test
        fun `볼넷 0이고 삼진이 있으면 삼진 수에 setScale 적용된다`() {
            // walksAllowed == 0, strikeouts > 0 → BigDecimal(strikeouts)
            repeat(3) { pitchingRecord.recordOut(isStrikeout = true) }
            assertThat(pitchingRecord.strikeoutToWalkRatio)
                .isEqualByComparingTo(BigDecimal("3.00"))
            assertThat(pitchingRecord.strikeoutToWalkRatio.scale()).isEqualTo(2)
        }

        @Test
        fun `볼넷만 있고 삼진이 없으면 나눗셈 결과 0_00이다`() {
            // walksAllowed > 0, strikeouts == 0 → 0 / walksAllowed = 0.00
            pitchingRecord.recordWalk()
            assertThat(pitchingRecord.strikeoutToWalkRatio)
                .isEqualByComparingTo(BigDecimal("0.00"))
        }
    }

    @Nested
    @DisplayName("partial branch 커버리지 - strikePercentage 조건 분기")
    inner class StrikePercentagePartialBranchTest {

        @Test
        fun `pitchesThrown만 설정되고 strikesThrown은 null이면 null이다`() {
            // pitchesThrown != null, strikesThrown == null → null 반환
            val field =
                PitchingRecord::class.java.getDeclaredField("pitchesThrown")
            field.isAccessible = true
            field.set(pitchingRecord, 50)

            assertThat(pitchingRecord.strikePercentage).isNull()
        }

        @Test
        fun `pitchesThrown과 strikesThrown 모두 양수이면 비율을 계산한다`() {
            // 모든 null/0 조건 통과 → 정상 계산 분기
            pitchingRecord.recordPitchCount(totalPitches = 80, strikes = 50)
            assertThat(pitchingRecord.strikePercentage)
                .isEqualByComparingTo(BigDecimal("0.625"))
        }
    }

    @Nested
    @DisplayName("partial branch 커버리지 - assignWin 조건 분기")
    inner class AssignWinPartialBranchTest {

        @Test
        fun `선발 투수는 decision이 NONE이 아니어도 assignWin 가능하다`() {
            // isStartingPitcher == true → || short-circuit으로 require 통과
            pitchingRecord.setAsStartingPitcher()
            val decisionField =
                PitchingRecord::class.java.getDeclaredField("decision")
            decisionField.isAccessible = true
            decisionField.set(pitchingRecord, PitchingDecision.HOLD)

            pitchingRecord.assignWin()
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.WIN)
        }

        @Test
        fun `비선발 투수에게 이미 다른 결정이 있으면 assignWin 실패한다`() {
            // isStartingPitcher == false, decision != NONE → 예외
            pitchingRecord.assignLoss()
            assertThatThrownBy { pitchingRecord.assignWin() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이미 다른 결정")
        }
    }

    @Nested
    @DisplayName("partial branch 커버리지 - assignSave HOLD에서 전환")
    inner class AssignSaveFromHoldBranchTest {

        @Test
        fun `HOLD 상태에서 assignSave를 호출하면 SAVE로 변경된다`() {
            // decision == HOLD 분기 (assignSave require의 두 번째 OR 조건)
            pitchingRecord.assignHold()
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.HOLD)

            pitchingRecord.assignSave()
            assertThat(pitchingRecord.decision).isEqualTo(PitchingDecision.SAVE)
        }

        @Test
        fun `WIN 상태에서 assignSave를 호출하면 예외가 발생한다`() {
            // decision이 NONE도 HOLD도 아닌 경우 → 예외
            pitchingRecord.assignWin()
            assertThatThrownBy { pitchingRecord.assignSave() }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("이미 다른 결정")
        }
    }

    @Nested
    @DisplayName("partial branch 커버리지 - validate 조건 분기")
    inner class ValidatePartialBranchTest {

        private fun setField(
            fieldName: String,
            value: Any?,
        ) {
            val field = PitchingRecord::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(pitchingRecord, value)
        }

        @Test
        fun `pitchesThrown만 설정되고 strikesThrown은 null이면 투구수 검증을 건너뛴다`() {
            // pitchesThrown != null && strikesThrown != null 에서 두 번째가 false
            setField("pitchesThrown", 100)
            pitchingRecord.validate()
        }

        @Test
        fun `strikesThrown만 설정되고 pitchesThrown은 null이면 투구수 검증을 건너뛴다`() {
            // pitchesThrown != null 이 false → && short-circuit
            setField("strikesThrown", 50)
            pitchingRecord.validate()
        }

        @Test
        fun `비선발 투수가 WIN이면 선발 승리 자격 검증을 건너뛴다`() {
            // isStartingPitcher == false → && short-circuit
            pitchingRecord.assignWin()
            pitchingRecord.validate()
        }

        @Test
        fun `선발 투수가 LOSS이면 선발 승리 자격 검증을 건너뛴다`() {
            // isStartingPitcher == true, decision != WIN
            pitchingRecord.setAsStartingPitcher()
            pitchingRecord.assignLoss()
            pitchingRecord.validate()
        }

        @Test
        fun `선발 투수가 5이닝 이상 소화 후 WIN이면 검증에 통과한다`() {
            // isStartingPitcher == true, decision == WIN, isQualifiedForWin == true
            pitchingRecord.setAsStartingPitcher()
            repeat(15) { pitchingRecord.recordOut() }
            pitchingRecord.assignWin()
            pitchingRecord.validate()
        }

        @Test
        fun `선발 투수가 NONE이면 선발 승리 자격 검증을 건너뛴다`() {
            // isStartingPitcher == true, decision == NONE → 검증 불필요
            pitchingRecord.setAsStartingPitcher()
            pitchingRecord.validate()
        }
    }

    @Nested
    @DisplayName("partial branch 커버리지 - earnedRunAverage 분기")
    inner class EarnedRunAveragePartialBranchTest {

        @Test
        fun `이닝 0이고 자책점 0이면 BigDecimal_ZERO를 반환한다`() {
            assertThat(pitchingRecord.earnedRunAverage)
                .isEqualByComparingTo(BigDecimal("0.00"))
        }

        @Test
        fun `이닝 0이고 자책점이 있으면 null을 반환한다`() {
            pitchingRecord.recordRun(isEarned = true)
            assertThat(pitchingRecord.earnedRunAverage).isNull()
        }

        @Test
        fun `이닝이 있고 자책점이 0이면 ERA는 0이다`() {
            repeat(9) { pitchingRecord.recordOut() }
            assertThat(pitchingRecord.earnedRunAverage)
                .isEqualByComparingTo(BigDecimal("0.00"))
        }
    }
}
