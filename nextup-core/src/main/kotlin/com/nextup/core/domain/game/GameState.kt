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
     */
    fun setRunner(
        base: Base,
        playerId: Long?,
    ) {
        when (base) {
            Base.FIRST -> runnerOnFirstId = playerId
            Base.SECOND -> runnerOnSecondId = playerId
            Base.THIRD -> runnerOnThirdId = playerId
            Base.HOME -> {} // HOME은 득점이므로 무시
        }
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
     */
    fun clearBases() {
        runnerOnFirstId = null
        runnerOnSecondId = null
        runnerOnThirdId = null
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
}
