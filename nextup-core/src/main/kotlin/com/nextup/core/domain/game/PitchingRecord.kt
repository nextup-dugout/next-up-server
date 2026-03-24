package com.nextup.core.domain.game

import com.nextup.core.common.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal

/**
 * 투수 기록 엔티티
 *
 * 경기 출전 선수(GamePlayer)의 투수 기록을 저장합니다.
 * 한 경기에서 선수당 하나의 투수 기록만 존재합니다.
 *
 * 통계 계산은 [PitchingStatCalculator]에 위임합니다.
 * 관리자 정정은 [PitchingRecordCorrector] + [PitchingCorrectionField]에 위임합니다.
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

    /**
     * 도루 허용 (SB Allowed) - 투구 중 허용한 도루 횟수
     */
    @Column(name = "stolen_bases_allowed", nullable = false)
    var stolenBasesAllowed: Int = 0
        protected set

    /**
     * 도루 저지 (CS, Runners Caught Stealing) - 투구 중 도루 실패 횟수
     */
    @Column(name = "runners_caught_stealing", nullable = false)
    var runnersCaughtStealing: Int = 0
        protected set

    /**
     * 견제 아웃 (Pickoff) - 견제로 주자를 아웃시킨 횟수
     */
    @Column(name = "pickoffs", nullable = false)
    var pickoffs: Int = 0
        protected set

    // ========== Calculated properties (PitchingStatCalculator에 위임) ==========

    /**
     * 현재 기록 기반 통계 계산기를 생성합니다.
     */
    val statCalculator: PitchingStatCalculator
        get() = PitchingStatCalculator.from(this)

    val completeInnings: Int get() = statCalculator.completeInnings

    val remainingOuts: Int get() = statCalculator.remainingOuts

    val inningsPitched: BigDecimal get() = statCalculator.inningsPitched

    val inningsPitchedDisplay: String get() = statCalculator.inningsPitchedDisplay

    val earnedRunAverage: BigDecimal? get() = statCalculator.earnedRunAverage

    val whip: BigDecimal get() = statCalculator.whip

    val strikeoutsPer9: BigDecimal get() = statCalculator.strikeoutsPer9

    val walksPer9: BigDecimal get() = statCalculator.walksPer9

    val strikeoutToWalkRatio: BigDecimal get() = statCalculator.strikeoutToWalkRatio

    val strikePercentage: BigDecimal? get() = statCalculator.strikePercentage

    val unearnedRuns: Int get() = statCalculator.unearnedRuns

    fun isQualifiedForWin(starterWinQualificationOuts: Int = 15): Boolean =
        statCalculator.isQualifiedForWin(starterWinQualificationOuts)

    // ========== 경기 기록 비즈니스 로직 ==========

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
     * 도루 허용을 기록합니다.
     */
    fun recordStolenBaseAllowed() {
        stolenBasesAllowed++
    }

    /**
     * 도루 저지를 기록합니다.
     */
    fun recordCaughtStealing() {
        runnersCaughtStealing++
    }

    /**
     * 견제 아웃을 기록합니다.
     */
    fun recordPickoff() {
        pickoffs++
    }

    /**
     * 도루 허용을 취소합니다 (Undo용).
     */
    fun revertStolenBaseAllowed() {
        require(stolenBasesAllowed > 0) { "롤백할 도루 허용 기록이 없습니다." }
        stolenBasesAllowed--
    }

    /**
     * 도루 저지를 취소합니다 (Undo용).
     */
    fun revertCaughtStealing() {
        require(runnersCaughtStealing > 0) { "롤백할 도루 저지 기록이 없습니다." }
        runnersCaughtStealing--
    }

    /**
     * 견제 아웃을 취소합니다 (Undo용).
     */
    fun revertPickoff() {
        require(pickoffs > 0) { "롤백할 견제 아웃 기록이 없습니다." }
        pickoffs--
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
     * 투구 수 한계 상태를 확인합니다 (D-20: 투구 수 경고).
     *
     * @param limit 투구 수 제한
     * @param warningThreshold 경고 임박 기준 (제한 대비 몇 구 전에 경고할지)
     * @return 투구 수 경고 상태 (null이면 정상 또는 투구 수 미기록)
     */
    fun checkPitchCountStatus(
        limit: Int,
        warningThreshold: Int = 10,
    ): PitchCountStatus? {
        val thrown = pitchesThrown ?: return null
        return when {
            thrown >= limit -> PitchCountStatus.LIMIT_REACHED
            thrown >= limit - warningThreshold -> PitchCountStatus.APPROACHING_LIMIT
            else -> null
        }
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
     *
     * 블론세이브(BLOWN_SAVE)가 이미 부여된 투수에게도 패전을 부여할 수 있습니다.
     * 야구 규칙상 세이브 상황에서 역전을 허용한 투수는 블론세이브와 패전을 동시에
     * 기록할 수 있으며, 이 경우 패전(LOSS)이 최종 결정으로 기록됩니다.
     */
    fun assignLoss() {
        require(decision == PitchingDecision.NONE || decision == PitchingDecision.BLOWN_SAVE) {
            "이미 다른 결정이 부여된 투수입니다: $decision"
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
     * 투수 교체 시 현재 이닝/아웃 기준으로 이닝을 마감합니다.
     *
     * 교체 시점까지 투수가 소화한 이닝을 기록합니다.
     * 이미 기록된 inningsPitchedOuts가 있는 경우에는 추가 보정하지 않습니다
     * (BoxScore에서 실시간으로 기록되고 있으므로).
     *
     * @param currentInning 현재 이닝 번호
     * @param currentOuts 현재 아웃 카운트 (0-2)
     */
    fun closeInning(
        currentInning: Int,
        currentOuts: Int,
    ) {
        require(currentInning >= 1) { "이닝은 1 이상이어야 합니다." }
        require(currentOuts in 0..2) { "아웃 카운트는 0-2 사이여야 합니다: $currentOuts" }
        // inningsPitchedOuts가 이미 BoxScore를 통해 실시간 기록되므로
        // 추가 갱신은 필요하지 않습니다. 이 메서드는 교체 시점을 명시적으로 마킹하는 역할입니다.
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
            PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD -> {
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
     * 아웃 카운트를 기록합니다 (이닝 소화 전용).
     * battersFaced는 증가시키지 않으므로, applyBatterFaced()와 함께 사용합니다.
     * 단독으로 아웃을 기록하려면 recordOut(isStrikeout)을 사용하세요.
     */
    fun recordInningOut() {
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
            PlateAppearanceResult.STRIKEOUT_DROPPED_THIRD -> {
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
     * recordInningOut()의 역연산입니다.
     */
    fun revertInningOut() {
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

    // ========== 검증 ==========

    /**
     * 기록 유효성을 검증합니다.
     * @param starterWinQualificationOuts 선발 승리 자격 최소 아웃 수 (기본 15 = 5이닝)
     */
    fun validate(starterWinQualificationOuts: Int = 15) {
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
        require(stolenBasesAllowed >= 0) { "도루 허용($stolenBasesAllowed)은 음수일 수 없습니다." }
        require(runnersCaughtStealing >= 0) { "도루 저지($runnersCaughtStealing)는 음수일 수 없습니다." }
        require(pickoffs >= 0) { "견제 아웃($pickoffs)은 음수일 수 없습니다." }
        require(earnedRuns <= runsAllowed) {
            "자책점($earnedRuns)이 실점($runsAllowed)보다 클 수 없습니다."
        }
        if (pitchesThrown != null && strikesThrown != null) {
            require(strikesThrown!! <= pitchesThrown!!) {
                "스트라이크 수($strikesThrown)가 총 투구 수($pitchesThrown)보다 클 수 없습니다."
            }
        }
        if (isStartingPitcher && decision == PitchingDecision.WIN) {
            require(isQualifiedForWin(starterWinQualificationOuts)) {
                "선발 승리 자격(${starterWinQualificationOuts / 3}이닝 이상)을 충족하지 못합니다."
            }
        }
    }

    /**
     * L-5: 이닝 축소 정정 시 기존 기록과 충돌하는지 검증합니다.
     *
     * 정정으로 이닝 수가 축소될 경우, 기존에 기록된 아웃 수가
     * 축소된 이닝의 최대 아웃 수를 초과하지 않는지 확인합니다.
     *
     * @param newTotalInnings 새로운 총 이닝 수
     * @throws IllegalArgumentException 기존 기록이 축소된 이닝과 충돌하는 경우
     */
    fun validateInningReduction(newTotalInnings: Int) {
        val maxOuts = newTotalInnings * 3
        require(inningsPitchedOuts <= maxOuts) {
            "이닝 축소(${newTotalInnings}이닝, 최대 ${maxOuts}아웃) 시 기존 기록(${inningsPitchedOuts}아웃, " +
                "${inningsPitchedDisplay}이닝)과 충돌합니다. 기존 투구 기록을 먼저 정정해주세요."
        }
    }

    // ========== 관리자 정정 (PitchingRecordCorrector에 위임) ==========

    /**
     * 관리자 기록 정정: 특정 필드 값을 직접 설정합니다.
     *
     * 내부적으로 [PitchingRecordCorrector]에 위임하여 타입 안전한 정정을 수행합니다.
     * 정정 후 validate()를 호출하여 일관성을 검증합니다.
     *
     * @param fieldName 정정할 필드명
     * @param newValue 새로운 값 (문자열, 파싱하여 적용)
     * @param starterWinQualificationOuts 선발 승리 자격 최소 아웃 수 (기본 15 = 5이닝)
     * @return 정정 전 이전 값 (문자열)
     * @throws IllegalArgumentException 유효하지 않은 필드명 또는 값
     */
    fun correctField(
        fieldName: String,
        newValue: String,
        starterWinQualificationOuts: Int = 15,
    ): String = PitchingRecordCorrector.correctField(this, fieldName, newValue, starterWinQualificationOuts)

    // ========== 정정 내부 메서드 (PitchingCorrectionField에서 호출) ==========

    internal fun applyCorrectionInningsPitchedOuts(newValue: Int): Int =
        inningsPitchedOuts.also { inningsPitchedOuts = newValue }

    internal fun applyCorrectionEarnedRuns(newValue: Int): Int = earnedRuns.also { earnedRuns = newValue }

    internal fun applyCorrectionRunsAllowed(newValue: Int): Int = runsAllowed.also { runsAllowed = newValue }

    internal fun applyCorrectionHitsAllowed(newValue: Int): Int = hitsAllowed.also { hitsAllowed = newValue }

    internal fun applyCorrectionWalksAllowed(newValue: Int): Int = walksAllowed.also { walksAllowed = newValue }

    internal fun applyCorrectionStrikeouts(newValue: Int): Int = strikeouts.also { strikeouts = newValue }

    internal fun applyCorrectionHomeRunsAllowed(newValue: Int): Int =
        homeRunsAllowed.also { homeRunsAllowed = newValue }

    internal fun applyCorrectionHitBatsmen(newValue: Int): Int = hitBatsmen.also { hitBatsmen = newValue }

    internal fun applyCorrectionWildPitches(newValue: Int): Int = wildPitches.also { wildPitches = newValue }

    internal fun applyCorrectionBalks(newValue: Int): Int = balks.also { balks = newValue }

    internal fun applyCorrectionBattersFaced(newValue: Int): Int = battersFaced.also { battersFaced = newValue }

    internal fun applyCorrectionPitchesThrown(newValue: Int): Int =
        (pitchesThrown ?: 0).also { pitchesThrown = newValue }

    internal fun applyCorrectionStrikesThrown(newValue: Int): Int =
        (strikesThrown ?: 0).also { strikesThrown = newValue }

    internal fun applyCorrectionStolenBasesAllowed(newValue: Int): Int =
        stolenBasesAllowed.also { stolenBasesAllowed = newValue }

    internal fun applyCorrectionRunnersCaughtStealing(newValue: Int): Int =
        runnersCaughtStealing.also { runnersCaughtStealing = newValue }

    internal fun applyCorrectionPickoffs(newValue: Int): Int = pickoffs.also { pickoffs = newValue }

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
