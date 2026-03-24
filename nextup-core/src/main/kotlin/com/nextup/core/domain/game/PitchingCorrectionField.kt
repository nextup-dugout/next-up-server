package com.nextup.core.domain.game

/**
 * 투수 기록 정정 필드 sealed class
 *
 * correctField()의 stringly-typed 디스패치를 타입 안전한 sealed class로 대체합니다.
 * 각 서브클래스는 정정 가능한 필드를 나타내며, 문자열 파싱 로직을 캡슐화합니다.
 */
sealed class PitchingCorrectionField(
    val fieldName: String,
) {
    /**
     * 문자열 값을 파싱하여 정수로 변환합니다.
     */
    protected fun parseIntValue(newValue: String): Int {
        val intValue =
            newValue.toIntOrNull()
                ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
        require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
        return intValue
    }

    /**
     * PitchingRecord에 정정을 적용하고 이전 값을 반환합니다.
     */
    abstract fun apply(
        record: PitchingRecord,
        newValue: String,
    ): String

    data object InningsPitchedOuts : PitchingCorrectionField("inningsPitchedOuts") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionInningsPitchedOuts(intValue).toString()
        }
    }

    data object EarnedRuns : PitchingCorrectionField("earnedRuns") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionEarnedRuns(intValue).toString()
        }
    }

    data object RunsAllowed : PitchingCorrectionField("runsAllowed") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionRunsAllowed(intValue).toString()
        }
    }

    data object HitsAllowed : PitchingCorrectionField("hitsAllowed") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionHitsAllowed(intValue).toString()
        }
    }

    data object WalksAllowed : PitchingCorrectionField("walksAllowed") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionWalksAllowed(intValue).toString()
        }
    }

    data object Strikeouts : PitchingCorrectionField("strikeouts") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionStrikeouts(intValue).toString()
        }
    }

    data object HomeRunsAllowed : PitchingCorrectionField("homeRunsAllowed") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionHomeRunsAllowed(intValue).toString()
        }
    }

    data object HitBatsmen : PitchingCorrectionField("hitBatsmen") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionHitBatsmen(intValue).toString()
        }
    }

    data object WildPitches : PitchingCorrectionField("wildPitches") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionWildPitches(intValue).toString()
        }
    }

    data object Balks : PitchingCorrectionField("balks") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionBalks(intValue).toString()
        }
    }

    data object BattersFaced : PitchingCorrectionField("battersFaced") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionBattersFaced(intValue).toString()
        }
    }

    data object PitchesThrown : PitchingCorrectionField("pitchesThrown") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionPitchesThrown(intValue).toString()
        }
    }

    data object StrikesThrown : PitchingCorrectionField("strikesThrown") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionStrikesThrown(intValue).toString()
        }
    }

    data object StolenBasesAllowed : PitchingCorrectionField("stolenBasesAllowed") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionStolenBasesAllowed(intValue).toString()
        }
    }

    data object RunnersCaughtStealing : PitchingCorrectionField("runnersCaughtStealing") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionRunnersCaughtStealing(intValue).toString()
        }
    }

    data object Pickoffs : PitchingCorrectionField("pickoffs") {
        override fun apply(
            record: PitchingRecord,
            newValue: String,
        ): String {
            val intValue = parseIntValue(newValue)
            return record.applyCorrectionPickoffs(intValue).toString()
        }
    }

    companion object {
        /**
         * 필드명 문자열로부터 해당 PitchingCorrectionField를 찾습니다.
         *
         * @param fieldName 정정할 필드명
         * @return 해당 PitchingCorrectionField
         * @throws IllegalArgumentException 유효하지 않은 필드명
         */
        fun fromFieldName(fieldName: String): PitchingCorrectionField =
            when (fieldName) {
                "inningsPitchedOuts" -> InningsPitchedOuts
                "earnedRuns" -> EarnedRuns
                "runsAllowed" -> RunsAllowed
                "hitsAllowed" -> HitsAllowed
                "walksAllowed" -> WalksAllowed
                "strikeouts" -> Strikeouts
                "homeRunsAllowed" -> HomeRunsAllowed
                "hitBatsmen" -> HitBatsmen
                "wildPitches" -> WildPitches
                "balks" -> Balks
                "battersFaced" -> BattersFaced
                "pitchesThrown" -> PitchesThrown
                "strikesThrown" -> StrikesThrown
                "stolenBasesAllowed" -> StolenBasesAllowed
                "runnersCaughtStealing" -> RunnersCaughtStealing
                "pickoffs" -> Pickoffs
                else -> throw IllegalArgumentException("유효하지 않은 투수 기록 필드입니다: $fieldName")
            }
    }
}
