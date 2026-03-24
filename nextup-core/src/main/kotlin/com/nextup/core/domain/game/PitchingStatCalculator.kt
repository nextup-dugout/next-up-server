package com.nextup.core.domain.game

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 투수 통계 계산기 Value Object
 *
 * PitchingRecord의 raw 데이터로부터 파생 통계(ERA, WHIP, K/9, BB/9 등)를 계산합니다.
 * 불변 객체로, 계산 시점의 스냅샷 데이터를 기반으로 합니다.
 */
data class PitchingStatCalculator(
    val inningsPitchedOuts: Int,
    val earnedRuns: Int,
    val runsAllowed: Int,
    val hitsAllowed: Int,
    val walksAllowed: Int,
    val strikeouts: Int,
    val pitchesThrown: Int?,
    val strikesThrown: Int?,
    val isStartingPitcher: Boolean,
    val decision: PitchingDecision,
) {
    /**
     * 완전한 이닝 수
     */
    val completeInnings: Int
        get() = inningsPitchedOuts / 3

    /**
     * 이닝의 잔여 아웃 수
     */
    val remainingOuts: Int
        get() = inningsPitchedOuts % 3

    /**
     * 이닝 (실수 형태, 계산용)
     */
    val inningsPitched: BigDecimal
        get() = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 2, RoundingMode.HALF_UP)

    /**
     * 이닝 표시 문자열 (예: "5.1", "7.0", "6.2")
     */
    val inningsPitchedDisplay: String
        get() = "$completeInnings.$remainingOuts"

    /**
     * 자책점 평균자책점 (ERA) = (자책점 / 이닝) * 9
     * 이닝이 0이고 자책점이 있으면 null (계산 불가 - 무한대)
     * 이닝이 0이고 자책점이 없으면 0.00
     */
    val earnedRunAverage: BigDecimal?
        get() =
            if (inningsPitchedOuts == 0) {
                if (earnedRuns > 0) null else BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(earnedRuns)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * WHIP = (피안타 + 볼넷) / 이닝
     */
    val whip: BigDecimal
        get() =
            if (inningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(hitsAllowed + walksAllowed).divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * 9이닝당 삼진 (K/9) = (삼진 / 이닝) * 9
     */
    val strikeoutsPer9: BigDecimal
        get() =
            if (inningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(strikeouts)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * 9이닝당 볼넷 (BB/9) = (볼넷 / 이닝) * 9
     */
    val walksPer9: BigDecimal
        get() =
            if (inningsPitchedOuts == 0) {
                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            } else {
                val innings = BigDecimal(inningsPitchedOuts).divide(BigDecimal(3), 10, RoundingMode.HALF_UP)
                BigDecimal(walksAllowed)
                    .multiply(BigDecimal(9))
                    .divide(innings, 2, RoundingMode.HALF_UP)
            }

    /**
     * 삼진/볼넷 비율 (K/BB)
     */
    val strikeoutToWalkRatio: BigDecimal
        get() =
            if (walksAllowed == 0) {
                if (strikeouts == 0) BigDecimal.ZERO else BigDecimal(strikeouts)
            } else {
                BigDecimal(strikeouts).divide(BigDecimal(walksAllowed), 2, RoundingMode.HALF_UP)
            }.setScale(2, RoundingMode.HALF_UP)

    /**
     * 스트라이크 비율 (투구 수 대비 스트라이크)
     */
    val strikePercentage: BigDecimal?
        get() =
            if (pitchesThrown == null || strikesThrown == null || pitchesThrown == 0) {
                null
            } else {
                BigDecimal(strikesThrown!!)
                    .divide(BigDecimal(pitchesThrown!!), 3, RoundingMode.HALF_UP)
            }

    /**
     * 비자책 실점 = 실점 - 자책점
     */
    val unearnedRuns: Int
        get() = runsAllowed - earnedRuns

    /**
     * 선발 승리 자격이 있는지 확인합니다.
     * @param starterWinQualificationOuts 선발 승리 자격 최소 아웃 수 (기본 15 = 5이닝)
     */
    fun isQualifiedForWin(starterWinQualificationOuts: Int = 15): Boolean =
        isStartingPitcher && inningsPitchedOuts >= starterWinQualificationOuts

    companion object {
        /**
         * PitchingRecord로부터 통계 계산기를 생성합니다.
         */
        fun from(record: PitchingRecord): PitchingStatCalculator =
            PitchingStatCalculator(
                inningsPitchedOuts = record.inningsPitchedOuts,
                earnedRuns = record.earnedRuns,
                runsAllowed = record.runsAllowed,
                hitsAllowed = record.hitsAllowed,
                walksAllowed = record.walksAllowed,
                strikeouts = record.strikeouts,
                pitchesThrown = record.pitchesThrown,
                strikesThrown = record.strikesThrown,
                isStartingPitcher = record.isStartingPitcher,
                decision = record.decision,
            )
    }
}
