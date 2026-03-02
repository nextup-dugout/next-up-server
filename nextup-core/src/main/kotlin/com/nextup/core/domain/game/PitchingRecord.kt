package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 투수 기록 엔티티
 *
 * 경기 출전 선수(GamePlayer)의 투수 기록을 저장합니다.
 * 한 경기에서 선수당 하나의 투수 기록만 존재합니다.
 */
@Entity
@Table(
    name = "pitching_records",
    indexes = [
        Index(name = "idx_pitching_records_game_player", columnList = "game_player_id"),
        Index(name = "idx_pitching_records_decision", columnList = "decision"),
    ],
)
class PitchingRecord(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_player_id", nullable = false, unique = true)
    val gamePlayer: GamePlayer,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
) : BaseTimeEntity() {
    /**
     * 이닝 수를 아웃 카운트로 저장 (5.1이닝 = 16 outs)
     */
    @Column(name = "innings_pitched_outs", nullable = false)
    var inningsPitchedOuts: Int = 0
        protected set

    @Column(name = "earned_runs", nullable = false)
    var earnedRuns: Int = 0
        protected set

    @Column(name = "runs_allowed", nullable = false)
    var runsAllowed: Int = 0
        protected set

    @Column(name = "hits_allowed", nullable = false)
    var hitsAllowed: Int = 0
        protected set

    @Column(name = "walks_allowed", nullable = false)
    var walksAllowed: Int = 0
        protected set

    @Column(nullable = false)
    var strikeouts: Int = 0
        protected set

    @Column(name = "home_runs_allowed", nullable = false)
    var homeRunsAllowed: Int = 0
        protected set

    @Column(name = "hit_batsmen", nullable = false)
    var hitBatsmen: Int = 0
        protected set

    @Column(name = "wild_pitches", nullable = false)
    var wildPitches: Int = 0
        protected set

    @Column(nullable = false)
    var balks: Int = 0
        protected set

    @Column(name = "batters_faced", nullable = false)
    var battersFaced: Int = 0
        protected set

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var decision: PitchingDecision = PitchingDecision.NONE
        protected set

    @Column(name = "is_starting_pitcher", nullable = false)
    var isStartingPitcher: Boolean = false
        protected set

    @Column(name = "pitches_thrown")
    var pitchesThrown: Int? = null
        protected set

    @Column(name = "strikes_thrown")
    var strikesThrown: Int? = null
        protected set

    // Calculated properties

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
     * 선발 승리 자격이 있는지 확인 (5이닝 이상)
     */
    val isQualifiedForWin: Boolean
        get() = isStartingPitcher && inningsPitchedOuts >= 15

    // Business logic

    /**
     * 아웃을 기록합니다.
     */
    fun recordOut(isStrikeout: Boolean = false) {
        inningsPitchedOuts++
        battersFaced++
        if (isStrikeout) {
            strikeouts++
        }
    }

    /**
     * 피안타를 기록합니다.
     */
    fun recordHit(
        isHomeRun: Boolean = false,
        runsScored: Int = 0,
        earnedRuns: Int = 0,
    ) {
        require(earnedRuns <= runsScored) {
            "자책점($earnedRuns)이 실점($runsScored)보다 클 수 없습니다."
        }

        hitsAllowed++
        battersFaced++
        if (isHomeRun) {
            homeRunsAllowed++
        }
        this.runsAllowed += runsScored
        this.earnedRuns += earnedRuns
    }

    /**
     * 볼넷을 기록합니다.
     */
    fun recordWalk() {
        walksAllowed++
        battersFaced++
    }

    /**
     * 사구를 기록합니다.
     */
    fun recordHitByPitch() {
        hitBatsmen++
        battersFaced++
    }

    /**
     * 실점을 기록합니다 (주루 중 득점 등).
     */
    fun recordRun(isEarned: Boolean = true) {
        runsAllowed++
        if (isEarned) {
            earnedRuns++
        }
    }

    /**
     * 와일드피치를 기록합니다.
     */
    fun recordWildPitch() {
        wildPitches++
    }

    /**
     * 보크를 기록합니다.
     */
    fun recordBalk() {
        balks++
    }

    /**
     * 투구 수를 기록합니다.
     */
    fun recordPitchCount(
        totalPitches: Int,
        strikes: Int,
    ) {
        require(strikes <= totalPitches) {
            "스트라이크 수($strikes)가 총 투구 수($totalPitches)보다 클 수 없습니다."
        }
        this.pitchesThrown = totalPitches
        this.strikesThrown = strikes
    }

    /**
     * 선발 투수로 설정합니다.
     */
    fun setAsStartingPitcher() {
        this.isStartingPitcher = true
    }

    /**
     * 승리 결정을 부여합니다.
     */
    fun assignWin() {
        require(isStartingPitcher || decision == PitchingDecision.NONE) {
            "이미 다른 결정이 부여된 투수입니다."
        }
        this.decision = PitchingDecision.WIN
    }

    /**
     * 패배 결정을 부여합니다.
     */
    fun assignLoss() {
        require(decision == PitchingDecision.NONE) {
            "이미 다른 결정이 부여된 투수입니다."
        }
        this.decision = PitchingDecision.LOSS
    }

    /**
     * 세이브 결정을 부여합니다.
     */
    fun assignSave() {
        require(!isStartingPitcher) {
            "선발 투수는 세이브를 기록할 수 없습니다."
        }
        require(decision == PitchingDecision.NONE || decision == PitchingDecision.HOLD) {
            "이미 다른 결정이 부여된 투수입니다."
        }
        this.decision = PitchingDecision.SAVE
    }

    /**
     * 홀드 결정을 부여합니다.
     */
    fun assignHold() {
        require(!isStartingPitcher) {
            "선발 투수는 홀드를 기록할 수 없습니다."
        }
        require(decision == PitchingDecision.NONE) {
            "이미 다른 결정이 부여된 투수입니다."
        }
        this.decision = PitchingDecision.HOLD
    }

    /**
     * 블론세이브를 기록합니다.
     */
    fun assignBlownSave() {
        require(!isStartingPitcher) {
            "선발 투수는 블론세이브를 기록할 수 없습니다."
        }
        this.decision = PitchingDecision.BLOWN_SAVE
    }

    /**
     * 타자와 대결한 결과를 기록합니다 (BoxScore 계산용).
     */
    fun applyBatterFaced(result: PlateAppearanceResult) {
        battersFaced++

        when (result) {
            PlateAppearanceResult.SINGLE,
            PlateAppearanceResult.DOUBLE,
            PlateAppearanceResult.TRIPLE,
            -> {
                hitsAllowed++
            }
            PlateAppearanceResult.HOME_RUN -> {
                hitsAllowed++
                homeRunsAllowed++
            }
            PlateAppearanceResult.STRIKEOUT -> {
                strikeouts++
            }
            PlateAppearanceResult.WALK,
            PlateAppearanceResult.INTENTIONAL_WALK,
            -> {
                walksAllowed++
            }
            PlateAppearanceResult.HIT_BY_PITCH -> {
                hitBatsmen++
            }
            else -> {
                // 다른 결과는 투수 기록에 직접적인 영향 없음
            }
        }
    }

    /**
     * 아웃 카운트를 기록합니다 (이닝 소화).
     */
    fun recordOut() {
        inningsPitchedOuts++
    }

    /**
     * 자책점을 기록합니다.
     */
    fun recordEarnedRun(runs: Int) {
        require(runs >= 0) { "자책점은 0 이상이어야 합니다." }
        earnedRuns += runs
        runsAllowed += runs
    }

    /**
     * 비자책 실점을 기록합니다.
     */
    fun recordUnearnedRun(runs: Int) {
        require(runs >= 0) { "비자책 실점은 0 이상이어야 합니다." }
        runsAllowed += runs
    }

    /**
     * 타자 대결 결과를 역방향으로 롤백합니다 (Undo용).
     * applyBatterFaced의 역연산입니다.
     */
    fun revertBatterFaced(result: PlateAppearanceResult) {
        require(battersFaced > 0) { "롤백할 대면 타자 기록이 없습니다." }
        battersFaced--

        when (result) {
            PlateAppearanceResult.SINGLE,
            PlateAppearanceResult.DOUBLE,
            PlateAppearanceResult.TRIPLE,
            -> {
                require(hitsAllowed > 0) { "롤백할 피안타 기록이 없습니다." }
                hitsAllowed--
            }
            PlateAppearanceResult.HOME_RUN -> {
                require(hitsAllowed > 0) { "롤백할 피안타 기록이 없습니다." }
                require(homeRunsAllowed > 0) { "롤백할 피홈런 기록이 없습니다." }
                hitsAllowed--
                homeRunsAllowed--
            }
            PlateAppearanceResult.STRIKEOUT -> {
                require(strikeouts > 0) { "롤백할 삼진 기록이 없습니다." }
                strikeouts--
            }
            PlateAppearanceResult.WALK,
            PlateAppearanceResult.INTENTIONAL_WALK,
            -> {
                require(walksAllowed > 0) { "롤백할 볼넷 기록이 없습니다." }
                walksAllowed--
            }
            PlateAppearanceResult.HIT_BY_PITCH -> {
                require(hitBatsmen > 0) { "롤백할 사구 기록이 없습니다." }
                hitBatsmen--
            }
            else -> {
                // 다른 결과는 투수 기록에 직접적인 영향 없음
            }
        }
    }

    /**
     * 아웃 카운트를 롤백합니다 (Undo용).
     */
    fun revertOut() {
        require(inningsPitchedOuts > 0) { "롤백할 아웃 카운트가 없습니다." }
        inningsPitchedOuts--
    }

    /**
     * 자책점을 롤백합니다 (Undo용).
     */
    fun revertEarnedRun(runs: Int) {
        require(runs >= 0) { "롤백할 자책점은 0 이상이어야 합니다." }
        require(earnedRuns >= runs) { "롤백할 자책점($runs)이 현재 자책점($earnedRuns)보다 클 수 없습니다." }
        require(runsAllowed >= runs) { "롤백할 실점($runs)이 현재 실점($runsAllowed)보다 클 수 없습니다." }
        earnedRuns -= runs
        runsAllowed -= runs
    }

    /**
     * 기록 유효성을 검증합니다.
     */
    fun validate() {
        require(inningsPitchedOuts >= 0) { "이닝 아웃 수($inningsPitchedOuts)는 음수일 수 없습니다." }
        require(earnedRuns >= 0) { "자책점($earnedRuns)은 음수일 수 없습니다." }
        require(runsAllowed >= 0) { "실점($runsAllowed)은 음수일 수 없습니다." }
        require(hitsAllowed >= 0) { "피안타($hitsAllowed)는 음수일 수 없습니다." }
        require(walksAllowed >= 0) { "볼넷 허용($walksAllowed)은 음수일 수 없습니다." }
        require(strikeouts >= 0) { "삼진($strikeouts)은 음수일 수 없습니다." }
        require(homeRunsAllowed >= 0) { "피홈런($homeRunsAllowed)은 음수일 수 없습니다." }
        require(hitBatsmen >= 0) { "사구($hitBatsmen)는 음수일 수 없습니다." }
        require(wildPitches >= 0) { "와일드피치($wildPitches)는 음수일 수 없습니다." }
        require(balks >= 0) { "보크($balks)는 음수일 수 없습니다." }
        require(battersFaced >= 0) { "대면 타자 수($battersFaced)는 음수일 수 없습니다." }
        require(earnedRuns <= runsAllowed) {
            "자책점($earnedRuns)이 실점($runsAllowed)보다 클 수 없습니다."
        }
        if (pitchesThrown != null && strikesThrown != null) {
            require(strikesThrown!! <= pitchesThrown!!) {
                "스트라이크 수($strikesThrown)가 총 투구 수($pitchesThrown)보다 클 수 없습니다."
            }
        }
        if (isStartingPitcher && decision == PitchingDecision.WIN) {
            require(isQualifiedForWin) {
                "선발 승리 자격(5이닝 이상)을 충족하지 못합니다."
            }
        }
    }

    /**
     * 관리자 기록 정정: 특정 필드 값을 직접 설정합니다.
     *
     * 경기 상태와 무관하게 관리자 권한으로 정정 가능합니다.
     * 정정 후 validate()를 호출하여 일관성을 검증합니다.
     *
     * @param fieldName 정정할 필드명
     * @param newValue 새로운 값 (문자열, 파싱하여 적용)
     * @throws IllegalArgumentException 유효하지 않은 필드명 또는 값
     */
    fun correctField(
        fieldName: String,
        newValue: String,
    ): String {
        val oldValue =
            when (fieldName) {
                "inningsPitchedOuts" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    inningsPitchedOuts.also { inningsPitchedOuts = intValue }
                }
                "earnedRuns" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    earnedRuns.also { earnedRuns = intValue }
                }
                "runsAllowed" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    runsAllowed.also { runsAllowed = intValue }
                }
                "hitsAllowed" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    hitsAllowed.also { hitsAllowed = intValue }
                }
                "walksAllowed" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    walksAllowed.also { walksAllowed = intValue }
                }
                "strikeouts" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    strikeouts.also { strikeouts = intValue }
                }
                "homeRunsAllowed" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    homeRunsAllowed.also { homeRunsAllowed = intValue }
                }
                "hitBatsmen" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    hitBatsmen.also { hitBatsmen = intValue }
                }
                "wildPitches" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    wildPitches.also { wildPitches = intValue }
                }
                "balks" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    balks.also { balks = intValue }
                }
                "battersFaced" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    battersFaced.also { battersFaced = intValue }
                }
                "pitchesThrown" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    (pitchesThrown ?: 0).also { pitchesThrown = intValue }
                }
                "strikesThrown" -> {
                    val intValue =
                        newValue.toIntOrNull()
                            ?: throw IllegalArgumentException("정정 값은 정수여야 합니다: $newValue")
                    require(intValue >= 0) { "정정 값은 0 이상이어야 합니다: $intValue" }
                    (strikesThrown ?: 0).also { strikesThrown = intValue }
                }
                else -> throw IllegalArgumentException("유효하지 않은 투수 기록 필드입니다: $fieldName")
            }

        validate()
        return oldValue.toString()
    }

    companion object {
        /**
         * 경기 출전 선수의 투수 기록을 생성합니다.
         */
        fun create(
            gamePlayer: GamePlayer,
            isStartingPitcher: Boolean = false,
        ): PitchingRecord =
            PitchingRecord(gamePlayer = gamePlayer).apply {
                this.isStartingPitcher = isStartingPitcher
            }

        /**
         * 이닝 문자열을 아웃 수로 변환합니다 (예: "5.1" -> 16)
         */
        fun parseInningsToOuts(inningsString: String): Int {
            val parts = inningsString.split(".")
            val completeInnings = parts[0].toIntOrNull() ?: 0
            val remainingOuts = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
            require(remainingOuts in 0..2) {
                "잔여 아웃 수는 0, 1, 2 중 하나여야 합니다."
            }
            return (completeInnings * 3) + remainingOuts
        }
    }
}
