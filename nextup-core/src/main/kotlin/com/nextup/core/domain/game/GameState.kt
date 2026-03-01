package com.nextup.core.domain.game

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

/**
 * 경기 실시간 상태 (Embeddable)
 *
 * 현재 진행 중인 경기의 세부 상태를 나타냅니다.
 * - 아웃/볼/스트라이크 카운트
 * - 주자 상태
 * - 타순 및 현재 선수
 */
@Embeddable
class GameState(
    @Column(name = "outs")
    var outs: Int = 0,
    @Column(name = "balls")
    var balls: Int = 0,
    @Column(name = "strikes")
    var strikes: Int = 0,
    @Column(name = "runner_on_first_id")
    var runnerOnFirstId: Long? = null,
    @Column(name = "runner_on_second_id")
    var runnerOnSecondId: Long? = null,
    @Column(name = "runner_on_third_id")
    var runnerOnThirdId: Long? = null,
    @Column(name = "home_batting_order")
    var homeBattingOrder: Int = 1,
    @Column(name = "away_batting_order")
    var awayBattingOrder: Int = 1,
    @Column(name = "current_pitcher_id")
    var currentPitcherId: Long? = null,
    @Column(name = "current_batter_id")
    var currentBatterId: Long? = null,
    /**
     * 계승 주자(Inherited Runner) 추적: 각 베이스 주자가 출루할 당시의 담당 투수 ID.
     * 투수 교체 후 남긴 주자가 득점할 경우 원래 투수의 책임을 추적하기 위해 사용됩니다.
     */
    @Column(name = "runner_on_first_pitcher_id")
    var runnerOnFirstPitcherId: Long? = null,
    @Column(name = "runner_on_second_pitcher_id")
    var runnerOnSecondPitcherId: Long? = null,
    @Column(name = "runner_on_third_pitcher_id")
    var runnerOnThirdPitcherId: Long? = null,
) {
    init {
        require(outs in 0..3) { "아웃 카운트는 0-3 사이여야 합니다: $outs" }
        require(balls in 0..4) { "볼 카운트는 0-4 사이여야 합니다: $balls" }
        require(strikes in 0..3) { "스트라이크 카운트는 0-3 사이여야 합니다: $strikes" }
        require(homeBattingOrder in 1..9) { "홈팀 타순은 1-9 사이여야 합니다: $homeBattingOrder" }
        require(awayBattingOrder in 1..9) { "원정팀 타순은 1-9 사이여야 합니다: $awayBattingOrder" }
    }

    /**
     * 아웃을 기록합니다.
     * @return 3아웃으로 이닝이 종료되었는지 여부
     */
    fun recordOut(): Boolean {
        require(outs < 3) { "이미 3아웃입니다" }
        outs++
        return outs == 3
    }

    /**
     * 볼 카운트를 추가합니다.
     * @return 4볼로 볼넷 여부
     */
    fun addBall(): Boolean {
        require(balls < 4) { "이미 4볼입니다" }
        balls++
        return balls == 4
    }

    /**
     * 스트라이크 카운트를 추가합니다.
     * @return 3스트라이크로 삼진 여부
     */
    fun addStrike(): Boolean {
        require(strikes < 3) { "이미 3스트라이크입니다" }
        strikes++
        return strikes == 3
    }

    /**
     * 볼카운트를 리셋합니다 (새로운 타자).
     */
    fun resetCount() {
        balls = 0
        strikes = 0
    }

    /**
     * 주자를 설정합니다.
     * 주자가 출루할 때 현재 담당 투수 ID를 함께 기록합니다.
     * playerId가 null이면(주자 제거) 담당 투수 ID도 함께 클리어합니다.
     *
     * @param base 베이스
     * @param playerId 주자 GamePlayer ID (null이면 주자 제거)
     * @param pitcherId 담당 투수 GamePlayer ID. null이면 currentPitcherId를 자동 사용.
     *                 주자 진루 시 기존 pitcherId를 유지하려면 명시적으로 전달해야 합니다.
     */
    fun setRunner(
        base: Base,
        playerId: Long?,
        pitcherId: Long? = if (playerId != null) currentPitcherId else null,
    ) {
        when (base) {
            Base.FIRST -> {
                runnerOnFirstId = playerId
                runnerOnFirstPitcherId = if (playerId != null) pitcherId else null
            }
            Base.SECOND -> {
                runnerOnSecondId = playerId
                runnerOnSecondPitcherId = if (playerId != null) pitcherId else null
            }
            Base.THIRD -> {
                runnerOnThirdId = playerId
                runnerOnThirdPitcherId = if (playerId != null) pitcherId else null
            }
            Base.HOME -> {} // HOME은 득점이므로 무시
        }
    }

    /**
     * 특정 베이스 주자의 담당 투수 ID를 반환합니다.
     */
    fun getRunnerPitcherId(base: Base): Long? =
        when (base) {
            Base.FIRST -> runnerOnFirstPitcherId
            Base.SECOND -> runnerOnSecondPitcherId
            Base.THIRD -> runnerOnThirdPitcherId
            Base.HOME -> null
        }

    /**
     * 특정 베이스의 주자를 반환합니다.
     */
    fun getRunner(base: Base): Long? =
        when (base) {
            Base.FIRST -> runnerOnFirstId
            Base.SECOND -> runnerOnSecondId
            Base.THIRD -> runnerOnThirdId
            Base.HOME -> null
        }

    /**
     * 모든 베이스를 클리어합니다 (3아웃 시).
     * 담당 투수 ID도 함께 클리어합니다.
     */
    fun clearBases() {
        runnerOnFirstId = null
        runnerOnSecondId = null
        runnerOnThirdId = null
        runnerOnFirstPitcherId = null
        runnerOnSecondPitcherId = null
        runnerOnThirdPitcherId = null
    }

    /**
     * 다음 타자로 타순을 진행합니다.
     * @param isHomeTeam 홈팀 여부
     */
    fun advanceBatter(isHomeTeam: Boolean) {
        if (isHomeTeam) {
            homeBattingOrder = if (homeBattingOrder == 9) 1 else homeBattingOrder + 1
        } else {
            awayBattingOrder = if (awayBattingOrder == 9) 1 else awayBattingOrder + 1
        }
    }

    /**
     * 이닝 종료 시 상태를 초기화합니다.
     */
    fun resetForNewInning() {
        outs = 0
        balls = 0
        strikes = 0
        clearBases()
    }

    /**
     * 타순을 한 칸 되돌립니다 (Undo 시 타순 롤백용).
     * @param isHomeTeam 홈팀 여부
     */
    fun revertBatter(isHomeTeam: Boolean) {
        if (isHomeTeam) {
            homeBattingOrder = if (homeBattingOrder == 1) 9 else homeBattingOrder - 1
        } else {
            awayBattingOrder = if (awayBattingOrder == 1) 9 else awayBattingOrder - 1
        }
    }

    /**
     * 아웃 카운트를 복원합니다 (Undo용).
     */
    fun restoreOuts(outs: Int) {
        require(outs in 0..3) { "아웃 카운트는 0-3 사이여야 합니다: $outs" }
        this.outs = outs
    }

    /**
     * 주자를 JSON 문자열로부터 복원합니다 (Undo용).
     * JSON 형식: "1루:playerId,2루:playerId,3루:playerId" 또는 null
     * 담당 투수 ID도 함께 클리어됩니다.
     */
    fun restoreRunners(runnersJson: String?) {
        clearBases()
        if (runnersJson.isNullOrBlank()) return

        runnersJson.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val base = parts[0].trim()
                val playerId = parts[1].trim().toLongOrNull()
                when (base) {
                    "1루" -> runnerOnFirstId = playerId
                    "2루" -> runnerOnSecondId = playerId
                    "3루" -> runnerOnThirdId = playerId
                }
            }
        }
    }

    /**
     * 주자의 담당 투수 ID를 JSON 문자열로부터 복원합니다 (Undo용).
     * JSON 형식: "1루:pitcherId,2루:pitcherId,3루:pitcherId" 또는 null
     * restoreRunners 호출 후 사용하여 pitcher ID를 복원합니다.
     */
    fun restoreRunnerPitchers(runnerPitchersJson: String?) {
        runnerOnFirstPitcherId = null
        runnerOnSecondPitcherId = null
        runnerOnThirdPitcherId = null
        if (runnerPitchersJson.isNullOrBlank()) return

        runnerPitchersJson.split(",").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val base = parts[0].trim()
                val pitcherId = parts[1].trim().toLongOrNull()
                when (base) {
                    "1루" -> runnerOnFirstPitcherId = pitcherId
                    "2루" -> runnerOnSecondPitcherId = pitcherId
                    "3루" -> runnerOnThirdPitcherId = pitcherId
                }
            }
        }
    }

    /**
     * 현재 주자-담당투수 상태를 JSON 문자열로 직렬화합니다 (Undo 스냅샷 저장용).
     * JSON 형식: "1루:pitcherId,2루:pitcherId,3루:pitcherId"
     * 담당 투수가 없는 베이스는 포함되지 않습니다.
     */
    fun serializeRunnerPitchers(): String? {
        val entries =
            buildList {
                runnerOnFirstId?.let {
                    runnerOnFirstPitcherId?.let { p -> add("1루:$p") }
                }
                runnerOnSecondId?.let {
                    runnerOnSecondPitcherId?.let { p -> add("2루:$p") }
                }
                runnerOnThirdId?.let {
                    runnerOnThirdPitcherId?.let { p -> add("3루:$p") }
                }
            }
        return if (entries.isEmpty()) null else entries.joinToString(",")
    }

    /**
     * 주자가 있는지 확인합니다.
     */
    fun hasRunner(base: Base): Boolean = getRunner(base) != null

    /**
     * 만루 상태인지 확인합니다.
     */
    fun isBasesLoaded(): Boolean = runnerOnFirstId != null && runnerOnSecondId != null && runnerOnThirdId != null

    /**
     * 현재 볼카운트 문자열을 반환합니다 (예: "2B-1S").
     */
    val countDisplay: String
        get() = "${balls}B-${strikes}S"

    /**
     * 타자의 타순이 올바른지 검증합니다 (D-17: 타순 위반 감지).
     *
     * 사회인 야구의 유연성을 고려하여 예외가 아닌 검증 결과를 반환합니다.
     * 위반 시 호출자(Service)가 경고 이벤트를 발행합니다.
     *
     * @param batterBattingOrder 타석에 들어선 타자의 타순
     * @param isHomeTeam 홈팀 여부
     * @return 타순 위반 결과 (null이면 정상)
     */
    fun validateBattingOrder(
        batterBattingOrder: Int?,
        isHomeTeam: Boolean,
    ): BattingOrderViolation? {
        if (batterBattingOrder == null) return null
        val expectedOrder = if (isHomeTeam) homeBattingOrder else awayBattingOrder
        return if (batterBattingOrder != expectedOrder) {
            BattingOrderViolation(
                expectedBattingOrder = expectedOrder,
                actualBattingOrder = batterBattingOrder,
            )
        } else {
            null
        }
    }
}

/**
 * 타순 위반 정보
 *
 * @property expectedBattingOrder 예상 타순
 * @property actualBattingOrder 실제 입력된 타순
 */
data class BattingOrderViolation(
    val expectedBattingOrder: Int,
    val actualBattingOrder: Int,
)
